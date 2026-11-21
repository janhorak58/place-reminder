package io.github.janhorak58.placereminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.janhorak58.placereminder.data.AppDatabase
import io.github.janhorak58.placereminder.data.Frequency
import io.github.janhorak58.placereminder.data.Level
import io.github.janhorak58.placereminder.data.Reminder
import io.github.janhorak58.placereminder.data.Trigger
import io.github.janhorak58.placereminder.geofence.GeofenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Debug-only test helper. Not part of release builds.
 * adb shell am broadcast -n <pkg>/.DebugSeedReceiver --es name X --ed lat 50.0 --ed lon 14.4 --ei radius 1000 \
 *   --es trigger ENTER --es freq ONE_TIME --es level NORMAL --ez headsup true --ei headsupm 10000 --ei dwell 1
 * Use --ez clear true to delete all reminders.
 */
class DebugSeedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val dao = AppDatabase.get(app).reminderDao()
                if (intent.getBooleanExtra("clear", false)) {
                    dao.getAll().forEach { dao.delete(it.id) }
                } else {
                    val newId = dao.insert(
                        Reminder(
                            name = intent.getStringExtra("name") ?: "Test",
                            message = intent.getStringExtra("message") ?: "Test message",
                            latitude = intent.getDoubleExtra("lat", 0.0),
                            longitude = intent.getDoubleExtra("lon", 0.0),
                            radiusMeters = intent.getIntExtra("radius", 1000),
                            trigger = Trigger.valueOf(intent.getStringExtra("trigger") ?: "ENTER"),
                            dwellMinutes = intent.getIntExtra("dwell", 1),
                            frequency = Frequency.valueOf(intent.getStringExtra("freq") ?: "ONE_TIME"),
                            level = Level.valueOf(intent.getStringExtra("level") ?: "NORMAL"),
                            headsUpEnabled = intent.getBooleanExtra("headsup", false),
                            headsUpMeters = intent.getIntExtra("headsupm", 10_000),
                        ),
                    )
                    android.util.Log.i(io.github.janhorak58.placereminder.geofence.TAG, "seeded id=$newId")
                }
                GeofenceManager.sync(app)
            } finally {
                pending.finish()
            }
        }
    }
}
