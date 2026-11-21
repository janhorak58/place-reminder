package io.github.janhorak58.placereminder.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import io.github.janhorak58.placereminder.data.AppDatabase
import io.github.janhorak58.placereminder.data.FireRules
import io.github.janhorak58.placereminder.data.Reminder
import io.github.janhorak58.placereminder.data.Trigger
import io.github.janhorak58.placereminder.notify.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.w(TAG, "geofence event error ${GeofenceStatusCodes.getStatusCodeString(event.errorCode)}")
            return
        }
        val transition = event.geofenceTransition
        val ids = event.triggeringGeofences.orEmpty().map { it.requestId }
        Log.i(TAG, "geofence event transition=${transitionName(transition)} ids=$ids")
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val disabled = mutableSetOf<Long>()
                for (requestId in ids) {
                    val (id, headsUp) = GeofenceManager.parseId(requestId) ?: continue
                    if (handle(app, id, headsUp, transition)) disabled += id
                }
                if (disabled.isNotEmpty()) GeofenceManager.update(app, disabled)
            } finally {
                pending.finish()
            }
        }
    }

    /** Returns true if the reminder was disabled and its geofences must be removed. */
    private suspend fun handle(context: Context, id: Long, headsUp: Boolean, transition: Int): Boolean {
        val dao = AppDatabase.get(context).reminderDao()
        val r = dao.get(id) ?: return false
        val now = System.currentTimeMillis()
        if (headsUp) {
            if (transition != Geofence.GEOFENCE_TRANSITION_ENTER) return false
            if (!FireRules.shouldFireHeadsUp(r, now)) {
                Log.i(TAG, "reminder $id heads-up suppressed")
                return false
            }
            dao.update(r.copy(headsUpLastFiredAt = now))
            Notifier.notifyReminder(context, r, headsUp = true)
            Log.i(TAG, "reminder $id heads-up FIRED")
            return false
        }
        if (transition != expectedTransition(r)) return false
        if (!FireRules.shouldFireMain(r, now)) {
            Log.i(TAG, "reminder $id main suppressed")
            return false
        }
        val updated = FireRules.afterMainFired(r, now)
        dao.update(updated)
        Notifier.notifyReminder(context, r, headsUp = false)
        Log.i(TAG, "reminder $id main FIRED, enabled=${updated.enabled}")
        return !updated.enabled
    }

    private fun expectedTransition(r: Reminder) = when (r.trigger) {
        Trigger.ENTER -> Geofence.GEOFENCE_TRANSITION_ENTER
        Trigger.EXIT -> Geofence.GEOFENCE_TRANSITION_EXIT
        Trigger.DWELL -> Geofence.GEOFENCE_TRANSITION_DWELL
    }

    private fun transitionName(t: Int) = when (t) {
        Geofence.GEOFENCE_TRANSITION_ENTER -> "ENTER"
        Geofence.GEOFENCE_TRANSITION_EXIT -> "EXIT"
        Geofence.GEOFENCE_TRANSITION_DWELL -> "DWELL"
        else -> t.toString()
    }
}
