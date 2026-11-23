package io.github.janhorak58.placereminder.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

/** Text on a tint-coloured fill. Black keeps contrast high on orange. */
val OnTint = Color.Black

// ---------------------------------------------------------------------------------------------
// Pin and rings: the launcher icon motif, drawn in the icon's own units (pin tip at 54,68).
// ---------------------------------------------------------------------------------------------

private const val PIN_HEIGHT_UNITS = 37f

/** Draws the icon pin with its tip at [tip]. The pin is [height] px tall. */
fun DrawScope.drawPin(tip: Offset, height: Float, color: Color) {
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
    drawPath(path, color)
}

/** Pin with a radius ring and a wider heads-up ring, drawn in thin strokes. */
@Composable
fun RingGlyph(modifier: Modifier = Modifier, pin: Color, ring: Color, radiusRing: Color = pin) {
    Canvas(modifier) {
        val k = size.width / 62f
        val cx = size.width / 2f
        val cy = size.height - 13f * k
        val line = 1.5.dp.toPx()
        drawOval(ring, Offset(cx - 29f * k, cy - 10.5f * k), Size(58f * k, 21f * k), style = Stroke(line))
        drawOval(radiusRing.copy(alpha = 0.14f), Offset(cx - 17f * k, cy - 6f * k), Size(34f * k, 12f * k))
        drawOval(radiusRing, Offset(cx - 17f * k, cy - 6f * k), Size(34f * k, 12f * k), style = Stroke(line))
        drawPin(Offset(cx, cy), 37f * k, pin)
    }
}

// ---------------------------------------------------------------------------------------------
// Thin line glyphs on a 24 unit grid, 1.5dp stroke with round caps like SF Symbols.
// ---------------------------------------------------------------------------------------------

enum class GlyphKind { ARRIVE, LEAVE, STAY, ONCE, REPEAT, SILENT, NORMAL, ALARM, RADIUS, EARLY, PIN, PLUS, SLIDERS, CHECK, CHEVRON, WARNING, LOCATE }

private val bellPath = PathParser().parsePathString("M12 3.5c-3.2 0-5.5 2.3-5.5 5.5v3.2L4.5 16h15l-2-3.8V9c0-3.2-2.3-5.5-5.5-5.5z").toPath()
private val pinPath = PathParser().parsePathString("M12 21.5C12 21.5 5 15 5 9.5a7 7 0 0 1 14 0C19 15 12 21.5 12 21.5z").toPath()
private val locatePath = PathParser().parsePathString("M20 4L4 11l7 2l2 7z").toPath()
private val warnPath = PathParser().parsePathString("M12 3.5L21.5 20h-19z").toPath()

@Composable
fun Glyph(kind: GlyphKind, tint: Color, size: Dp = 20.dp, strokeWidth: Dp = 1.5.dp) {
    Canvas(Modifier.size(size)) {
        val s = this.size.minDimension / 24f
        val w = strokeWidth.toPx()
        val stroke = Stroke(w, cap = StrokeCap.Round, join = StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        fun line(a: Offset, b: Offset) = drawLine(tint, a, b, strokeWidth = w, cap = StrokeCap.Round)
        fun ring(cx: Float, cy: Float, r: Float, filled: Boolean = false) {
            if (filled) drawCircle(tint, r * s, p(cx, cy)) else drawCircle(tint, r * s, p(cx, cy), style = stroke)
        }
        fun outline(path: Path) {
            val m = Matrix().apply { scale(s, s) }
            drawPath(Path().apply { addPath(path); transform(m) }, tint, style = stroke)
        }
        fun bell() {
            outline(bellPath)
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
            GlyphKind.RADIUS -> { ring(12f, 12f, 8.5f); ring(12f, 12f, 2.2f, filled = true) }
            GlyphKind.EARLY -> {
                drawArc(
                    tint, 0f, 360f, false, p(3f, 3f), Size(18f * s, 18f * s),
                    style = Stroke(w, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(2.4f * s, 2.4f * s))),
                )
                ring(12f, 12f, 4f)
            }
            GlyphKind.PIN -> { outline(pinPath); ring(12f, 9.5f, 2.4f) }
            GlyphKind.PLUS -> { line(p(12f, 4.5f), p(12f, 19.5f)); line(p(4.5f, 12f), p(19.5f, 12f)) }
            GlyphKind.SLIDERS -> {
                line(p(3.5f, 7f), p(10.5f, 7f)); line(p(17.5f, 7f), p(20.5f, 7f)); ring(14f, 7f, 3.2f)
                line(p(3.5f, 17f), p(6.5f, 17f)); line(p(13.5f, 17f), p(20.5f, 17f)); ring(10f, 17f, 3.2f)
            }
            GlyphKind.CHECK -> { line(p(5f, 12.5f), p(10f, 17.5f)); line(p(10f, 17.5f), p(19f, 7f)) }
            GlyphKind.CHEVRON -> { line(p(9f, 5f), p(16f, 12f)); line(p(16f, 12f), p(9f, 19f)) }
            GlyphKind.WARNING -> {
                outline(warnPath); line(p(12f, 10f), p(12f, 14.5f)); ring(12f, 17.3f, 0.6f, filled = true)
            }
            GlyphKind.LOCATE -> outline(locatePath)
        }
    }
}

