package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val NexusDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF00262C),
    primaryContainer = Color(0xFF004E5B),
    onPrimaryContainer = Color(0xFFB2F5FF),
    secondary = CyberEmerald,
    onSecondary = Color(0xFF002912),
    secondaryContainer = Color(0xFF005227),
    onSecondaryContainer = Color(0xFFB9F6CA),
    tertiary = ThermalAmber,
    onTertiary = Color(0xFF2B1B00),
    tertiaryContainer = Color(0xFF543800),
    onTertiaryContainer = Color(0xFFFFE082),
    error = AlertCrimson,
    onError = Color.White,
    background = ObsidianBackground,
    onBackground = TitaniumSilver,
    surface = ObsidianSurface,
    onSurface = TitaniumSilver,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = SlateMuted,
    outline = ObsidianCardBorder
)

private val NexusLightColorScheme = lightColorScheme(
    primary = LabCyanPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF4FC),
    onPrimaryContainer = Color(0xFF00262C),
    secondary = LabEmeraldSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD1FAE5),
    onSecondaryContainer = Color(0xFF002912),
    tertiary = LabAmberTertiary,
    onTertiary = Color.White,
    error = AlertCrimson,
    onError = Color.White,
    background = LabLightBackground,
    onBackground = Color(0xFF0F172A),
    surface = LabLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = LabLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> NexusDarkColorScheme
        else -> NexusLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
