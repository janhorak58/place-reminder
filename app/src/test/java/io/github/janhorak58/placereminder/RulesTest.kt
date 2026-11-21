package io.github.janhorak58.placereminder

import io.github.janhorak58.placereminder.data.FireRules
import io.github.janhorak58.placereminder.data.Frequency
import io.github.janhorak58.placereminder.data.GeofenceBudget
import io.github.janhorak58.placereminder.data.Limits
import io.github.janhorak58.placereminder.data.RadiusScale
import io.github.janhorak58.placereminder.data.Reminder
import io.github.janhorak58.placereminder.data.Trigger
import io.github.janhorak58.placereminder.data.Validation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RulesTest {
    private val now = 10_000_000L
    private val min = 60_000L
    private val base = Reminder(id = 1, name = "x", latitude = 1.0, longitude = 1.0)

    @Test fun oneTimeFiresOnceThenDisables() {
        assertTrue(FireRules.shouldFireMain(base, now))
        val after = FireRules.afterMainFired(base, now)
        assertFalse(after.enabled)
        assertEquals(now, after.lastFiredAt)
        assertFalse(FireRules.shouldFireMain(after, now + 60 * min))
    }

    @Test fun recurringRespectsFifteenMinuteGuard() {
        val r = base.copy(frequency = Frequency.RECURRING, lastFiredAt = now)
        assertFalse(FireRules.shouldFireMain(r, now + 14 * min))
        assertTrue(FireRules.shouldFireMain(r, now + 15 * min))
        val after = FireRules.afterMainFired(r, now + 20 * min)
        assertTrue(after.enabled)
        assertEquals(now + 20 * min, after.lastFiredAt)
    }

    @Test fun recurringFirstFireHasNoGuard() {
        assertTrue(FireRules.shouldFireMain(base.copy(frequency = Frequency.RECURRING), now))
    }

    @Test fun disabledNeverFires() {
        assertFalse(FireRules.shouldFireMain(base.copy(enabled = false), now))
        assertFalse(FireRules.shouldFireHeadsUp(base.copy(enabled = false, headsUpEnabled = true), now))
    }

    @Test fun headsUpOneTimeFiresAtMostOnce() {
        val r = base.copy(headsUpEnabled = true)
        assertTrue(FireRules.shouldFireHeadsUp(r, now))
        assertFalse(FireRules.shouldFireHeadsUp(r.copy(headsUpLastFiredAt = now), now + 600 * min))
    }

    @Test fun headsUpRecurringUsesGuard() {
        val r = base.copy(frequency = Frequency.RECURRING, headsUpEnabled = true, headsUpLastFiredAt = now)
        assertFalse(FireRules.shouldFireHeadsUp(r, now + 10 * min))
        assertTrue(FireRules.shouldFireHeadsUp(r, now + 15 * min))
    }

    @Test fun headsUpNotForExit() {
        assertFalse(FireRules.shouldFireHeadsUp(base.copy(trigger = Trigger.EXIT, headsUpEnabled = true), now))
        assertTrue(FireRules.shouldFireHeadsUp(base.copy(trigger = Trigger.DWELL, headsUpEnabled = true), now))
    }

    @Test fun reEnablingResetsHeadsUpState() {
        val r = base.copy(enabled = false, headsUpEnabled = true, headsUpLastFiredAt = now)
        val again = FireRules.afterReEnabled(r)
        assertTrue(again.enabled)
        assertNull(again.headsUpLastFiredAt)
        assertTrue(FireRules.shouldFireHeadsUp(again, now))
    }

    @Test fun sliderMappingEndpointsAndRoundTrip() {
        assertEquals(50, RadiusScale.toMeters(0f))
        assertEquals(50_000, RadiusScale.toMeters(1f))
        assertEquals(0f, RadiusScale.toPosition(50), 1e-6f)
        assertEquals(1f, RadiusScale.toPosition(50_000), 1e-6f)
        for (m in listOf(100, 500, 1000, 5000, 20_000)) {
            val back = RadiusScale.toMeters(RadiusScale.toPosition(m))
            assertTrue("$m -> $back", kotlin.math.abs(back - m) <= m * 0.06)
        }
    }

    @Test fun sliderMappingIsMonotonicAndClamped() {
        var prev = 0
        for (i in 0..100) {
            val m = RadiusScale.toMeters(i / 100f)
            assertTrue(m >= prev)
            assertTrue(m in Limits.MIN_RADIUS_M..Limits.MAX_RADIUS_M)
            prev = m
        }
        assertEquals(50, RadiusScale.toMeters(-3f))
        assertEquals(50_000, RadiusScale.toMeters(9f))
    }

    @Test fun validation() {
        fun v(r: Reminder, loc: Boolean = true) = Validation.validate(r, loc)
        assertEquals(Validation.Ok, v(base))
        assertTrue((v(base.copy(name = " ")) as Validation.Error).field == Validation.Field.NAME)
        assertTrue((v(base, loc = false) as Validation.Error).field == Validation.Field.LOCATION)
        assertTrue((v(base.copy(radiusMeters = 49)) as Validation.Error).field == Validation.Field.RADIUS)
        assertTrue((v(base.copy(radiusMeters = 50_001)) as Validation.Error).field == Validation.Field.RADIUS)
        assertEquals(Validation.Ok, v(base.copy(radiusMeters = 50)))
        assertEquals(Validation.Ok, v(base.copy(radiusMeters = 50_000)))
        assertTrue((v(base.copy(trigger = Trigger.DWELL, dwellMinutes = 0)) as Validation.Error).field == Validation.Field.DWELL)
        assertTrue((v(base.copy(trigger = Trigger.DWELL, dwellMinutes = 121)) as Validation.Error).field == Validation.Field.DWELL)
        // dwell minutes are irrelevant unless trigger is DWELL
        assertEquals(Validation.Ok, v(base.copy(dwellMinutes = 0)))
    }

    @Test fun headsUpValidation() {
        fun v(r: Reminder) = Validation.validate(r, true)
        val h = base.copy(headsUpEnabled = true, radiusMeters = 2000)
        assertEquals(Validation.Ok, v(h.copy(headsUpMeters = 10_000)))
        assertTrue(v(h.copy(headsUpMeters = 2000)) is Validation.Error)
        assertTrue(v(h.copy(headsUpMeters = 999)) is Validation.Error)
        assertTrue(v(h.copy(headsUpMeters = 100_001)) is Validation.Error)
        // ignored for EXIT
        assertEquals(Validation.Ok, v(h.copy(trigger = Trigger.EXIT, headsUpMeters = 5)))
    }

    @Test fun geofenceBudget() {
        val list = (1L..50L).map { base.copy(id = it, headsUpEnabled = true) } // 100 fences
        assertEquals(100, GeofenceBudget.used(list))
        assertFalse(GeofenceBudget.fits(list, base.copy(id = 99)))
        assertTrue(GeofenceBudget.fits(list, base.copy(id = 99, enabled = false)))
        assertTrue(GeofenceBudget.fits(list, list[0])) // editing an existing one
        val onFree = list.dropLast(1) // 98 fences used, 2 free
        assertTrue(GeofenceBudget.fits(onFree, base.copy(id = 99, headsUpEnabled = true)))
        assertFalse(GeofenceBudget.fits(onFree + base.copy(id = 98), base.copy(id = 99, headsUpEnabled = true)))
    }
}