// ---------------------------------------------------------------------------------------------
// Navigation bar with a large title that collapses on scroll.
// ---------------------------------------------------------------------------------------------

/** Scroll-driven values for the nav bar: background/hairline opacity and small-title opacity. */
class BarProgress(val background: Float, val title: Float)

@Composable
fun rememberBarProgress(state: LazyListState, titleHeight: Dp = 48.dp): BarProgress {
    val px = with(LocalDensity.current) { titleHeight.toPx() }
    val index = state.firstVisibleItemIndex
    val offset = state.firstVisibleItemScrollOffset
    if (index > 0) return BarProgress(1f, 1f)
    return BarProgress((offset / 10f).coerceIn(0f, 1f), ((offset - px * 0.4f) / (px * 0.5f)).coerceIn(0f, 1f))
}

val NavBarHeight = 44.dp

@Composable
fun IosNavBar(
    title: String,
    progress: BarProgress,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = {},
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val c = ios
    Box(modifier.fillMaxWidth()) {
        Box(Modifier.matchParentSize().alpha(progress.background).background(c.bar))
        Box(Modifier.fillMaxWidth().statusBarsPadding().height(NavBarHeight)) {
            Box(Modifier.align(Alignment.CenterStart).padding(start = 8.dp)) { leading() }
            Text(
                title, style = MaterialTheme.typography.titleMedium, color = c.label, maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.align(Alignment.Center).padding(horizontal = 96.dp).alpha(progress.title),
            )
            Row(Modifier.align(Alignment.CenterEnd).padding(end = 8.dp), verticalAlignment = Alignment.CenterVertically) { trailing() }
        }
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(0.5.dp).alpha(progress.background).background(c.separator))
    }
}

/** Bar icon button with a 44dp touch target. */
@Composable
fun BarIconButton(kind: GlyphKind, description: String, onClick: () -> Unit, tint: Color = ios.tint, size: Dp = 24.dp) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).semantics { contentDescription = description }
            .clickable(onClick = onClick, role = Role.Button),
        contentAlignment = Alignment.Center,
    ) { Glyph(kind, tint, size, 1.6.dp) }
}

// ---------------------------------------------------------------------------------------------
// Grouped list pieces
// ---------------------------------------------------------------------------------------------

/**
 * Inset grouped section: uppercase footnote [header], rounded cell container, footnote [footer].
 * [error] shows as a red footnote above the footer.
 */
@Composable
fun IosSection(
    modifier: Modifier = Modifier,
    header: String? = null,
    footer: String? = null,
    error: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = ios
    Column(modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        if (header != null) {
            Text(
                header.uppercase(), style = MaterialTheme.typography.bodySmall, color = c.secondary,
                modifier = Modifier.padding(start = 16.dp, bottom = 7.dp),
            )
        }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.cell), content = content)
        if (error != null) {
            Text(error, style = MaterialTheme.typography.bodySmall, color = c.red, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 7.dp))
        }
        if (footer != null) {
            Text(footer, style = MaterialTheme.typography.bodySmall, color = c.secondary, modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 7.dp))
        }
    }
}

