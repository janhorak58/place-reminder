@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.janhorak58.placereminder.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import io.github.janhorak58.placereminder.data.Frequency
import io.github.janhorak58.placereminder.data.Level
import io.github.janhorak58.placereminder.data.Limits
import io.github.janhorak58.placereminder.data.RadiusScale
import io.github.janhorak58.placereminder.data.Trigger
import io.github.janhorak58.placereminder.data.Validation
import org.osmdroid.util.GeoPoint

@SuppressLint("MissingPermission")
@Composable
fun EditorScreen(vm: EditorViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val client = remember { LocationServices.getFusedLocationProviderClient(context) }
    var startCenter by remember { mutableStateOf<GeoPoint?>(null) }

    fun hasFine() = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun useCurrentLocation() {
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { loc ->
                if (loc != null) vm.setLocation(loc.latitude, loc.longitude)
                else Toast.makeText(context, "Current location is not available. Is location turned on?", Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Could not get the current location", Toast.LENGTH_LONG).show()
            }
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) useCurrentLocation()
        else Toast.makeText(context, "Location permission is needed for this", Toast.LENGTH_LONG).show()
    }

    LaunchedEffect(Unit) {
        if (hasFine() && !vm.hasLocation) {
            client.lastLocation.addOnSuccessListener { loc -> if (loc != null) startCenter = GeoPoint(loc.latitude, loc.longitude) }
        }
    }

    BackHandler(onBack = onClose)
    val error = vm.error
    fun errorFor(f: Validation.Field) = error?.takeIf { it.field == f }?.message

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reminder") },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = { TextButton(onClick = { vm.save(onClose) }) { Text("Save") } },
            )
        },
    ) { padding ->
        if (!vm.loaded) return@Scaffold
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = vm.name, onValueChange = { vm.name = it }, label = { Text("Name") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                isError = errorFor(Validation.Field.NAME) != null,
                supportingText = errorFor(Validation.Field.NAME)?.let { { Text(it) } },
            )
            OutlinedTextField(
                value = vm.message, onValueChange = { vm.message = it }, label = { Text("Message") },
                modifier = Modifier.fillMaxWidth(),
            )

            Section("Place")
            LocationMap(
                modifier = Modifier.fillMaxWidth().height(280.dp),
                point = if (vm.hasLocation) GeoPoint(vm.latitude!!, vm.longitude!!) else null,
                radiusMeters = vm.radiusMeters?.takeIf { it > 0 },
                headsUpMeters = if (vm.headsUp && vm.trigger != Trigger.EXIT) vm.headsUpMeters?.takeIf { it > 0 } else null,
                fitKey = vm.fitTick,
                startCenter = startCenter,
                onPick = { vm.setLocation(it.latitude, it.longitude) },
            )
            Text(
                errorFor(Validation.Field.LOCATION) ?: "Tap or long-press the map to place the pin.",
                style = MaterialTheme.typography.bodySmall,
                color = if (errorFor(Validation.Field.LOCATION) != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(onClick = { if (hasFine()) useCurrentLocation() else permission.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                Icon(Icons.Filled.LocationOn, contentDescription = null)
                Text("Use current location", modifier = Modifier.padding(start = 8.dp))
            }

            Section("Radius")
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = vm.radiusText, onValueChange = { vm.radiusText = it; vm.requestFit() },
                    label = { Text("Radius") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                    isError = errorFor(Validation.Field.RADIUS) != null,
                    supportingText = errorFor(Validation.Field.RADIUS)?.let { { Text(it) } },
                )
                SingleChoiceSegmentedButtonRow(Modifier.padding(top = 8.dp)) {
                    SegmentedButton(selected = !vm.radiusInKm, onClick = { vm.setRadiusUnit(false) }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("m") }
                    SegmentedButton(selected = vm.radiusInKm, onClick = { vm.setRadiusUnit(true) }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("km") }
                }
            }
            Slider(
                value = vm.sliderPosition(),
                onValueChange = { vm.setRadiusMeters(RadiusScale.toMeters(it)) },
                onValueChangeFinished = { vm.requestFit() },
            )
            val radius = vm.radiusMeters
            if (radius != null && radius < Limits.UNRELIABLE_BELOW_M) {
                Text("Below 100 m, alerts can be unreliable", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Section("When")
            Segmented(
                listOf(Trigger.ENTER to "Enter", Trigger.EXIT to "Exit", Trigger.DWELL to "Stay"),
                vm.trigger,
            ) { vm.trigger = it }
            if (vm.trigger == Trigger.DWELL) {
                OutlinedTextField(
                    value = vm.dwellText, onValueChange = { vm.dwellText = it }, label = { Text("Stay for (minutes)") },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorFor(Validation.Field.DWELL) != null,
                    supportingText = errorFor(Validation.Field.DWELL)?.let { { Text(it) } },
                )
            }

            Section("Frequency")
            Segmented(listOf(Frequency.ONE_TIME to "One-time", Frequency.RECURRING to "Recurring"), vm.frequency) { vm.frequency = it }

            Section("Alert level")
            Segmented(listOf(Level.SILENT to "Silent", Level.NORMAL to "Normal", Level.ALARM to "Alarm"), vm.level) { vm.level = it }

            if (vm.trigger != Trigger.EXIT) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = vm.headsUp, onCheckedChange = { vm.toggleHeadsUp(it) })
                    Text("Remind me before I arrive")
                }
                Text(
                    "Use heads-up for an early warning while driving.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (vm.headsUp) {
                    OutlinedTextField(
                        value = vm.headsUpKmText, onValueChange = { vm.headsUpKmText = it; vm.requestFit() },
                        label = { Text("Warn me at (km)") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        isError = errorFor(Validation.Field.HEADS_UP) != null,
                        supportingText = errorFor(Validation.Field.HEADS_UP)?.let { { Text(it) } },
                    )
                }
            }

            Button(onClick = { vm.save(onClose) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) { Text("Save") }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { i, (value, label) ->
            SegmentedButton(
                selected = value == selected, onClick = { onSelect(value) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
            ) { Text(label) }
        }
    }
}
