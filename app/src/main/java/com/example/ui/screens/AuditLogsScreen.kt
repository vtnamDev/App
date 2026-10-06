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
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.local.ErrorEventEntity
import com.example.data.local.RollbackSnapshotEntity
import com.example.data.local.TuningActionEntity
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ThermalAmber

@Composable
fun AuditLogsScreen(
    rollbackSnapshots: List<RollbackSnapshotEntity>,
    tuningActions: List<TuningActionEntity>,
    errorEvents: List<ErrorEventEntity>,
    onEmergencyRestore: () -> Unit,
    onExportAudit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSnapshotsCount = rollbackSnapshots.count { !it.isRestored }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("audit_logs_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Safety & Transactional Rollback Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = ElectricCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "SAFETY BOUNDARY & ROLLBACK LEDGER",
                                style = MaterialTheme.typography.titleMedium,
                                color = ElectricCyan
                            )
                            Text(
                                text = "Pending Un-restored Snapshots: $activeSnapshotsCount • Total Logged Operations: ${tuningActions.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Safety Guarantees: Zero voltage writes • Zero thermal daemon disabling • Validated OPPs only • Reverse transactional restore on Stop, Crash, or Binder Death.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onEmergencyRestore,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ThermalAmber,
                                contentColor = Color.Black
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("emergency_restore_button")
                        ) {
                            Icon(Icons.Default.Restore, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("RESTORE BASELINE NOW", style = MaterialTheme.typography.labelSmall)
                        }

                        OutlinedButton(
                            onClick = onExportAudit,
                            modifier = Modifier.testTag("export_audit_button")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("EXPORT", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        // Rollback Snapshots Section
        if (rollbackSnapshots.isNotEmpty()) {
            item {
                Text(
                    text = "TRANSACTIONAL ROLLBACK SNAPSHOTS (${rollbackSnapshots.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = ElectricCyan
                )
            }
            items(rollbackSnapshots.take(15), key = { "snap_${it.id}" }) { snap ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "#${snap.id} ${snap.featureId} (${snap.mechanismType})",
                                style = MaterialTheme.typography.labelMedium
                            )
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = (if (snap.isRestored) CyberEmerald else ThermalAmber).copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = if (snap.isRestored) "RESTORED" else "ACTIVE SNAPSHOT",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (snap.isRestored) CyberEmerald else ThermalAmber,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Target: ${snap.pathOrTarget} | Baseline: `${snap.originalValue}` → Applied: `${snap.appliedValue}`",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Verification: ${snap.restoreVerificationNote}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ElectricCyan
                        )
                    }
                }
            }
        }

        // Audit Trail Section
        item {
            Text(
                text = "PRIVILEGED & API OPERATION AUDIT LOG (${tuningActions.size})",
                style = MaterialTheme.typography.labelLarge,
                color = CyberEmerald
            )
        }
        if (tuningActions.isEmpty()) {
            item {
                Text(
                    text = "No privileged operations executed yet in this session.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(tuningActions.take(25), key = { "act_${it.id}" }) { action ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "[${action.operation}] ${action.featureId}",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricCyan
                            )
                            Text(
                                text = action.resultState,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (action.verificationPassed) CyberEmerald else AlertCrimson
                            )
                        }
                        Text(
                            text = "Command/API: ${action.commandOrApi}",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Prev: ${action.previousValue} → Target: ${action.targetValue} | Read-back: ${action.readBackValue}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Error & Safety Events
        if (errorEvents.isNotEmpty()) {
            item {
                Text(
                    text = "SAFETY & RECOVERY EVENTS (${errorEvents.size})",
                    style = MaterialTheme.typography.labelLarge,
                    color = ThermalAmber
                )
            }
            items(errorEvents.take(15), key = { "err_${it.id}" }) { err ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, AlertCrimson.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "[${err.logLevel}] ${err.subsystem}",
                            style = MaterialTheme.typography.labelMedium,
                            color = AlertCrimson
                        )
                        Text(
                            text = err.message,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "Recovery: ${err.recoveryAction}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ThermalAmber
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
