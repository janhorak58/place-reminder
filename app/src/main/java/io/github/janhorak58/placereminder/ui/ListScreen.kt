@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.janhorak58.placereminder.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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
    Trigger.DWELL -> "After staying ${r.dwellMinutes} min"
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
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Place Reminder") },
                actions = {
                    IconButton(onClick = onSetup) { Icon(Icons.Filled.Settings, contentDescription = "Permissions and setup") }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) { Icon(Icons.Filled.Add, contentDescription = "Add reminder") }
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val list = reminders
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!status.allGranted) {
                item {
                    Banner("Setup incomplete: ${status.missing().joinToString(", ")}.", "Fix", onSetup)
                }
            }
            if (geofenceState == GeofenceState.LOCATION_OFF) {
                item {
                    Banner("Location is turned off, so reminders cannot trigger.", "Open settings") {
                        context.startActivity(SettingsIntents.locationSettings())
                    }
                }
            }
            if (list != null && list.isEmpty()) {
                item { EmptyState() }
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
            item { Box(Modifier.padding(40.dp)) }
        }
    }
}

@Composable
private fun Banner(text: String, action: String, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
            Text(
                text, color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
            )
            Button(onClick = onClick) { Text(action) }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 64.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text("No reminders yet", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 12.dp))
        Text(
            "Tap + to get a reminder when you arrive at or leave a place.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
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
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).alpha(if (r.enabled) 1f else 0.5f)) {
                Text(r.name, style = MaterialTheme.typography.titleMedium)
                if (r.message.isNotBlank()) Text(r.message, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                Text(
                    "${triggerSummary(r)} · within ${formatDistance(r.radiusMeters)}",
                    style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp),
                )
                val freq = if (r.frequency == Frequency.ONE_TIME) "One-time" else "Recurring"
                val level = when (r.level) { Level.SILENT -> "Silent"; Level.NORMAL -> "Normal"; Level.ALARM -> "Alarm" }
                val early = if (r.usesHeadsUp) " · heads-up at ${formatDistance(r.headsUpMeters)}" else ""
                Text("$freq · $level$early", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Switch(checked = r.enabled, onCheckedChange = onToggle)
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, contentDescription = "More") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menu = false; onClick() })
                    DropdownMenuItem(text = { Text("Duplicate") }, onClick = { menu = false; onDuplicate() })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete() })
                }
            }
        }
    }
}
