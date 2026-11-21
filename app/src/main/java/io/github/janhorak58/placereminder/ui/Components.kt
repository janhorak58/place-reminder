package io.github.janhorak58.placereminder.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** True unless the user turned system animations off. */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember { animationsEnabled(context) }
}

private fun animationsEnabled(context: Context) =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f

// ---------------------------------------------------------------------------------------------
// Pin and rings: the launcher icon motif, drawn in the icon's own units (pin tip at 54,68).
// ---------------------------------------------------------------------------------------------

private const val PIN_HEIGHT_UNITS = 37f

/** Draws the icon pin with its tip at [tip]. The pin is [height] px tall. */
fun DrawScope.drawPin(tip: Offset, height: Float, brush: Brush) {
    val k = height / PIN_HEIGHT_UNITS
    val c = Offset(tip.x, tip.y - 24f * k)
    val r = 13f * k
    val path = Path().apply {
        fillType = PathFillType.EvenOdd
        moveTo(tip.x, tip.y)
        lineTo(tip.x - 10.93f * k, tip.y - 16.96f * k)
        arcTo(Rect(c.x - r, c.y - r, c.x + r, c.y + r), 147.2f, 245.6f, false)
        close()
        addOval(Rect(c.x - 5.5f * k, c.y - 5.5f * k, c.x + 5.5f * k, c.y + 5.5f * k))
    }
    drawPath(path, brush)
}

fun amberBrush(top: Float, bottom: Float) =
    Brush.verticalGradient(listOf(Brand.Amber, Brand.AmberDeep), startY = top, endY = bottom)

/**
 * Pin with its radius ring and, when enabled, the heads-up ring. Muted renders everything in
 * neutral tones for disabled or finished reminders.
 */
@Composable
fun RingGlyph(modifier: Modifier = Modifier, headsUp: Boolean, muted: Boolean, showOuter: Boolean = true) {
    val neutral = MaterialTheme.colorScheme.outline
    Canvas(modifier) {
        val k = size.width / 62f
        val cx = size.width / 2f
        val cy = size.height - 13f * k
        val ringColor = if (muted) neutral else Brand.Ring
        val amber = if (muted) neutral else Brand.Amber
        if (headsUp && showOuter) {
            drawOval(ringColor.copy(alpha = 0.85f), Offset(cx - 29f * k, cy - 10.5f * k), Size(58f * k, 21f * k), style = Stroke(1.8f * k))
        }
        drawOval(amber.copy(alpha = 0.16f), Offset(cx - 17f * k, cy - 6f * k), Size(34f * k, 12f * k))
        drawOval(amber, Offset(cx - 17f * k, cy - 6f * k), Size(34f * k, 12f * k), style = Stroke(2f * k))
        val h = 37f * k
        val brush = if (muted) Brush.verticalGradient(listOf(neutral, neutral)) else amberBrush(cy - h, cy)
        drawPin(Offset(cx, cy), h, brush)
    }
}

// ---------------------------------------------------------------------------------------------
// Small line glyphs on a 24 unit grid.
// ---------------------------------------------------------------------------------------------

enum class GlyphKind { ARRIVE, LEAVE, STAY, ONCE, REPEAT, SILENT, NORMAL, ALARM, RADIUS, EARLY }

private val bellPath = PathParser().parsePathString("M12 3.5c-3.2 0-5.5 2.3-5.5 5.5v3.2L4.5 16h15l-2-3.8V9c0-3.2-2.3-5.5-5.5-5.5z").toPath()

