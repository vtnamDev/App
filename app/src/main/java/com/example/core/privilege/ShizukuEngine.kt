package com.example.core.privilege

import android.content.Context
import android.content.pm.PackageManager
import com.example.core.command.CommandValidator
import com.example.data.local.ErrorEventEntity
import com.example.data.local.NexusDao
import com.example.data.local.PrivilegeStateEntity
import com.example.model.LogLevel
import com.example.model.PrivilegeLevel
import com.example.model.ShizukuDiagnostics
import java.io.File
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import rikka.shizuku.Shizuku

data class CommandExecutionResult(
    val command: String,
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean = false,
    val rejectedReason: String? = null
) {
    val isSuccess: Boolean
        get() = exitCode == 0 && !timedOut && rejectedReason == null
}

class ShizukuEngine(
    private val context: Context,
    private val dao: NexusDao,
    private val scope: CoroutineScope
) {
    companion object {
        const val SHIZUKU_PACKAGE_NAME = "moe.shizuku.privileged.api"
        const val REQUEST_CODE_SHIZUKU_PERMISSION = 4091
    }

    private val _diagnostics = MutableStateFlow(ShizukuDiagnostics())
    val diagnostics: StateFlow<ShizukuDiagnostics> = _diagnostics.asStateFlow()

    private var onBinderDeadCallback: (() -> Unit)? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        refreshStatus("Binder Received")
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        scope.launch {
            dao.insertErrorEvent(
                ErrorEventEntity(
                    logLevel = LogLevel.WARN.name,
                    subsystem = "ShizukuEngine",
                    message = "Shizuku binder died unexpectedly.",
                    recoveryAction = "Marked privilege degraded and triggered local state recovery."
                )
            )
        }
        refreshStatus("Binder Dead — Service Disconnected")
        onBinderDeadCallback?.invoke()
    }

    private val permissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode == REQUEST_CODE_SHIZUKU_PERMISSION) {
                val granted = grantResult == PackageManager.PERMISSION_GRANTED
                refreshStatus(if (granted) "Permission Granted" else "Permission Denied by User")
            }
        }

    fun registerListeners(onBinderDead: () -> Unit) {
        this.onBinderDeadCallback = onBinderDead
        try {
            Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(permissionResultListener)
        } catch (_: Throwable) {
            // Safe guard for unit/Robolectric environments where Shizuku binder isn't initialized
        }
        refreshStatus("Initial Probe")
    }

    fun unregisterListeners() {
        try {
            Shizuku.removeBinderReceivedListener(binderReceivedListener)
            Shizuku.removeBinderDeadListener(binderDeadListener)
            Shizuku.removeRequestPermissionResultListener(permissionResultListener)
        } catch (_: Throwable) {
        }
    }

    fun refreshStatus(reason: String = "Manual Refresh"): ShizukuDiagnostics {
        val installed = isShizukuInstalled()
        var binderAlive = false
        var permissionGranted = false
        var uid = -1
        var apiVersion = -1

        try {
            binderAlive = Shizuku.pingBinder()
            if (binderAlive && !Shizuku.isPreV11()) {
                permissionGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
                if (permissionGranted) {
                    uid = Shizuku.getUid()
                    apiVersion = Shizuku.getVersion()
                }
            }
        } catch (_: Throwable) {
            binderAlive = false
        }

        val hasRootBinary = checkStandardRootAvailable()
        val privilegeLevel = when {
            (binderAlive && permissionGranted && uid == 0) || hasRootBinary -> PrivilegeLevel.ROOT
            binderAlive && permissionGranted && uid == 2000 -> PrivilegeLevel.SHIZUKU_SHELL
            binderAlive && permissionGranted -> PrivilegeLevel.ADB_SHELL
            else -> PrivilegeLevel.NORMAL
        }

        val supportedOps = buildList {
            add("ADPF PerformanceHintManager Session (NORMAL)")
            add("PowerManager Thermal Status & Headroom (NORMAL)")
            add("ActivityManager Background Memory Trim (NORMAL)")
            add("Choreographer Hardware VSYNC Telemetry (NORMAL)")
            if (privilegeLevel.rank >= PrivilegeLevel.SHIZUKU_SHELL.rank) {
                add("Android GameManager Mode Override (`cmd game mode`)")
                add("Android GameManager FPS/Scaling Intervention (`cmd game set`)")
                add("PowerManager Fixed Performance Mode (`cmd power`)")
                add("SurfaceFlinger / GfxInfo FrameStats (`dumpsys gfxinfo`)")
            }
            if (privilegeLevel == PrivilegeLevel.ROOT) {
                add("Kernel CPUFreq Policy Min/Max Clamping (`/sys/.../cpufreq`)")
                add("Kernel GPU Devfreq OPP Floor (`/sys/class/devfreq`)")
            }
        }

        val binderText = when {
            !installed -> "Shizuku Not Installed ($reason)"
            !binderAlive -> "Shizuku Service Stopped ($reason)"
            !permissionGranted -> "Shizuku Running — Permission Required ($reason)"
            uid == 0 -> "Connected (ROOT UID 0)"
            uid == 2000 -> "Connected (ADB SHELL UID 2000)"
            else -> "Connected (UID $uid)"
        }

        val updated = _diagnostics.value.copy(
            isInstalled = installed,
            isBinderAlive = binderAlive,
            isPermissionGranted = permissionGranted,
            uid = uid,
            privilegeLevel = privilegeLevel,
            apiVersion = apiVersion,
            binderStatusText = binderText,
            supportedOperations = supportedOps
        )
        _diagnostics.value = updated

        scope.launch {
            dao.insertPrivilegeState(
                PrivilegeStateEntity(
                    shizukuInstalled = installed,
                    binderAlive = binderAlive,
                    permissionGranted = permissionGranted,
                    uid = uid,
                    privilegeTier = privilegeLevel.name,
                    statusDetail = binderText
                )
            )
        }
        return updated
    }

    fun requestShizukuPermission(): Boolean {
        return try {
            if (!Shizuku.pingBinder()) {
                refreshStatus("Cannot Request — Binder Offline")
                false
            } else if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                refreshStatus("Already Granted")
                true
            } else {
                Shizuku.requestPermission(REQUEST_CODE_SHIZUKU_PERMISSION)
                true
            }
        } catch (t: Throwable) {
            _diagnostics.value = _diagnostics.value.copy(
                lastError = "Permission request failed: ${t.message ?: "Binder unavailable"}"
            )
            false
        }
    }

    suspend fun executeAllowlistedCommand(
        command: String,
        timeoutMs: Long = 4000L
    ): CommandExecutionResult = withContext(Dispatchers.IO) {
        if (!CommandValidator.isAllowlistedCommand(command)) {
            val err = "Blocked by CommandValidator allowlist: $command"
            _diagnostics.value = _diagnostics.value.copy(
                lastCommand = command,
                lastExitCode = -1,
                lastError = err
            )
            dao.insertErrorEvent(
                ErrorEventEntity(
                    logLevel = LogLevel.ERROR.name,
                    subsystem = "CommandValidator",
                    message = err,
                    recoveryAction = "Command rejected prior to execution."
                )
            )
            return@withContext CommandExecutionResult(
                command = command,
                exitCode = -1,
                stdout = "",
                stderr = err,
                rejectedReason = err
            )
        }

        val currentDiag = refreshStatus("Pre-Command Check")
        if (currentDiag.privilegeLevel.rank < PrivilegeLevel.SHIZUKU_SHELL.rank) {
            val err = "Insufficient privilege (${currentDiag.privilegeLevel.label}) for command: $command"
            _diagnostics.value = _diagnostics.value.copy(
                lastCommand = command,
                lastExitCode = -2,
                lastError = err
            )
            return@withContext CommandExecutionResult(
                command = command,
                exitCode = -2,
                stdout = "",
                stderr = err,
                rejectedReason = err
            )
        }

        val result = withTimeoutOrNull(timeoutMs) {
            runShizukuOrShellProcess(command, timeoutMs)
        } ?: CommandExecutionResult(
            command = command,
            exitCode = -124,
            stdout = "",
            stderr = "Command timed out after ${timeoutMs}ms",
            timedOut = true
        )

        _diagnostics.value = _diagnostics.value.copy(
            lastCommand = command,
            lastExitCode = result.exitCode,
            lastError = if (result.isSuccess) "None" else result.stderr.ifBlank { "Exit ${result.exitCode}" }
        )
        result
    }

    private fun runShizukuOrShellProcess(command: String, timeoutMs: Long): CommandExecutionResult {
        return try {
            val tokens = command.trim().split(Regex("\\s+")).toTypedArray()
            val process: Process = if (Shizuku.pingBinder() &&
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            ) {
                // Invoke Shizuku remote process via reflection-safe call or fallback
                val newProcessMethod = Shizuku::class.java.getDeclaredMethod(
                    "newProcess",
                    Array<String>::class.java,
                    Array<String>::class.java,
                    String::class.java
                )
                newProcessMethod.isAccessible = true
                newProcessMethod.invoke(null, tokens, null, null) as Process
            } else {
                ProcessBuilder(*tokens).redirectErrorStream(false).start()
            }

            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                return CommandExecutionResult(
                    command = command,
                    exitCode = -124,
                    stdout = "",
                    stderr = "Process timed out",
                    timedOut = true
                )
            }
            val stdout = process.inputStream.bufferedReader().use { it.readText().trim() }
            val stderr = process.errorStream.bufferedReader().use { it.readText().trim() }
            CommandExecutionResult(
                command = command,
                exitCode = process.exitValue(),
                stdout = stdout,
                stderr = stderr
            )
        } catch (t: Throwable) {
            CommandExecutionResult(
                command = command,
                exitCode = -99,
                stdout = "",
                stderr = t.message ?: "Process execution exception"
            )
        }
    }

    private fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo(SHIZUKU_PACKAGE_NAME, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Throwable) {
            false
        }
    }

    private fun checkStandardRootAvailable(): Boolean {
        return try {
            val suPaths = listOf("/system/bin/su", "/system/xbin/su", "/sbin/su", "/debug_ramdisk/su")
            suPaths.any { File(it).canExecute() }
        } catch (_: Throwable) {
            false
        }
    }
}
