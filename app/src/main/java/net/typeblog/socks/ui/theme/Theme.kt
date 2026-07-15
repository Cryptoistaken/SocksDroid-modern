package net.typeblog.socks.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.preference.PreferenceManager
import net.typeblog.socks.util.Constants.PREF_DYNAMIC_COLORS
import net.typeblog.socks.util.Constants.PREF_THEME_MODE

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    surfaceTint = LightSurfaceTint,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    onSurface = LightOnSurface,
    onSurfaceVariant = LightOnSurfaceVariant,
    tertiary = LightTertiary,
    error = LightError,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainerHighest = LightSurfaceContainerHighest
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    surfaceTint = DarkSurfaceTint,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    onSurface = DarkOnSurface,
    onSurfaceVariant = DarkOnSurfaceVariant,
    tertiary = DarkTertiary,
    error = DarkError,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainerHighest = DarkSurfaceContainerHighest
)

@Composable
fun KiloProxyTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    val themeMode = prefs.getString(PREF_THEME_MODE, "light") ?: "light"
    val dynamicColorsEnabled = prefs.getBoolean(PREF_DYNAMIC_COLORS, true)

    val useDarkTheme = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColorsEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (useDarkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    // Update status bar color to match theme
    val statusBarColor = colorScheme.surface.toArgb()
    val window = (context as? android.app.Activity)?.window
    if (window != null) {
        SideEffect {
            window.statusBarColor = statusBarColor
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !useDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KiloProxyTypography,
        content = content
    )
}
