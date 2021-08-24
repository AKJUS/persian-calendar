package com.byagowi.persiancalendar.utils

import android.content.Context
import android.content.res.Resources
import android.view.View
import com.byagowi.persiancalendar.LANG_EN_US
import com.byagowi.persiancalendar.LANG_ES
import com.byagowi.persiancalendar.LANG_FR
import com.byagowi.persiancalendar.LANG_JA

// See the naming here, https://developer.mozilla.org/en-US/docs/Web/CSS/list-style-type
val PERSIAN_DIGITS = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
val ARABIC_DIGITS = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
val ARABIC_INDIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
// CJK digits: charArrayOf('０', '１', '２', '３', '４', '５', '６', '７', '８', '９')
// but they weren't looking nice in the UI

val Resources.isRtl get() = configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

val isNonArabicScriptSelected
    get() = when (language) {
        LANG_EN_US, LANG_JA, LANG_FR, LANG_ES -> true
        else -> false
    }

fun formatNumber(number: Double): String {
    if (isArabicDigitSelected) return number.toString()
    return formatNumber(number.toString()).replace(".", "٫") // U+066B, Arabic Decimal Separator
}

fun formatNumber(number: Int): String = formatNumber(number.toString())

fun formatNumber(number: String): String {
    if (isArabicDigitSelected) return number
    return number.map { preferredDigits.getOrNull(Character.getNumericValue(it)) ?: it }
        .joinToString("")
}

val isArabicDigitSelected: Boolean get() = preferredDigits === ARABIC_DIGITS

fun getOrderedCalendarTypes(): List<CalendarType> =
    getEnabledCalendarTypes().let { it + (CalendarType.values().toList() - it) }

fun getOrderedCalendarEntities(
    context: Context, abbreviation: Boolean = false
): List<Pair<CalendarType, String>> {
    applyAppLanguage(context)
    return getOrderedCalendarTypes().map { calendarType ->
        calendarType to context.getString(
            if (abbreviation) calendarType.titleShort else calendarType.title
        )
    }
}

fun <T> listOf31Items(
    x1: T, x2: T, x3: T, x4: T, x5: T, x6: T, x7: T, x8: T, x9: T, x10: T, x11: T, x12: T,
    x13: T, x14: T, x15: T, x16: T, x17: T, x18: T, x19: T, x20: T, x21: T, x22: T,
    x23: T, x24: T, x25: T, x26: T, x27: T, x28: T, x29: T, x30: T, x31: T
) = listOf(
    x1, x2, x3, x4, x5, x6, x7, x8, x9, x10, x11, x12,
    x13, x14, x15, x16, x17, x18, x19, x20, x21, x22,
    x23, x24, x25, x26, x27, x28, x29, x30, x31
)

fun <T> listOf12Items(
    x1: T, x2: T, x3: T, x4: T, x5: T, x6: T, x7: T, x8: T, x9: T, x10: T, x11: T, x12: T
) = listOf(x1, x2, x3, x4, x5, x6, x7, x8, x9, x10, x11, x12)

fun <T> listOf7Items(
    x1: T, x2: T, x3: T, x4: T, x5: T, x6: T, x7: T
) = listOf(x1, x2, x3, x4, x5, x6, x7)
