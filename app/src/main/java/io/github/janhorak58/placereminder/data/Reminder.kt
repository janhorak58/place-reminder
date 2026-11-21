package io.github.janhorak58.placereminder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class Trigger { ENTER, EXIT, DWELL }
enum class Frequency { ONE_TIME, RECURRING }
enum class Level { SILENT, NORMAL, ALARM }

/** Placeholder for future third-party actions (Tasker, webhook, ...). */
enum class Action { NOTIFY }

@Entity(tableName = "reminders")
data class Reminder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",
    val message: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val radiusMeters: Int = Limits.DEFAULT_RADIUS_M,
    val trigger: Trigger = Trigger.ENTER,
    val dwellMinutes: Int = Limits.DEFAULT_DWELL_MIN,
    val frequency: Frequency = Frequency.ONE_TIME,
    val level: Level = Level.NORMAL,
    val headsUpEnabled: Boolean = false,
    val headsUpMeters: Int = Limits.DEFAULT_HEADS_UP_M,
    val action: Action = Action.NOTIFY,
    val enabled: Boolean = true,
    val lastFiredAt: Long? = null,
    val headsUpLastFiredAt: Long? = null,
) {
    /** Heads-up is only meaningful for ENTER and DWELL. */
    val usesHeadsUp: Boolean get() = headsUpEnabled && trigger != Trigger.EXIT

    /** Number of geofences this reminder occupies while enabled. */
    val geofenceCount: Int get() = if (usesHeadsUp) 2 else 1
}

object Limits {
    const val MIN_RADIUS_M = 50
    const val MAX_RADIUS_M = 50_000
    const val DEFAULT_RADIUS_M = 1_000
    const val MIN_DWELL_MIN = 1
    const val MAX_DWELL_MIN = 120
    const val DEFAULT_DWELL_MIN = 10
    const val MIN_HEADS_UP_M = 1_000
    const val MAX_HEADS_UP_M = 100_000
    const val DEFAULT_HEADS_UP_M = 10_000
    const val MAX_GEOFENCES = 100
    const val GUARD_MS = 15 * 60_000L
    const val UNRELIABLE_BELOW_M = 100
}
