package com.byagowi.persiancalendar.ui.preferences.interfacecalendar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import com.byagowi.persiancalendar.AppLocalesData
import com.byagowi.persiancalendar.DEFAULT_ISLAMIC_OFFSET
import com.byagowi.persiancalendar.DEFAULT_WEEK_ENDS
import com.byagowi.persiancalendar.DEFAULT_WEEK_START
import com.byagowi.persiancalendar.LANG_AR
import com.byagowi.persiancalendar.LANG_EN_US
import com.byagowi.persiancalendar.LANG_ES
import com.byagowi.persiancalendar.LANG_FR
import com.byagowi.persiancalendar.LANG_JA
import com.byagowi.persiancalendar.PREF_APP_LANGUAGE
import com.byagowi.persiancalendar.PREF_ASTRONOMICAL_FEATURES
import com.byagowi.persiancalendar.PREF_EASTERN_GREGORIAN_ARABIC_MONTHS
import com.byagowi.persiancalendar.PREF_HOLIDAY_TYPES
import com.byagowi.persiancalendar.PREF_ISLAMIC_OFFSET
import com.byagowi.persiancalendar.PREF_PERSIAN_DIGITS
import com.byagowi.persiancalendar.PREF_SHOW_DEVICE_CALENDAR_EVENTS
import com.byagowi.persiancalendar.PREF_SHOW_WEEK_OF_YEAR_NUMBER
import com.byagowi.persiancalendar.PREF_THEME
import com.byagowi.persiancalendar.PREF_WEEK_ENDS
import com.byagowi.persiancalendar.PREF_WEEK_START
import com.byagowi.persiancalendar.PREF_WIDGET_IN_24
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.SYSTEM_DEFAULT_THEME
import com.byagowi.persiancalendar.ui.preferences.PREF_DESTINATION
import com.byagowi.persiancalendar.ui.preferences.build
import com.byagowi.persiancalendar.ui.preferences.clickable
import com.byagowi.persiancalendar.ui.preferences.dialogTitle
import com.byagowi.persiancalendar.ui.preferences.interfacecalendar.calendarsorder.showCalendarPreferenceDialog
import com.byagowi.persiancalendar.ui.preferences.multiSelect
import com.byagowi.persiancalendar.ui.preferences.section
import com.byagowi.persiancalendar.ui.preferences.singleSelect
import com.byagowi.persiancalendar.ui.preferences.summary
import com.byagowi.persiancalendar.ui.preferences.switch
import com.byagowi.persiancalendar.ui.preferences.title
import com.byagowi.persiancalendar.utils.askForCalendarPermission
import com.byagowi.persiancalendar.utils.formatNumber
import com.byagowi.persiancalendar.utils.getCompatDrawable
import com.byagowi.persiancalendar.utils.language

class InterfaceCalendarFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val destination = arguments?.getString(PREF_DESTINATION)
        if (destination == PREF_HOLIDAY_TYPES) showHolidaysTypesDialog()

        preferenceScreen = preferenceManager.createPreferenceScreen(context).build {
            section(R.string.pref_interface) {
                clickable(onClick = { showLanguagePreferenceDialog() }) {
                    if (destination == PREF_APP_LANGUAGE) {
                        title = "Language"
                        icon = context.getCompatDrawable(R.drawable.ic_translator)
                    } else title(R.string.language)
                }
                singleSelect(
                    PREF_THEME,
                    resources.getStringArray(R.array.themeNames).toList(),
                    resources.getStringArray(R.array.themeKeys).toList(),
                    SYSTEM_DEFAULT_THEME
                ) {
                    title(R.string.select_skin)
                    dialogTitle(R.string.select_skin)
                    summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
                }
                switch(PREF_EASTERN_GREGORIAN_ARABIC_MONTHS, false) {
                    if (language == LANG_AR) {
                        title = "السنة الميلادية بالاسماء الشرقية"
                        summary = "كانون الثاني، شباط، آذار، …"
                    } else isVisible = false
                }
                switch(PREF_PERSIAN_DIGITS, true) {
                    title(R.string.persian_digits)
                    summary(R.string.enable_persian_digits)
                    when (language) {
                        LANG_EN_US, LANG_JA, LANG_FR, LANG_ES -> isVisible = false
                    }
                }
            }
            section(R.string.calendar) {
                // Mark the rest of options as advanced
                initialExpandedChildrenCount = 6
                clickable(onClick = { showHolidaysTypesDialog() }) {
                    title(R.string.events)
                    summary(R.string.events_summary)
                }
                switch(PREF_SHOW_DEVICE_CALENDAR_EVENTS, false) {
                    title(R.string.show_device_calendar_events)
                    summary(R.string.show_device_calendar_events_summary)
                    setOnPreferenceChangeListener { _, _ ->
                        val activity = activity ?: return@setOnPreferenceChangeListener false
                        isChecked = if (ActivityCompat.checkSelfPermission(
                                activity, Manifest.permission.READ_CALENDAR
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            askForCalendarPermission(activity)
                            false
                        } else {
                            !isChecked
                        }
                        false
                    }
                }
                clickable(onClick = { showCalendarPreferenceDialog() }) {
                    title(R.string.calendars_priority)
                    summary(R.string.calendars_priority_summary)
                }
                switch(PREF_ASTRONOMICAL_FEATURES, false) {
                    title(R.string.astronomical_info)
                    summary(R.string.astronomical_info_summary)
                }
                switch(PREF_SHOW_WEEK_OF_YEAR_NUMBER, false) {
                    title(R.string.week_of_year)
                    summary(R.string.week_of_year_summary)
                }
                switch(PREF_WIDGET_IN_24, true) {
                    title(R.string.clock_in_24)
                    summary(R.string.showing_clock_in_24)
                }
                singleSelect(
                    PREF_ISLAMIC_OFFSET,
                    // One is formatted with locale's numerals and the other used for keys isn't
                    (-2..2).map { formatNumber(it.toString()) }, (-2..2).map { it.toString() },
                    DEFAULT_ISLAMIC_OFFSET
                ) {
                    title(R.string.islamic_offset)
                    summary(R.string.islamic_offset_summary)
                    dialogTitle(R.string.islamic_offset)
                }
                val weekDays = AppLocalesData.getWeekDays(language)
                val weekDaysValues = (0..6).map { it.toString() }
                singleSelect(PREF_WEEK_START, weekDays, weekDaysValues, DEFAULT_WEEK_START) {
                    title(R.string.week_start)
                    dialogTitle(R.string.week_start_summary)
                    summaryProvider = ListPreference.SimpleSummaryProvider.getInstance()
                }
                multiSelect(PREF_WEEK_ENDS, weekDays, weekDaysValues, DEFAULT_WEEK_ENDS) {
                    title(R.string.week_ends)
                    summary(R.string.week_ends_summary)
                    dialogTitle(R.string.week_ends_summary)
                }
            }
        }
    }
}
