package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.capability.CapabilityScanner
import com.example.core.performance.PerformanceEngine
import com.example.core.privilege.ShizukuEngine
import com.example.core.telemetry.TelemetryEngine
import com.example.data.local.NexusDatabase
import com.example.model.BoostControlState
import com.example.model.PerformanceProfileType
import com.example.model.PrivilegeLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var db: NexusDatabase
    private val testScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, NexusDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read app_name from context matches Nexus Boost`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Nexus Boost", appName)
    }

    @Test
    fun `capability scanner discovers capabilities and exports valid json and markdown`() = runBlocking {
        val scanner = CapabilityScanner(context)
        val report = scanner.scanDeviceCapabilities(PrivilegeLevel.NORMAL)
        assertNotNull(report)
        assertTrue(report.capabilities.isNotEmpty())

        val json = scanner.exportReportAsJson(report)
        val md = scanner.exportReportAsMarkdown(report)
        assertTrue(json.contains("adpf_hint_session"))
        assertTrue(md.contains("NEXUS BOOST — DEVICE CAPABILITY REPORT"))
    }

    @Test
    fun `performance engine transactional boost and rollback works on normal privilege`() = runBlocking {
        val dao = db.nexusDao()
        val shizukuEngine = ShizukuEngine(context, dao, testScope)
        val scanner = CapabilityScanner(context)
        val perfEngine = PerformanceEngine(context, dao, shizukuEngine)

        val report = scanner.scanDeviceCapabilities(PrivilegeLevel.NORMAL)
        perfEngine.syncCatalogWithCapabilities(
            report = report,
            privilegeLevel = PrivilegeLevel.NORMAL,
            selectedProfile = PerformanceProfileType.SUSTAINED,
            targetPackage = context.packageName
        )

        val (applied, rejected) = perfEngine.engageProfile(
            profile = PerformanceProfileType.SUSTAINED,
            targetPackage = context.packageName,
            report = report,
            privilegeLevel = PrivilegeLevel.NORMAL
        )

        assertTrue(applied.isNotEmpty())
        assertTrue(rejected.isNotEmpty()) // Shizuku & Root tweaks are honestly skipped on NORMAL tier
        assertEquals(BoostControlState.ACTIVE, perfEngine.boostState.value)

        // Test thermal safety guard hysteresis
        perfEngine.evaluateThermalSafetyGuard(currentTempCelsius = 43.5f, thermalStatusCode = 2)
        assertEquals(BoostControlState.LIMITED, perfEngine.boostState.value)

        // Cooling to 40.5°C should remain LIMITED due to 39.5°C hysteresis floor
        perfEngine.evaluateThermalSafetyGuard(currentTempCelsius = 40.5f, thermalStatusCode = 1)
        assertEquals(BoostControlState.LIMITED, perfEngine.boostState.value)

        // Cooling below 39.5°C clears thermal limit
        perfEngine.evaluateThermalSafetyGuard(currentTempCelsius = 38.8f, thermalStatusCode = 0)
        assertEquals(BoostControlState.ACTIVE, perfEngine.boostState.value)

        // Restore baseline
        val restored = perfEngine.restoreBaseline("Unit Test Complete")
        assertTrue(restored)
        assertEquals(BoostControlState.READY, perfEngine.boostState.value)
    }

    @Test
    fun `telemetry engine computes accurate P50 P90 P95 P99 frame percentiles`() {
        val dao = db.nexusDao()
        val scanner = CapabilityScanner(context)
        val telemetry = TelemetryEngine(context, scanner, dao, testScope)

        val samples = listOf(16.1f, 16.4f, 16.6f, 16.5f, 16.7f, 16.2f, 16.3f, 22.4f, 16.5f, 16.6f)
        val summary = telemetry.computeFramePercentiles(samples)
        assertTrue(summary.avgFps > 55f)
        assertTrue(summary.p99Ms >= 20.0f)
        assertEquals(1, summary.jankCount)
    }
}
