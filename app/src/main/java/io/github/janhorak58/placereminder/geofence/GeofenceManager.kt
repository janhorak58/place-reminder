package io.github.janhorak58.placereminder.geofence

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import io.github.janhorak58.placereminder.data.AppDatabase
import io.github.janhorak58.placereminder.data.Reminder
import io.github.janhorak58.placereminder.data.Trigger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

const val TAG = "PlaceReminder"

/** Result of the last geofence registration, surfaced in the UI. */
enum class GeofenceState { OK, NO_PERMISSION, LOCATION_OFF, ERROR }

object GeofenceManager {
    private const val RESPONSIVENESS_MS = 60_000

    private val mutex = Mutex()
    private val _state = MutableStateFlow(GeofenceState.OK)
    val state: StateFlow<GeofenceState> = _state

    fun mainId(id: Long) = "$id:main"
    fun headsUpId(id: Long) = "$id:pre"

    /** Parses a request id into (reminderId, isHeadsUp). */
    fun parseId(requestId: String): Pair<Long, Boolean>? {
        val parts = requestId.split(':')
        if (parts.size != 2) return null
        val id = parts[0].toLongOrNull() ?: return null
        return when (parts[1]) {
            "main" -> id to false
            "pre" -> id to true
            else -> null
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, GeofenceReceiver::class.java)
        // Play Services fills in the event extras, so the PendingIntent must be mutable.
        return PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }

    private fun hasLocationPermission(context: Context) =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Replaces all registered geofences with those of the enabled reminders (boot, update, app start). */
    suspend fun sync(context: Context) {
        val app = context.applicationContext
        mutex.withLock {
            val client = LocationServices.getGeofencingClient(app)
            val pi = pendingIntent(app)
            try {
                client.removeGeofences(pi).await()
            } catch (e: Exception) {
                Log.w(TAG, "removeGeofences failed: $e")
            }
            register(app, client, pi, AppDatabase.get(app).reminderDao().getEnabled(), "sync")
        }
    }

    /**
     * Re-registers only the given reminders (removing their fences, adding them back if still enabled).
     * Other reminders keep their fence state, so a dwell in progress is not reset by unrelated edits.
     */
    suspend fun update(context: Context, ids: Collection<Long>) {
        val app = context.applicationContext
        if (_state.value != GeofenceState.OK) return sync(app)
        mutex.withLock {
            val client = LocationServices.getGeofencingClient(app)
            val pi = pendingIntent(app)
            val requestIds = ids.flatMap { listOf(mainId(it), headsUpId(it)) }
            try {
                client.removeGeofences(requestIds).await()
            } catch (e: Exception) {
                Log.w(TAG, "removeGeofences($requestIds) failed: $e")
            }
            val dao = AppDatabase.get(app).reminderDao()
            val enabled = ids.mapNotNull { dao.get(it) }.filter { it.enabled }
            register(app, client, pi, enabled, "update $ids")
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun register(
        app: Context,
        client: com.google.android.gms.location.GeofencingClient,
        pi: PendingIntent,
        reminders: List<Reminder>,
        what: String,
    ) {
        if (reminders.isEmpty()) {
            Log.i(TAG, "$what: nothing to register")
            return
        }
        if (!hasLocationPermission(app)) {
            _state.value = GeofenceState.NO_PERMISSION
            Log.w(TAG, "$what: location permission missing")
            return
        }
        val fences = reminders.flatMap { build(it) }
        val request = GeofencingRequest.Builder()
            .setInitialTrigger(0)
            .addGeofences(fences)
            .build()
        try {
            client.addGeofences(request, pi).await()
            _state.value = GeofenceState.OK
            Log.i(TAG, "$what: registered ${fences.size} geofences for ${reminders.size} reminders")
        } catch (e: ApiException) {
            _state.value = if (e.statusCode == GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE) {
                GeofenceState.LOCATION_OFF
            } else {
                GeofenceState.ERROR
            }
            Log.w(TAG, "$what: addGeofences failed ${GeofenceStatusCodes.getStatusCodeString(e.statusCode)}")
        } catch (e: SecurityException) {
            _state.value = GeofenceState.NO_PERMISSION
            Log.w(TAG, "$what: security exception $e")
        }
    }

    private fun build(r: Reminder): List<Geofence> {
        val main = Geofence.Builder()
            .setRequestId(mainId(r.id))
            .setCircularRegion(r.latitude, r.longitude, r.radiusMeters.toFloat())
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setNotificationResponsiveness(RESPONSIVENESS_MS)
            .apply {
                when (r.trigger) {
                    Trigger.ENTER -> setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    Trigger.EXIT -> setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
                    Trigger.DWELL -> {
                        setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL)
                        setLoiteringDelay(r.dwellMinutes * 60_000)
                    }
                }
            }
            .build()
        if (!r.usesHeadsUp) return listOf(main)
        val pre = Geofence.Builder()
            .setRequestId(headsUpId(r.id))
            .setCircularRegion(r.latitude, r.longitude, r.headsUpMeters.toFloat())
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setNotificationResponsiveness(RESPONSIVENESS_MS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()
        return listOf(main, pre)
    }
}
