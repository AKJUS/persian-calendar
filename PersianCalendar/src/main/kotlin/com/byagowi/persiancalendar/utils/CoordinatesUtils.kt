package com.byagowi.persiancalendar.utils

import android.content.Context
import com.byagowi.persiancalendar.LANG_AR
import com.byagowi.persiancalendar.LANG_CKB
import com.byagowi.persiancalendar.LANG_EN_IR
import com.byagowi.persiancalendar.LANG_EN_US
import com.byagowi.persiancalendar.LANG_ES
import com.byagowi.persiancalendar.LANG_FR
import com.byagowi.persiancalendar.LANG_JA
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.entities.CityItem
import io.github.persiancalendar.praytimes.Coordinate
import java.util.*
import kotlin.math.abs

fun formatCoordinate(context: Context, coordinate: Coordinate, separator: String) =
    "%s: %.7f%s%s: %.7f".format(
        Locale.getDefault(),
        context.getString(R.string.latitude), coordinate.latitude, separator,
        context.getString(R.string.longitude), coordinate.longitude
    )

// https://stackoverflow.com/a/62499553
// https://en.wikipedia.org/wiki/ISO_6709#Representation_at_the_human_interface_(Annex_D)
fun formatCoordinateISO6709(lat: Double, long: Double, alt: Double? = null) = listOf(
    abs(lat) to if (lat >= 0) "N" else "S", abs(long) to if (long >= 0) "E" else "W"
).joinToString(" ") { (degree: Double, direction: String) ->
    val minutes = ((degree - degree.toInt()) * 60).toInt()
    val seconds = ((degree - degree.toInt()) * 3600 % 60).toInt()
    "%d°%02d′%02d″%s".format(Locale.US, degree.toInt(), minutes, seconds, direction)
} + (alt?.let { " %s%.1fm".format(Locale.US, if (alt < 0) "−" else "", abs(alt)) } ?: "")

val CityItem.localizedCountryName: String
    get() = when (language) {
        LANG_EN_IR, LANG_EN_US, LANG_JA, LANG_FR, LANG_ES -> this.countryEn
        LANG_AR -> this.countryAr
        LANG_CKB -> this.countryCkb
        else -> this.countryFa
    }

val CityItem.localizedCityName: String
    get() = when (language) {
        LANG_EN_IR, LANG_EN_US, LANG_JA, LANG_FR, LANG_ES -> this.en
        LANG_AR -> this.ar
        LANG_CKB -> this.ckb
        else -> this.fa
    }
