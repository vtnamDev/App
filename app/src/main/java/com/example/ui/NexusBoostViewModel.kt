package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.ai.AiAdvisorMode
import com.example.core.ai.ChatTurn
import com.example.core.ai.GeminiAdvisorService
import com.example.core.capability.CapabilityScanner
import com.example.core.performance.PerformanceEngine
import com.example.core.privilege.ShizukuEngine
import com.example.core.telemetry.TelemetryEngine
import com.example.data.local.BenchmarkSessionEntity
import com.example.data.local.DeviceSnapshotEntity
import com.example.data.local.ErrorEventEntity
import com.example.data.local.GameProfileEntity
import com.example.data.local.NexusDatabase
import com.example.data.local.RollbackSnapshotEntity
import com.example.data.local.TuningActionEntity
import com.example.model.BenchmarkMode
import com.example.model.BoostControlState
import com.example.model.DeviceCapabilityReport
import com.example.model.GameGenrePreset
import com.example.model.LiveTelemetryState
import com.example.model.PerformanceProfileType
import com.example.model.ShizukuDiagnostics
import com.example.model.TuningDefinition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "nexus_boost_prefs")

class NexusBoostViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val database = NexusDatabase.getInstance(context)
    private val dao = database.nexusDao()

    val shizukuEngine = ShizukuEngine(context, dao, viewModelScope)
    val capabilityScanner = CapabilityScanner(context)
    val performanceEngine = PerformanceEngine(context, dao, shizukuEngine)
    val telemetryEngine = TelemetryEngine(context, capabilityScanner, dao, viewModelScope)
    private val geminiAdvisor = GeminiAdvisorService()

    val shizukuDiagnostics: StateFlow<ShizukuDiagnostics> = shizukuEngine.diagnostics
    val boostState: StateFlow<BoostControlState> = performanceEngine.boostState
    val tuningCatalog: StateFlow<List<TuningDefinition>> = performanceEngine.tuningCatalog
    val statusBannerMessage: StateFlow<String> = performanceEngine.statusBannerMessage

    val liveTelemetry: StateFlow<LiveTelemetryState> = telemetryEngine.liveTelemetry
    val frameHistoryMs: StateFlow<List<Float>> = telemetryEngine.frameHistoryMs
    val thermalHistoryCelsius: StateFlow<List<Float>> = telemetryEngine.thermalHistoryCelsius
    val isBenchmarkRunning: StateFlow<Boolean> = telemetryEngine.isBenchmarkRunning
    val benchmarkProgressSeconds: StateFlow<Int> = telemetryEngine.benchmarkProgressSeconds

    private val _capabilityReport = MutableStateFlow<DeviceCapabilityReport?>(null)
    val capabilityReport: StateFlow<DeviceCapabilityReport?> = _capabilityReport.asStateFlow()

    private val _selectedProfile = MutableStateFlow(PerformanceProfileType.SUSTAINED)
    val selectedProfile: StateFlow<PerformanceProfileType> = _selectedProfile.asStateFlow()

    private val _selectedTargetPackage = MutableStateFlow(context.packageName)
    val selectedTargetPackage: StateFlow<String> = _selectedTargetPackage.asStateFlow()

    private val _darkThemeEnabled = MutableStateFlow(true)
    val darkThemeEnabled: StateFlow<Boolean> = _darkThemeEnabled.asStateFlow()

    private val _exportedReportTitle = MutableStateFlow<String?>(null)
    val exportedReportTitle: StateFlow<String?> = _exportedReportTitle.asStateFlow()

    private val _exportedReportContent = MutableStateFlow<String?>(null)
    val exportedReportContent: StateFlow<String?> = _exportedReportContent.asStateFlow()

    // AI Advisor State
    private val _selectedAiMode = MutableStateFlow(AiAdvisorMode.DEEP_KERNEL_THINKING)
    val selectedAiMode: StateFlow<AiAdvisorMode> = _selectedAiMode.asStateFlow()

    private val _chatHistory = MutableStateFlow<List<ChatTurn>>(
        listOf(
            ChatTurn(
                isUser = false,
                text = "NEXUS BOOST Systems Engineering Advisor initialized. I have access to your live `DeviceCapabilityReport`, `Shizuku` privilege state, CPU/GPU sysfs topology, and real-time P99 frame-time telemetry.\n\n" +
                    "Select **High-Thinking Architect** (`gemini-3.1-pro-preview` with `ThinkingLevel.HIGH`) for deep kernel/thermal analysis, **Search-Grounded Intel** (`gemini-3.5-flash` + Google Search) for live SoC/game optimization patches, or **Fast Telemetry Triage** (`gemini-3.1-flash-lite-preview`) for quick diagnostics.",
                modeBadge = "System Initialized"
            )
        )
    )
    val chatHistory: StateFlow<List<ChatTurn>> = _chatHistory.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // Room Flows
    val gameProfiles: StateFlow<List<GameProfileEntity>> = dao.observeGameProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val benchmarkSessions: StateFlow<List<BenchmarkSessionEntity>> = dao.observeBenchmarkSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tuningActions: StateFlow<List<TuningActionEntity>> = dao.observeTuningActions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rollbackSnapshots: StateFlow<List<RollbackSnapshotEntity>> = dao.observeRollbackSnapshots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val errorEvents: StateFlow<List<ErrorEventEntity>> = dao.observeErrorEvents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    companion object {
        private val KEY_DARK_THEME = booleanPreferencesKey("dark_theme_enabled")
        private val KEY_SELECTED_PROFILE = stringPreferencesKey("selected_profile")
    }

    init {
        shizukuEngine.registerListeners(
            onBinderDead = {
                viewModelScope.launch {
                    performanceEngine.restoreBaseline("Shizuku Binder Disconnected")
                }
            }
        )
        viewModelScope.launch {
            loadPreferences()
            performanceEngine.recoverPendingRollbacksOnStartup()
            refreshCapabilitiesAndCatalog()
            seedInitialInstalledGameProfiles()
            telemetryEngine.startTelemetry { tempCelsius, thermalStatus ->
                performanceEngine.evaluateThermalSafetyGuard(tempCelsius, thermalStatus)
            }
        }
    }

    private suspend fun loadPreferences() {
        try {
            val prefs = context.dataStore.data.first()
            _darkThemeEnabled.value = prefs[KEY_DARK_THEME] ?: true
            val profName = prefs[KEY_SELECTED_PROFILE]
            if (profName != null) {
                PerformanceProfileType.entries.find { it.name == profName }?.let {
                    _selectedProfile.value = it
                }
            }
        } catch (_: Throwable) {
        }
    }

    fun toggleDarkTheme() {
        val next = !_darkThemeEnabled.value
        _darkThemeEnabled.value = next
        viewModelScope.launch {
            try {
                context.dataStore.edit { it[KEY_DARK_THEME] = next }
            } catch (_: Throwable) {
            }
        }
    }

    fun selectPerformanceProfile(profile: PerformanceProfileType) {
        _selectedProfile.value = profile
        viewModelScope.launch {
            try {
                context.dataStore.edit { it[KEY_SELECTED_PROFILE] = profile.name }
            } catch (_: Throwable) {
            }
            _capabilityReport.value?.let { rep ->
                performanceEngine.syncCatalogWithCapabilities(
                    report = rep,
                    privilegeLevel = shizukuDiagnostics.value.privilegeLevel,
                    selectedProfile = profile,
                    targetPackage = _selectedTargetPackage.value
                )
            }
        }
    }

    fun selectTargetPackage(packageName: String) {
        _selectedTargetPackage.value = packageName
        _capabilityReport.value?.let { rep ->
            performanceEngine.syncCatalogWithCapabilities(
                report = rep,
                privilegeLevel = shizukuDiagnostics.value.privilegeLevel,
                selectedProfile = _selectedProfile.value,
                targetPackage = packageName
            )
        }
    }

    fun refreshCapabilitiesAndCatalog() {
        viewModelScope.launch {
            val diag = shizukuEngine.refreshStatus("Capability Scan")
            val report = capabilityScanner.scanDeviceCapabilities(diag.privilegeLevel)
            _capabilityReport.value = report
            performanceEngine.syncCatalogWithCapabilities(
                report = report,
                privilegeLevel = diag.privilegeLevel,
                selectedProfile = _selectedProfile.value,
                targetPackage = _selectedTargetPackage.value
            )
            dao.insertDeviceSnapshot(
                DeviceSnapshotEntity(
                    manufacturer = report.manufacturer,
                    model = report.model,
                    socHardware = report.socHardware,
                    sdkInt = report.sdkInt,
                    kernelVersion = report.kernelVersion,
                    cpuClusterCount = report.cpuPolicies.size,
                    gpuNodeCount = report.gpuNodes.size,
                    thermalZoneCount = report.thermalZones.size,
                    supportedCapabilitiesCount = report.capabilities.count {
                        it.status == com.example.model.CapabilityStatus.SUPPORTED
                    },
                    rawReportJson = capabilityScanner.exportReportAsJson(report)
                )
            )
        }
    }

    fun requestShizukuPermission() {
        shizukuEngine.requestShizukuPermission()
    }

    fun engageBoost() {
        viewModelScope.launch {
            val report = _capabilityReport.value
                ?: capabilityScanner.scanDeviceCapabilities(shizukuDiagnostics.value.privilegeLevel).also {
                    _capabilityReport.value = it
                }
            val targetPkg = _selectedTargetPackage.value
            val (applied, rejected) = performanceEngine.engageProfile(
                profile = _selectedProfile.value,
                targetPackage = targetPkg,
                report = report,
                privilegeLevel = shizukuDiagnostics.value.privilegeLevel
            )
            // Update GameProfileEntity if one exists for targetPkg
            val existing = dao.getGameProfile(targetPkg)
            if (existing != null) {
                dao.upsertGameProfile(
                    existing.copy(
                        profileType = _selectedProfile.value.name,
                        appliedTweaksSummary = applied.joinToString(", ").ifBlank { "None" },
                        rejectedTweaksSummary = rejected.joinToString(", ").ifBlank { "None" },
                        lastRestoreStatus = "Boost Active (${applied.size} applied)",
                        lastPlayedTimestamp = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun stopAndRestoreBaseline(reason: String = "User Requested Restore") {
        viewModelScope.launch {
            val ok = performanceEngine.restoreBaseline(reason)
            val targetPkg = _selectedTargetPackage.value
            val existing = dao.getGameProfile(targetPkg)
            if (existing != null) {
                dao.upsertGameProfile(
                    existing.copy(
                        lastRestoreStatus = if (ok) "Restored to Baseline ($reason)" else "Restore Warning ($reason)"
                    )
                )
            }
        }
    }

    private suspend fun seedInitialInstalledGameProfiles() {
        val currentProfiles = dao.observeGameProfiles().first()
        if (currentProfiles.isNotEmpty()) return

        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = try {
            pm.queryIntentActivities(mainIntent, 0)
        } catch (_: Throwable) {
            emptyList()
        }

        val discovered = mutableListOf<GameProfileEntity>()

        // Always include Nexus Boost self-instrumentation profile first
        discovered.add(
            GameProfileEntity(
                packageName = context.packageName,
                displayName = "Nexus Boost (Self-Telemetry & Stress Target)",
                genrePreset = GameGenrePreset.COMPETITIVE_FPS.name,
                profileType = PerformanceProfileType.SUSTAINED.name,
                cpuPolicy = "75% OPP Floor (When Root) / ADPF HintSession",
                gpuPolicy = "Dynamic Devfreq / GameManager Mode 2",
                gameModeIntervention = "PERFORMANCE (Mode 2)",
                preferredRefreshRateHz = _capabilityReport.value?.supportedRefreshRatesHz?.maxOrNull() ?: 60f,
                backgroundTrimEnabled = true,
                ioPriorityEnabled = true,
                networkLowLatencyEnabled = true,
                thermalStrategy = "Hysteresis Step-Down @ 42.0°C"
            )
        )

        // Add user-installed or game-categorized packages discovered on the device
        for (info in apps) {
            val pkg = info.activityInfo?.packageName ?: continue
            if (pkg == context.packageName) continue
            val appInfo = info.activityInfo.applicationInfo
            val isGame = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                appInfo.category == ApplicationInfo.CATEGORY_GAME
            } else {
                @Suppress("DEPRECATION")
                (appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0
            }
            val isUserApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
            if (isGame || isUserApp) {
                val label = try {
                    info.loadLabel(pm).toString()
                } catch (_: Throwable) {
                    pkg.substringAfterLast('.')
                }
                val preset = if (isGame) GameGenrePreset.COMPETITIVE_FPS else GameGenrePreset.GENERAL_3D
                discovered.add(
                    GameProfileEntity(
                        packageName = pkg,
                        displayName = label,
                        genrePreset = preset.name,
                        profileType = preset.recommendedProfile.name,
                        cpuPolicy = "ADPF HintSession + Policy Floor",
                        gpuPolicy = "GameManager Intervention",
                        gameModeIntervention = "PERFORMANCE",
                        preferredRefreshRateHz = _capabilityReport.value?.supportedRefreshRatesHz?.maxOrNull() ?: 60f,
                        backgroundTrimEnabled = true,
                        ioPriorityEnabled = true,
                        networkLowLatencyEnabled = true,
                        thermalStrategy = "Hysteresis Guard (42.0°C -> 39.5°C)"
                    )
                )
                if (discovered.size >= 6) break
            }
        }

        discovered.forEach { dao.upsertGameProfile(it) }
    }

    fun createOrUpdateGameProfile(
        packageName: String,
        displayName: String,
        preset: GameGenrePreset,
        profileType: PerformanceProfileType,
        refreshRateHz: Float,
        backgroundTrim: Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val cleanPkg = packageName.trim()
            if (cleanPkg.isBlank()) return@launch
            val existing = dao.getGameProfile(cleanPkg)
            val entity = (existing ?: GameProfileEntity(
                packageName = cleanPkg,
                displayName = displayName.ifBlank { cleanPkg },
                genrePreset = preset.name,
                profileType = profileType.name,
                cpuPolicy = "OPP Percentile ${(profileType.cpuFreqPercentile * 100).toInt()}%",
                gpuPolicy = "Verified Devfreq / GameManager",
                gameModeIntervention = if (profileType.preferPerformanceGameMode) "PERFORMANCE" else "STANDARD",
                preferredRefreshRateHz = refreshRateHz,
                backgroundTrimEnabled = backgroundTrim,
                ioPriorityEnabled = true,
                networkLowLatencyEnabled = true,
                thermalStrategy = "Hysteresis Step-Down @ ${profileType.thermalCeilingCelsius}°C"
            )).copy(
                displayName = displayName.ifBlank { cleanPkg },
                genrePreset = preset.name,
                profileType = profileType.name,
                preferredRefreshRateHz = refreshRateHz,
                backgroundTrimEnabled = backgroundTrim,
                lastPlayedTimestamp = System.currentTimeMillis()
            )
            dao.upsertGameProfile(entity)
            selectTargetPackage(cleanPkg)
            selectPerformanceProfile(profileType)
        }
    }

    fun applyGameProfileAndBoost(profile: GameProfileEntity) {
        val profType = PerformanceProfileType.entries.find { it.name == profile.profileType }
            ?: PerformanceProfileType.SUSTAINED
        selectTargetPackage(profile.packageName)
        selectPerformanceProfile(profType)
        engageBoost()
    }

    fun runLiveBenchmark(mode: BenchmarkMode, durationSeconds: Int = 6) {
        if (isBenchmarkRunning.value) return
        viewModelScope.launch {
            val activeControls = tuningCatalog.value
                .filter { it.executionState == com.example.model.OptimizationState.ACTIVE }
                .joinToString(", ") { it.name }
                .ifBlank { "Baseline (No active overrides)" }

            val targetPkg = _selectedTargetPackage.value
            val session = telemetryEngine.runBenchmarkSession(
                mode = mode,
                targetPackage = targetPkg,
                profileName = _selectedProfile.value.displayName,
                durationSeconds = durationSeconds,
                appliedControlsSummary = activeControls
            )

            // Update game profile benchmark metrics
            val existing = dao.getGameProfile(targetPkg)
            if (existing != null) {
                val updated = if (mode == BenchmarkMode.BASELINE) {
                    existing.copy(
                        baselineBenchmarkFps = session.avgFps,
                        baselineBenchmarkP99Ms = session.p99FrameTimeMs,
                        lastBenchmarkFps = session.avgFps,
                        lastBenchmarkP99Ms = session.p99FrameTimeMs
                    )
                } else {
                    existing.copy(
                        lastBenchmarkFps = session.avgFps,
                        lastBenchmarkP99Ms = session.p99FrameTimeMs
                    )
                }
                dao.upsertGameProfile(updated)
            }
        }
    }

    fun showCapabilityExport(asJson: Boolean) {
        val rep = _capabilityReport.value ?: return
        _exportedReportTitle.value = if (asJson) "capability-report.json" else "capability-report.md"
        _exportedReportContent.value = if (asJson) {
            capabilityScanner.exportReportAsJson(rep)
        } else {
            capabilityScanner.exportReportAsMarkdown(rep)
        }
    }

    fun showBenchmarkExport(format: String) {
        val sessions = benchmarkSessions.value
        _exportedReportTitle.value = "benchmark-results.$format"
        _exportedReportContent.value = when (format.lowercase()) {
            "json" -> telemetryEngine.exportBenchmarksAsJson(sessions)
            "csv" -> telemetryEngine.exportBenchmarksAsCsv(sessions)
            else -> telemetryEngine.exportBenchmarksAsMarkdown(sessions)
        }
    }

    fun showDiagnosticsAuditExport() {
        viewModelScope.launch {
            val actions = dao.getRecentTuningActions()
            val pending = dao.getPendingRollbackSnapshots()
            val content = buildString {
                appendLine("# NEXUS BOOST — AUDIT & ROLLBACK DIAGNOSTICS EXPORT")
                appendLine("- Privilege Tier: ${shizukuDiagnostics.value.privilegeLevel.label}")
                appendLine("- Binder Status: ${shizukuDiagnostics.value.binderStatusText}")
                appendLine("- Pending Un-restored Snapshots: ${pending.size}")
                appendLine()
                appendLine("## Recent Privileged & API Operations")
                actions.forEach { a ->
                    appendLine("- [${a.operation}] `${a.featureId}` via `${a.commandOrApi}` -> State: **${a.resultState}** (Verified=${a.verificationPassed}, Rollback=${a.rollbackStatus})")
                }
            }
            _exportedReportTitle.value = "security-audit-export.md"
            _exportedReportContent.value = content
        }
    }

    fun dismissExportDialog() {
        _exportedReportTitle.value = null
        _exportedReportContent.value = null
    }

    // AI Advisor Methods
    fun selectAiMode(mode: AiAdvisorMode) {
        _selectedAiMode.value = mode
    }

    fun sendAiAdvisorQuery(userMessage: String) {
        val cleanMsg = userMessage.trim()
        if (cleanMsg.isEmpty() || _isAiLoading.value) return

        val mode = _selectedAiMode.value
        val userTurn = ChatTurn(
            isUser = true,
            text = cleanMsg,
            modeBadge = "Operator"
        )
        val currentHistory = _chatHistory.value + userTurn
        _chatHistory.value = currentHistory
        _isAiLoading.value = true

        viewModelScope.launch {
            val hardwareContext = buildHardwareContextSummary()
            val replyTurn = geminiAdvisor.sendMultiTurnMessage(
                history = currentHistory,
                userPrompt = cleanMsg,
                hardwareContextSummary = hardwareContext,
                mode = mode
            )
            _chatHistory.value = _chatHistory.value + replyTurn
            _isAiLoading.value = false
        }
    }

    private fun buildHardwareContextSummary(): String {
        val rep = _capabilityReport.value
        val tel = liveTelemetry.value
        val diag = shizukuDiagnostics.value
        return buildString {
            appendLine("Device: ${rep?.manufacturer ?: "Android"} ${rep?.model ?: ""} | SoC: ${rep?.socHardware ?: "Unknown"} | SDK: ${rep?.sdkInt ?: 0}")
            appendLine("OEM Adapter: ${rep?.oemAdapterName ?: "Generic"} | Kernel: ${rep?.kernelVersion ?: "Unknown"}")
            appendLine("Privilege State: ${diag.privilegeLevel.label} | Shizuku Binder: ${diag.binderStatusText}")
            appendLine("Active Profile: ${_selectedProfile.value.displayName} | Boost State: ${boostState.value.label}")
            appendLine("Live Telemetry: FPS=${"%.1f".format(tel.currentFps)} (P50=${"%.2f".format(tel.p50FrameTimeMs)}ms, P95=${"%.2f".format(tel.p95FrameTimeMs)}ms, P99=${"%.2f".format(tel.p99FrameTimeMs)}ms, Stability=${"%.1f".format(tel.frameStabilityPercent)}%)")
            appendLine("Thermal: Battery=${"%.1f".format(tel.batteryTempCelsius)}°C, MaxZone=${"%.1f".format(tel.maxThermalZoneCelsius)}°C, Status=${tel.thermalStatusLabel}, Headroom=${"%.2f".format(tel.thermalHeadroom)}")
            appendLine("Memory: ${tel.availRamMb}MB free / ${tel.totalRamMb}MB total")
            if (rep != null) {
                appendLine("Supported Capabilities: ${rep.capabilities.filter { it.status == com.example.model.CapabilityStatus.SUPPORTED }.joinToString { it.id }}")
                appendLine("Restricted Capabilities: ${rep.capabilities.filter { it.status != com.example.model.CapabilityStatus.SUPPORTED }.joinToString { "${it.id}(${it.status.name})" }}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        telemetryEngine.stopTelemetry()
        shizukuEngine.unregisterListeners()
    }
}
