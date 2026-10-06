package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.NexusBoostViewModel
import com.example.ui.screens.AiAdvisorScreen
import com.example.ui.screens.AuditLogsScreen
import com.example.ui.screens.BenchmarksScreen
import com.example.ui.screens.CapabilitiesScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.GamesScreen
import com.example.ui.screens.TuningScreen
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.MyApplicationTheme

enum class NexusDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Dashboard", Icons.Default.Dashboard),
    CAPABILITIES("capabilities", "Capabilities", Icons.Default.DeveloperBoard),
    TUNING("tuning", "Tuning", Icons.Default.Tune),
    GAMES("games", "Games", Icons.Default.SportsEsports),
    BENCHMARKS("benchmarks", "Benchmarks", Icons.Default.Speed),
    AI_ADVISOR("ai_advisor", "AI Advisor", Icons.Default.AutoAwesome),
    AUDIT_SAFETY("audit_safety", "Audit", Icons.Default.Security)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: NexusBoostViewModel = viewModel()
            val darkTheme by vm.darkThemeEnabled.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = darkTheme) {
                NexusBoostApp(viewModel = vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NexusBoostApp(viewModel: NexusBoostViewModel) {
    var currentDestination by rememberSaveable { mutableStateOf(NexusDestination.DASHBOARD) }

    val capabilityReport by viewModel.capabilityReport.collectAsStateWithLifecycle()
    val shizukuDiagnostics by viewModel.shizukuDiagnostics.collectAsStateWithLifecycle()
    val boostState by viewModel.boostState.collectAsStateWithLifecycle()
    val selectedProfile by viewModel.selectedProfile.collectAsStateWithLifecycle()
    val selectedTargetPackage by viewModel.selectedTargetPackage.collectAsStateWithLifecycle()
    val liveTelemetry by viewModel.liveTelemetry.collectAsStateWithLifecycle()
    val frameHistoryMs by viewModel.frameHistoryMs.collectAsStateWithLifecycle()
    val thermalHistoryCelsius by viewModel.thermalHistoryCelsius.collectAsStateWithLifecycle()
    val statusBanner by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val tuningCatalog by viewModel.tuningCatalog.collectAsStateWithLifecycle()
    val gameProfiles by viewModel.gameProfiles.collectAsStateWithLifecycle()
    val benchmarkSessions by viewModel.benchmarkSessions.collectAsStateWithLifecycle()
    val isBenchmarkRunning by viewModel.isBenchmarkRunning.collectAsStateWithLifecycle()
    val benchmarkProgressSeconds by viewModel.benchmarkProgressSeconds.collectAsStateWithLifecycle()
    val selectedAiMode by viewModel.selectedAiMode.collectAsStateWithLifecycle()
    val chatHistory by viewModel.chatHistory.collectAsStateWithLifecycle()
    val isAiLoading by viewModel.isAiLoading.collectAsStateWithLifecycle()
    val rollbackSnapshots by viewModel.rollbackSnapshots.collectAsStateWithLifecycle()
    val tuningActions by viewModel.tuningActions.collectAsStateWithLifecycle()
    val errorEvents by viewModel.errorEvents.collectAsStateWithLifecycle()
    val darkTheme by viewModel.darkThemeEnabled.collectAsStateWithLifecycle()

    val exportedTitle by viewModel.exportedReportTitle.collectAsStateWithLifecycle()
    val exportedContent by viewModel.exportedReportContent.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (currentDestination != NexusDestination.DASHBOARD) {
        BackHandler {
            currentDestination = NexusDestination.DASHBOARD
        }
    }

    // Export Preview & Copy Modal
    if (exportedTitle != null && exportedContent != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissExportDialog() },
            title = { Text(text = exportedTitle.orEmpty(), style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = exportedContent.orEmpty(),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cm?.setPrimaryClip(ClipData.newPlainText(exportedTitle, exportedContent))
                        viewModel.dismissExportDialog()
                    },
                    modifier = Modifier.testTag("copy_export_button")
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("COPY TO CLIPBOARD")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissExportDialog() },
                    modifier = Modifier.testTag("close_export_button")
                ) {
                    Text("CLOSE")
                }
            }
        )
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 720.dp
        val bottomNavDestinations = listOf(
            NexusDestination.DASHBOARD,
            NexusDestination.CAPABILITIES,
            NexusDestination.TUNING,
            NexusDestination.GAMES,
            NexusDestination.BENCHMARKS,
            NexusDestination.AI_ADVISOR
        )

        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "NEXUS BOOST",
                                style = MaterialTheme.typography.titleLarge,
                                color = ElectricCyan
                            )
                            Text(
                                text = "PERFORMANCE • SHIZUKU • ADPF • TELEMETRY",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { currentDestination = NexusDestination.AUDIT_SAFETY },
                            modifier = Modifier.testTag("nav_audit_top_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Audit & Rollback Safety Ledger",
                                tint = if (currentDestination == NexusDestination.AUDIT_SAFETY) {
                                    ElectricCyan
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        }
                        IconButton(
                            onClick = { viewModel.toggleDarkTheme() },
                            modifier = Modifier.testTag("toggle_theme_button")
                        ) {
                            Icon(
                                imageVector = if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Dark/Light Theme"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                if (!isExpandedScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.testTag("bottom_navigation_bar")
                    ) {
                        bottomNavDestinations.forEach { dest ->
                            NavigationBarItem(
                                selected = currentDestination == dest,
                                onClick = { currentDestination = dest },
                                icon = {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = {
                                    Text(
                                        text = dest.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1
                                    )
                                },
                                modifier = Modifier.testTag("nav_tab_${dest.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    if (isExpandedScreen) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxHeight()
                                .testTag("side_navigation_rail")
                        ) {
                            NexusDestination.entries.forEach { dest ->
                                NavigationRailItem(
                                    selected = currentDestination == dest,
                                    onClick = { currentDestination = dest },
                                    icon = { Icon(dest.icon, contentDescription = dest.label) },
                                    label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                                    modifier = Modifier.testTag("rail_tab_${dest.route}")
                                )
                            }
                        }
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        when (currentDestination) {
                            NexusDestination.DASHBOARD -> DashboardScreen(
                                capabilityReport = capabilityReport,
                                shizukuDiagnostics = shizukuDiagnostics,
                                boostState = boostState,
                                selectedProfile = selectedProfile,
                                targetPackage = selectedTargetPackage,
                                liveTelemetry = liveTelemetry,
                                frameHistoryMs = frameHistoryMs,
                                statusBanner = statusBanner,
                                onSelectProfile = { viewModel.selectPerformanceProfile(it) },
                                onEngageBoost = { viewModel.engageBoost() },
                                onRestoreBaseline = { viewModel.stopAndRestoreBaseline("Dashboard Restore Button") },
                                onRefreshCapabilities = { viewModel.refreshCapabilitiesAndCatalog() },
                                onRequestShizukuPermission = { viewModel.requestShizukuPermission() }
                            )
                            NexusDestination.CAPABILITIES -> CapabilitiesScreen(
                                report = capabilityReport,
                                onRescan = { viewModel.refreshCapabilitiesAndCatalog() },
                                onExportJson = { viewModel.showCapabilityExport(true) },
                                onExportMarkdown = { viewModel.showCapabilityExport(false) }
                            )
                            NexusDestination.TUNING -> TuningScreen(
                                tuningCatalog = tuningCatalog,
                                shizukuDiagnostics = shizukuDiagnostics,
                                onRequestShizukuPermission = { viewModel.requestShizukuPermission() },
                                onRefreshShizuku = { viewModel.refreshCapabilitiesAndCatalog() },
                                onEngageBoost = { viewModel.engageBoost() },
                                onRestoreBaseline = { viewModel.stopAndRestoreBaseline("Tuning Screen Rollback") }
                            )
                            NexusDestination.GAMES -> GamesScreen(
                                gameProfiles = gameProfiles,
                                selectedTargetPackage = selectedTargetPackage,
                                supportedRefreshRates = capabilityReport?.supportedRefreshRatesHz ?: listOf(60f),
                                onApplyGameProfile = { viewModel.applyGameProfileAndBoost(it) },
                                onSaveCustomProfile = { pkg, name, preset, prof, hz, trim ->
                                    viewModel.createOrUpdateGameProfile(pkg, name, preset, prof, hz, trim)
                                }
                            )
                            NexusDestination.BENCHMARKS -> BenchmarksScreen(
                                sessions = benchmarkSessions,
                                frameHistoryMs = frameHistoryMs,
                                thermalHistoryCelsius = thermalHistoryCelsius,
                                isBenchmarkRunning = isBenchmarkRunning,
                                benchmarkProgressSeconds = benchmarkProgressSeconds,
                                onRunBenchmark = { viewModel.runLiveBenchmark(it) },
                                onExportFormat = { viewModel.showBenchmarkExport(it) }
                            )
                            NexusDestination.AI_ADVISOR -> AiAdvisorScreen(
                                selectedMode = selectedAiMode,
                                chatHistory = chatHistory,
                                isLoading = isAiLoading,
                                onSelectMode = { viewModel.selectAiMode(it) },
                                onSendMessage = { viewModel.sendAiAdvisorQuery(it) }
                            )
                            NexusDestination.AUDIT_SAFETY -> AuditLogsScreen(
                                rollbackSnapshots = rollbackSnapshots,
                                tuningActions = tuningActions,
                                errorEvents = errorEvents,
                                onEmergencyRestore = { viewModel.stopAndRestoreBaseline("Audit Emergency Restore") },
                                onExportAudit = { viewModel.showDiagnosticsAuditExport() }
                            )
                        }
                    }
                }
            }
        }
    }
}
