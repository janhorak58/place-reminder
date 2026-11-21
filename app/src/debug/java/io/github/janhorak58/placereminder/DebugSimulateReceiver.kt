package io.github.janhorak58.placereminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.common.internal.safeparcel.SafeParcelable
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.location.Geofence
import io.github.janhorak58.placereminder.geofence.GeofenceManager
import io.github.janhorak58.placereminder.geofence.GeofenceReceiver

/**
 * Debug-only: feeds a synthetic geofence transition into the real [GeofenceReceiver], for emulators where
 * Play Services does not evaluate geofences. Not part of release builds.
 * adb shell am broadcast -n <pkg>/.DebugSimulateReceiver --el id 1 --ez headsup false --ei transition 1
 */
class DebugSimulateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra("id", 0)
        val requestId = if (intent.getBooleanExtra("headsup", false)) GeofenceManager.headsUpId(id) else GeofenceManager.mainId(id)
        val transition = intent.getIntExtra("transition", Geofence.GEOFENCE_TRANSITION_ENTER)
        val fence = Geofence.Builder()
            .setRequestId(requestId)
            .setCircularRegion(0.0, 0.0, 100f)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(transition)
            .apply { if (transition == Geofence.GEOFENCE_TRANSITION_DWELL) setLoiteringDelay(1000) }
            .build()
        val bytes = SafeParcelableSerializer.serializeToBytes(fence as SafeParcelable)
        val event = Intent(context, GeofenceReceiver::class.java)
            .putExtra("com.google.android.location.intent.extra.transition", transition)
            .putExtra("com.google.android.location.intent.extra.geofence_list", arrayListOf(bytes))
        context.sendBroadcast(event)
    }
}
