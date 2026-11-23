package io.github.janhorak58.placereminder.ui

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** The [Address] fields the display text is built from, kept plain so the formatting is unit-testable. */
data class AddressParts(
    val feature: String? = null,
    val street: String? = null,
    val number: String? = null,
    val locality: String? = null,
    val subAdmin: String? = null,
    val admin: String? = null,
    val country: String? = null,
    val line0: String? = null,
)

data class Candidate(val parts: AddressParts, val lat: Double, val lon: Double)

data class PlaceResult(val title: String, val subtitle: String, val lat: Double, val lon: Double)

private fun String?.clean(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/** First line: a named place if there is one, otherwise "street number", otherwise the town. */
fun placeTitle(a: AddressParts): String {
    val street = a.street.clean()
    val number = a.number.clean()
    val streetLine = street?.let { if (number != null) "$it $number" else it }
    val feature = a.feature.clean()?.takeUnless { f ->
        f.all { it.isDigit() || it in "-/, " } || f == number || f.equals(street, true)
    }
    return feature ?: streetLine ?: a.locality.clean() ?: a.line0.clean()?.substringBefore(',')?.trim() ?: "Unnamed place"
}

/** Second line: town, region and country without repeats, and without repeating the title. */
fun placeSubtitle(a: AddressParts, title: String): String =
    listOf(a.locality.clean() ?: a.subAdmin.clean(), a.admin.clean(), a.country.clean())
        .filterNotNull()
        .distinctBy { it.lowercase() }
        .filterNot { it.equals(title, true) }
        .joinToString(", ")

fun buildResults(candidates: List<Candidate>, max: Int = 5): List<PlaceResult> =
    candidates.map {
        val title = placeTitle(it.parts)
        PlaceResult(title, placeSubtitle(it.parts, title), it.lat, it.lon)
    }.distinctBy { "${it.title}|${it.subtitle}".lowercase() }.take(max)

/** Search box [south, west, north, east] around a point, clamped to valid coordinates. */
fun biasBox(lat: Double, lon: Double, halfSpanDeg: Double = 0.5): DoubleArray = doubleArrayOf(
    (lat - halfSpanDeg).coerceAtLeast(-90.0), (lon - halfSpanDeg).coerceAtLeast(-180.0),
    (lat + halfSpanDeg).coerceAtMost(90.0), (lon + halfSpanDeg).coerceAtMost(180.0),
)

private fun Address.toCandidate() = Candidate(
    AddressParts(featureName, thoroughfare, subThoroughfare, locality, subAdminArea, adminArea, countryName, getAddressLine(0)),
    latitude, longitude,
)

/** Address search through the platform geocoder. Throws [IOException] when the service is unreachable. */
class PlaceSearch(context: Context) {
    private val appContext = context.applicationContext
    val available: Boolean = Geocoder.isPresent()
    private val geocoder by lazy { Geocoder(appContext, Locale.getDefault()) }

    /** Looks near [near] first, then falls back to an unbounded query when that finds nothing. */
    suspend fun search(query: String, near: Pair<Double, Double>?): List<PlaceResult> {
        if (near != null) {
            val biased = lookup(query, biasBox(near.first, near.second))
            if (biased.isNotEmpty()) return biased
        }
        return lookup(query, null)
    }

    private suspend fun lookup(query: String, box: DoubleArray?): List<PlaceResult> {
        val raw: List<Address> = if (Build.VERSION.SDK_INT >= 33) {
            suspendCancellableCoroutine { cont ->
                val listener = object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) = cont.resume(addresses)
                    override fun onError(errorMessage: String?) =
                        cont.resumeWithException(IOException(errorMessage ?: "Geocoder error"))
                }
                if (box != null) geocoder.getFromLocationName(query, MAX, box[0], box[1], box[2], box[3], listener)
                else geocoder.getFromLocationName(query, MAX, listener)
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                val found = if (box != null) geocoder.getFromLocationName(query, MAX, box[0], box[1], box[2], box[3])
                else geocoder.getFromLocationName(query, MAX)
                found.orEmpty()
            }
        }
        return buildResults(raw.filter { it.hasLatitude() && it.hasLongitude() }.map { it.toCandidate() }, MAX)
    }

    private companion object {
        const val MAX = 5
    }
}
