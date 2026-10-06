package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.OptimizationState
import com.example.model.ShizukuDiagnostics
import com.example.model.TuningDefinition
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ThermalAmber

@Composable
fun TuningScreen(
    tuningCatalog: List<TuningDefinition>,
    shizukuDiagnostics: ShizukuDiagnostics,
    onRequestShizukuPermission: () -> Unit,
    onRefreshShizuku: () -> Unit,
    onEngageBoost: () -> Unit,
    onRestoreBaseline: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("tuning_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Shizuku Privileged Diagnostics Card
        item {
            ShizukuDiagnosticsCard(
                diagnostics = shizukuDiagnostics,
                onRequestPermission = onRequestShizukuPermission,
                onRefresh = onRefreshShizuku
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "VERIFIED TUNING CATALOG (${tuningCatalog.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = ElectricCyan
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onRestoreBaseline,
                        modifier = Modifier.testTag("tuning_restore_button")
                    ) {
                        Text("ROLLBACK ALL", style = MaterialTheme.typography.labelSmall)
                    }
                    Button(
                        onClick = onEngageBoost,
                        modifier = Modifier.testTag("tuning_apply_button")
                    ) {
                        Text("APPLY SUPPORTED", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        items(tuningCatalog, key = { it.id }) { tuning ->
            TuningCatalogItemCard(tuning = tuning)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ShizukuDiagnosticsCard(
    diagnostics: ShizukuDiagnostics,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shizuku_diagnostics_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = ElectricCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "SHIZUKU PRIVILEGED ENGINE DIAGNOSTICS",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = diagnostics.binderStatusText,
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                    }
                }
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.testTag("refresh_shizuku_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh Shizuku")
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            DiagRow("Installed (`moe.shizuku.privileged.api`)", if (diagnostics.isInstalled) "YES" else "NO")
            DiagRow("Binder Alive (`pingBinder`)", if (diagnostics.isBinderAlive) "ALIVE" else "OFFLINE")
            DiagRow("Runtime Permission", if (diagnostics.isPermissionGranted) "GRANTED" else "NOT GRANTED")
            DiagRow("Execution Identity", if (diagnostics.uid >= 0) "UID ${diagnostics.uid} (${diagnostics.privilegeLevel.name})" else diagnostics.privilegeLevel.label)
            DiagRow("Shizuku API Version", if (diagnostics.apiVersion > 0) "v${diagnostics.apiVersion}" else "N/A")
            DiagRow("Last Command", diagnostics.lastCommand)
            DiagRow("Last Error / Status", diagnostics.lastError)

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Supported Operations on Current Tier:",
                style = MaterialTheme.typography.labelSmall,
                color = CyberEmerald
            )
            diagnostics.supportedOperations.forEach { op ->
                Text(
                    text = "• $op",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (diagnostics.isBinderAlive && !diagnostics.isPermissionGranted) {
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onRequestPermission,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("request_shizuku_permission_button")
                ) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("REQUEST SHIZUKU BINDER PERMISSION")
                }
            }
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun TuningCatalogItemCard(tuning: TuningDefinition) {
    val stateColor = when (tuning.executionState) {
        OptimizationState.ACTIVE, OptimizationState.APPLIED -> CyberEmerald
        OptimizationState.READY, OptimizationState.SUPPORTED, OptimizationState.RESTORED -> ElectricCyan
        OptimizationState.PERMISSION_REQUIRED, OptimizationState.THERMAL_LIMIT -> ThermalAmber
        else -> AlertCrimson
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("tuning_card_${tuning.id}"),
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${tuning.layer} • Risk: ${tuning.risk}",
                        style = MaterialTheme.typography.labelSmall,
                        color = ThermalAmber
                    )
                    Text(
                        text = tuning.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = stateColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, stateColor)
                ) {
                    Text(
                        text = tuning.executionState.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = stateColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = tuning.mechanism,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            DiagRow("Current Value", tuning.currentValue)
            DiagRow("Target Value", tuning.targetValue)
            DiagRow("Read / Write Path", tuning.writePathOrApi)
            DiagRow("Required Privilege", tuning.requiredPrivilege.label)
            DiagRow("Evidence Provenance", tuning.evidenceIds.joinToString(", "))
            DiagRow("Verification Status", tuning.lastVerificationDetail)
            DiagRow("Rollback Mechanism", tuning.rollbackMethod)
        }
    }
}
