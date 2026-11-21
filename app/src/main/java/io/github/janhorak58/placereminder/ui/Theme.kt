package io.github.janhorak58.placereminder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.janhorak58.placereminder.R

/** Brand palette, shared with the launcher icon and the landing page. */
object Brand {
    val Ink = Color(0xFF0E2233)
    val InkDeep = Color(0xFF08141F)
    val InkRaised = Color(0xFF16344B)
    val Amber = Color(0xFFFFB800)
    val AmberDeep = Color(0xFFFF9500)
    val Ring = Color(0xFF5FB3D9)
    val Paper = Color(0xFFF7FAFC)
    val Text = Color(0xFF14283A)
    val Muted = Color(0xFF4D6475)
}

private val Light = lightColorScheme(
    primary = Brand.Ink, onPrimary = Brand.Paper,
    primaryContainer = Color(0xFFFFE7A3), onPrimaryContainer = Brand.Ink,
    secondary = Color(0xFF1B6A92), onSecondary = Color.White,
    secondaryContainer = Color(0xFFD9EEF8), onSecondaryContainer = Brand.Ink,
    tertiary = Brand.Amber, onTertiary = Brand.Ink,
    tertiaryContainer = Color(0xFFFFEFC2), onTertiaryContainer = Brand.Text,
    background = Brand.Paper, onBackground = Brand.Text,
    surface = Brand.Paper, onSurface = Brand.Text,
    surfaceVariant = Color(0xFFE6EEF3), onSurfaceVariant = Brand.Muted,
    surfaceTint = Brand.Ink,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color.White,
    surfaceContainer = Color(0xFFEEF4F8),
    surfaceContainerHigh = Color(0xFFE6EEF3),
    surfaceContainerHighest = Color(0xFFDCE7EE),
    outline = Color(0xFF6A8090), outlineVariant = Color(0xFFD2DFE8),
    error = Color(0xFFB3261E), onError = Color.White,
    errorContainer = Color(0xFFFCE0DD), onErrorContainer = Color(0xFF5C0F0A),
    inverseSurface = Brand.Ink, inverseOnSurface = Brand.Paper, inversePrimary = Brand.Amber,
)

private val Dark = darkColorScheme(
    primary = Brand.Amber, onPrimary = Brand.Ink,
    primaryContainer = Color(0xFF4A3900), onPrimaryContainer = Color(0xFFFFE08A),
    secondary = Brand.Ring, onSecondary = Brand.Ink,
    secondaryContainer = Color(0xFF1A4660), onSecondaryContainer = Color(0xFFD6EEF9),
    tertiary = Brand.Amber, onTertiary = Brand.Ink,
    tertiaryContainer = Color(0xFF3F3109), onTertiaryContainer = Color(0xFFFFE9B0),
    background = Brand.InkDeep, onBackground = Color(0xFFEAF2F7),
    surface = Brand.InkDeep, onSurface = Color(0xFFEAF2F7),
    surfaceVariant = Color(0xFF1B3449), onSurfaceVariant = Color(0xFFABC0CE),
    surfaceTint = Brand.Amber,
    surfaceContainerLowest = Color(0xFF06101A),
    surfaceContainerLow = Brand.Ink,
    surfaceContainer = Color(0xFF12293D),
    surfaceContainerHigh = Color(0xFF173249),
    surfaceContainerHighest = Color(0xFF1E3D56),
    outline = Color(0xFF8AA3B4), outlineVariant = Color(0xFF2B4A62),
    error = Color(0xFFFF8A80), onError = Color(0xFF3B0603),
    errorContainer = Color(0xFF5C1A15), onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Brand.Paper, inverseOnSurface = Brand.Ink, inversePrimary = Brand.Ink,
)

/** Barlow Semi Condensed gives headings a road-sign voice. Barlow carries the reading text. */
private val Display = FontFamily(
    Font(R.font.barlowsemicondensed_semibold, FontWeight.SemiBold),
    Font(R.font.barlowsemicondensed_bold, FontWeight.Bold),
)
private val Body = FontFamily(
    Font(R.font.barlow_regular, FontWeight.Normal),
    Font(R.font.barlow_medium, FontWeight.Medium),
    Font(R.font.barlow_semibold, FontWeight.SemiBold),
)

private fun display(size: Int, line: Int, weight: FontWeight = FontWeight.Bold, spacing: Double = 0.0) =
    TextStyle(fontFamily = Display, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)

private fun body(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, spacing: Double = 0.0) =
    TextStyle(fontFamily = Body, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)

private val AppTypography = Typography(
    displayLarge = display(56, 60), displayMedium = display(44, 48), displaySmall = display(36, 40),
    headlineLarge = display(34, 38, spacing = -0.2), headlineMedium = display(28, 32), headlineSmall = display(24, 28, FontWeight.SemiBold),
    titleLarge = display(22, 26, FontWeight.SemiBold), titleMedium = display(19, 23, FontWeight.SemiBold, 0.1),
    titleSmall = body(15, 20, FontWeight.SemiBold, 0.1),
    bodyLarge = body(17, 24), bodyMedium = body(15, 21), bodySmall = body(13, 18),
    labelLarge = body(16, 20, FontWeight.SemiBold, 0.2), labelMedium = body(13, 16, FontWeight.Medium, 0.3),
    labelSmall = body(12, 16, FontWeight.Medium, 0.4),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PlaceReminderTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