@Composable
fun Glyph(kind: GlyphKind, tint: Color, size: Dp = 20.dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension / 24f
        val stroke = Stroke(2f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        fun line(a: Offset, b: Offset) = drawLine(tint, a, b, strokeWidth = 2f * s, cap = StrokeCap.Round)
        fun ring(cx: Float, cy: Float, r: Float, filled: Boolean = false) {
            if (filled) drawCircle(tint, r * s, p(cx, cy)) else drawCircle(tint, r * s, p(cx, cy), style = stroke)
        }
        fun bell() {
            val m = Matrix().apply { scale(s, s) }
            val path = Path().apply { addPath(bellPath); transform(m) }
            drawPath(path, tint, style = stroke)
            line(p(10f, 19.5f), p(14f, 19.5f))
        }
        when (kind) {
            GlyphKind.ARRIVE -> {
                ring(17.5f, 12f, 4.5f)
                line(p(2.5f, 12f), p(13f, 12f)); line(p(9.5f, 8.5f), p(13f, 12f)); line(p(9.5f, 15.5f), p(13f, 12f))
            }
            GlyphKind.LEAVE -> {
                ring(6.5f, 12f, 4.5f)
                line(p(11f, 12f), p(21.5f, 12f)); line(p(18f, 8.5f), p(21.5f, 12f)); line(p(18f, 15.5f), p(21.5f, 12f))
            }
            GlyphKind.STAY -> {
                ring(12f, 12f, 9f)
                line(p(12f, 6.5f), p(12f, 12f)); line(p(12f, 12f), p(15.8f, 14.2f))
            }
            GlyphKind.ONCE -> {
                ring(12f, 12f, 9f)
                line(p(10f, 9.8f), p(12.4f, 8f)); line(p(12.4f, 8f), p(12.4f, 16f))
            }
            GlyphKind.REPEAT -> {
                val r = 7.5f
                drawArc(tint, -60f, 290f, false, Offset((12 - r) * s, (12 - r) * s), Size(2 * r * s, 2 * r * s), style = stroke)
                val a = Math.toRadians(230.0)
                val tip = p(12f + r * cos(a).toFloat(), 12f + r * sin(a).toFloat())
                val dir = Offset(-sin(a).toFloat(), cos(a).toFloat())
                val perp = Offset(-dir.y, dir.x)
                val head = Path().apply {
                    moveTo(tip.x + dir.x * 3.2f * s, tip.y + dir.y * 3.2f * s)
                    lineTo(tip.x + perp.x * 3.2f * s, tip.y + perp.y * 3.2f * s)
                    lineTo(tip.x - perp.x * 3.2f * s, tip.y - perp.y * 3.2f * s)
                    close()
                }
                drawPath(head, tint)
            }
            GlyphKind.SILENT -> { bell(); line(p(4f, 3.5f), p(20f, 20.5f)) }
            GlyphKind.NORMAL -> bell()
            GlyphKind.ALARM -> {
                bell()
                drawArc(tint, 200f, 70f, false, p(-1f, 1f), Size(10f * s, 10f * s), style = stroke)
                drawArc(tint, 270f, 70f, false, p(15f, 1f), Size(10f * s, 10f * s), style = stroke)
            }
            GlyphKind.RADIUS -> {
                ring(12f, 12f, 8.5f); ring(12f, 12f, 2.2f, filled = true)
            }
            GlyphKind.EARLY -> {
                drawArc(tint, 0f, 360f, false, p(3f, 3f), Size(18f * s, 18f * s), style = Stroke(1.6f * s, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2.4f * s, 2.4f * s))))
                ring(12f, 12f, 4f)
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Layout pieces
// ---------------------------------------------------------------------------------------------

/** Navy header band with faint geofence rings. Sits under the status bar on every screen. */
@Composable
fun InkHeader(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brand.Ink)
            .drawBehind {
                val cx = size.width * 0.9f
                val cy = size.height * 0.85f
                val widths = listOf(70.dp, 130.dp, 200.dp, 290.dp)
                widths.forEachIndexed { i, w ->
                    val wp = w.toPx()
                    val hp = wp * 0.36f
                    val color = if (i == 0) Brand.Amber else Brand.Ring
                    drawOval(
                        color.copy(alpha = if (i == 0) 0.55f else 0.30f - i * 0.06f),
                        Offset(cx - wp / 2, cy - hp / 2), Size(wp, hp), style = Stroke(1.5.dp.toPx()),
                    )
                }
            }
            .statusBarsPadding(),
        content = content,
    )
}

/** Titled group of fields. */
@Composable
fun SectionCard(title: String, modifier: Modifier = Modifier, hint: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(4.dp).height(18.dp).clip(CircleShape).background(Brand.Amber))
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 10.dp))
            }
            if (hint != null) {
                Text(
                    hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, start = 14.dp),
                )
            }
        }
        content()
    }
}

/** Compact label with a glyph, used for the facts on a reminder card. */
@Composable
fun Pill(text: String, kind: GlyphKind?, modifier: Modifier = Modifier, container: Color, content: Color) {
    Row(
        modifier.clip(CircleShape).background(container).padding(start = if (kind != null) 8.dp else 10.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (kind != null) Glyph(kind, content, 15.dp)
        Text(text, style = MaterialTheme.typography.labelMedium, color = content)
    }
}

@Composable
private fun optionColors(selected: Boolean): Triple<Color, Color, BorderStroke> {
    val cs = MaterialTheme.colorScheme
    return if (selected) {
        Triple(cs.primaryContainer, cs.onPrimaryContainer, BorderStroke(2.dp, cs.primary))
    } else {
        Triple(cs.surface, cs.onSurface, BorderStroke(1.dp, cs.outline.copy(alpha = 0.55f)))
    }
}

/** Vertical choice tile: glyph over a label. Place several in a Row with weight. */
@Composable
fun OptionTile(selected: Boolean, onClick: () -> Unit, kind: GlyphKind, label: String, modifier: Modifier = Modifier) {
    val (bg, fg, border) = optionColors(selected)
    Column(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .background(bg)
            .border(border, MaterialTheme.shapes.medium)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Glyph(kind, fg, 26.dp)
        Text(label, style = MaterialTheme.typography.labelLarge, color = fg)
    }
}

/** Horizontal choice row: glyph, title and a one-line description of what happens. */
@Composable
fun OptionRow(selected: Boolean, onClick: () -> Unit, kind: GlyphKind, title: String, description: String, modifier: Modifier = Modifier) {
    val (bg, fg, border) = optionColors(selected)
    val tick = MaterialTheme.colorScheme.onPrimary
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(bg)
            .border(border, MaterialTheme.shapes.medium)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Glyph(kind, fg, 26.dp)
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = fg)
            Text(description, style = MaterialTheme.typography.bodySmall, color = if (selected) fg else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            Modifier.size(22.dp).clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Canvas(Modifier.size(12.dp)) {
                    val c = tick
                    drawLine(c, Offset(size.width * 0.1f, size.height * 0.55f), Offset(size.width * 0.4f, size.height * 0.85f), 2.2f * density, StrokeCap.Round)
                    drawLine(c, Offset(size.width * 0.4f, size.height * 0.85f), Offset(size.width * 0.92f, size.height * 0.15f), 2.2f * density, StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    errorTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = MaterialTheme.colorScheme.surface,
    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
    errorContainerColor = MaterialTheme.colorScheme.surface,
    focusedBorderColor = MaterialTheme.colorScheme.secondary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    focusedLabelColor = MaterialTheme.colorScheme.secondary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    cursorColor = MaterialTheme.colorScheme.secondary,
    focusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
)
