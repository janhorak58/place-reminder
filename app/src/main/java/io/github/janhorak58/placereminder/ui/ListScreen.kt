@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package io.github.janhorak58.placereminder.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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

@Composable
fun ListScreen(
    vm: ListViewModel,
    status: PermStatus,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onSetup: () -> Unit,
) {
    val context = LocalContext.current
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
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
        floatingActionButton = {
            if (!list.isNullOrEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = onAdd,
                    containerColor = Brand.Amber,
                    contentColor = Brand.Ink,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New reminder", style = MaterialTheme.typography.labelLarge) },
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            InkHeader {
                Row(
                    Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Place Reminder", style = MaterialTheme.typography.headlineLarge, color = Brand.Paper)
                        Text(statusLine(list), style = MaterialTheme.typography.titleMedium, color = Brand.Amber)
                    }
                    IconButton(onClick = onSetup) {
                        Icon(Icons.Filled.Settings, contentDescription = "Permissions and setup", tint = Brand.Paper)
                    }
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (!status.allGranted) {
                    item {
                        Banner("Reminders are limited. Missing: ${status.missing().joinToString(", ")}.", "Fix", onSetup)
                    }
                }
                if (geofenceState == GeofenceState.LOCATION_OFF) {
                    item {
                        Banner("Location is off, so reminders cannot trigger.", "Open settings") {
                            context.startActivity(SettingsIntents.locationSettings())
                        }
                    }
                }
                if (list != null && list.isEmpty()) {
                    item { EmptyState(onAdd) }
                }
                items(list.orEmpty(), key = { it.id }) { r ->
                    ReminderCard(
                        r,
                        onClick = { onEdit(r.id) },
                        onToggle = { vm.setEnabled(r, it) },
                        onDuplicate = { vm.duplicate(r) },
                        onDelete = { vm.delete(r) },
                    )
                }
                item { Box(Modifier.height(72.dp)) }
            }
        }
    }
}

@Composable
private fun Banner(text: String, action: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(cs.tertiaryContainer)
            .border(1.5.dp, Brand.Amber, MaterialTheme.shapes.large).padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Warning, contentDescription = null, tint = Brand.AmberDeep)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = cs.onTertiaryContainer, modifier = Modifier.weight(1f).padding(horizontal = 12.dp))
        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = Brand.Ink, contentColor = Brand.Paper),
        ) { Text(action) }
    }
}

@Composable
private fun EmptyState(onAdd: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(top = 40.dp, start = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RingGlyph(Modifier.size(width = 186.dp, height = 160.dp), headsUp = true, muted = false)
        Text(
            "No reminders yet", style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            "Pick a place on the map and get a nudge when you arrive, leave or stay.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
        )
        Button(
            onClick = onAdd,
            modifier = Modifier.height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Brand.Amber, contentColor = Brand.Ink),
            contentPadding = PaddingValues(horizontal = 24.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("New reminder", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun ReminderCard(
    r: Reminder,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val cs = MaterialTheme.colorScheme
    val fired = r.frequency == Frequency.ONE_TIME && r.lastFiredAt != null
    val muted = !r.enabled || fired
    Row(
        Modifier.fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(cs.surfaceContainerLow)
            .border(1.dp, cs.outlineVariant, MaterialTheme.shapes.large)
            .clickable(onClick = onClick)
            .padding(start = 12.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
    ) {
        Row(Modifier.weight(1f).alpha(if (muted) 0.55f else 1f)) {
            RingGlyph(Modifier.size(width = 62.dp, height = 54.dp).padding(top = 4.dp), headsUp = r.usesHeadsUp, muted = muted)
            Column(Modifier.weight(1f).padding(start = 12.dp, end = 4.dp)) {
                Text(r.name, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (r.message.isNotBlank()) {
                    Text(
                        r.message, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
                FlowRow(
                    Modifier.padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    val neutral = cs.surfaceContainerHigh
                    val triggerKind = when (r.trigger) { Trigger.ENTER -> GlyphKind.ARRIVE; Trigger.EXIT -> GlyphKind.LEAVE; Trigger.DWELL -> GlyphKind.STAY }
                    Pill(triggerSummary(r), triggerKind, container = cs.secondaryContainer, content = cs.onSecondaryContainer)
                    Pill(formatDistance(r.radiusMeters), GlyphKind.RADIUS, container = neutral, content = cs.onSurface)
                    Pill(
                        if (r.frequency == Frequency.ONE_TIME) "Once" else "Every time",
                        if (r.frequency == Frequency.ONE_TIME) GlyphKind.ONCE else GlyphKind.REPEAT,
                        container = neutral, content = cs.onSurface,
                    )
                    when (r.level) {
                        Level.SILENT -> Pill("Silent", GlyphKind.SILENT, container = neutral, content = cs.onSurface)
                        Level.NORMAL -> Pill("Normal", GlyphKind.NORMAL, container = neutral, content = cs.onSurface)
                        Level.ALARM -> Pill("Alarm", GlyphKind.ALARM, container = cs.primaryContainer, content = cs.onPrimaryContainer)
                    }
                    if (r.usesHeadsUp) {
                        Pill("Warns at ${formatDistance(r.headsUpMeters)}", GlyphKind.EARLY, container = cs.secondaryContainer, content = cs.onSecondaryContainer)
                    }
                    if (fired) Pill("Done", null, container = neutral, content = cs.onSurface)
                }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Switch(
                checked = r.enabled, onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Brand.Ink, checkedTrackColor = Brand.Amber, checkedBorderColor = Brand.Amber,
                    uncheckedThumbColor = cs.outline, uncheckedTrackColor = cs.surfaceContainerHigh, uncheckedBorderColor = cs.outline,
                ),
                modifier = Modifier.padding(end = 8.dp),
            )
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = cs.onSurfaceVariant) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onClick() })
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}
