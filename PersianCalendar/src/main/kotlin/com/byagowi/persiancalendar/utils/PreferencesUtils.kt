package com.byagowi.persiancalendar.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.byagowi.persiancalendar.DARK_THEME
import com.byagowi.persiancalendar.DEFAULT_CITY
import com.byagowi.persiancalendar.LIGHT_THEME
import com.byagowi.persiancalendar.PREF_GEOCODED_CITYNAME
import com.byagowi.persiancalendar.PREF_SELECTED_LOCATION
import com.byagowi.persiancalendar.PREF_SHOW_DEVICE_CALENDAR_EVENTS
import com.byagowi.persiancalendar.PREF_THEME
import com.byagowi.persiancalendar.SYSTEM_DEFAULT_THEME
import com.byagowi.persiancalendar.entities.CityItem
import com.byagowi.persiancalendar.entities.Jdn
import com.byagowi.persiancalendar.generated.citiesStore

val Context.appPrefs: SharedPreferences get() = PreferenceManager.getDefaultSharedPreferences(this)

fun SharedPreferences.Editor.putJdn(key: String, jdn: Jdn?) {
    if (jdn == null) remove(jdn) else putLong(key, jdn.value)
}

fun SharedPreferences.getJdnOrNull(key: String): Jdn? =
    getLong(key, -1).takeIf { it != -1L }?.let { Jdn(it) }

fun getCityName(context: Context, fallbackToCoordinates: Boolean): String {
    val prefs = context.appPrefs
    return prefs.getStoredCity()?.localizedCityName
        ?: prefs.getString(PREF_GEOCODED_CITYNAME, null)?.takeIf { it.isNotEmpty() }
        ?: coordinates?.takeIf { fallbackToCoordinates }
            ?.let { formatCoordinate(context, it, spacedComma) }
        ?: ""
}

fun SharedPreferences.getStoredCity(): CityItem? {
    return getString(PREF_SELECTED_LOCATION, null)
        ?.takeIf { it.isNotEmpty() && it != DEFAULT_CITY }?.let { citiesStore[it] }
}

fun getThemeFromPreference(context: Context, prefs: SharedPreferences): String =
    prefs.getString(PREF_THEME, null)?.takeIf { it != SYSTEM_DEFAULT_THEME }
        ?: if (isNightModeEnabled(context)) DARK_THEME else LIGHT_THEME

fun getEnabledCalendarTypes() = listOf(mainCalendar) + otherCalendars

fun toggleShowDeviceCalendarOnPreference(context: Context, enable: Boolean) =
    context.appPrefs.edit { putBoolean(PREF_SHOW_DEVICE_CALENDAR_EVENTS, enable) }
