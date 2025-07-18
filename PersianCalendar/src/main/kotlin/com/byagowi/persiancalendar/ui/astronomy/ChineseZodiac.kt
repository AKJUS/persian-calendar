package com.byagowi.persiancalendar.ui.astronomy

import android.content.res.Resources
import android.icu.util.ChineseCalendar
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import com.byagowi.persiancalendar.R
import io.github.persiancalendar.calendar.PersianDate

/**
 * The following table is copied from https://en.wikipedia.org/wiki/Chinese_zodiac
 *
 * | Number |  Animal | Yin/Yang | Trine | Fixed Element |
 * |:------:|:-------:|:--------:|:-----:|:-------------:|
 * | 1      | Rat     | Yang     | 1st   | Water         |
 * | 2      | Ox      | Yin      | 2nd   | Earth         |
 * | 3      | Tiger   | Yang     | 3rd   | Wood          |
 * | 4      | Rabbit  | Yin      | 4th   | Wood          |
 * | 5      | Dragon  | Yang     | 1st   | Earth         |
 * | 6      | Snake   | Yin      | 2nd   | Fire          |
 * | 7      | Horse   | Yang     | 3rd   | Fire          |
 * | 8      | Goat    | Yin      | 4th   | Earth         |
 * | 9      | Monkey  | Yang     | 1st   | Metal         |
 * | 10     | Rooster | Yin      | 2nd   | Metal         |
 * | 11     | Dog     | Yang     | 3rd   | Earth         |
 * | 12     | Pig     | Yin      | 4th   | Water         |
 *
 * The following poem is copied from https://fa.wikipedia.org/wiki/گاه‌شماری_حیوانی
 *
 * موش و بقر و پلنگ و خرگوش شمار - زان چار چو بگذری نهنگ آید و مار
 *آنگاه به اسب و گوسفند است حساب - حمدونه و مرغ و سگ و خوک آخر کار
 */
