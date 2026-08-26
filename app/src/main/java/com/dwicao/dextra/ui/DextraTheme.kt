package com.dwicao.dextra.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.dwicao.dextra.data.ThemeMode

private val LightColors = lightColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFF4E4BB5),
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = androidx.compose.ui.graphics.Color(0xFFE4E1FF),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF17145F),
    secondary = androidx.compose.ui.graphics.Color(0xFF5F5D72),
    surface = androidx.compose.ui.graphics.Color(0xFFFAF8FF),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFFEFEFF7),
)

private val DarkColors = darkColorScheme(
    primary = androidx.compose.ui.graphics.Color(0xFFC3C0FF),
    onPrimary = androidx.compose.ui.graphics.Color(0xFF29256F),
    primaryContainer = androidx.compose.ui.graphics.Color(0xFF39358A),
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFE4E1FF),
    secondary = androidx.compose.ui.graphics.Color(0xFFC6C4DD),
    surface = androidx.compose.ui.graphics.Color(0xFF12121A),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFF20202A),
)

@Composable
fun DextraTheme(
    themeMode: ThemeMode,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
