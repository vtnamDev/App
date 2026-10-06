package com.example.core.command

/**
 * Enforces strict input validation, package name regex, frequency OPP bounds,
 * and command allowlists before any privileged or sysfs operation can execute.
 */
object CommandValidator {
    private val PACKAGE_NAME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z0-9_]+)+$")
    private val FORBIDDEN_METACHARACTERS = charArrayOf(';', '|', '&', '$', '`', '\n', '\r', '>', '<', '(', ')')

    private val ALLOWED_COMMAND_PREFIXES = listOf(
        "cmd game mode ",
        "cmd game set ",
        "cmd game reset ",
        "cmd game status ",
        "cmd power set-fixed-performance-mode-enabled ",
        "cmd activity send-trim-memory ",
        "dumpsys game_manager",
        "dumpsys gfxinfo ",
        "dumpsys SurfaceFlinger --latency"
    )

    private val BANNED_DANGEROUS_KEYWORDS = listOf(
        "thermal-engine",
        "thermald",
        "msm_thermal",
        "trip_point",
        "regulator",
        "voltage",
        "drop_caches",
        "reboot",
        "recovery",
        "fastboot",
        "dd ",
        "rm ",
        "chmod ",
        "chown ",
        "setenforce",
        "mount"
    )

    fun isValidPackageName(packageName: String): Boolean {
        if (packageName.isBlank() || packageName.length > 128) return false
        if (packageName.any { it in FORBIDDEN_METACHARACTERS }) return false
        return PACKAGE_NAME_REGEX.matches(packageName)
    }

    fun isAllowlistedCommand(command: String): Boolean {
        val trimmed = command.trim()
        if (trimmed.isEmpty() || trimmed.length > 256) return false
        if (trimmed.any { it in FORBIDDEN_METACHARACTERS }) return false
        val lower = trimmed.lowercase()
        if (BANNED_DANGEROUS_KEYWORDS.any { lower.contains(it) }) return false
        return ALLOWED_COMMAND_PREFIXES.any { trimmed.startsWith(it) }
    }

    fun isSafeSysfsWritePath(path: String): Boolean {
        val normalized = path.trim()
        if (normalized.contains("..") || normalized.any { it in FORBIDDEN_METACHARACTERS }) {
            return false
        }
        val lower = normalized.lowercase()
        if (BANNED_DANGEROUS_KEYWORDS.any { lower.contains(it) }) {
            return false
        }
        val isCpuFreq = Regex("^/sys/devices/system/cpu/cpufreq/policy[0-9]+/scaling_(min_freq|max_freq|governor)$")
            .matches(normalized)
        val isGpuDevfreq = Regex("^/sys/class/(devfreq|kgsl)/[a-zA-Z0-9_.,:-]+(/devfreq)?/(min_freq|max_freq|governor)$")
            .matches(normalized)
        return isCpuFreq || isGpuDevfreq
    }

    fun isFrequencyInValidRange(targetFreq: Long, availableFrequencies: List<Long>): Boolean {
        if (targetFreq <= 0L || availableFrequencies.isEmpty()) return false
        return availableFrequencies.contains(targetFreq)
    }

    fun isGovernorInValidList(targetGovernor: String, availableGovernors: List<String>): Boolean {
        if (targetGovernor.isBlank() || availableGovernors.isEmpty()) return false
        return availableGovernors.contains(targetGovernor.trim())
    }

    fun selectTargetFrequency(
        availableFrequencies: List<Long>,
        percentile: Float
    ): Long? {
        val sorted = availableFrequencies.filter { it > 0L }.sorted()
        if (sorted.isEmpty()) return null
        val clamped = percentile.coerceIn(0f, 1f)
        val index = ((sorted.size - 1) * clamped).toInt().coerceIn(0, sorted.lastIndex)
        return sorted[index]
    }
}
