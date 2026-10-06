package com.example

import com.example.core.command.CommandValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun `package name validation accepts valid Android packages and blocks shell injection`() {
        assertTrue(CommandValidator.isValidPackageName("com.example"))
        assertTrue(CommandValidator.isValidPackageName("com.tencent.ig"))
        assertTrue(CommandValidator.isValidPackageName("com.miHoYo.GenshinImpact"))

        // Red-team injection attempts must be rejected
        assertFalse(CommandValidator.isValidPackageName("com.game; rm -rf /"))
        assertFalse(CommandValidator.isValidPackageName("com.game && stop thermal-engine"))
        assertFalse(CommandValidator.isValidPackageName("com.game | cat /etc/passwd"))
        assertFalse(CommandValidator.isValidPackageName("invalid_single_segment"))
        assertFalse(CommandValidator.isValidPackageName(""))
    }

    @Test
    fun `allowlisted command validator permits verified game and power commands and blocks dangerous commands`() {
        assertTrue(CommandValidator.isAllowlistedCommand("cmd game mode performance com.example"))
        assertTrue(CommandValidator.isAllowlistedCommand("cmd game mode standard com.example"))
        assertTrue(CommandValidator.isAllowlistedCommand("cmd power set-fixed-performance-mode-enabled true"))
        assertTrue(CommandValidator.isAllowlistedCommand("dumpsys gfxinfo com.example framestats"))

        // Dangerous or unverified commands must be rejected
        assertFalse(CommandValidator.isAllowlistedCommand("stop thermal-engine"))
        assertFalse(CommandValidator.isAllowlistedCommand("echo 0 > /sys/class/thermal/thermal_zone0/mode"))
        assertFalse(CommandValidator.isAllowlistedCommand("cmd game mode performance com.example; reboot"))
        assertFalse(CommandValidator.isAllowlistedCommand("setprop debug.performance.tuning 1"))
    }

    @Test
    fun `sysfs write path validator restricts writes to cpufreq and devfreq nodes`() {
        assertTrue(CommandValidator.isSafeSysfsWritePath("/sys/devices/system/cpu/cpufreq/policy0/scaling_min_freq"))
        assertTrue(CommandValidator.isSafeSysfsWritePath("/sys/devices/system/cpu/cpufreq/policy4/scaling_governor"))
        assertTrue(CommandValidator.isSafeSysfsWritePath("/sys/class/devfreq/1c00000.qcom,kgsl-3d0/min_freq"))

        // Forbidden paths
        assertFalse(CommandValidator.isSafeSysfsWritePath("/sys/class/thermal/thermal_zone0/trip_point_0_temp"))
        assertFalse(CommandValidator.isSafeSysfsWritePath("/sys/class/regulator/regulator.0/microvolts"))
        assertFalse(CommandValidator.isSafeSysfsWritePath("/sys/devices/system/cpu/cpufreq/policy0/../../thermal"))
    }

    @Test
    fun `frequency selection picks valid hardware OPP step within range`() {
        val opps = listOf(300_000L, 710_400L, 1_171_200L, 1_785_600L, 2_419_200L)
        assertTrue(CommandValidator.isFrequencyInValidRange(1_785_600L, opps))
        assertFalse(CommandValidator.isFrequencyInValidRange(9_999_999L, opps))

        val maxFreq = CommandValidator.selectTargetFrequency(opps, 1.0f)
        assertEquals(2_419_200L, maxFreq)

        val p75Freq = CommandValidator.selectTargetFrequency(opps, 0.75f)
        assertEquals(1_785_600L, p75Freq)
    }
}
