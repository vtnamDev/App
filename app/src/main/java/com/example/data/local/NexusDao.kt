package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NexusDao {
    // Device Snapshots
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeviceSnapshot(snapshot: DeviceSnapshotEntity): Long

    @Query("SELECT * FROM device_snapshots ORDER BY timestamp DESC LIMIT 10")
    fun observeDeviceSnapshots(): Flow<List<DeviceSnapshotEntity>>

    // Game Profiles
    @Query("SELECT * FROM game_profiles ORDER BY lastPlayedTimestamp DESC")
    fun observeGameProfiles(): Flow<List<GameProfileEntity>>

    @Query("SELECT * FROM game_profiles WHERE packageName = :packageName LIMIT 1")
    suspend fun getGameProfile(packageName: String): GameProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertGameProfile(profile: GameProfileEntity)

    @Query("DELETE FROM game_profiles WHERE packageName = :packageName")
    suspend fun deleteGameProfile(packageName: String)

    // Tuning Actions (Audit Trail)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTuningAction(action: TuningActionEntity): Long

    @Query("SELECT * FROM tuning_actions ORDER BY timestamp DESC LIMIT 100")
    fun observeTuningActions(): Flow<List<TuningActionEntity>>

    @Query("SELECT * FROM tuning_actions ORDER BY timestamp DESC LIMIT 100")
    suspend fun getRecentTuningActions(): List<TuningActionEntity>

    // Benchmark Sessions
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBenchmarkSession(session: BenchmarkSessionEntity): Long

    @Query("SELECT * FROM benchmark_sessions ORDER BY timestamp DESC")
    fun observeBenchmarkSessions(): Flow<List<BenchmarkSessionEntity>>

    @Query("SELECT * FROM benchmark_sessions ORDER BY timestamp DESC")
    suspend fun getAllBenchmarkSessions(): List<BenchmarkSessionEntity>

    // Telemetry Samples
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTelemetrySample(sample: TelemetrySampleEntity)

    @Query("SELECT * FROM telemetry_samples WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun observeSamplesForSession(sessionId: Long): Flow<List<TelemetrySampleEntity>>

    // Privilege State History
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrivilegeState(state: PrivilegeStateEntity)

    @Query("SELECT * FROM privilege_states ORDER BY timestamp DESC LIMIT 20")
    fun observePrivilegeStates(): Flow<List<PrivilegeStateEntity>>

    // Transactional Rollback Snapshots
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRollbackSnapshot(snapshot: RollbackSnapshotEntity): Long

    @Update
    suspend fun updateRollbackSnapshot(snapshot: RollbackSnapshotEntity)

    @Query("SELECT * FROM rollback_snapshots WHERE isRestored = 0 ORDER BY id DESC")
    suspend fun getPendingRollbackSnapshots(): List<RollbackSnapshotEntity>

    @Query("SELECT * FROM rollback_snapshots ORDER BY timestamp DESC LIMIT 100")
    fun observeRollbackSnapshots(): Flow<List<RollbackSnapshotEntity>>

    // Error Events
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertErrorEvent(event: ErrorEventEntity)

    @Query("SELECT * FROM error_events ORDER BY timestamp DESC LIMIT 100")
    fun observeErrorEvents(): Flow<List<ErrorEventEntity>>

    @Query("DELETE FROM tuning_actions")
    suspend fun clearAuditLogs()

    @Query("DELETE FROM error_events")
    suspend fun clearErrorEvents()
}
