package io.github.janhorak58.placereminder

import io.github.janhorak58.placereminder.ui.AddressParts
import io.github.janhorak58.placereminder.ui.Candidate
import io.github.janhorak58.placereminder.ui.biasBox
import io.github.janhorak58.placereminder.ui.buildResults
import io.github.janhorak58.placereminder.ui.placeSubtitle
import io.github.janhorak58.placereminder.ui.placeTitle
import org.junit.Assert.assertEquals
import org.junit.Test

class GeocodingTest {
    @Test fun streetAddressUsesStreetAndNumberNotTheNumericFeatureName() {
        val a = AddressParts(feature = "1", street = "Václavské náměstí", number = "1", locality = "Praha", admin = "Praha", country = "Czechia")
        assertEquals("Václavské náměstí 1", placeTitle(a))
        assertEquals("Praha, Czechia", placeSubtitle(a, placeTitle(a)))
    }

    @Test fun namedPlaceWinsOverStreet() {
        val a = AddressParts(feature = "Letiště Václava Havla", street = "Aviatická", locality = "Praha", country = "Czechia")
        assertEquals("Letiště Václava Havla", placeTitle(a))
    }

    @Test fun subtitleDropsLocalityThatRepeatsTheTitleOrTheRegion() {
        val a = AddressParts(feature = "Praha", locality = "Praha", admin = "praha", country = "Czechia")
        assertEquals("Praha", placeTitle(a))
        assertEquals("Czechia", placeSubtitle(a, "Praha"))
    }

    @Test fun missingFieldsFallBackWithoutCrashing() {
        assertEquals("Unnamed place", placeTitle(AddressParts()))
        assertEquals("Brno", placeTitle(AddressParts(line0 = "Brno, Czechia")))
        assertEquals("", placeSubtitle(AddressParts(), "Unnamed place"))
    }

    @Test fun duplicateResultsAreCollapsedAndCapped() {
        val a = AddressParts(street = "Main", number = "1", locality = "X")
        val list = List(8) { Candidate(a, 1.0 + it, 2.0) }
        assertEquals(1, buildResults(list).size)
        val distinct = List(8) { Candidate(a.copy(number = "$it"), 1.0, 2.0) }
        assertEquals(5, buildResults(distinct).size)
    }

    @Test fun biasBoxIsClampedAtThePoles() {
        val b = biasBox(89.8, 179.9)
        assertEquals(90.0, b[2], 0.0)
        assertEquals(180.0, b[3], 0.0)
    }
}
