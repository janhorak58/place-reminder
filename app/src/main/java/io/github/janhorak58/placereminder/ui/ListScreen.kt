@file:OptIn(ExperimentalFoundationApi::class)

package io.github.janhorak58.placereminder.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.janhorak58.placereminder.data.Frequency
import io.github.janhorak58.placereminder.data.Level
import io.github.janhorak58.placereminder.data.Reminder
import io.github.janhorak58.placereminder.data.Trigger
import io.github.janhorak58.placereminder.geofence.GeofenceManager
import io.github.janhorak58.placereminder.geofence.GeofenceState
import java.util.Locale

fun formatDistance(meters: Int): String =
    if (meters < 1000) "$meters m" else {
        val km = meters / 1000.0
        if (km % 1.0 == 0.0) "${km.toInt()} km" else String.format(Locale.US, "%.1f km", km)
    }

fun triggerSummary(r: Reminder): String = when (r.trigger) {
    Trigger.ENTER -> "On arrival"
    Trigger.EXIT -> "On leaving"
    Trigger.DWELL -> "Stay ${r.dwellMinutes} min"
}

private fun statusLine(list: List<Reminder>?): String {
    if (list == null) return ""
    if (list.isEmpty()) return "Nothing set up yet"
    val active = list.count { it.enabled }
    val off = list.size - active
    return if (off == 0) "$active active" else "$active active, $off off"
}

private fun detailLine(r: Reminder, fired: Boolean): String = buildList {
    add(triggerSummary(r))
    add(formatDistance(r.radiusMeters))
    add(if (r.frequency == Frequency.ONE_TIME) "Once" else "Every time")
    add(when (r.level) { Level.SILENT -> "Silent"; Level.NORMAL -> "Normal"; Level.ALARM -> "Alarm" })
    if (r.usesHeadsUp) add("Warns at ${formatDistance(r.headsUpMeters)}")
    if (fired) add("Done")
}.joinToString(" · ")

@Composable
fun ListScreen(
    vm: ListViewModel,
    status: PermStatus,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onSetup: () -> Unit,
) {
    val context = LocalContext.current
    val c = ios
    val reminders by vm.reminders.collectAsState()
    val geofenceState by GeofenceManager.state.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.message.value = null
        }
    }
    val list = reminders
    val listState = rememberLazyListState()
    val progress = rememberBarProgress(listState, titleHeight = 56.dp)
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + NavBarHeight
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize().background(c.background)) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = topInset, bottom = bottomInset + 24.dp),
        ) {
            item(key = "title") {
                Column(Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)) {
                    Text("Place Reminder", style = MaterialTheme.typography.headlineLarge, color = c.label)
                    val line = statusLine(list)
                    if (line.isNotEmpty()) {
                        Text(line, style = MaterialTheme.typography.bodyMedium, color = c.secondary)
                    }
                }
            }
            if (!status.allGranted) {
                item(key = "perm") {
                    Banner("Reminders are limited. Missing: ${status.missing().joinToString(", ")}.", "Fix", onSetup)
                }
            }
            if (geofenceState == GeofenceState.LOCATION_OFF) {
                item(key = "location") {
                    Banner("Location is off, so reminders cannot trigger.", "Settings") {
                        context.startActivity(SettingsIntents.locationSettings())
                    }
                }
            }
            if (list != null && list.isEmpty()) {
                item(key = "empty") { EmptyState(onAdd) }
            }
            val items = list.orEmpty()
            itemsIndexed(items, key = { _, r -> r.id }) { index, r ->
                ReminderCell(
                    r,
                    first = index == 0,
                    last = index == items.lastIndex,
                    onClick = { onEdit(r.id) },
                    onToggle = { vm.setEnabled(r, it) },
                    onDuplicate = { vm.duplicate(r) },
                    onDelete = { vm.delete(r) },
                )
            }
            if (items.isNotEmpty()) {
                item(key = "hint") {
                    Text(
                        "Touch and hold a reminder to edit, duplicate or delete it.",
                        style = MaterialTheme.typography.bodySmall, color = c.secondary,
                        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
                    )
                }
            }
        }
        IosNavBar(
            title = "Place Reminder",
            progress = progress,
            leading = { BarIconButton(GlyphKind.SLIDERS, "Permissions and setup", onSetup) },
            trailing = { BarIconButton(GlyphKind.PLUS, "New reminder", onAdd) },
        )
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = bottomInset))
    }
}

@Composable
private fun Banner(text: String, action: String, onClick: () -> Unit) {
    val c = ios
    Box(Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.cell).padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconTile(c.tint) { Glyph(GlyphKind.WARNING, Color.White, 20.dp) }
            Text(text, style = MaterialTheme.typography.bodyMedium, color = c.label, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
            IosTextButton(action, onClick, bold = true)
        }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    val c = ios
    Column(
        Modifier.fillMaxWidth().padding(top = 48.dp, start = 32.dp, end = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RingGlyph(Modifier.size(width = 186.dp, height = 160.dp), pin = c.tint, ring = c.blue)
        Text(
            "No reminders yet", style = MaterialTheme.typography.titleLarge, color = c.label,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            "Pick a place on the map and get a nudge when you arrive, leave or stay.",
            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge, color = c.secondary,
            modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
        )
        IosTextButton("New reminder", onAdd, bold = true)
    }
}

@Composable
private fun ReminderCell(
    r: Reminder,
    first: Boolean,
    last: Boolean,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val c = ios
    val fired = r.frequency == Frequency.ONE_TIME && r.lastFiredAt != null
    val muted = !r.enabled || fired
    val shape = RoundedCornerShape(
        topStart = if (first) 12.dp else 0.dp, topEnd = if (first) 12.dp else 0.dp,
        bottomStart = if (last) 12.dp else 0.dp, bottomEnd = if (last) 12.dp else 0.dp,
    )
    Box(Modifier.padding(horizontal = 16.dp)) {
        Column(Modifier.fillMaxWidth().clip(shape).background(c.cell)) {
            Row(
                Modifier.fillMaxWidth()
                    .combinedClickable(onClick = onClick, onLongClick = { menu = true })
                    .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val kind = when (r.trigger) { Trigger.ENTER -> GlyphKind.ARRIVE; Trigger.EXIT -> GlyphKind.LEAVE; Trigger.DWELL -> GlyphKind.STAY }
                IconTile(if (muted) c.gray else c.tint) { Glyph(kind, Color.White, 20.dp) }
                Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    Text(
                        r.name, style = MaterialTheme.typography.bodyLarge, color = if (muted) c.secondary else c.label,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                    if (r.message.isNotBlank()) {
                        Text(
                            r.message, style = MaterialTheme.typography.bodyMedium, color = c.secondary,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        detailLine(r, fired), style = MaterialTheme.typography.bodySmall,
                        color = c.secondary.copy(alpha = c.secondary.alpha * 0.9f), maxLines = 2, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp),
                    )
                }
                IosSwitch(checked = r.enabled, onCheckedChange = onToggle)
            }
            if (!last) IosDivider(inset = 58.dp)
        }
        DropdownMenu(
            expanded = menu, onDismissRequest = { menu = false },
            shape = RoundedCornerShape(14.dp), containerColor = c.elevated,
        ) {
            DropdownMenuItem(text = { Text("Edit", color = c.label) }, onClick = { menu = false; onClick() })
            DropdownMenuItem(text = { Text("Duplicate", color = c.label) }, onClick = { menu = false; onDuplicate() })
            DropdownMenuItem(text = { Text("Delete", color = c.red) }, onClick = { menu = false; onDelete() })
        }
    }
}
