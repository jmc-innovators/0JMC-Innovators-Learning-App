package lk.jmcinnovators.learning.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val JmcDarkScheme = darkColorScheme(
    primary = JmcPalette.Blue,
    onPrimary = JmcPalette.DarkText0,
    secondary = JmcPalette.Violet,
    tertiary = JmcPalette.Gold,
    background = JmcPalette.DarkBg0,
    onBackground = JmcPalette.DarkText0,
    surface = JmcPalette.DarkBg1,
    onSurface = JmcPalette.DarkText0,
    surfaceVariant = JmcPalette.DarkBg2,
    onSurfaceVariant = JmcPalette.DarkText1,
    error = JmcPalette.Pink
)

private val JmcLightScheme = lightColorScheme(
    primary = JmcPalette.Indigo,
    onPrimary = Color(0xFFFFFFFF),
    secondary = JmcPalette.Violet,
    tertiary = JmcPalette.Gold,
    background = JmcPalette.LightBg0,
    onBackground = JmcPalette.LightText0,
    surface = JmcPalette.LightBg1,
    onSurface = JmcPalette.LightText0,
    surfaceVariant = JmcPalette.LightBg2,
    onSurfaceVariant = JmcPalette.LightText1,
    error = JmcPalette.Pink
)

/** themePreference is "light" | "dark" | "system", the same three values the website stores. */
@Composable
fun JmcTheme(
    themePreference: String = "system",
    content: @Composable () -> Unit
) {
    val useDark = when (themePreference) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (useDark) JmcDarkScheme else JmcLightScheme,
        typography = JmcTypography,
        content = content
    )
}
