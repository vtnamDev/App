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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import com.example.model.CapabilityItem
import com.example.model.CapabilityStatus
import com.example.model.DeviceCapabilityReport
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ThermalAmber

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CapabilitiesScreen(
    report: DeviceCapabilityReport?,
    onRescan: () -> Unit,
    onExportJson: () -> Unit,
    onExportMarkdown: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    val filters = listOf("ALL", "SUPPORTED", "REQUIRES_SHIZUKU", "REQUIRES_ROOT", "UNSUPPORTED", "OEM")

    val filteredCapabilities = remember(report, selectedFilter) {
        val all = report?.capabilities.orEmpty()
        when (selectedFilter) {
            "SUPPORTED" -> all.filter { it.status == CapabilityStatus.SUPPORTED || it.status == CapabilityStatus.PARTIAL }
            "REQUIRES_SHIZUKU" -> all.filter { it.status == CapabilityStatus.REQUIRES_SHIZUKU }
            "REQUIRES_ROOT" -> all.filter { it.status == CapabilityStatus.REQUIRES_ROOT }
            "UNSUPPORTED" -> all.filter { it.status == CapabilityStatus.UNSUPPORTED }
            "OEM" -> all.filter { it.isOemSpecific }
            else -> all
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("capabilities_screen"),
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
                        text = "DYNAMIC CAPABILITY DISCOVERY ENGINE",
                        style = MaterialTheme.typography.titleMedium,
                        color = ElectricCyan
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = report?.let {
                            "Kernel: ${it.kernelVersion}\nOEM Adapter: ${it.oemAdapterName}\nABI: ${it.abi} • Thermal Zones: ${it.thermalZones.size} • CPU Policies: ${it.cpuPolicies.size}"
                        } ?: "Scanning...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onRescan,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("rescan_capabilities_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("RESCAN", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = onExportJson,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_cap_json_button")
                        ) {
                            Icon(Icons.Default.Code, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("JSON", style = MaterialTheme.typography.labelMedium)
                        }
                        OutlinedButton(
                            onClick = onExportMarkdown,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_cap_md_button")
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("REPORT", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }

        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filters.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("cap_filter_$filter")
                    )
                }
            }
        }

        items(filteredCapabilities, key = { it.id }) { item ->
            CapabilityEvidenceCard(item = item)
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun CapabilityEvidenceCard(item: CapabilityItem) {
    val badgeColor = when (item.status) {
        CapabilityStatus.SUPPORTED -> CyberEmerald
        CapabilityStatus.PARTIAL -> ElectricCyan
        CapabilityStatus.REQUIRES_SHIZUKU -> ThermalAmber
        CapabilityStatus.REQUIRES_ROOT -> ThermalAmber
        CapabilityStatus.UNSUPPORTED, CapabilityStatus.UNKNOWN -> AlertCrimson
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("capability_card_${item.id}"),
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
                        text = item.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, badgeColor)
                ) {
                    Text(
                        text = item.status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            EvidenceRow("Path / API", item.pathOrApi)
            EvidenceRow("Read Result", item.currentReadValue)
            EvidenceRow("Writable", if (item.writable) "YES (Verified)" else "NO (Read-Only / Gated)")
            EvidenceRow("Valid Range", item.validRange)
            EvidenceRow("Min Privilege", "${item.requiredPrivilege.label} • Source: ${item.evidenceSource}")
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Fallback Strategy: ${item.fallbackExplanation}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EvidenceRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(100.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
