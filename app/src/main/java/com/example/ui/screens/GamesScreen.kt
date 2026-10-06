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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.example.data.local.GameProfileEntity
import com.example.model.GameGenrePreset
import com.example.model.PerformanceProfileType
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ThermalAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GamesScreen(
    gameProfiles: List<GameProfileEntity>,
    selectedTargetPackage: String,
    supportedRefreshRates: List<Float>,
    onApplyGameProfile: (GameProfileEntity) -> Unit,
    onSaveCustomProfile: (
        packageName: String,
        displayName: String,
        preset: GameGenrePreset,
        profileType: PerformanceProfileType,
        refreshRateHz: Float,
        backgroundTrim: Boolean
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddForm by remember { mutableStateOf(false) }
    var customPkg by remember { mutableStateOf("") }
    var customTitle by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableStateOf(GameGenrePreset.COMPETITIVE_FPS) }
    var selectedProfile by remember { mutableStateOf(PerformanceProfileType.LOW_LATENCY) }
    val maxRefresh = supportedRefreshRates.maxOrNull() ?: 60f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("games_screen"),
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "GAME PROFILE & BOOST ORCHESTRATOR",
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricCyan
                            )
                            Text(
                                text = "Per-package ADPF, GameManager, refresh-rate, and rollback lifecycle.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { showAddForm = !showAddForm },
                            modifier = Modifier.testTag("toggle_add_game_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (showAddForm) "CLOSE" else "NEW PROFILE", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    if (showAddForm) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = customTitle,
                            onValueChange = { customTitle = it },
                            label = { Text("Game Display Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("game_name_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = customPkg,
                            onValueChange = { customPkg = it },
                            label = { Text("Validated Package Name (e.g. com. studio.game)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("game_package_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Genre Preset:", style = MaterialTheme.typography.labelSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            GameGenrePreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = selectedPreset == preset,
                                    onClick = {
                                        selectedPreset = preset
                                        selectedProfile = preset.recommendedProfile
                                    },
                                    label = { Text(preset.title, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        Text(
                            text = selectedPreset.rationale,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                if (customPkg.isNotBlank()) {
                                    onSaveCustomProfile(
                                        customPkg,
                                        customTitle.ifBlank { customPkg },
                                        selectedPreset,
                                        selectedProfile,
                                        maxRefresh,
                                        true
                                    )
                                    customPkg = ""
                                    customTitle = ""
                                    showAddForm = false
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("save_game_profile_button")
                        ) {
                            Text("SAVE & VALIDATE PROFILE")
                        }
                    }
                }
            }
        }

        items(gameProfiles, key = { it.packageName }) { profile ->
            val isTarget = profile.packageName == selectedTargetPackage
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("game_profile_card_${profile.packageName}"),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    width = if (isTarget) 1.5.dp else 1.dp,
                    color = if (isTarget) CyberEmerald else MaterialTheme.colorScheme.outline
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SportsEsports,
                                contentDescription = null,
                                tint = if (isTarget) CyberEmerald else ElectricCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = profile.displayName,
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = profile.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = ElectricCyan.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "${profile.genrePreset} • ${profile.profileType}",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "CPU: ${profile.cpuPolicy} | GPU: ${profile.gpuPolicy} | Refresh: ${profile.preferredRefreshRateHz.toInt()}Hz",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Thermal Strategy: ${profile.thermalStrategy}",
                        style = MaterialTheme.typography.bodySmall,
                        color = ThermalAmber
                    )
                    Text(
                        text = "Applied Tweaks: ${profile.appliedTweaksSummary}",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberEmerald
                    )
                    if (profile.rejectedTweaksSummary != "None") {
                        Text(
                            text = "Skipped/Unsupported: ${profile.rejectedTweaksSummary}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Restore Status: ${profile.lastRestoreStatus}",
                        style = MaterialTheme.typography.labelSmall
                    )

                    if (profile.lastBenchmarkFps > 0f || profile.baselineBenchmarkFps > 0f) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Measured Benchmark: Baseline ${"%.1f".format(profile.baselineBenchmarkFps)} FPS (P99 ${"%.1f".format(profile.baselineBenchmarkP99Ms)}ms) → Last ${"%.1f".format(profile.lastBenchmarkFps)} FPS (P99 ${"%.1f".format(profile.lastBenchmarkP99Ms)}ms)",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onApplyGameProfile(profile) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("boost_game_${profile.packageName}")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isTarget) "RE-APPLY & VERIFY PROFILE" else "SELECT & ENGAGE PROFILE",
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
