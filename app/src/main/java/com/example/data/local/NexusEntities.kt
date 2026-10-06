package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "device_snapshots")
data class DeviceSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val manufacturer: String,
    val model: String,
    val socHardware: String,
    val sdkInt: Int,
    val kernelVersion: String,
    val cpuClusterCount: Int,
    val gpuNodeCount: Int,
    val thermalZoneCount: Int,
    val supportedCapabilitiesCount: Int,
    val rawReportJson: String
)

@Serializable
@Entity(tableName = "game_profiles")
data class GameProfileEntity(
    @PrimaryKey val packageName: String,
    val displayName: String,
    val genrePreset: String,
    val profileType: String,
    val cpuPolicy: String,
    val gpuPolicy: String,
    val gameModeIntervention: String,
    val preferredRefreshRateHz: Float,
    val backgroundTrimEnabled: Boolean,
    val ioPriorityEnabled: Boolean,
    val networkLowLatencyEnabled: Boolean,
    val thermalStrategy: String,
    val lastBenchmarkFps: Float = 0f,
    val lastBenchmarkP99Ms: Float = 0f,
    val baselineBenchmarkFps: Float = 0f,
    val baselineBenchmarkP99Ms: Float = 0f,
    val appliedTweaksSummary: String = "None",
    val rejectedTweaksSummary: String = "None",
    val lastRestoreStatus: String = "Nominal / Baseline",
    val lastPlayedTimestamp: Long = System.currentTimeMillis()
)

@Serializable
@Entity(tableName = "tuning_actions")
data class TuningActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val operation: String,
    val featureId: String,
    val requiredPrivilege: String,
    val commandOrApi: String,
    val previousValue: String,
    val targetValue: String,
    val readBackValue: String,
    val resultState: String,
    val exitCode: Int,
    val verificationPassed: Boolean,
    val rollbackStatus: String
)

@Serializable
@Entity(tableName = "benchmark_sessions")
data class BenchmarkSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val mode: String,
    val targetPackage: String,
    val profileName: String,
    val durationSeconds: Int,
    val avgFps: Float,
    val p50FrameTimeMs: Float,
    val p90FrameTimeMs: Float,
    val p95FrameTimeMs: Float,
    val p99FrameTimeMs: Float,
    val frameStabilityPercent: Float,
    val jankFrameCount: Int,
    val startTempCelsius: Float,
    val endTempCelsius: Float,
    val thermalThrottleEvents: Int,
    val displayRefreshRateHz: Float,
    val batteryLevelPercent: Int,
    val appliedControlsSummary: String,
    val confidenceLevel: String,
    val deviceModel: String,
    val androidVersion: String
)

@Serializable
@Entity(tableName = "telemetry_samples")
data class TelemetrySampleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val fps: Float,
    val p99FrameTimeMs: Float,
    val cpuFreqMhz: Long,
    val gpuFreqMhz: Long,
    val tempCelsius: Float,
    val thermalStatus: Int,
    val availRamMb: Long
)

@Serializable
@Entity(tableName = "privilege_states")
data class PrivilegeStateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val shizukuInstalled: Boolean,
    val binderAlive: Boolean,
    val permissionGranted: Boolean,
    val uid: Int,
    val privilegeTier: String,
    val statusDetail: String
)

@Serializable
@Entity(tableName = "rollback_snapshots")
data class RollbackSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val bootSessionId: Long,
    val targetPackage: String,
    val featureId: String,
    val mechanismType: String, // "ADPF", "GAME_MODE", "POWER_FIXED", "SYSFS_WRITE"
    val pathOrTarget: String,
    val originalValue: String,
    val appliedValue: String,
    val isRestored: Boolean = false,
    val restoredTimestamp: Long = 0L,
    val restoreVerificationNote: String = "Pending"
)

@Serializable
@Entity(tableName = "error_events")
data class ErrorEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val logLevel: String,
    val subsystem: String,
    val message: String,
    val recoveryAction: String
)
