package com.example.model

import kotlinx.serialization.Serializable

@Serializable
enum class PrivilegeLevel(val rank: Int, val label: String) {
    NORMAL(0, "NORMAL (Android SDK)"),
    ADB_SHELL(1, "ADB / SHELL"),
    SHIZUKU_SHELL(2, "SHIZUKU (UID 2000 SHELL)"),
    ROOT(3, "ROOT (UID 0 SUPERSU/MAGISK)")
}

@Serializable
enum class CapabilityStatus(val label: String) {
    SUPPORTED("SUPPORTED"),
    PARTIAL("PARTIAL"),
    UNSUPPORTED("UNSUPPORTED"),
    UNKNOWN("UNKNOWN"),
    REQUIRES_SHIZUKU("REQUIRES SHIZUKU"),
    REQUIRES_ROOT("REQUIRES ROOT")
}

@Serializable
enum class OptimizationState {
    DISCOVERED,
    SUPPORTED,
    READY,
    APPLYING,
    APPLIED,
    VERIFYING,
    ACTIVE,
    ROLLING_BACK,
    RESTORED,
    // Failure states
    UNSUPPORTED,
    PERMISSION_REQUIRED,
    VALIDATION_FAILED,
    EXECUTION_FAILED,
    VERIFICATION_FAILED,
    THERMAL_LIMIT,
    ROLLBACK_FAILED
}

@Serializable
enum class BoostControlState(val label: String) {
    READY("READY"),
    BOOSTING("BOOSTING"),
    ACTIVE("ACTIVE"),
    LIMITED("THERMAL LIMITED"),
    FAILED("FAILED"),
    RESTORING("RESTORING")
}

@Serializable
enum class PerformanceProfileType(
    val displayName: String,
    val description: String,
    val targetFrameDurationNanos: Long,
    val cpuFreqPercentile: Float,
    val preferPerformanceGameMode: Boolean,
    val enableFixedPerformanceMode: Boolean,
    val trimBackgroundMemory: Boolean,
    val thermalCeilingCelsius: Float
) {
    MAXIMUM(
        displayName = "MAXIMUM",
        description = "Selects the highest valid performance state exposed by device hardware while preserving thermal shutdown guards.",
        targetFrameDurationNanos = 8_333_333L, // 120Hz target
        cpuFreqPercentile = 1.0f,
        preferPerformanceGameMode = true,
        enableFixedPerformanceMode = true,
        trimBackgroundMemory = true,
        thermalCeilingCelsius = 43.0f
    ),
    SUSTAINED(
        displayName = "SUSTAINED",
        description = "Optimizes for consistent P99 frame pacing and stable thermal equilibrium over long sessions.",
        targetFrameDurationNanos = 11_111_111L, // 90Hz target
        cpuFreqPercentile = 0.75f,
        preferPerformanceGameMode = true,
        enableFixedPerformanceMode = false,
        trimBackgroundMemory = true,
        thermalCeilingCelsius = 41.0f
    ),
    LOW_LATENCY(
        displayName = "LOW LATENCY",
        description = "Prioritizes ADPF scheduling hints, input/render thread responsiveness, and minimum frequency floors.",
        targetFrameDurationNanos = 8_333_333L,
        cpuFreqPercentile = 0.85f,
        preferPerformanceGameMode = true,
        enableFixedPerformanceMode = true,
        trimBackgroundMemory = true,
        thermalCeilingCelsius = 42.0f
    ),
    BALANCED(
        displayName = "BALANCED",
        description = "Moderate ADPF hints and background memory trim without aggressive frequency floors.",
        targetFrameDurationNanos = 16_666_666L, // 60Hz target
        cpuFreqPercentile = 0.50f,
        preferPerformanceGameMode = false,
        enableFixedPerformanceMode = false,
        trimBackgroundMemory = true,
        thermalCeilingCelsius = 40.0f
    ),
    BATTERY(
        displayName = "BATTERY SAVER",
        description = "Selects Android Game Mode Battery intervention and conservative frequency scaling.",
        targetFrameDurationNanos = 16_666_666L,
        cpuFreqPercentile = 0.25f,
        preferPerformanceGameMode = false,
        enableFixedPerformanceMode = false,
        trimBackgroundMemory = false,
        thermalCeilingCelsius = 38.5f
    )
}

@Serializable
enum class GameGenrePreset(
    val title: String,
    val recommendedProfile: PerformanceProfileType,
    val rationale: String
) {
    COMPETITIVE_FPS(
        "Competitive FPS",
        PerformanceProfileType.LOW_LATENCY,
        "Minimizes frame-time P99 variance and touch-to-photon latency via ADPF & high refresh targets."
    ),
    MOBA(
        "MOBA",
        PerformanceProfileType.SUSTAINED,
        "Balances high frame pacing during team-fight bursts with thermal headroom stability."
    ),
    BATTLE_ROYALE(
        "Battle Royale",
        PerformanceProfileType.SUSTAINED,
        "Prevents mid-match thermal throttling across 25-minute open-world sessions."
    ),
    RPG(
        "Open-World RPG",
        PerformanceProfileType.MAXIMUM,
        "Maximizes GPU/CPU OPP headroom and trims cached memory for heavy shader/asset streaming."
    ),
    EMULATOR(
        "Console Emulator",
        PerformanceProfileType.MAXIMUM,
        "Prioritizes single-core Prime/Performance CPU cluster frequency floors and ADPF hints."
    ),
    CLOUD_GAMING(
        "Cloud Gaming",
        PerformanceProfileType.BALANCED,
        "Keeps decode latency low while avoiding unnecessary SoC heat generation."
    ),
    GENERAL_3D(
        "General 3D",
        PerformanceProfileType.BALANCED,
        "Standard ADPF thread hints and safe background memory reclamation."
    )
}

