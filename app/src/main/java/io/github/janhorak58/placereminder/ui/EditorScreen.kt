@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.janhorak58.placereminder.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.ui.draw.clip
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
        modifier = Modifier.imePadding(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shadowElevation = 8.dp) {
                Column(Modifier.navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (error != null) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Text(error.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    Button(
                        onClick = { vm.save(onClose) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Brand.Amber, contentColor = Brand.Ink),
                    ) { Text("Save reminder", style = MaterialTheme.typography.labelLarge) }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            InkHeader {
                Row(Modifier.padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Brand.Paper)
                    }
                    Text(
                        if (vm.isNew) "New reminder" else "Edit reminder",
                        style = MaterialTheme.typography.headlineSmall, color = Brand.Paper,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                }
            }
            if (!vm.loaded) return@Column
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.fillMaxWidth().height(340.dp).clip(MaterialTheme.shapes.extraLarge)) {
                        LocationMap(
                            modifier = Modifier.fillMaxSize(),
                            point = if (vm.hasLocation) GeoPoint(vm.latitude!!, vm.longitude!!) else null,
                            radiusMeters = vm.radiusMeters?.takeIf { it > 0 },
                            headsUpMeters = if (vm.headsUp && vm.trigger != Trigger.EXIT) vm.headsUpMeters?.takeIf { it > 0 } else null,
                            fitKey = vm.fitTick,
                            startCenter = startCenter,
                            onPick = { vm.setLocation(it.latitude, it.longitude) },
                        )
                        Button(
                            onClick = { if (hasFine()) useCurrentLocation() else permission.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                            modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Brand.Ink, contentColor = Brand.Paper),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                        ) {
                            Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Brand.Amber, modifier = Modifier.size(18.dp))
                            Text("Use my location", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                    val locationError = errorFor(Validation.Field.LOCATION)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 6.dp)) {
                        if (locationError != null) Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                        Text(
                            locationError ?: "Tap or long-press the map to place the pin.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (locationError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                SectionCard("What to remember") {
                    OutlinedTextField(
                        value = vm.name, onValueChange = { vm.name = it }, label = { Text("Name") },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), colors = fieldColors(), shape = MaterialTheme.shapes.medium,
                        isError = errorFor(Validation.Field.NAME) != null,
                        supportingText = errorFor(Validation.Field.NAME)?.let { { Text(it) } },
                    )
                    OutlinedTextField(
                        value = vm.message, onValueChange = { vm.message = it }, label = { Text("Message") },
                        modifier = Modifier.fillMaxWidth(), colors = fieldColors(), shape = MaterialTheme.shapes.medium,
                    )
                }

                SectionCard("Radius", hint = "How close you need to be for the reminder to fire.") {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = vm.radiusText, onValueChange = { vm.radiusText = it; vm.requestFit() },
                            label = { Text("Radius") }, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1f), colors = fieldColors(), shape = MaterialTheme.shapes.medium,
                            isError = errorFor(Validation.Field.RADIUS) != null,
                            supportingText = errorFor(Validation.Field.RADIUS)?.let { { Text(it) } },
                        )
                        val segColors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MaterialTheme.colorScheme.primary,
                            activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            activeBorderColor = MaterialTheme.colorScheme.primary,
                            inactiveContainerColor = MaterialTheme.colorScheme.surface,
                            inactiveContentColor = MaterialTheme.colorScheme.onSurface,
                            inactiveBorderColor = MaterialTheme.colorScheme.outline,
                        )
                        SingleChoiceSegmentedButtonRow(Modifier.padding(top = 8.dp)) {
                            SegmentedButton(selected = !vm.radiusInKm, onClick = { vm.setRadiusUnit(false) }, shape = SegmentedButtonDefaults.itemShape(0, 2), colors = segColors) { Text("m") }
                            SegmentedButton(selected = vm.radiusInKm, onClick = { vm.setRadiusUnit(true) }, shape = SegmentedButtonDefaults.itemShape(1, 2), colors = segColors) { Text("km") }
                        }
                    }
                    Slider(
                        value = vm.sliderPosition(),
                        onValueChange = { vm.setRadiusMeters(RadiusScale.toMeters(it)) },
                        onValueChangeFinished = { vm.requestFit() },
                        colors = SliderDefaults.colors(
                            thumbColor = Brand.Amber, activeTrackColor = Brand.Amber,
                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ),
                    )
                    val radius = vm.radiusMeters
                    if (radius != null && radius < Limits.UNRELIABLE_BELOW_M) {
                        Text("Below 100 m, alerts can be unreliable", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                SectionCard("Fires when you") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OptionTile(vm.trigger == Trigger.ENTER, { vm.trigger = Trigger.ENTER }, GlyphKind.ARRIVE, "Arrive", Modifier.weight(1f))
                        OptionTile(vm.trigger == Trigger.EXIT, { vm.trigger = Trigger.EXIT }, GlyphKind.LEAVE, "Leave", Modifier.weight(1f))
                        OptionTile(vm.trigger == Trigger.DWELL, { vm.trigger = Trigger.DWELL }, GlyphKind.STAY, "Stay", Modifier.weight(1f))
                    }
                    Text(
                        when (vm.trigger) {
                            Trigger.ENTER -> "Fires as soon as you get inside the radius."
                            Trigger.EXIT -> "Fires when you leave the radius."
                            Trigger.DWELL -> "Fires after you stay inside the radius for a set time."
                        },
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (vm.trigger == Trigger.DWELL) {
                        OutlinedTextField(
                            value = vm.dwellText, onValueChange = { vm.dwellText = it }, label = { Text("Stay for (minutes)") },
                            singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(), colors = fieldColors(), shape = MaterialTheme.shapes.medium,
                            isError = errorFor(Validation.Field.DWELL) != null,
                            supportingText = errorFor(Validation.Field.DWELL)?.let { { Text(it) } },
                        )
                    }
                }

                SectionCard("How often") {
                    OptionRow(vm.frequency == Frequency.ONE_TIME, { vm.frequency = Frequency.ONE_TIME }, GlyphKind.ONCE, "Once", "Turns itself off after it fires.")
                    OptionRow(vm.frequency == Frequency.RECURRING, { vm.frequency = Frequency.RECURRING }, GlyphKind.REPEAT, "Every time", "Fires again each time it happens.")
                }

                SectionCard("Alert level") {
                    OptionRow(vm.level == Level.SILENT, { vm.level = Level.SILENT }, GlyphKind.SILENT, "Silent", "No sound, shows in your notifications.")
                    OptionRow(vm.level == Level.NORMAL, { vm.level = Level.NORMAL }, GlyphKind.NORMAL, "Normal", "Notification with your usual sound.")
                    OptionRow(vm.level == Level.ALARM, { vm.level = Level.ALARM }, GlyphKind.ALARM, "Alarm", "Rings over the lockscreen until dismissed.")
                }

                if (vm.trigger != Trigger.EXIT) {
                    SectionCard("Early warning") {
                        Row(
                            Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).clickable { vm.toggleHeadsUp(!vm.headsUp) },
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = vm.headsUp, onCheckedChange = { vm.toggleHeadsUp(it) },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary, checkmarkColor = MaterialTheme.colorScheme.onPrimary,
                                    uncheckedColor = MaterialTheme.colorScheme.outline,
                                ),
                            )
                            Column(Modifier.padding(start = 4.dp)) {
                                Text("Remind me before I arrive", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "A second, wider ring gives you an early warning while driving.",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (vm.headsUp) {
                            OutlinedTextField(
                                value = vm.headsUpKmText, onValueChange = { vm.headsUpKmText = it; vm.requestFit() },
                                label = { Text("Warn me at (km)") }, singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(), colors = fieldColors(), shape = MaterialTheme.shapes.medium,
                                isError = errorFor(Validation.Field.HEADS_UP) != null,
                                supportingText = errorFor(Validation.Field.HEADS_UP)?.let { { Text(it) } },
                            )
                        }
                    }
                }
            }
        }
    }
}