@Composable
fun IosDivider(inset: Dp = 16.dp) {
    Box(Modifier.fillMaxWidth().padding(start = inset).height(0.5.dp).background(ios.separator))
}

/** A 44dp minimum cell row. */
@Composable
fun IosRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier.fillMaxWidth().heightIn(min = 44.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/** Rounded-square tile that holds a white glyph, like the icons in iOS Settings. */
@Composable
fun IconTile(color: Color, modifier: Modifier = Modifier, size: Dp = 30.dp, content: @Composable () -> Unit) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size * 0.23f)).background(color),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun IosSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = ios
    val x by animateDpAsState(if (checked) 20.dp else 0.dp, tween(180), label = "thumb")
    val track by animateColorAsState(if (checked) c.green else c.segmentTrack, tween(180), label = "track")
    Box(
        modifier.size(width = 51.dp, height = 31.dp).clip(CircleShape).background(track).padding(2.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange),
    ) {
        Box(
            Modifier.offset(x = x).size(27.dp).shadow(2.dp, CircleShape).background(Color.White, CircleShape),
        )
    }
}

/** iOS segmented control: grey track with a white sliding selection. */
@Composable
fun IosSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = ios
    BoxWithConstraints(modifier.height(32.dp).clip(RoundedCornerShape(9.dp)).background(c.segmentTrack).padding(2.dp)) {
        val segment = maxWidth / options.size
        val x by animateDpAsState(segment * selected, tween(200), label = "segment")
        Box(
            Modifier.offset(x = x).width(segment).fillMaxHeight()
                .shadow(1.dp, RoundedCornerShape(7.dp)).background(c.segmentThumb, RoundedCornerShape(7.dp)),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                val source = remember { MutableInteractionSource() }
                Box(
                    Modifier.weight(1f).fillMaxHeight().selectable(
                        selected = i == selected, interactionSource = source, indication = null,
                        role = Role.RadioButton, onClick = { onSelect(i) },
                    ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label, color = c.label, maxLines = 1, fontSize = 13.sp,
                        fontWeight = if (i == selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

/** Borderless text field for use inside a cell. */
@Composable
fun IosTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    textAlign: TextAlign = TextAlign.Start,
) {
    val c = ios
    val style = MaterialTheme.typography.bodyLarge.copy(color = c.label, textAlign = textAlign)
    BasicTextField(
        value = value, onValueChange = onValueChange, modifier = modifier,
        singleLine = singleLine, maxLines = if (singleLine) 1 else 4,
        keyboardOptions = keyboardOptions, textStyle = style, cursorBrush = SolidColor(c.tint),
        decorationBox = { inner ->
            Box(contentAlignment = if (textAlign == TextAlign.End) Alignment.CenterEnd else Alignment.CenterStart) {
                if (value.isEmpty()) Text(placeholder, style = style.copy(color = c.secondary), maxLines = 1)
                inner()
            }
        },
    )
}

@Composable
fun IosTextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, bold: Boolean = false) {
    val c = ios
    Box(
        modifier.heightIn(min = 44.dp).clickable(enabled = enabled, role = Role.Button, onClick = onClick).padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal),
            color = if (enabled) c.tint else c.secondary.copy(alpha = 0.5f),
        )
    }
}

/** Small tinted capsule button, like the iOS "Get" button. */
@Composable
fun IosCapsuleButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val c = ios
    Text(
        text, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center,
        color = if (enabled) c.tint else c.secondary.copy(alpha = 0.6f),
        modifier = modifier.clip(CircleShape).background(if (enabled) c.tint.copy(alpha = 0.16f) else c.segmentTrack)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp),
    )
}

/** Large rounded button filled with the tint colour. */
@Composable
fun IosFilledButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, radius: Dp = 14.dp) {
    val c = ios
    Box(
        modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(radius))
            .background(if (enabled) c.tint else c.segmentTrack)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = if (enabled) OnTint else c.secondary)
    }
}