@Serializable
enum class ConfidenceLevel(val badge: String) {
    HIGH("HIGH (Hardware / SurfaceFlinger)"),
    MEDIUM("MEDIUM (App Choreographer)"),
    LOW("LOW (Estimated)"),
    UNAVAILABLE("UNAVAILABLE")
}

@Serializable
enum class BenchmarkMode(val label: String, val description: String) {
    BASELINE("BASELINE", "Measures un-tuned hardware frame-time percentiles and thermal slope."),
    OPTIMIZED("OPTIMIZED", "Measures active performance profile with verified tunings engaged."),
    SUSTAINED("SUSTAINED", "Evaluates P99 frame stability and hysteresis behavior under continuous load."),
    THERMAL_STRESS_OBSERVATION("THERMAL OBSERVATION", "Tracks thermal zone trajectory and throttling events without synthetic overheating.")
}

@Serializable
enum class LogLevel {
    ERROR, WARN, INFO, DEBUG, TRACE
}

@Serializable
data class CpuPolicyNode(
    val policyId: Int,
    val path: String,
    val relatedCpus: String,
    val currentGovernor: String,
    val availableGovernors: List<String>,
    val curFreqKhz: Long,
    val minFreqKhz: Long,
    val maxFreqKhz: Long,
    val availableFrequenciesKhz: List<Long>,
    val isReadable: Boolean,
    val isWritable: Boolean
)

@Serializable
data class GpuDevfreqNode(
    val name: String,
    val path: String,
    val currentGovernor: String,
    val curFreqHz: Long,
    val minFreqHz: Long,
    val maxFreqHz: Long,
    val availableFrequenciesHz: List<Long>,
    val isReadable: Boolean,
    val isWritable: Boolean
)

@Serializable
data class ThermalZoneNode(
    val index: Int,
    val type: String,
    val path: String,
    val tempCelsius: Float,
    val isReadable: Boolean
)

@Serializable
data class CapabilityItem(
    val id: String,
    val name: String,
    val category: String,
    val status: CapabilityStatus,
    val requiredPrivilege: PrivilegeLevel,
    val pathOrApi: String,
    val currentReadValue: String,
    val writable: Boolean,
    val validRange: String,
    val evidenceSource: String,
    val fallbackExplanation: String,
    val isOemSpecific: Boolean = false
)

@Serializable
data class DeviceCapabilityReport(
    val scanTimestamp: Long,
    val manufacturer: String,
    val brand: String,
    val model: String,
    val socHardware: String,
    val board: String,
    val abi: String,
    val sdkInt: Int,
    val releaseVersion: String,
    val kernelVersion: String,
    val oemAdapterName: String,
    val totalRamMb: Long,
    val availRamMb: Long,
    val supportedRefreshRatesHz: List<Float>,
    val currentRefreshRateHz: Float,
    val adpfSupported: Boolean,
    val adpfPreferredRateNanos: Long,
    val gameManagerSupported: Boolean,
    val powerThermalApiSupported: Boolean,
    val currentThermalStatus: Int,
    val thermalHeadroom10s: Float,
    val cpuPolicies: List<CpuPolicyNode>,
    val gpuNodes: List<GpuDevfreqNode>,
    val thermalZones: List<ThermalZoneNode>,
    val capabilities: List<CapabilityItem>
)

@Serializable
data class TuningDefinition(
    val id: String,
    val name: String,
    val mechanism: String,
    val layer: String,
    val requiredPrivilege: PrivilegeLevel,
    val readPathOrApi: String,
    val writePathOrApi: String,
    val currentValue: String,
    val targetValue: String,
    val risk: String,
    val evidenceIds: List<String>,
    val rollbackMethod: String,
    val status: CapabilityStatus,
    val executionState: OptimizationState = OptimizationState.DISCOVERED,
    val lastVerificationDetail: String = "Not yet applied"
)

@Serializable
data class ShizukuDiagnostics(
    val isInstalled: Boolean = false,
    val isBinderAlive: Boolean = false,
    val isPermissionGranted: Boolean = false,
    val uid: Int = -1,
    val privilegeLevel: PrivilegeLevel = PrivilegeLevel.NORMAL,
    val apiVersion: Int = -1,
    val binderStatusText: String = "Unbound",
    val lastCommand: String = "None",
    val lastExitCode: Int = 0,
    val lastError: String = "None",
    val supportedOperations: List<String> = emptyList()
)

@Serializable
data class LiveTelemetryState(
    val timestamp: Long = System.currentTimeMillis(),
    val currentFps: Float = 0f,
    val p50FrameTimeMs: Float = 0f,
    val p90FrameTimeMs: Float = 0f,
    val p95FrameTimeMs: Float = 0f,
    val p99FrameTimeMs: Float = 0f,
    val frameStabilityPercent: Float = 100f,
    val jankFramesCount: Int = 0,
    val fpsConfidence: ConfidenceLevel = ConfidenceLevel.MEDIUM,
    val fpsSourceLabel: String = "Choreographer Hardware VSYNC",
    val cpuLoadPercent: Float = 0f,
    val cpuClusterFreqsMhz: List<Pair<String, Long>> = emptyList(),
    val gpuFreqMhz: Long = 0L,
    val gpuLoadPercent: Float = -1f, // -1 if unreadable
    val batteryTempCelsius: Float = 0f,
    val maxThermalZoneCelsius: Float = 0f,
    val thermalStatusCode: Int = 0,
    val thermalStatusLabel: String = "NONE (Nominal)",
    val thermalHeadroom: Float = 0f,
    val availRamMb: Long = 0L,
    val totalRamMb: Long = 0L,
    val batteryCurrentMicroAmps: Long = 0L,
    val isThrottling: Boolean = false
)
