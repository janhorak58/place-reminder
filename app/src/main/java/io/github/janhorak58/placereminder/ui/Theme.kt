package io.github.janhorak58.placereminder.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.janhorak58.placereminder.R

/** iOS system palette. Tint orange is shared with the launcher icon. */
@Immutable
class IosColors(
    val background: Color,
    val cell: Color,
    val elevated: Color,
    val label: Color,
    val secondary: Color,
    val separator: Color,
    val segmentTrack: Color,
    val segmentThumb: Color,
    val tint: Color,
    val blue: Color,
    val green: Color,
    val red: Color,
    val gray: Color,
    val bar: Color,
)

private val LightIos = IosColors(
    background = Color(0xFFF2F2F7),
    cell = Color.White,
    elevated = Color.White,
    label = Color.Black,
    secondary = Color(0x993C3C43),
    separator = Color(0xFFC6C6C8),
    segmentTrack = Color(0x1F767680),
    segmentThumb = Color.White,
    tint = Color(0xFFFF9500),
    blue = Color(0xFF007AFF),
    green = Color(0xFF34C759),
    red = Color(0xFFFF3B30),
    gray = Color(0xFFAEAEB2),
    bar = Color(0xEBF2F2F7),
)

private val DarkIos = IosColors(
    background = Color.Black,
    cell = Color(0xFF1C1C1E),
    elevated = Color(0xFF2C2C2E),
    label = Color.White,
    secondary = Color(0x99EBEBF5),
    separator = Color(0xFF38383A),
    segmentTrack = Color(0x3D767680),
    segmentThumb = Color(0xFF636366),
    tint = Color(0xFFFF9F0A),
    blue = Color(0xFF0A84FF),
    green = Color(0xFF34C759),
    red = Color(0xFFFF3B30),
    gray = Color(0xFF636366),
    bar = Color(0xEB000000),
)

val LocalIos = staticCompositionLocalOf { LightIos }

/** Shortcut for the iOS palette of the current theme. */
val ios: IosColors @Composable @ReadOnlyComposable get() = LocalIos.current

private fun scheme(c: IosColors, dark: Boolean): ColorScheme =
    if (dark) {
        darkColorScheme(
            primary = c.tint, onPrimary = Color.Black,
            secondary = c.blue, onSecondary = Color.White,
            tertiary = c.green, onTertiary = Color.White,
            background = c.background, onBackground = c.label,
            surface = c.cell, onSurface = c.label,
            surfaceVariant = c.elevated, onSurfaceVariant = c.secondary,
            surfaceTint = Color.Transparent,
            surfaceContainerLowest = c.background, surfaceContainerLow = c.cell,
            surfaceContainer = c.elevated, surfaceContainerHigh = c.elevated, surfaceContainerHighest = c.elevated,
            outline = c.separator, outlineVariant = c.separator,
            error = c.red, onError = Color.White,
            inverseSurface = Color.White, inverseOnSurface = Color.Black, inversePrimary = c.tint,
        )
    } else {
        lightColorScheme(
            primary = c.tint, onPrimary = Color.Black,
            secondary = c.blue, onSecondary = Color.White,
            tertiary = c.green, onTertiary = Color.White,
            background = c.background, onBackground = c.label,
            surface = c.cell, onSurface = c.label,
            surfaceVariant = c.background, onSurfaceVariant = c.secondary,
            surfaceTint = Color.Transparent,
            surfaceContainerLowest = c.cell, surfaceContainerLow = c.cell,
            surfaceContainer = c.elevated, surfaceContainerHigh = c.elevated, surfaceContainerHighest = c.elevated,
            outline = c.separator, outlineVariant = c.separator,
            error = c.red, onError = Color.White,
            inverseSurface = Color(0xFF1C1C1E), inverseOnSurface = Color.White, inversePrimary = c.tint,
        )
    }

/** Inter stands in for SF Pro, which cannot be bundled on Android. */
private val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_regular, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private fun t(size: Int, line: Int, weight: FontWeight = FontWeight.Normal, spacing: Double = 0.0) =
    TextStyle(fontFamily = Inter, fontWeight = weight, fontSize = size.sp, lineHeight = line.sp, letterSpacing = spacing.sp)

/** iOS type scale: large title 34, title 22, body 17, footnote 13, caption 12. */
private val AppTypography = Typography(
    displayLarge = t(34, 41, FontWeight.Bold, -0.6), displayMedium = t(34, 41, FontWeight.Bold, -0.6),
    displaySmall = t(34, 41, FontWeight.Bold, -0.6),
    headlineLarge = t(34, 41, FontWeight.Bold, -0.6), headlineMedium = t(28, 34, FontWeight.Bold, -0.4),
    headlineSmall = t(22, 28, FontWeight.SemiBold, -0.3),
    titleLarge = t(22, 28, FontWeight.SemiBold, -0.3), titleMedium = t(17, 22, FontWeight.SemiBold, -0.4),
    titleSmall = t(15, 20, FontWeight.SemiBold, -0.2),
    bodyLarge = t(17, 22, spacing = -0.4), bodyMedium = t(15, 20, spacing = -0.2), bodySmall = t(13, 18, spacing = -0.1),
    labelLarge = t(17, 22, FontWeight.SemiBold, -0.4), labelMedium = t(13, 18, FontWeight.Medium, -0.1),
    labelSmall = t(12, 16, FontWeight.Medium),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(7.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(14.dp),
)

@Composable
fun PlaceReminderTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkIos else LightIos
    CompositionLocalProvider(LocalIos provides colors) {
        MaterialTheme(
            colorScheme = scheme(colors, darkTheme),
            typography = AppTypography,
            shapes = AppShapes,
            content = content,
        )
    }
}
