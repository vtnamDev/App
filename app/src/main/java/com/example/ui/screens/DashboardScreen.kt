package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BoostControlState
import com.example.model.DeviceCapabilityReport
import com.example.model.LiveTelemetryState
import com.example.model.PerformanceProfileType
import com.example.model.PrivilegeLevel
import com.example.model.ShizukuDiagnostics
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ThermalAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    capabilityReport: DeviceCapabilityReport?,
    shizukuDiagnostics: ShizukuDiagnostics,
    boostState: BoostControlState,
    selectedProfile: PerformanceProfileType,
    targetPackage: String,
    liveTelemetry: LiveTelemetryState,
    frameHistoryMs: List<Float>,
    statusBanner: String,
    onSelectProfile: (PerformanceProfileType) -> Unit,
    onEngageBoost: () -> Unit,
    onRestoreBaseline: () -> Unit,
    onRefreshCapabilities: () -> Unit,
    onRequestShizukuPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("dashboard_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Hero Hardware Telemetry Banner
        item {
            HeroStatusBanner(
                capabilityReport = capabilityReport,
                shizukuDiagnostics = shizukuDiagnostics,
                statusBanner = statusBanner,
                onRefreshCapabilities = onRefreshCapabilities,
                onRequestShizukuPermission = onRequestShizukuPermission
            )
        }

        // Primary Stateful Boost Control Card
        item {
            PrimaryBoostControlCard(
                boostState = boostState,
                selectedProfile = selectedProfile,
                targetPackage = targetPackage,
                onSelectProfile = onSelectProfile,
                onEngageBoost = onEngageBoost,
                onRestoreBaseline = onRestoreBaseline
            )
        }

        // Live Frame-Time & FPS Instrumentation Card
        item {
            LiveFrameTelemetryCard(
                liveTelemetry = liveTelemetry,
                frameHistoryMs = frameHistoryMs
            )
        }

        // Hardware Subsystems Grid (CPU Clusters, GPU Devfreq, Thermal & RAM)
        item {
            HardwareTelemetryGrid(
                liveTelemetry = liveTelemetry,
                capabilityReport = capabilityReport
            )
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun HeroStatusBanner(
    capabilityReport: DeviceCapabilityReport?,
    shizukuDiagnostics: ShizukuDiagnostics,
    statusBanner: String,
    onRefreshCapabilities: () -> Unit,
    onRequestShizukuPermission: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Image(
                painter = painterResource(id = R.drawable.img_nexus_boost_hero),
                contentDescription = "Hardware SoC Telemetry Schematic",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xCC0B0F17),
                                Color(0xF20B0F17)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = capabilityReport?.let { "${it.manufacturer.uppercase()} ${it.model}" }
                                    ?: "SCANNING HARDWARE TOPOLOGY...",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White
                            )
                            Text(
                                text = capabilityReport?.let {
                                    "SoC: ${it.socHardware} • API ${it.sdkInt} • ${it.oemAdapterName}"
                                } ?: "Probing CPUFreq, Devfreq, ADPF & Thermal Zones",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                        }
                        IconButton(
                            onClick = onRefreshCapabilities,
                            modifier = Modifier.testTag("refresh_capabilities_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Rescan Hardware Capabilities",
                                tint = ElectricCyan
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PrivilegeStatusPill(
                            diagnostics = shizukuDiagnostics,
                            onRequestPermission = onRequestShizukuPermission
                        )
                        Text(
                            text = capabilityReport?.let {
                                "${it.capabilities.count { c -> c.status == com.example.model.CapabilityStatus.SUPPORTED }}/${it.capabilities.size} Controls Verified"
                            } ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }
            }
        }
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = statusBanner,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun PrivilegeStatusPill(
    diagnostics: ShizukuDiagnostics,
    onRequestPermission: () -> Unit
) {
    val badgeColor = when (diagnostics.privilegeLevel) {
        PrivilegeLevel.ROOT -> CyberEmerald
        PrivilegeLevel.SHIZUKU_SHELL, PrivilegeLevel.ADB_SHELL -> ElectricCyan
        PrivilegeLevel.NORMAL -> ThermalAmber
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = badgeColor.copy(alpha = 0.16f),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.7f)),
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable { onRequestPermission() }
            .testTag("shizuku_status_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = diagnostics.privilegeLevel.label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PrimaryBoostControlCard(
    boostState: BoostControlState,
    selectedProfile: PerformanceProfileType,
    targetPackage: String,
    onSelectProfile: (PerformanceProfileType) -> Unit,
    onEngageBoost: () -> Unit,
    onRestoreBaseline: () -> Unit
) {
    val stateAccentColor by animateColorAsState(
        targetValue = when (boostState) {
            BoostControlState.READY -> ElectricCyan
            BoostControlState.BOOSTING -> ThermalAmber
            BoostControlState.ACTIVE -> CyberEmerald
            BoostControlState.LIMITED -> ThermalAmber
            BoostControlState.FAILED -> AlertCrimson
            BoostControlState.RESTORING -> ElectricCyan
        },
        label = "boostStateColor"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, stateAccentColor.copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "PERFORMANCE ORCHESTRATOR",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Target: $targetPackage",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = stateAccentColor.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, stateAccentColor),
                    modifier = Modifier.testTag("boost_state_badge")
                ) {
                    Text(
                        text = boostState.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = stateAccentColor,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Profile Selection Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PerformanceProfileType.entries.forEach { profile ->
                    val isSelected = profile == selectedProfile
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectProfile(profile) },
                        label = {
                            Text(
                                text = profile.displayName,
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("profile_chip_${profile.name.lowercase()}")
                    )
                }
            }

            Text(
                text = selectedProfile.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onEngageBoost,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("engage_boost_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (boostState == BoostControlState.ACTIVE) CyberEmerald else ElectricCyan,
                        contentColor = Color(0xFF001F26)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (boostState == BoostControlState.ACTIVE) {
                            "RE-VERIFY ${selectedProfile.displayName}"
                        } else {
                            "ENGAGE ${selectedProfile.displayName}"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onRestoreBaseline,
                    modifier = Modifier
                        .height(52.dp)
                        .testTag("restore_baseline_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = "Restore Baseline"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "RESTORE",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveFrameTelemetryCard(
    liveTelemetry: LiveTelemetryState,
    frameHistoryMs: List<Float>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "REAL-TIME VSYNC & FRAME PACING",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Source: ${liveTelemetry.fpsSourceLabel}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = liveTelemetry.fpsConfidence.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = if (liveTelemetry.currentFps > 0f) {
                            "${"%.1f".format(liveTelemetry.currentFps)} FPS"
                        } else {
                            "SAMPLING..."
                        },
                        style = MaterialTheme.typography.displayMedium,
                        color = CyberEmerald
                    )
                    Text(
                        text = "Stability: ${"%.1f".format(liveTelemetry.frameStabilityPercent)}% • Jank (>16.8ms): ${liveTelemetry.jankFramesCount}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PercentileMetricBox("P50", liveTelemetry.p50FrameTimeMs)
                    PercentileMetricBox("P90", liveTelemetry.p90FrameTimeMs)
                    PercentileMetricBox("P95", liveTelemetry.p95FrameTimeMs)
                    PercentileMetricBox("P99", liveTelemetry.p99FrameTimeMs)
                }
            }

            if (frameHistoryMs.size >= 2) {
                Spacer(modifier = Modifier.height(12.dp))
                FrameTimeSparkline(
                    samplesMs = frameHistoryMs,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                )
            }
        }
    }
}

@Composable
private fun PercentileMetricBox(label: String, valueMs: Float) {
    val valueColor = when {
        valueMs <= 0f -> MaterialTheme.colorScheme.onSurfaceVariant
        valueMs <= 16.8f -> CyberEmerald
        valueMs <= 25.0f -> ThermalAmber
        else -> AlertCrimson
    }
    Column(horizontalAlignment = Alignment.End) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${"%.1f".format(valueMs)}ms",
            style = MaterialTheme.typography.labelLarge,
            color = valueColor
        )
    }
}

@Composable
fun FrameTimeSparkline(
    samplesMs: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = ElectricCyan
) {
    Canvas(modifier = modifier) {
        if (samplesMs.size < 2) return@Canvas
        val maxVal = (samplesMs.maxOrNull() ?: 33.3f).coerceAtLeast(20f)
        val minVal = 0f
        val stepX = size.width / (samplesMs.size - 1).coerceAtLeast(1)

        // Draw 16.67ms (60Hz) reference threshold line
        val refY = size.height - ((16.67f - minVal) / (maxVal - minVal)).coerceIn(0f, 1f) * size.height
        drawLine(
            color = ThermalAmber.copy(alpha = 0.35f),
            start = Offset(0f, refY),
            end = Offset(size.width, refY),
            strokeWidth = 2f
        )

        val path = Path()
        samplesMs.forEachIndexed { idx, value ->
            val x = idx * stepX
            val normalizedY = ((value - minVal) / (maxVal - minVal)).coerceIn(0f, 1f)
            val y = size.height - (normalizedY * size.height)
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 4f)
        )
    }
}

