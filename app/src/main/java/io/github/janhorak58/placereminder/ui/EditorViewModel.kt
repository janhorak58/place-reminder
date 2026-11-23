package io.github.janhorak58.placereminder.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.janhorak58.placereminder.data.AppDatabase
import io.github.janhorak58.placereminder.data.Frequency
import io.github.janhorak58.placereminder.data.GeofenceBudget
import io.github.janhorak58.placereminder.data.Level
import io.github.janhorak58.placereminder.data.Limits
import io.github.janhorak58.placereminder.data.RadiusScale
import io.github.janhorak58.placereminder.data.Reminder
import io.github.janhorak58.placereminder.data.Trigger
import io.github.janhorak58.placereminder.data.Validation
import io.github.janhorak58.placereminder.geofence.GeofenceManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

/** Editor form state. [reminderId] 0 creates a new reminder. */
class EditorViewModel(app: Application, private val reminderId: Long) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).reminderDao()
    private var original: Reminder? = null

    val isNew get() = reminderId == 0L

    var loaded by mutableStateOf(reminderId == 0L)
        private set

    var name by mutableStateOf("")
    var message by mutableStateOf("")
    var latitude by mutableStateOf<Double?>(null)
    var longitude by mutableStateOf<Double?>(null)
    var radiusText by mutableStateOf(Limits.DEFAULT_RADIUS_M.toString())
    var radiusInKm by mutableStateOf(false)
    var trigger by mutableStateOf(Trigger.ENTER)
    var dwellText by mutableStateOf(Limits.DEFAULT_DWELL_MIN.toString())
    var frequency by mutableStateOf(Frequency.ONE_TIME)
    var level by mutableStateOf(Level.NORMAL)
    var headsUp by mutableStateOf(false)
    var headsUpKmText by mutableStateOf("10")
    var error by mutableStateOf<Validation.Error?>(null)

    /** Bumped when the map should re-fit to the circles. */
    var fitTick by mutableIntStateOf(0)
        private set

    init {
        if (reminderId != 0L) {
            viewModelScope.launch {
                dao.get(reminderId)?.let { r ->
                    original = r
                    name = r.name
                    message = r.message
                    latitude = r.latitude
                    longitude = r.longitude
                    radiusInKm = r.radiusMeters >= 1000 && r.radiusMeters % 1000 == 0
                    radiusText = formatRadiusText(r.radiusMeters, radiusInKm)
                    trigger = r.trigger
                    dwellText = r.dwellMinutes.toString()
                    frequency = r.frequency
                    level = r.level
                    headsUp = r.headsUpEnabled
                    headsUpKmText = formatPlain(r.headsUpMeters / 1000.0)
                    fitTick++
                }
                loaded = true
            }
        }
    }

    val radiusMeters: Int? get() = parseDistance(radiusText, if (radiusInKm) 1000.0 else 1.0)
    val headsUpMeters: Int? get() = parseDistance(headsUpKmText, 1000.0)
    val hasLocation get() = latitude != null && longitude != null

    fun setLocation(lat: Double, lon: Double) {
        latitude = lat
        longitude = lon
        fitTick++
    }

    // --- Address search -------------------------------------------------------------------

    private val places = PlaceSearch(app)
    private var searchJob: Job? = null

    /** False when the device has no geocoder, so the search field is hidden. */
    val searchAvailable get() = places.available

    var searchQuery by mutableStateOf("")
        private set
    var searchState by mutableStateOf<SearchState>(SearchState.Idle)
        private set

    /** Called as the user types. Waits for a pause, and ignores queries that are too short. */
    fun onSearchQuery(text: String, near: Pair<Double, Double>?) {
        searchQuery = text
        val q = text.trim()
        searchJob?.cancel()
        if (q.length < MIN_SEARCH_CHARS) {
            searchState = SearchState.Idle
            return
        }
        searchState = SearchState.Loading
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            runSearch(q, near)
        }
    }

    /** Called on the keyboard Search action: no debounce, no minimum length beyond one character. */
    fun submitSearch(near: Pair<Double, Double>?) {
        val q = searchQuery.trim()
        searchJob?.cancel()
        if (q.isEmpty()) {
            searchState = SearchState.Idle
            return
        }
        searchState = SearchState.Loading
        searchJob = viewModelScope.launch { runSearch(q, near) }
    }

    private suspend fun runSearch(q: String, near: Pair<Double, Double>?) {
        searchState = try {
            val found = places.search(q, near)
            if (found.isEmpty()) SearchState.Empty else SearchState.Results(found)
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            SearchState.Unavailable
        } catch (e: Exception) {
            SearchState.Unavailable
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        searchQuery = ""
        searchState = SearchState.Idle
    }

    /** Places the pin on a result, fills the name only when it is still empty, and collapses the list. */
    fun pickPlace(r: PlaceResult) {
        searchJob?.cancel()
        setLocation(r.lat, r.lon)
        if (name.isBlank()) name = r.title
        searchQuery = r.title
        searchState = SearchState.Idle
    }

    fun setRadiusMeters(m: Int) {
        radiusText = formatRadiusText(m, radiusInKm)
    }

    fun setRadiusUnit(km: Boolean) {
        val m = radiusMeters
        radiusInKm = km
        if (m != null) radiusText = formatRadiusText(m, km)
    }

    fun toggleHeadsUp(on: Boolean) {
        headsUp = on
        val r = radiusMeters ?: return
        val h = headsUpMeters
        if (on && (h == null || h <= r)) {
            val km = ceil(r * 2 / 1000.0).coerceIn(1.0, 100.0)
            headsUpKmText = formatPlain(km)
        }
    }

    fun sliderPosition(): Float = RadiusScale.toPosition(radiusMeters ?: Limits.DEFAULT_RADIUS_M)

    fun requestFit() {
        fitTick++
    }

    private fun draft(): Reminder = (original ?: Reminder()).copy(
        name = name.trim(),
        message = message.trim(),
        latitude = latitude ?: 0.0,
        longitude = longitude ?: 0.0,
        radiusMeters = radiusMeters ?: -1,
        trigger = trigger,
        dwellMinutes = dwellText.trim().toIntOrNull() ?: -1,
        frequency = frequency,
        level = level,
        headsUpEnabled = headsUp,
        headsUpMeters = headsUpMeters ?: -1,
    )

    /** The current form validation problem, or null when the form can be saved. */
    val liveError: Validation.Error? get() = Validation.validate(draft(), hasLocation) as? Validation.Error

    /** Validates and stores the reminder. Calls [onDone] on success. */
    fun save(onDone: () -> Unit) {
        val draft = draft()
        val result = Validation.validate(draft, hasLocation)
        if (result is Validation.Error) {
            error = result
            return
        }
        error = null
        viewModelScope.launch {
            if (!GeofenceBudget.fits(dao.getAll(), draft)) {
                error = Validation.Error(Validation.Field.HEADS_UP, LIMIT_MESSAGE)
                return@launch
            }
            val id = if (original == null) dao.insert(draft) else draft.id.also { dao.update(draft) }
            GeofenceManager.update(getApplication(), listOf(id))
            onDone()
        }
    }

    companion object {
        fun formatRadiusText(meters: Int, km: Boolean) = if (km) formatPlain(meters / 1000.0) else meters.toString()

        fun formatPlain(v: Double): String {
            val s = String.format(Locale.US, "%.2f", v).trimEnd('0').trimEnd('.')
            return s.ifEmpty { "0" }
        }

        fun parseDistance(text: String, factor: Double): Int? {
            val v = text.trim().replace(',', '.').toDoubleOrNull() ?: return null
            if (v.isNaN() || v.isInfinite() || v < 0) return null
            return (v * factor).roundToInt()
        }
    }
}

sealed interface SearchState {
    data object Idle : SearchState
    data object Loading : SearchState
    data object Empty : SearchState
    data object Unavailable : SearchState
    data class Results(val places: List<PlaceResult>) : SearchState
}

private const val MIN_SEARCH_CHARS = 3
private const val SEARCH_DEBOUNCE_MS = 400L
