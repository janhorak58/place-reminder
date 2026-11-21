package io.github.janhorak58.placereminder.data

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

/** Pure decision logic for geofence events. */
object FireRules {
    /** Main alert: one-time fires while enabled; recurring additionally needs the 15 min guard to have passed. */
    fun shouldFireMain(r: Reminder, now: Long): Boolean {
        if (!r.enabled) return false
        if (r.frequency == Frequency.ONE_TIME) return true
        return guardPassed(r.lastFiredAt, now)
    }

    /** Heads-up: one-time fires at most once total; recurring is limited by the 15 min guard. */
    fun shouldFireHeadsUp(r: Reminder, now: Long): Boolean {
        if (!r.enabled || !r.usesHeadsUp) return false
        val last = r.headsUpLastFiredAt ?: return true
        return r.frequency == Frequency.RECURRING && guardPassed(last, now)
    }

    fun guardPassed(last: Long?, now: Long): Boolean = last == null || now - last >= Limits.GUARD_MS

    /** Reminder state after the main alert fired. */
    fun afterMainFired(r: Reminder, now: Long): Reminder =
        r.copy(lastFiredAt = now, enabled = if (r.frequency == Frequency.ONE_TIME) false else r.enabled)

    /** Reminder state after being switched on again: heads-up state is reset. */
    fun afterReEnabled(r: Reminder): Reminder = r.copy(enabled = true, headsUpLastFiredAt = null)
}

/** Logarithmic mapping between slider position (0..1) and radius in metres. */
object RadiusScale {
    private val lnMin = ln(Limits.MIN_RADIUS_M.toDouble())
    private val lnMax = ln(Limits.MAX_RADIUS_M.toDouble())

    fun toMeters(position: Float): Int {
        val p = position.coerceIn(0f, 1f)
        val raw = exp(lnMin + (lnMax - lnMin) * p)
        return roundNice(raw).coerceIn(Limits.MIN_RADIUS_M, Limits.MAX_RADIUS_M)
    }

    fun toPosition(meters: Int): Float {
        val m = meters.coerceIn(Limits.MIN_RADIUS_M, Limits.MAX_RADIUS_M)
        return ((ln(m.toDouble()) - lnMin) / (lnMax - lnMin)).toFloat()
    }

    /** Rounds to 2 significant digits so slider values read cleanly (e.g. 1200 m, not 1187 m). */
    private fun roundNice(v: Double): Int {
        val step = when {
            v < 100 -> 5.0
            v < 1_000 -> 10.0
            v < 10_000 -> 100.0
            else -> 1_000.0
        }
        return (Math.round(v / step) * step).roundToInt()
    }
}

object GeofenceBudget {
    /** Geofences used by all enabled reminders except [excludeId]. */
    fun used(all: List<Reminder>, excludeId: Long? = null): Int =
        all.filter { it.enabled && it.id != excludeId }.sumOf { it.geofenceCount }

    fun fits(all: List<Reminder>, candidate: Reminder): Boolean =
        !candidate.enabled || used(all, candidate.id) + candidate.geofenceCount <= Limits.MAX_GEOFENCES
}

sealed interface Validation {
    data object Ok : Validation
    data class Error(val field: Field, val message: String) : Validation

    enum class Field { NAME, LOCATION, RADIUS, DWELL, HEADS_UP }

    companion object {
        fun validate(r: Reminder, hasLocation: Boolean): Validation = when {
            r.name.isBlank() -> Error(Field.NAME, "Enter a name")
            !hasLocation -> Error(Field.LOCATION, "Pick a location on the map")
            r.radiusMeters !in Limits.MIN_RADIUS_M..Limits.MAX_RADIUS_M ->
                Error(Field.RADIUS, "Radius must be between 50 m and 50 km")
            r.trigger == Trigger.DWELL && r.dwellMinutes !in Limits.MIN_DWELL_MIN..Limits.MAX_DWELL_MIN ->
                Error(Field.DWELL, "Stay time must be 1 to 120 minutes")
            r.usesHeadsUp && r.headsUpMeters !in Limits.MIN_HEADS_UP_M..Limits.MAX_HEADS_UP_M ->
                Error(Field.HEADS_UP, "Heads-up distance must be 1 to 100 km")
            r.usesHeadsUp && r.headsUpMeters <= r.radiusMeters ->
                Error(Field.HEADS_UP, "Heads-up distance must be larger than the radius")
            else -> Ok
        }
    }
}
