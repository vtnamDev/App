package com.example.core.capability

import android.app.ActivityManager
import android.app.GameManager
import android.content.Context
import android.os.Build
import android.os.PerformanceHintManager
import android.os.PowerManager
import android.view.WindowManager
import com.example.model.CapabilityItem
import com.example.model.CapabilityStatus
import com.example.model.CpuPolicyNode
import com.example.model.DeviceCapabilityReport
import com.example.model.GpuDevfreqNode
import com.example.model.PrivilegeLevel
import com.example.model.ThermalZoneNode
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CapabilityScanner(private val context: Context) {

    private val jsonFormatter = Json { prettyPrint = true }

    suspend fun scanDeviceCapabilities(currentPrivilege: PrivilegeLevel): DeviceCapabilityReport =
        withContext(Dispatchers.IO) {
            val manufacturer = Build.MANUFACTURER ?: "Unknown"
            val brand = Build.BRAND ?: "Unknown"
            val model = Build.MODEL ?: "Unknown"
            val socHardware = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                "${Build.SOC_MANUFACTURER} ${Build.SOC_MODEL}".trim().ifBlank { Build.HARDWARE ?: "Unknown" }
            } else {
                Build.HARDWARE ?: "Unknown"
            }
            val board = Build.BOARD ?: "Unknown"
            val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"
            val sdkInt = Build.VERSION.SDK_INT
            val releaseVersion = Build.VERSION.RELEASE ?: "Unknown"
            val kernelVersion = System.getProperty("os.version") ?: readFirstLine(File("/proc/version")) ?: "Unknown"

            val oemAdapter = resolveOemAdapter(manufacturer, brand)

            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memInfo)
            val totalRamMb = (memInfo.totalMem / (1024 * 1024)).coerceAtLeast(1L)
            val availRamMb = (memInfo.availMem / (1024 * 1024)).coerceAtLeast(0L)

            val (supportedRefreshRates, currentRefreshRate) = probeDisplayRefreshRates()

            // Probe ADPF PerformanceHintManager
            var adpfSupported = false
            var adpfRateNanos = -1L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val phm = context.getSystemService(PerformanceHintManager::class.java)
                    adpfRateNanos = phm?.preferredUpdateRateNanos ?: -1L
                    adpfSupported = adpfRateNanos > 0L
                } catch (_: Throwable) {
                }
            }

            // Probe Android GameManager
            var gameManagerSupported = false
            var currentGameMode = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    val gm = context.getSystemService(GameManager::class.java)
                    if (gm != null) {
                        gameManagerSupported = true
                        currentGameMode = gm.gameMode
                    }
                } catch (_: Throwable) {
                }
            }

            // Probe PowerManager Thermal APIs
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            var powerThermalSupported = false
            var currentThermalStatus = 0
            var thermalHeadroom = 0f
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && powerManager != null) {
                try {
                    currentThermalStatus = powerManager.currentThermalStatus
                    powerThermalSupported = true
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val headroom = powerManager.getThermalHeadroom(10)
                        if (!headroom.isNaN()) {
                            thermalHeadroom = headroom
                        }
                    }
                } catch (_: Throwable) {
                }
            }

            // Scan CPUFreq policies dynamically
            val cpuPolicies = scanCpuFreqPolicies()

            // Scan GPU Devfreq nodes dynamically
            val gpuNodes = scanGpuDevfreqNodes()

            // Scan Thermal Zones dynamically
            val thermalZones = scanThermalZones()

            // Assemble Capability Matrix with concrete evidence
            val capabilities = buildCapabilityMatrix(
                currentPrivilege = currentPrivilege,
                sdkInt = sdkInt,
                adpfSupported = adpfSupported,
                adpfRateNanos = adpfRateNanos,
                gameManagerSupported = gameManagerSupported,
                currentGameMode = currentGameMode,
                powerThermalSupported = powerThermalSupported,
                currentThermalStatus = currentThermalStatus,
                thermalHeadroom = thermalHeadroom,
                cpuPolicies = cpuPolicies,
                gpuNodes = gpuNodes,
                thermalZones = thermalZones,
                supportedRefreshRates = supportedRefreshRates,
                currentRefreshRate = currentRefreshRate,
                availRamMb = availRamMb,
                totalRamMb = totalRamMb,
                oemAdapter = oemAdapter
            )

            DeviceCapabilityReport(
                scanTimestamp = System.currentTimeMillis(),
                manufacturer = manufacturer,
                brand = brand,
                model = model,
                socHardware = socHardware,
                board = board,
                abi = abi,
                sdkInt = sdkInt,
                releaseVersion = releaseVersion,
                kernelVersion = kernelVersion,
                oemAdapterName = oemAdapter,
                totalRamMb = totalRamMb,
                availRamMb = availRamMb,
                supportedRefreshRatesHz = supportedRefreshRates,
                currentRefreshRateHz = currentRefreshRate,
                adpfSupported = adpfSupported,
                adpfPreferredRateNanos = adpfRateNanos,
                gameManagerSupported = gameManagerSupported,
                powerThermalApiSupported = powerThermalSupported,
                currentThermalStatus = currentThermalStatus,
                thermalHeadroom10s = thermalHeadroom,
                cpuPolicies = cpuPolicies,
                gpuNodes = gpuNodes,
                thermalZones = thermalZones,
                capabilities = capabilities
            )
        }

    fun exportReportAsJson(report: DeviceCapabilityReport): String {
        return jsonFormatter.encodeToString(report)
    }

    fun exportReportAsMarkdown(report: DeviceCapabilityReport): String {
        return buildString {
            appendLine("# NEXUS BOOST — DEVICE CAPABILITY REPORT")
            appendLine("- **Device**: ${report.manufacturer} ${report.model} (${report.board})")
            appendLine("- **SoC / ABI**: ${report.socHardware} (${report.abi})")
            appendLine("- **Android SDK**: API ${report.sdkInt} (Android ${report.releaseVersion})")
            appendLine("- **Kernel**: ${report.kernelVersion}")
            appendLine("- **OEM Adapter**: ${report.oemAdapterName}")
            appendLine("- **Memory**: ${report.availRamMb} MB available / ${report.totalRamMb} MB total")
            appendLine("- **Refresh Rates**: ${report.supportedRefreshRatesHz.joinToString(", ") { "${it.toInt()}Hz" }} (Current: ${report.currentRefreshRateHz.toInt()}Hz)")
            appendLine()
            appendLine("## Capability Matrix")
            appendLine("| Feature | Status | Privilege | Path / API | Current Value | Valid Range |")
            appendLine("| :--- | :--- | :--- | :--- | :--- | :--- |")
            report.capabilities.forEach { cap ->
                appendLine("| ${cap.name} | **${cap.status.label}** | ${cap.requiredPrivilege.name} | `${cap.pathOrApi}` | `${cap.currentReadValue}` | `${cap.validRange}` |")
            }
        }
    }

    fun scanCpuFreqPolicies(): List<CpuPolicyNode> {
        val cpufreqDir = File("/sys/devices/system/cpu/cpufreq")
        val policyDirs = cpufreqDir.listFiles { file ->
            file.isDirectory && file.name.startsWith("policy")
        }?.sortedBy { it.name.removePrefix("policy").toIntOrNull() ?: 0 } ?: emptyList()

        if (policyDirs.isNotEmpty()) {
            return policyDirs.mapIndexed { idx, dir ->
                val policyId = dir.name.removePrefix("policy").toIntOrNull() ?: idx
                val relatedCpus = readFirstLine(File(dir, "related_cpus"))
                    ?: readFirstLine(File(dir, "affected_cpus"))
                    ?: "cpu$policyId"
                val curGovernor = readFirstLine(File(dir, "scaling_governor")) ?: "schedutil"
                val availGovs = readFirstLine(File(dir, "scaling_available_governors"))
                    ?.split(Regex("\\s+"))
                    ?.filter { it.isNotBlank() }
                    ?: listOf(curGovernor)
                val curFreq = readFirstLine(File(dir, "scaling_cur_freq"))?.toLongOrNull()
                    ?: readFirstLine(File(dir, "cpuinfo_cur_freq"))?.toLongOrNull()
                    ?: 0L
                val minFreq = readFirstLine(File(dir, "scaling_min_freq"))?.toLongOrNull()
                    ?: readFirstLine(File(dir, "cpuinfo_min_freq"))?.toLongOrNull()
                    ?: 0L
                val maxFreq = readFirstLine(File(dir, "scaling_max_freq"))?.toLongOrNull()
                    ?: readFirstLine(File(dir, "cpuinfo_max_freq"))?.toLongOrNull()
                    ?: 0L
                val availFreqs = readFirstLine(File(dir, "scaling_available_frequencies"))
                    ?.split(Regex("\\s+"))
                    ?.mapNotNull { it.toLongOrNull() }
                    ?: listOfNotNull(minFreq.takeIf { it > 0 }, maxFreq.takeIf { it > 0 }).distinct()

                val minFreqFile = File(dir, "scaling_min_freq")
                CpuPolicyNode(
                    policyId = policyId,
                    path = dir.absolutePath,
                    relatedCpus = relatedCpus,
                    currentGovernor = curGovernor,
                    availableGovernors = availGovs,
                    curFreqKhz = curFreq,
                    minFreqKhz = minFreq,
                    maxFreqKhz = maxFreq,
                    availableFrequenciesKhz = availFreqs,
                    isReadable = curFreq > 0L || maxFreq > 0L,
                    isWritable = minFreqFile.canWrite()
                )
            }
        }

        // Fallback scan of /sys/devices/system/cpu/cpu0/cpufreq if policy* symlink layout differs
        val cpu0FreqDir = File("/sys/devices/system/cpu/cpu0/cpufreq")
        if (cpu0FreqDir.exists()) {
            val curFreq = readFirstLine(File(cpu0FreqDir, "scaling_cur_freq"))?.toLongOrNull() ?: 0L
            val minFreq = readFirstLine(File(cpu0FreqDir, "scaling_min_freq"))?.toLongOrNull() ?: 0L
            val maxFreq = readFirstLine(File(cpu0FreqDir, "scaling_max_freq"))?.toLongOrNull() ?: 0L
            val curGov = readFirstLine(File(cpu0FreqDir, "scaling_governor")) ?: "schedutil"
            return listOf(
                CpuPolicyNode(
                    policyId = 0,
                    path = cpu0FreqDir.absolutePath,
                    relatedCpus = "0-${Runtime.getRuntime().availableProcessors() - 1}",
                    currentGovernor = curGov,
                    availableGovernors = listOf(curGov),
                    curFreqKhz = curFreq,
                    minFreqKhz = minFreq,
                    maxFreqKhz = maxFreq,
                    availableFrequenciesKhz = listOfNotNull(minFreq.takeIf { it > 0 }, maxFreq.takeIf { it > 0 }).distinct(),
                    isReadable = curFreq > 0L || maxFreq > 0L,
                    isWritable = File(cpu0FreqDir, "scaling_min_freq").canWrite()
                )
            )
        }

        return emptyList()
    }

    fun scanGpuDevfreqNodes(): List<GpuDevfreqNode> {
        val candidates = mutableListOf<File>()
        val kgslDir = File("/sys/class/kgsl/kgsl-3d0/devfreq")
        if (kgslDir.exists() && kgslDir.isDirectory) {
            candidates.add(kgslDir)
        }
        val devfreqRoot = File("/sys/class/devfreq")
        devfreqRoot.listFiles()?.forEach { node ->
            val nameLower = node.name.lowercase()
            if (nameLower.contains("kgsl") || nameLower.contains("mali") ||
                nameLower.contains("gpu") || nameLower.contains("gpufreq") ||
                nameLower.contains("3d")
            ) {
                if (candidates.none { it.canonicalPath == node.canonicalPath }) {
                    candidates.add(node)
                }
            }
        }

        return candidates.map { dir ->
            val curFreq = readFirstLine(File(dir, "cur_freq"))?.toLongOrNull() ?: 0L
            val minFreq = readFirstLine(File(dir, "min_freq"))?.toLongOrNull() ?: 0L
            val maxFreq = readFirstLine(File(dir, "max_freq"))?.toLongOrNull() ?: 0L
            val gov = readFirstLine(File(dir, "governor")) ?: "unknown"
            val avail = readFirstLine(File(dir, "available_frequencies"))
                ?.split(Regex("\\s+"))
                ?.mapNotNull { it.toLongOrNull() }
                ?: emptyList()
            val minFile = File(dir, "min_freq")
            GpuDevfreqNode(
                name = dir.name,
                path = dir.absolutePath,
                currentGovernor = gov,
                curFreqHz = curFreq,
                minFreqHz = minFreq,
                maxFreqHz = maxFreq,
                availableFrequenciesHz = avail,
                isReadable = curFreq > 0L || maxFreq > 0L,
                isWritable = minFile.canWrite()
            )
        }
    }

    fun scanThermalZones(): List<ThermalZoneNode> {
        val thermalRoot = File("/sys/class/thermal")
        val zones = thermalRoot.listFiles { f -> f.isDirectory && f.name.startsWith("thermal_zone") }
            ?.sortedBy { it.name.removePrefix("thermal_zone").toIntOrNull() ?: 0 }
            ?.take(16)
            ?: emptyList()

        return zones.mapIndexedNotNull { idx, dir ->
            val zoneIdx = dir.name.removePrefix("thermal_zone").toIntOrNull() ?: idx
            val type = readFirstLine(File(dir, "type")) ?: return@mapIndexedNotNull null
            val rawTemp = readFirstLine(File(dir, "temp"))?.toFloatOrNull() ?: return@mapIndexedNotNull null
            val celsius = when {
                rawTemp > 1000f -> rawTemp / 1000f
                rawTemp > 200f -> rawTemp / 10f
                else -> rawTemp
            }
            if (celsius in 10f..115f) {
                ThermalZoneNode(
                    index = zoneIdx,
                    type = type,
                    path = dir.absolutePath,
                    tempCelsius = celsius,
                    isReadable = true
                )
            } else {
                null
            }
        }
    }

    private fun buildCapabilityMatrix(
        currentPrivilege: PrivilegeLevel,
        sdkInt: Int,
        adpfSupported: Boolean,
        adpfRateNanos: Long,
        gameManagerSupported: Boolean,
        currentGameMode: Int,
        powerThermalSupported: Boolean,
        currentThermalStatus: Int,
        thermalHeadroom: Float,
        cpuPolicies: List<CpuPolicyNode>,
        gpuNodes: List<GpuDevfreqNode>,
        thermalZones: List<ThermalZoneNode>,
        supportedRefreshRates: List<Float>,
        currentRefreshRate: Float,
        availRamMb: Long,
        totalRamMb: Long,
        oemAdapter: String
    ): List<CapabilityItem> {
        val list = mutableListOf<CapabilityItem>()

        // 1. ADPF PerformanceHintManager
        list.add(
            CapabilityItem(
                id = "adpf_hint_session",
                name = "ADPF PerformanceHintManager",
                category = "Android Performance API",
                status = if (adpfSupported) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
                requiredPrivilege = PrivilegeLevel.NORMAL,
                pathOrApi = "android.os.PerformanceHintManager",
                currentReadValue = if (adpfSupported) "Preferred update rate: ${adpfRateNanos / 1_000_000.0} ms" else "Unavailable on API $sdkInt",
                writable = adpfSupported,
                validRange = "Target frame work duration 8.33ms (120Hz) - 16.67ms (60Hz)",
                evidenceSource = "SRC-AOSP-ADPF",
                fallbackExplanation = "Uses standard thread priority and background memory trim if ADPF is unsupported."
            )
        )

        // 2. PowerManager Thermal API
        list.add(
            CapabilityItem(
                id = "power_thermal_api",
                name = "PowerManager Thermal Status & Headroom",
                category = "Thermal & Safety",
                status = if (powerThermalSupported) CapabilityStatus.SUPPORTED else CapabilityStatus.UNSUPPORTED,
                requiredPrivilege = PrivilegeLevel.NORMAL,
                pathOrApi = "PowerManager.getCurrentThermalStatus() / getThermalHeadroom(10)",
                currentReadValue = "Status=$currentThermalStatus, Headroom=${"%.2f".format(thermalHeadroom)}",
                writable = false,
                validRange = "THERMAL_STATUS_NONE (0) .. SHUTDOWN (6)",
                evidenceSource = "SRC-AOSP-ADPF",
                fallbackExplanation = "Falls back to BatteryManager temperature sticky broadcast."
            )
        )

        // 3. Background Process Memory Trim
        list.add(
            CapabilityItem(
                id = "background_memory_trim",
                name = "Safe Background Memory Reclamation",
                category = "Process & Memory",
                status = CapabilityStatus.SUPPORTED,
                requiredPrivilege = PrivilegeLevel.NORMAL,
                pathOrApi = "ActivityManager.killBackgroundProcesses()",
                currentReadValue = "$availRamMb MB free / $totalRamMb MB total",
                writable = true,
                validRange = "Non-persistent cached background packages",
                evidenceSource = "SRC-AOSP-ADPF",
                fallbackExplanation = "Always available via normal KILL_BACKGROUND_PROCESSES permission."
            )
        )

        // 4. Android Game Mode Override
        val gameModeStatus = when {
            sdkInt < Build.VERSION_CODES.S -> CapabilityStatus.UNSUPPORTED
            currentPrivilege.rank >= PrivilegeLevel.SHIZUKU_SHELL.rank -> CapabilityStatus.SUPPORTED
            gameManagerSupported -> CapabilityStatus.REQUIRES_SHIZUKU
            else -> CapabilityStatus.UNSUPPORTED
        }
        list.add(
            CapabilityItem(
                id = "android_game_mode_perf",
                name = "Android Game Mode & Interventions",
                category = "Game Mode",
                status = gameModeStatus,
                requiredPrivilege = PrivilegeLevel.SHIZUKU_SHELL,
                pathOrApi = "GameManager / `cmd game mode [performance|standard|battery] <pkg>`",
                currentReadValue = if (gameManagerSupported) "GameManager active (mode=$currentGameMode)" else "API $sdkInt",
                writable = currentPrivilege.rank >= PrivilegeLevel.SHIZUKU_SHELL.rank,
                validRange = "1 (Standard), 2 (Performance), 3 (Battery)",
                evidenceSource = "SRC-AOSP-GAMEMODE",
                fallbackExplanation = "Applies ADPF HintSession and Background Memory Trim when Shizuku is not connected."
            )
        )

        // 5. PowerManager Fixed Performance Mode
        val fixedPerfStatus = if (currentPrivilege.rank >= PrivilegeLevel.SHIZUKU_SHELL.rank) {
            CapabilityStatus.SUPPORTED
        } else {
            CapabilityStatus.REQUIRES_SHIZUKU
        }
        list.add(
            CapabilityItem(
                id = "power_fixed_performance_mode",
                name = "PowerHAL Fixed Performance Mode",
                category = "Power & Performance HAL",
                status = fixedPerfStatus,
                requiredPrivilege = PrivilegeLevel.SHIZUKU_SHELL,
                pathOrApi = "`cmd power set-fixed-performance-mode-enabled [true|false]`",
                currentReadValue = if (fixedPerfStatus == CapabilityStatus.SUPPORTED) "Ready via Shizuku Shell" else "Requires Shizuku UID 2000",
                writable = fixedPerfStatus == CapabilityStatus.SUPPORTED,
                validRange = "true | false",
                evidenceSource = "SRC-AOSP-ADPF",
                fallbackExplanation = "Uses ADPF HintSession to request dynamic boost from PowerHAL."
            )
        )

        // 6. CPUFreq Policy Control
        val cpuReadable = cpuPolicies.any { it.isReadable }
        val cpuWritable = cpuPolicies.any { it.isWritable } || currentPrivilege == PrivilegeLevel.ROOT
        val cpuStatus = when {
            cpuPolicies.isEmpty() -> CapabilityStatus.UNSUPPORTED
            cpuWritable -> CapabilityStatus.SUPPORTED
            cpuReadable -> CapabilityStatus.REQUIRES_ROOT
            else -> CapabilityStatus.PARTIAL
        }
        val cpuSummary = if (cpuPolicies.isNotEmpty()) {
            cpuPolicies.joinToString("; ") {
                "policy${it.policyId}: ${it.curFreqKhz / 1000}MHz [${it.minFreqKhz / 1000}-${it.maxFreqKhz / 1000}MHz] (${it.currentGovernor})"
            }
        } else {
            "No readable policy nodes in /sys/devices/system/cpu/cpufreq"
        }
        list.add(
            CapabilityItem(
                id = "cpufreq_policy_floor",
                name = "Kernel CPUFreq Policy & Governor Control",
                category = "Kernel CPUFreq",
                status = cpuStatus,
                requiredPrivilege = PrivilegeLevel.ROOT,
                pathOrApi = "/sys/devices/system/cpu/cpufreq/policy*/scaling_min_freq",
                currentReadValue = cpuSummary,
                writable = cpuWritable,
                validRange = cpuPolicies.firstOrNull()?.availableFrequenciesKhz
                    ?.joinToString(", ") { "${it / 1000}MHz" }
                    ?.ifBlank { "Hardware OPP table" } ?: "Hardware OPP table",
                evidenceSource = "SRC-KERNEL-CPUFREQ",
                fallbackExplanation = "Read-only telemetry + Shizuku `cmd power set-fixed-performance-mode-enabled`."
            )
        )

        // 7. GPU Devfreq Control
        val gpuReadable = gpuNodes.any { it.isReadable }
        val gpuWritable = gpuNodes.any { it.isWritable } || (gpuNodes.isNotEmpty() && currentPrivilege == PrivilegeLevel.ROOT)
        val gpuStatus = when {
            gpuNodes.isEmpty() -> CapabilityStatus.UNSUPPORTED
            gpuWritable -> CapabilityStatus.SUPPORTED
            gpuReadable -> CapabilityStatus.REQUIRES_ROOT
            else -> CapabilityStatus.REQUIRES_ROOT
        }
        list.add(
            CapabilityItem(
                id = "gpu_devfreq_min_floor",
                name = "Kernel GPU Devfreq OPP Control",
                category = "Kernel GPU Devfreq",
                status = gpuStatus,
                requiredPrivilege = PrivilegeLevel.ROOT,
                pathOrApi = gpuNodes.firstOrNull()?.path ?: "/sys/class/devfreq/* (Not exposed by kernel/SELinux)",
                currentReadValue = if (gpuNodes.isNotEmpty()) {
                    gpuNodes.joinToString { "${it.name}: ${it.curFreqHz / 1_000_000}MHz (${it.currentGovernor})" }
                } else {
                    "Restricted by SELinux / Virtualized GPU"
                },
                writable = gpuWritable,
                validRange = gpuNodes.firstOrNull()?.availableFrequenciesHz
                    ?.joinToString(", ") { "${it / 1_000_000}MHz" }
                    ?.ifBlank { "Discrete GPU OPPs" } ?: "N/A",
                evidenceSource = "SRC-KERNEL-DEVFREQ",
                fallbackExplanation = "Uses Android Game Mode GPU scaling intervention (`cmd game set`)."
            )
        )

        // 8. Display Refresh Rate Selection
        list.add(
            CapabilityItem(
                id = "display_refresh_rate",
                name = "Display Refresh Rate & Frame Pacing",
                category = "Display & SurfaceFlinger",
                status = if (supportedRefreshRates.size > 1) CapabilityStatus.SUPPORTED else CapabilityStatus.PARTIAL,
                requiredPrivilege = PrivilegeLevel.NORMAL,
                pathOrApi = "Display.getSupportedModes() / WindowManager.LayoutParams.preferredDisplayModeId",
                currentReadValue = "${currentRefreshRate.toInt()} Hz",
                writable = supportedRefreshRates.size > 1,
                validRange = supportedRefreshRates.joinToString(", ") { "${it.toInt()} Hz" },
                evidenceSource = "SRC-AOSP-FRAMEMETRICS",
                fallbackExplanation = "Locks frame pacing target to native ${currentRefreshRate.toInt()} Hz VSYNC."
            )
        )

        // 9. Thermal Zone Sysfs Sensors
        list.add(
            CapabilityItem(
                id = "thermal_zone_sysfs",
                name = "Kernel Thermal Zones (/sys/class/thermal)",
                category = "Thermal & Safety",
                status = if (thermalZones.isNotEmpty()) CapabilityStatus.SUPPORTED else CapabilityStatus.PARTIAL,
                requiredPrivilege = PrivilegeLevel.NORMAL,
                pathOrApi = "/sys/class/thermal/thermal_zone*/temp",
                currentReadValue = if (thermalZones.isNotEmpty()) {
                    "${thermalZones.size} readable zones (Max: ${"%.1f".format(thermalZones.maxOf { it.tempCelsius })}°C)"
                } else {
                    "SELinux restricted — using BatteryManager + PowerManager thermal API"
                },
                writable = false, // NEVER writable by safety policy!
                validRange = "Read-only safety telemetry (Write strictly prohibited)",
                evidenceSource = "SRC-KERNEL-THERMAL",
                fallbackExplanation = "Combines PowerManager thermal headroom and BatteryManager thermistor."
            )
        )

        // 10. OEM Adapter Profile
        list.add(
            CapabilityItem(
                id = "oem_vendor_adapter",
                name = "OEM Vendor Performance Adapter ($oemAdapter)",
                category = "OEM Framework",
                status = CapabilityStatus.SUPPORTED,
                requiredPrivilege = PrivilegeLevel.NORMAL,
                pathOrApi = "Build.MANUFACTURER / SoC HAL Profile",
                currentReadValue = oemAdapter,
                writable = false,
                validRange = "Isolated vendor profile (Zero unverified proprietary writes)",
                evidenceSource = "SRC-AOSP-ADPF",
                fallbackExplanation = "Uses standard AOSP GameManager + ADPF HAL interfaces.",
                isOemSpecific = true
            )
        )

        return list
    }

    private fun resolveOemAdapter(manufacturer: String, brand: String): String {
        val m = "$manufacturer $brand".lowercase()
        return when {
            m.contains("google") -> "Google Pixel / Tensor ADPF Adapter"
            m.contains("samsung") -> "Samsung Galaxy / GameSDK Standard Adapter"
            m.contains("xiaomi") || m.contains("redmi") || m.contains("poco") -> "Xiaomi / HyperOS PowerHAL Adapter"
            m.contains("oneplus") -> "OnePlus / Trinity Engine Standard Adapter"
            m.contains("oppo") -> "OPPO / ColorOS HyperBoost Adapter"
            m.contains("vivo") || m.contains("iqoo") -> "vivo / Multi-Turbo Standard Adapter"
            m.contains("realme") -> "realme UI Performance Adapter"
            m.contains("motorola") -> "Motorola / ReadyFor Performance Adapter"
            m.contains("asus") -> "ASUS ROG / Armoury Crate Standard Adapter"
            else -> "AOSP Generic HAL Adapter ($manufacturer)"
        }
    }

    private fun probeDisplayRefreshRates(): Pair<List<Float>, Float> {
        return try {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            @Suppress("DEPRECATION")
            val display = wm?.defaultDisplay
            @Suppress("DEPRECATION")
            val modes = display?.supportedModes?.map { it.refreshRate }?.distinct()?.sorted()
                ?: listOf(60f)
            @Suppress("DEPRECATION")
            val current = display?.refreshRate ?: 60f
            Pair(modes.ifEmpty { listOf(60f) }, current)
        } catch (_: Throwable) {
            Pair(listOf(60f), 60f)
        }
    }

    private fun readFirstLine(file: File): String? {
        return try {
            if (file.exists() && file.canRead()) {
                file.bufferedReader().use { it.readLine()?.trim() }
            } else {
                null
            }
        } catch (_: Throwable) {
            null
        }
    }
}
