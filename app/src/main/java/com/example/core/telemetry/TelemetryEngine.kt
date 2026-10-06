package com.example.core.telemetry

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.view.Choreographer
import android.view.WindowManager
import com.example.core.capability.CapabilityScanner
import com.example.data.local.BenchmarkSessionEntity
import com.example.data.local.NexusDao
import com.example.data.local.TelemetrySampleEntity
import com.example.model.BenchmarkMode
import com.example.model.ConfidenceLevel
import com.example.model.LiveTelemetryState
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class TelemetryEngine(
    private val context: Context,
    private val capabilityScanner: CapabilityScanner,
    private val dao: NexusDao,
    private val scope: CoroutineScope
) {
    private val _liveTelemetry = MutableStateFlow(LiveTelemetryState())
    val liveTelemetry: StateFlow<LiveTelemetryState> = _liveTelemetry.asStateFlow()

    private val _frameHistoryMs = MutableStateFlow<List<Float>>(emptyList())
    val frameHistoryMs: StateFlow<List<Float>> = _frameHistoryMs.asStateFlow()

    private val _thermalHistoryCelsius = MutableStateFlow<List<Float>>(emptyList())
    val thermalHistoryCelsius: StateFlow<List<Float>> = _thermalHistoryCelsius.asStateFlow()

    private val _isBenchmarkRunning = MutableStateFlow(false)
    val isBenchmarkRunning: StateFlow<Boolean> = _isBenchmarkRunning.asStateFlow()

    private val _benchmarkProgressSeconds = MutableStateFlow(0)
    val benchmarkProgressSeconds: StateFlow<Int> = _benchmarkProgressSeconds.asStateFlow()

    private val frameDurationsWindowMs = ArrayDeque<Float>(240)
    private var lastFrameTimeNanos: Long = 0L
    private var choreographerAttached = false
    private var pollingJob: Job? = null

    private val jsonFormatter = Json { prettyPrint = true }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!choreographerAttached) return
            if (lastFrameTimeNanos != 0L) {
                val deltaMs = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000f
                if (deltaMs in 1.0f..250.0f) {
                    synchronized(frameDurationsWindowMs) {
                        if (frameDurationsWindowMs.size >= 180) {
                            frameDurationsWindowMs.removeFirst()
                        }
                        frameDurationsWindowMs.addLast(deltaMs)
                    }
                }
            }
            lastFrameTimeNanos = frameTimeNanos
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    fun startTelemetry(onThermalSample: suspend (Float, Int) -> Unit) {
        try {
            if (!choreographerAttached) {
                choreographerAttached = true
                lastFrameTimeNanos = 0L
                Choreographer.getInstance().postFrameCallback(frameCallback)
            }
        } catch (_: Throwable) {
            // Safe guard for headless JVM unit tests
            choreographerAttached = false
        }

        if (pollingJob?.isActive == true) return
        pollingJob = scope.launch(Dispatchers.Default) {
            while (isActive) {
                val snapshot = sampleHardwareTelemetry()
                _liveTelemetry.value = snapshot

                val p99 = snapshot.p99FrameTimeMs
                if (p99 > 0f) {
                    _frameHistoryMs.value = (_frameHistoryMs.value + p99).takeLast(40)
                }
                val temp = maxOf(snapshot.batteryTempCelsius, snapshot.maxThermalZoneCelsius)
                if (temp > 0f) {
                    _thermalHistoryCelsius.value = (_thermalHistoryCelsius.value + temp).takeLast(40)
                }

                onThermalSample(temp, snapshot.thermalStatusCode)
                delay(1000L) // Intelligent 1Hz low-overhead hardware sampling
            }
        }
    }

    fun stopTelemetry() {
        choreographerAttached = false
        try {
            Choreographer.getInstance().removeFrameCallback(frameCallback)
        } catch (_: Throwable) {
        }
        pollingJob?.cancel()
        pollingJob = null
    }

    suspend fun sampleHardwareTelemetry(): LiveTelemetryState = withContext(Dispatchers.IO) {
        val frameSamples: List<Float> = synchronized(frameDurationsWindowMs) {
            frameDurationsWindowMs.toList()
        }

        val percentiles = computeFramePercentiles(frameSamples)

        // CPU Policies
        val cpuPolicies = capabilityScanner.scanCpuFreqPolicies()
        val clusterFreqs = cpuPolicies.map {
            "Policy ${it.policyId}" to (it.curFreqKhz / 1000L)
        }
        val cpuLoad = if (cpuPolicies.isNotEmpty()) {
            val ratios = cpuPolicies.mapNotNull {
                if (it.maxFreqKhz > 0L) (it.curFreqKhz.toFloat() / it.maxFreqKhz.toFloat()).coerceIn(0f, 1f)
                else null
            }
            if (ratios.isNotEmpty()) (ratios.average().toFloat() * 100f) else 0f
        } else {
            0f
        }

        // GPU Nodes
        val gpuNodes = capabilityScanner.scanGpuDevfreqNodes()
        val gpuFreqMhz = gpuNodes.firstOrNull()?.let { it.curFreqHz / 1_000_000L } ?: 0L

        // Battery Temperature & Current
        val batteryIntent = try {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Throwable) {
            null
        }
        val rawBatteryTemp = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 310) ?: 310
        val batteryTempCelsius = rawBatteryTemp / 10.0f

        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val currentNowMicroAmps = try {
            bm?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0L
        } catch (_: Throwable) {
            0L
        }

        // Thermal Zones & PowerManager
        val thermalZones = capabilityScanner.scanThermalZones()
        val maxZoneCelsius = thermalZones.maxOfOrNull { it.tempCelsius } ?: batteryTempCelsius

        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        var thermalStatus = 0
        var thermalHeadroom = 0f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && pm != null) {
            try {
                thermalStatus = pm.currentThermalStatus
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val h = pm.getThermalHeadroom(10)
                    if (!h.isNaN()) thermalHeadroom = h
                }
            } catch (_: Throwable) {
            }
        }

        val thermalLabel = when (thermalStatus) {
            0 -> "NONE (Nominal)"
            1 -> "LIGHT"
            2 -> "MODERATE (Step-Down Threshold)"
            3 -> "SEVERE (Throttling)"
            4 -> "CRITICAL"
            5 -> "EMERGENCY"
            6 -> "SHUTDOWN"
            else -> "UNKNOWN ($thermalStatus)"
        }

        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)
        val availRamMb = (memInfo.availMem / (1024 * 1024)).coerceAtLeast(0L)
        val totalRamMb = (memInfo.totalMem / (1024 * 1024)).coerceAtLeast(1L)

        LiveTelemetryState(
            timestamp = System.currentTimeMillis(),
            currentFps = percentiles.avgFps,
            p50FrameTimeMs = percentiles.p50Ms,
            p90FrameTimeMs = percentiles.p90Ms,
            p95FrameTimeMs = percentiles.p95Ms,
            p99FrameTimeMs = percentiles.p99Ms,
            frameStabilityPercent = percentiles.stabilityPercent,
            jankFramesCount = percentiles.jankCount,
            fpsConfidence = percentiles.confidence,
            fpsSourceLabel = percentiles.sourceLabel,
            cpuLoadPercent = cpuLoad,
            cpuClusterFreqsMhz = clusterFreqs,
            gpuFreqMhz = gpuFreqMhz,
            gpuLoadPercent = -1f,
            batteryTempCelsius = batteryTempCelsius,
            maxThermalZoneCelsius = maxZoneCelsius,
            thermalStatusCode = thermalStatus,
            thermalStatusLabel = thermalLabel,
            thermalHeadroom = thermalHeadroom,
            availRamMb = availRamMb,
            totalRamMb = totalRamMb,
            batteryCurrentMicroAmps = currentNowMicroAmps,
            isThrottling = thermalStatus >= 2 || maxZoneCelsius >= 42.0f || batteryTempCelsius >= 42.0f
        )
    }

    data class FramePercentileSummary(
        val avgFps: Float,
        val p50Ms: Float,
        val p90Ms: Float,
        val p95Ms: Float,
        val p99Ms: Float,
        val stabilityPercent: Float,
        val jankCount: Int,
        val confidence: ConfidenceLevel,
        val sourceLabel: String
    )

    fun computeFramePercentiles(samplesMs: List<Float>): FramePercentileSummary {
        if (samplesMs.isEmpty()) {
            return FramePercentileSummary(
                avgFps = 0f,
                p50Ms = 0f,
                p90Ms = 0f,
                p95Ms = 0f,
                p99Ms = 0f,
                stabilityPercent = 0f,
                jankCount = 0,
                confidence = ConfidenceLevel.UNAVAILABLE,
                sourceLabel = "Awaiting Choreographer VSYNC samples"
            )
        }
        val sorted = samplesMs.sorted()
        val meanMs = sorted.average().toFloat().coerceAtLeast(0.1f)
        val avgFps = (1000f / meanMs).coerceIn(1f, 240f)

        fun percentile(p: Float): Float {
            val idx = ((sorted.size - 1) * p).roundToInt().coerceIn(0, sorted.lastIndex)
            return sorted[idx]
        }

        val p50 = percentile(0.50f)
        val p90 = percentile(0.90f)
        val p95 = percentile(0.95f)
        val p99 = percentile(0.99f)
        val jankCount = sorted.count { it > 16.8f }
        val smoothCount = sorted.count { it <= 16.8f }
        val stability = (smoothCount.toFloat() / sorted.size.toFloat()) * 100f

        return FramePercentileSummary(
            avgFps = avgFps,
            p50Ms = p50,
            p90Ms = p90,
            p95Ms = p95,
            p99Ms = p99,
            stabilityPercent = stability,
            jankCount = jankCount,
            confidence = ConfidenceLevel.MEDIUM,
            sourceLabel = "Choreographer Hardware VSYNC (${sorted.size} frames)"
        )
    }

    suspend fun runBenchmarkSession(
        mode: BenchmarkMode,
        targetPackage: String,
        profileName: String,
        durationSeconds: Int = 8,
        appliedControlsSummary: String
    ): BenchmarkSessionEntity = withContext(Dispatchers.IO) {
        _isBenchmarkRunning.value = true
        _benchmarkProgressSeconds.value = 0

        val startTelemetry = sampleHardwareTelemetry()
        val startTemp = maxOf(startTelemetry.batteryTempCelsius, startTelemetry.maxThermalZoneCelsius)
        val collectedSamples = mutableListOf<LiveTelemetryState>()
        var throttleEvents = 0

        for (sec in 1..durationSeconds) {
            delay(1000L)
            _benchmarkProgressSeconds.value = sec
            val sample = sampleHardwareTelemetry()
            collectedSamples.add(sample)
            if (sample.isThrottling) throttleEvents++
        }

        val endTelemetry = collectedSamples.lastOrNull() ?: startTelemetry
        val endTemp = maxOf(endTelemetry.batteryTempCelsius, endTelemetry.maxThermalZoneCelsius)

        val validFpsSamples = collectedSamples.filter { it.currentFps > 0f }
        val avgFps = if (validFpsSamples.isNotEmpty()) {
            validFpsSamples.map { it.currentFps }.average().toFloat()
        } else {
            endTelemetry.currentFps
        }
        val p50 = if (validFpsSamples.isNotEmpty()) {
            validFpsSamples.map { it.p50FrameTimeMs }.average().toFloat()
        } else {
            endTelemetry.p50FrameTimeMs
        }
        val p90 = if (validFpsSamples.isNotEmpty()) {
            validFpsSamples.map { it.p90FrameTimeMs }.average().toFloat()
        } else {
            endTelemetry.p90FrameTimeMs
        }
        val p95 = if (validFpsSamples.isNotEmpty()) {
            validFpsSamples.map { it.p95FrameTimeMs }.average().toFloat()
        } else {
            endTelemetry.p95FrameTimeMs
        }
        val p99 = if (validFpsSamples.isNotEmpty()) {
            validFpsSamples.maxOf { it.p99FrameTimeMs }
        } else {
            endTelemetry.p99FrameTimeMs
        }
        val stability = if (validFpsSamples.isNotEmpty()) {
            validFpsSamples.map { it.frameStabilityPercent }.average().toFloat()
        } else {
            endTelemetry.frameStabilityPercent
        }

        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        @Suppress("DEPRECATION")
        val refreshRate = wm?.defaultDisplay?.refreshRate ?: 60f

        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100

        val sessionEntity = BenchmarkSessionEntity(
            mode = mode.name,
            targetPackage = targetPackage,
            profileName = profileName,
            durationSeconds = durationSeconds,
            avgFps = avgFps,
            p50FrameTimeMs = p50,
            p90FrameTimeMs = p90,
            p95FrameTimeMs = p95,
            p99FrameTimeMs = p99,
            frameStabilityPercent = stability,
            jankFrameCount = endTelemetry.jankFramesCount,
            startTempCelsius = startTemp,
            endTempCelsius = endTemp,
            thermalThrottleEvents = throttleEvents,
            displayRefreshRateHz = refreshRate,
            batteryLevelPercent = batteryPct,
            appliedControlsSummary = appliedControlsSummary,
            confidenceLevel = endTelemetry.fpsConfidence.name,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})"
        )

        val sessionId = dao.insertBenchmarkSession(sessionEntity)
        collectedSamples.forEach { s ->
            dao.insertTelemetrySample(
                TelemetrySampleEntity(
                    sessionId = sessionId,
                    timestamp = s.timestamp,
                    fps = s.currentFps,
                    p99FrameTimeMs = s.p99FrameTimeMs,
                    cpuFreqMhz = s.cpuClusterFreqsMhz.firstOrNull()?.second ?: 0L,
                    gpuFreqMhz = s.gpuFreqMhz,
                    tempCelsius = maxOf(s.batteryTempCelsius, s.maxThermalZoneCelsius),
                    thermalStatus = s.thermalStatusCode,
                    availRamMb = s.availRamMb
                )
            )
        }

        _isBenchmarkRunning.value = false
        sessionEntity.copy(id = sessionId)
    }

    fun exportBenchmarksAsJson(sessions: List<BenchmarkSessionEntity>): String {
        return jsonFormatter.encodeToString(sessions)
    }

    fun exportBenchmarksAsCsv(sessions: List<BenchmarkSessionEntity>): String {
        return buildString {
            appendLine("id,timestamp,mode,package,profile,durationSec,avgFps,p50Ms,p90Ms,p95Ms,p99Ms,stabilityPct,startTempC,endTempC,throttleEvents,confidence,device")
            sessions.forEach { s ->
                appendLine(
                    "${s.id},${s.timestamp},${s.mode},${s.targetPackage},${s.profileName},${s.durationSeconds}," +
                        "${"%.1f".format(s.avgFps)},${"%.2f".format(s.p50FrameTimeMs)},${"%.2f".format(s.p90FrameTimeMs)}," +
                        "${"%.2f".format(s.p95FrameTimeMs)},${"%.2f".format(s.p99FrameTimeMs)},${"%.1f".format(s.frameStabilityPercent)}," +
                        "${"%.1f".format(s.startTempCelsius)},${"%.1f".format(s.endTempCelsius)},${s.thermalThrottleEvents},${s.confidenceLevel},\"${s.deviceModel}\""
                )
            }
        }
    }

    fun exportBenchmarksAsMarkdown(sessions: List<BenchmarkSessionEntity>): String {
        return buildString {
            appendLine("# NEXUS BOOST — BENCHMARK REPRODUCIBILITY REPORT")
            appendLine()
            appendLine("| Mode | Game / Target | Profile | Avg FPS | P50 / P95 / P99 (ms) | Stability | Temp (Start→End) | Confidence |")
            appendLine("| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |")
            sessions.forEach { s ->
                appendLine(
                    "| **${s.mode}** | `${s.targetPackage}` | ${s.profileName} | **${"%.1f".format(s.avgFps)}** | " +
                        "${"%.2f".format(s.p50FrameTimeMs)} / ${"%.2f".format(s.p95FrameTimeMs)} / ${"%.2f".format(s.p99FrameTimeMs)} ms | " +
                        "${"%.1f".format(s.frameStabilityPercent)}% | ${"%.1f".format(s.startTempCelsius)}°C → ${"%.1f".format(s.endTempCelsius)}°C | ${s.confidenceLevel} |"
                )
            }
        }
    }
}