enum class ChineseZodiac(
    @get:StringRes private val title: Int, private val emoji: String,
    // For example used in https://rc.majlis.ir/fa/law/show/91137
    private val oldEraPersianName: String,
    private val persianAlternativeEmoji: String? = null,
    private val persianAlternativeTitle: String? = null,
) {
    RAT(R.string.animal_year_name_rat, "🐀", "سیچقان ئیل"),
    OX(R.string.animal_year_name_ox, "🐂", "اود ئیل"),
    TIGER(R.string.animal_year_name_tiger, "🐅", "بارس ئیل", "🐆", "پلنگ"),
    RABBIT(R.string.animal_year_name_rabbit, "🐇", "توشقان ئیل"),
    DRAGON(R.string.animal_year_name_dragon, "🐲", "لوی ئیل", "🐊", "نهنگ"),
    SNAKE(R.string.animal_year_name_snake, "🐍", "ئیلان ئیل"),
    HORSE(R.string.animal_year_name_horse, "🐎", "یونت ئیل"),
    GOAT(R.string.animal_year_name_goat, "🐐", "قوی ئیل", "🐑", "گوسفند"),
    MONKEY(R.string.animal_year_name_monkey, "🐒", "پیچی ئیل"),
    ROOSTER(R.string.animal_year_name_rooster, "🐓", "تخاقوی ئیل", "🐔", "مرغ"),
    DOG(R.string.animal_year_name_dog, "🐕", "ایت ئیل"),
    PIG(R.string.animal_year_name_pig, "🐖", "تنگوز ئیل");

    fun format(
        resources: Resources,
        withEmoji: Boolean,
        isPersian: Boolean,
        withOldEraName: Boolean = false,
    ): String = buildString {
        if (withEmoji) append("${resolveEmoji(isPersian)} ")
        append(resolveTitle(isPersian, resources))
        if (withOldEraName) append(" «$oldEraPersianName»")
    }

    fun formatForZodiac(resources: Resources, isPersian: Boolean): String = buildString {
        appendLine(resolveEmoji(isPersian))
        if (isPersian) appendLine(oldEraPersianName)
        append(resolveTitle(isPersian, resources))
    }

    private fun resolveEmoji(isPersian: Boolean): String =
        persianAlternativeEmoji.takeIf { isPersian } ?: emoji

    private fun resolveTitle(isPersian: Boolean, resources: Resources): String =
        persianAlternativeTitle.takeIf { isPersian } ?: resources.getString(title)

    val bestMatches get() = bestMatchesRaw[ordinal]
    val averageMatches get() = averageMatchesRaw[ordinal]
    val poorMatch get() = poorMatchRaw[ordinal]
    val harmfulMatch get() = harmfulMatchRaw[ordinal]

    companion object {
        fun fromPersianCalendar(persianDate: PersianDate): ChineseZodiac =
            entries.getOrNull((persianDate.year + 5) % 12) ?: RAT

        @RequiresApi(Build.VERSION_CODES.N)
        fun fromChineseCalendar(chineseDate: ChineseCalendar): ChineseZodiac =
            entries.getOrNull((chineseDate[ChineseCalendar.YEAR] - 1) % 12) ?: RAT

        /*
         * Compatibilities, they should be turned into formula eventually.
         *
         * The follow table is copied from https://en.wikipedia.org/wiki/Chinese_zodiac#Compatibility
         *
         * |   Sign  |      Best Match     |                    Average Match                   | Super Bad | Harmful |
         * |:-------:|:-------------------:|:--------------------------------------------------:|:---------:|---------|
         * | Rat     | Dragon, Monkey, Rat | Pig, Tiger, Dog, Snake, Rabbit, Rooster, Ox        | Horse     | Goat    |
         * | Ox      | Snake, Rooster, Ox  | Monkey, Dog, Rabbit, Tiger, Dragon, Pig, Rat       | Goat      | Horse   |
         * | Tiger   | Horse, Dog, Tiger   | Rabbit, Dragon, Rooster, Rat, Goat, Ox, Pig        | Monkey    | Snake   |
         * | Rabbit  | Pig, Goat, Rabbit   | Tiger, Monkey, Goat, Ox, Horse, Rat, Snake         | Rooster   | Dragon  |
         * | Dragon  | Rat, Monkey, Dragon | Tiger, Snake, Horse, Goat, Pig, Ox, Rooster        | Dog       | Rabbit  |
         * | Snake   | Ox, Rooster, Snake  | Horse, Dragon, Goat, Dog, Rabbit, Rat, Monkey      | Pig       | Tiger   |
         * | Horse   | Dog, Tiger, Horse   | Snake, Rabbit, Dragon, Rooster, Pig, Monkey, Goat  | Rat       | Ox      |
         * | Goat    | Rabbit, Pig, Goat   | Snake, Rabbit, Dragon, Monkey, Rooster, Dog, Tiger | Ox        | Rat     |
         * | Monkey  | Dragon, Rat, Monkey | Dragon, Dog, Ox, Goat, Rabbit, Rooster, Horse      | Tiger     | Pig     |
         * | Rooster | Ox, Snake, Rooster  | Horse, Snake, Goat, Pig, Tiger, Monkey, Rat        | Rabbit    | Dog     |
         * | Dog     | Tiger, Horse, Dog   | Monkey, Pig, Rat, Ox, Snake, Goat, Rabbit          | Dragon    | Rooster |
         * | Pig     | Rabbit, Goat, Pig   | Rat, Rooster, Dog, Dragon, Horse, Ox, Tiger        | Snake     | Monkey  |
         */

        private val bestMatchesRaw = listOf(
            setOf(DRAGON, MONKEY, RAT),
            setOf(SNAKE, ROOSTER, OX),
            setOf(HORSE, DOG, TIGER),
            setOf(PIG, GOAT, RABBIT),
            setOf(RAT, MONKEY, DRAGON),
            setOf(OX, ROOSTER, SNAKE),
            setOf(DOG, TIGER, HORSE),
            setOf(RABBIT, PIG, GOAT),
            setOf(DRAGON, RAT, MONKEY),
            setOf(OX, SNAKE, ROOSTER),
            setOf(TIGER, HORSE, DOG),
            setOf(RABBIT, GOAT, PIG)
        )
        private val averageMatchesRaw = listOf(
            setOf(PIG, TIGER, DOG, SNAKE, RABBIT, ROOSTER, OX),
            setOf(MONKEY, DOG, RABBIT, TIGER, DRAGON, PIG, RAT),
            setOf(RABBIT, DRAGON, ROOSTER, RAT, GOAT, OX, PIG),
            setOf(TIGER, MONKEY, DOG, OX, HORSE, RAT, SNAKE),
            setOf(TIGER, SNAKE, HORSE, GOAT, PIG, OX, ROOSTER),
            setOf(HORSE, DRAGON, GOAT, DOG, RABBIT, RAT, MONKEY),
            setOf(SNAKE, RABBIT, DRAGON, ROOSTER, PIG, MONKEY, GOAT),
            setOf(SNAKE, RABBIT, DRAGON, MONKEY, ROOSTER, DOG, TIGER),
            setOf(DRAGON, DOG, OX, GOAT, RABBIT, ROOSTER, HORSE),
            setOf(HORSE, SNAKE, GOAT, PIG, TIGER, MONKEY, RAT),
            setOf(MONKEY, PIG, RAT, OX, SNAKE, GOAT, RABBIT),
            setOf(RAT, ROOSTER, DOG, DRAGON, HORSE, OX, TIGER)
        )
        private val poorMatchRaw =
            listOf(HORSE, GOAT, MONKEY, ROOSTER, DOG, PIG, RAT, OX, TIGER, RABBIT, DRAGON, SNAKE)
        private val harmfulMatchRaw =
            listOf(GOAT, HORSE, SNAKE, DRAGON, RABBIT, TIGER, OX, RAT, PIG, DOG, ROOSTER, MONKEY)
    }
}
