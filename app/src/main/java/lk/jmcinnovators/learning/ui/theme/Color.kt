package lk.jmcinnovators.learning.ui.theme

import androidx.compose.ui.graphics.Color

// Ported 1:1 from legacy-web/css/app-shell.css custom properties so the native app and the
// website read as the same product.
object JmcPalette {
    // Dark theme (the site's default)
    val DarkBg0 = Color(0xFF05070F)
    val DarkBg1 = Color(0xFF080B16)
    val DarkBg2 = Color(0xFF0C1120)
    val DarkText0 = Color(0xFFF3F5FB)
    val DarkText1 = Color(0xFFC3CADD)
    val DarkText2 = Color(0xFF8891AC)

    // Light theme
    val LightBg0 = Color(0xFFF6F7FB)
    val LightBg1 = Color(0xFFFFFFFF)
    val LightBg2 = Color(0xFFEEF1F8)
    val LightText0 = Color(0xFF0F1728)
    val LightText1 = Color(0xFF3B4459)
    val LightText2 = Color(0xFF6B7488)

    // Shared accents (same in both themes)
    val Blue = Color(0xFF3B82F6)
    val BlueVariant = Color(0xFF6366F1)
    val Indigo = Color(0xFF4F46E5)
    val Violet = Color(0xFF8B5CF6)
    val Gold = Color(0xFFF2A71B)
    val GoldVariant = Color(0xFFFFCF6B)
    val Green = Color(0xFF22C55E)
    val Pink = Color(0xFFEC4899)
    val Teal = Color(0xFF14B8A6)
}
