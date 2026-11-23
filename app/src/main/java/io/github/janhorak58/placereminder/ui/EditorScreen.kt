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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    val c = ios
    val live = vm.liveError
    // Name and location are obvious from an empty form, so only the numeric fields report live.
    fun shown(f: Validation.Field) = (error ?: live?.takeIf { it.field != Validation.Field.NAME && it.field != Validation.Field.LOCATION })
        ?.takeIf { it.field == f }?.message
    val scroll = rememberScrollState()
    val canSave = vm.loaded && live == null

    Column(Modifier.fillMaxSize().background(c.background).imePadding()) {
        IosNavBar(
            title = if (vm.isNew) "New reminder" else "Edit reminder",
            progress = BarProgress(if (scroll.value > 0) 1f else 0f, 1f),
            leading = { IosTextButton("Cancel", onClose) },
            trailing = { IosTextButton("Save", { vm.save(onClose) }, enabled = canSave, bold = true) },
        )
        if (!vm.loaded) return@Column
        Column(
            Modifier.weight(1f).verticalScroll(scroll).navigationBarsPadding().padding(top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column {
            if (vm.searchAvailable) {
                val focus = LocalFocusManager.current
                val near = if (vm.hasLocation) Pair(vm.latitude!!, vm.longitude!!) else startCenter?.let { Pair(it.latitude, it.longitude) }
                SearchField(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    query = vm.searchQuery,
                    onQuery = { vm.onSearchQuery(it, near) },
                    onSubmit = { vm.submitSearch(near) },
                    onClear = { vm.clearSearch() },
                )
                SearchResults(vm.searchState) { place ->
                    vm.pickPlace(place)
                    focus.clearFocus()
                }
            }
                Column(Modifier.padding(horizontal = 16.dp)) {
                Box(Modifier.fillMaxWidth().height(300.dp).clip(RoundedCornerShape(12.dp))) {
                    LocationMap(
                        modifier = Modifier.fillMaxSize(),
                        point = if (vm.hasLocation) GeoPoint(vm.latitude!!, vm.longitude!!) else null,
                        radiusMeters = vm.radiusMeters?.takeIf { it > 0 },
                        headsUpMeters = if (vm.headsUp && vm.trigger != Trigger.EXIT) vm.headsUpMeters?.takeIf { it > 0 } else null,
                        fitKey = vm.fitTick,
                        startCenter = startCenter,
                        onPick = { vm.setLocation(it.latitude, it.longitude) },
                    )
                    Row(
                        Modifier.align(Alignment.BottomStart).padding(10.dp).clip(CircleShape).background(c.elevated)
                            .clickable { if (hasFine()) useCurrentLocation() else permission.launch(Manifest.permission.ACCESS_FINE_LOCATION) }
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Glyph(GlyphKind.LOCATE, c.tint, 16.dp)
                        Text("Use my location", style = MaterialTheme.typography.titleSmall, color = c.tint)
                    }
                }
                val locationError = errorFor(Validation.Field.LOCATION)
                Text(
                    locationError ?: "Tap or long-press the map to place the pin.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (locationError != null) c.red else c.secondary,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 7.dp),
                )
                }
            }

            IosSection(error = errorFor(Validation.Field.NAME)) {
                IosRow { IosTextField(vm.name, { vm.name = it }, "Name", Modifier.fillMaxWidth()) }
                IosDivider()
                IosRow(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 11.dp)) {
                    IosTextField(vm.message, { vm.message = it }, "Message", Modifier.fillMaxWidth(), singleLine = false)
                }
            }

            val radius = vm.radiusMeters
            IosSection(
                header = "Radius",
                footer = "How close you need to be for the reminder to fire.",
                error = shown(Validation.Field.RADIUS)
                    ?: if (radius != null && radius < Limits.UNRELIABLE_BELOW_M) "Below 100 m, alerts can be unreliable" else null,
            ) {
                IosRow {
                    Text("Distance", style = MaterialTheme.typography.bodyLarge, color = c.label, modifier = Modifier.weight(1f))
                    IosTextField(
                        vm.radiusText, { vm.radiusText = it; vm.requestFit() }, "0", Modifier.width(84.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textAlign = TextAlign.End,
                    )
                    Spacer(Modifier.width(12.dp))
                    IosSegmented(listOf("m", "km"), if (vm.radiusInKm) 1 else 0, { vm.setRadiusUnit(it == 1) }, Modifier.width(96.dp))
                }
                IosDivider()
                IosRow(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 2.dp)) {
                    val sliderColors = SliderDefaults.colors(activeTrackColor = c.tint, inactiveTrackColor = c.segmentTrack)
                    Slider(
                        value = vm.sliderPosition(),
                        onValueChange = { vm.setRadiusMeters(RadiusScale.toMeters(it)) },
                        onValueChangeFinished = { vm.requestFit() },
                        colors = sliderColors,
                        thumb = { Box(Modifier.size(28.dp).shadow(3.dp, CircleShape).background(Color.White, CircleShape)) },
                        track = { state ->
                            SliderDefaults.Track(
                                state, colors = sliderColors, thumbTrackGapSize = 0.dp, trackInsideCornerSize = 0.dp,
                                drawStopIndicator = null, modifier = Modifier.height(4.dp),
                            )
                        },
                    )
                }
            }

            IosSection(
                header = "Fires when you",
                footer = when (vm.trigger) {
                    Trigger.ENTER -> "Fires as soon as you get inside the radius."
                    Trigger.EXIT -> "Fires when you leave the radius."
                    Trigger.DWELL -> "Fires after you stay inside the radius for a set time."
                },
                error = shown(Validation.Field.DWELL),
            ) {
                IosRow(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                    val options = listOf(Trigger.ENTER to "Arrive", Trigger.EXIT to "Leave", Trigger.DWELL to "Stay")
                    IosSegmented(
                        options.map { it.second }, options.indexOfFirst { it.first == vm.trigger },
                        { vm.trigger = options[it].first }, Modifier.fillMaxWidth(),
                    )
                }
                if (vm.trigger == Trigger.DWELL) {
                    IosDivider()
                    IosRow {
                        Text("Stay for", style = MaterialTheme.typography.bodyLarge, color = c.label, modifier = Modifier.weight(1f))
                        IosTextField(
                            vm.dwellText, { vm.dwellText = it }, "10", Modifier.width(60.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), textAlign = TextAlign.End,
                        )
                        Text(" min", style = MaterialTheme.typography.bodyLarge, color = c.secondary)
                    }
                }
            }

            IosSection(
                header = "How often",
                footer = when (vm.frequency) {
                    Frequency.ONE_TIME -> "Turns itself off after it fires."
                    Frequency.RECURRING -> "Fires again each time it happens."
                },
            ) {
                IosRow(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                    IosSegmented(
                        listOf("Once", "Every time"), if (vm.frequency == Frequency.ONE_TIME) 0 else 1,
                        { vm.frequency = if (it == 0) Frequency.ONE_TIME else Frequency.RECURRING }, Modifier.fillMaxWidth(),
                    )
                }
            }

            IosSection(
                header = "Alert level",
                footer = when (vm.level) {
                    Level.SILENT -> "No sound. Shows in your notifications."
                    Level.NORMAL -> "A notification with your usual sound."
                    Level.ALARM -> "Rings over the lockscreen until you dismiss it."
                },
            ) {
                IosRow(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                    val options = listOf(Level.SILENT to "Silent", Level.NORMAL to "Normal", Level.ALARM to "Alarm")
                    IosSegmented(
                        options.map { it.second }, options.indexOfFirst { it.first == vm.level },
                        { vm.level = options[it].first }, Modifier.fillMaxWidth(),
                    )
                }
            }

            if (vm.trigger != Trigger.EXIT) {
                IosSection(
                    header = "Early warning",
                    footer = "A second, wider ring gives you an early warning while driving.",
                    error = shown(Validation.Field.HEADS_UP),
                ) {
                    IosRow(onClick = { vm.toggleHeadsUp(!vm.headsUp) }) {
                        Text("Remind me before I arrive", style = MaterialTheme.typography.bodyLarge, color = c.label, modifier = Modifier.weight(1f))
                        IosSwitch(vm.headsUp, { vm.toggleHeadsUp(it) })
                    }
                    if (vm.headsUp) {
                        IosDivider()
                        IosRow {
                            Text("Warn me at", style = MaterialTheme.typography.bodyLarge, color = c.label, modifier = Modifier.weight(1f))
                            IosTextField(
                                vm.headsUpKmText, { vm.headsUpKmText = it; vm.requestFit() }, "10", Modifier.width(60.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), textAlign = TextAlign.End,
                            )
                            Text(" km", style = MaterialTheme.typography.bodyLarge, color = c.secondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(modifier: Modifier, query: String, onQuery: (String) -> Unit, onSubmit: () -> Unit, onClear: () -> Unit) {
    val c = ios
    Row(
        modifier.fillMaxWidth().padding(bottom = 10.dp).height(40.dp).clip(RoundedCornerShape(10.dp)).background(c.segmentTrack)
            .padding(start = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Glyph(GlyphKind.MAGNIFY, c.secondary, 18.dp)
        IosTextField(
            query, onQuery, "Search for an address or place",
            Modifier.weight(1f).padding(horizontal = 8.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
        )
        if (query.isNotEmpty()) {
            BarIconButton(GlyphKind.CLEAR, "Clear search", onClear, tint = c.secondary, size = 16.dp)
        } else {
            Spacer(Modifier.width(10.dp))
        }
    }
}

@Composable
private fun SearchResults(state: SearchState, onPick: (PlaceResult) -> Unit) {
    val c = ios
    if (state is SearchState.Idle) return
    IosSection(modifier = Modifier.padding(bottom = 10.dp)) {
        when (state) {
            is SearchState.Results -> state.places.forEachIndexed { i, p ->
                if (i > 0) IosDivider()
                IosRow(onClick = { onPick(p) }) {
                    Glyph(GlyphKind.PIN, c.tint, 20.dp)
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(p.title, style = MaterialTheme.typography.bodyLarge, color = c.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (p.subtitle.isNotEmpty()) {
                            Text(p.subtitle, style = MaterialTheme.typography.bodySmall, color = c.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            SearchState.Loading -> IosRow {
                CircularProgressIndicator(Modifier.size(18.dp), color = c.secondary, strokeWidth = 2.dp)
                Text("Searching", style = MaterialTheme.typography.bodyLarge, color = c.secondary, modifier = Modifier.padding(start = 12.dp))
            }
            SearchState.Empty -> IosRow { Text("No results", style = MaterialTheme.typography.bodyLarge, color = c.secondary) }
            SearchState.Unavailable -> IosRow {
                Text("Search unavailable, check your connection", style = MaterialTheme.typography.bodyLarge, color = c.red)
            }
            SearchState.Idle -> {}
        }
    }
}