@Composable
private fun HardwareTelemetryGrid(
    liveTelemetry: LiveTelemetryState,
    capabilityReport: DeviceCapabilityReport?
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thermal Guard & Temperature Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (liveTelemetry.isThrottling) Icons.Default.Warning else Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = if (liveTelemetry.isThrottling) AlertCrimson else ThermalAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "THERMAL ENVELOPE",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val displayTemp = maxOf(liveTelemetry.batteryTempCelsius, liveTelemetry.maxThermalZoneCelsius)
                    Text(
                        text = "${"%.1f".format(displayTemp)} °C",
                        style = MaterialTheme.typography.headlineMedium,
                        color = if (displayTemp >= 42.0f) AlertCrimson else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = liveTelemetry.thermalStatusLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (liveTelemetry.isThrottling) AlertCrimson else CyberEmerald
                    )
                    Text(
                        text = "Guard Ceiling: 42.0°C (Hysteresis 39.5°C)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Memory Pressure Card
            Card(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MEMORY PRESSURE",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val usedMb = (liveTelemetry.totalRamMb - liveTelemetry.availRamMb).coerceAtLeast(0L)
                    val memRatio = (usedMb.toFloat() / liveTelemetry.totalRamMb.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                    Text(
                        text = "${liveTelemetry.availRamMb} MB Free",
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { memRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (memRatio > 0.85f) ThermalAmber else ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$usedMb / ${liveTelemetry.totalRamMb} MB Used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // CPU Cluster & GPU Devfreq Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CPU CLUSTERS & GPU DEVFREQ TOPOLOGY",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                    Text(
                        text = capabilityReport?.let { "Refresh: ${it.currentRefreshRateHz.toInt()} Hz" } ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                val policies = capabilityReport?.cpuPolicies.orEmpty()
                if (policies.isNotEmpty()) {
                    policies.forEach { policy ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Policy ${policy.policyId} (Cores ${policy.relatedCpus}) [${policy.currentGovernor}]",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${policy.curFreqKhz / 1000} MHz (Range: ${policy.minFreqKhz / 1000}–${policy.maxFreqKhz / 1000} MHz)",
                                style = MaterialTheme.typography.labelMedium,
                                color = CyberEmerald
                            )
                        }
                    }
                } else {
                    Text(
                        text = "CPUFreq sysfs read restricted by kernel SELinux policy • Active Cores: ${Runtime.getRuntime().availableProcessors()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                val gpus = capabilityReport?.gpuNodes.orEmpty()
                if (gpus.isNotEmpty()) {
                    gpus.forEach { gpu ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "GPU Node: ${gpu.name} (${gpu.currentGovernor})",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                text = "${gpu.curFreqHz / 1_000_000} MHz",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "GPU Devfreq Sysfs",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Managed via Android GameManager / ADPF HAL",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThermalAmber
                        )
                    }
                }
            }
        }
    }
}
