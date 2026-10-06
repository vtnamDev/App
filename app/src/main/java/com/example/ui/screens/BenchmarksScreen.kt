package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.BenchmarkSessionEntity
import com.example.model.BenchmarkMode
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ThermalAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BenchmarksScreen(
    sessions: List<BenchmarkSessionEntity>,
    frameHistoryMs: List<Float>,
    thermalHistoryCelsius: List<Float>,
    isBenchmarkRunning: Boolean,
    benchmarkProgressSeconds: Int,
    onRunBenchmark: (BenchmarkMode) -> Unit,
    onExportFormat: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(BenchmarkMode.OPTIMIZED) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("benchmarks_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "TELEMETRY & PERCENTILE BENCHMARK LAB",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricCyan
                    )
                    Text(
                        text = "Records hardware Choreographer P50/P90/P95/P99 frame times, stability %, and thermal delta.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BenchmarkMode.entries.forEach { mode ->
                            FilterChip(
                                selected = selectedMode == mode,
                                onClick = { selectedMode = mode },
                                label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag("bench_mode_${mode.name}")
                            )
                        }
                    }

                    Text(
                        text = selectedMode.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    if (isBenchmarkRunning) {
                        LinearProgressIndicator(
                            progress = { (benchmarkProgressSeconds / 6f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth(),
                            color = CyberEmerald
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Sampling hardware VSYNC & thermal sensors (${benchmarkProgressSeconds}s / 6s)...",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyberEmerald
                        )
                    } else {
                        Button(
                            onClick = { onRunBenchmark(selectedMode) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("start_benchmark_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RUN 6-SECOND ${selectedMode.label} SESSION")
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onExportFormat("json") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_bench_json_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("JSON", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { onExportFormat("csv") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_bench_csv_button")
                        ) {
                            Text("CSV", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { onExportFormat("md") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_bench_md_button")
                        ) {
                            Text("MARKDOWN", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Live Frame-Time & Thermal Graphs
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "P99 FRAME-TIME TRAJECTORY (ms)",
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (frameHistoryMs.size >= 2) {
                        FrameTimeSparkline(
                            samplesMs = frameHistoryMs,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(64.dp),
                            lineColor = ElectricCyan
                        )
                    } else {
                        Text(
                            text = "Collecting VSYNC frame samples...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "THERMAL TRAJECTORY (°C)",
                        style = MaterialTheme.typography.labelMedium,
                        color = ThermalAmber
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (thermalHistoryCelsius.size >= 2) {
                        FrameTimeSparkline(
                            samplesMs = thermalHistoryCelsius,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            lineColor = ThermalAmber
                        )
                    }
                }
            }
        }

        items(sessions, key = { it.id }) { session ->
            BenchmarkSessionCard(session = session)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun BenchmarkSessionCard(session: BenchmarkSessionEntity) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("benchmark_session_card_${session.id}"),
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
                Column {
                    Text(
                        text = "${session.mode} • ${session.profileName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = CyberEmerald
                    )
                    Text(
                        text = "Target: ${session.targetPackage} • ${session.deviceModel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ElectricCyan.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${"%.1f".format(session.avgFps)} FPS",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Frame Times: P50=${"%.2f".format(session.p50FrameTimeMs)}ms | P90=${"%.2f".format(session.p90FrameTimeMs)}ms | P95=${"%.2f".format(session.p95FrameTimeMs)}ms | P99=${"%.2f".format(session.p99FrameTimeMs)}ms",
                style = MaterialTheme.typography.labelSmall
            )
            Text(
                text = "Stability: ${"%.1f".format(session.frameStabilityPercent)}% | Thermal: ${"%.1f".format(session.startTempCelsius)}°C → ${"%.1f".format(session.endTempCelsius)}°C | Confidence: ${session.confidenceLevel}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Active Controls: ${session.appliedControlsSummary}",
                style = MaterialTheme.typography.bodySmall,
                color = ThermalAmber
            )
        }
    }
}
