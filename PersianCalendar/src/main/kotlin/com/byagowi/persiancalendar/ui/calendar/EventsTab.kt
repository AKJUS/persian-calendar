package com.byagowi.persiancalendar.ui.calendar

import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import android.widget.Toast
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Yard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.byagowi.persiancalendar.PREF_HOLIDAY_TYPES
import com.byagowi.persiancalendar.PREF_SHOW_DEVICE_CALENDAR_EVENTS
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.SHARED_CONTENT_KEY_EVENTS
import com.byagowi.persiancalendar.entities.Calendar
import com.byagowi.persiancalendar.entities.CalendarEvent
import com.byagowi.persiancalendar.entities.DeviceCalendarEventsStore
import com.byagowi.persiancalendar.entities.EventsRepository
import com.byagowi.persiancalendar.entities.Jdn
import com.byagowi.persiancalendar.global.eventsRepository
import com.byagowi.persiancalendar.global.holidayString
import com.byagowi.persiancalendar.global.isAstronomicalExtraFeaturesEnabled
import com.byagowi.persiancalendar.global.isGradient
import com.byagowi.persiancalendar.global.isTalkBackEnabled
import com.byagowi.persiancalendar.global.language
import com.byagowi.persiancalendar.global.mainCalendar
import com.byagowi.persiancalendar.global.spacedComma
import com.byagowi.persiancalendar.ui.common.AskForCalendarPermissionDialog
import com.byagowi.persiancalendar.ui.common.AutoSizedBodyText
import com.byagowi.persiancalendar.ui.common.equinoxTitle
import com.byagowi.persiancalendar.ui.theme.animateColor
import com.byagowi.persiancalendar.ui.theme.appCrossfadeSpec
import com.byagowi.persiancalendar.ui.theme.noTransitionSpec
import com.byagowi.persiancalendar.ui.utils.isLight
import com.byagowi.persiancalendar.utils.formatDate
import com.byagowi.persiancalendar.utils.formatNumber
import com.byagowi.persiancalendar.utils.getShiftWorkTitle
import com.byagowi.persiancalendar.utils.getShiftWorksInDaysDistance
import com.byagowi.persiancalendar.utils.logException
import com.byagowi.persiancalendar.utils.preferences
import com.byagowi.persiancalendar.utils.readDayDeviceEvents
import io.github.persiancalendar.calendar.PersianDate
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SharedTransitionScope.EventsTab(
    navigateToHolidaysSettings: () -> Unit,
    viewModel: CalendarViewModel,
    animatedContentScope: AnimatedContentScope,
    bottomPadding: Dp,
    isOnlyEventsTab: Boolean,
) {
    Column(Modifier.fillMaxWidth()) {
        Spacer(Modifier.height(8.dp))

        val jdn by viewModel.selectedDay.collectAsState()

        if (isOnlyEventsTab) AutoSizedBodyText(
            jdn.weekDayName + spacedComma + formatDate(jdn on mainCalendar),
            textStyle = MaterialTheme.typography.titleMedium,
            topPadding = 12.dp,
        )

        val refreshToken by viewModel.refreshToken.collectAsState()
        val shiftWorkTitle = remember(jdn, refreshToken) { getShiftWorkTitle(jdn) }
        this.AnimatedVisibility(visible = shiftWorkTitle != null) {
            AnimatedContent(
                targetState = shiftWorkTitle.orEmpty(),
                label = "shift work title",
                transitionSpec = appCrossfadeSpec,
            ) { state ->
                SelectionContainer {
                    Text(
                        state,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                    )
                }
            }
        }
        val shiftWorkInDaysDistance = getShiftWorksInDaysDistance(jdn)
        this.AnimatedVisibility(visible = shiftWorkInDaysDistance != null) {
            AnimatedContent(
                targetState = shiftWorkInDaysDistance.orEmpty(),
                label = "shift work days diff",
                transitionSpec = appCrossfadeSpec,
            ) { state ->
                SelectionContainer {
                    Text(
                        state,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Column(Modifier.padding(horizontal = 24.dp)) {
            val context = LocalContext.current
            val deviceEvents = remember(jdn, refreshToken) { context.readDayDeviceEvents(jdn) }
            val events = readEvents(jdn, viewModel, deviceEvents)
            Spacer(Modifier.height(16.dp))
            this.AnimatedVisibility(events.isEmpty()) {
                Text(
                    stringResource(R.string.no_event),
                    Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                )
            }
            Column(
                Modifier.sharedBounds(
                    rememberSharedContentState(SHARED_CONTENT_KEY_EVENTS),
                    animatedVisibilityScope = animatedContentScope,
                )
            ) { DayEvents(events) { viewModel.refreshCalendar() } }
        }

        val language by language.collectAsState()
        val context = LocalContext.current
        if (PREF_HOLIDAY_TYPES !in context.preferences && language.isIranExclusive) {
            Spacer(modifier = Modifier.height(16.dp))
            EncourageActionLayout(
                header = stringResource(R.string.warn_if_events_not_set),
                discardAction = {
                    context.preferences.edit {
                        putStringSet(PREF_HOLIDAY_TYPES, EventsRepository.iranDefault)
                    }
                },
                acceptAction = navigateToHolidaysSettings,
            )
        } else if (PREF_SHOW_DEVICE_CALENDAR_EVENTS !in context.preferences) {
            var showDialog by remember { mutableStateOf(false) }
            if (showDialog) AskForCalendarPermissionDialog { showDialog = false }

            Spacer(modifier = Modifier.height(16.dp))
            EncourageActionLayout(
                header = stringResource(R.string.ask_for_calendar_permission),
                discardAction = {
                    context.preferences.edit { putBoolean(PREF_SHOW_DEVICE_CALENDAR_EVENTS, false) }
                },
                acceptButton = stringResource(R.string.yes),
                acceptAction = { showDialog = true },
            )
        }

        // Events addition fab placeholder, so events can be scrolled after it
        Spacer(Modifier.height(76.dp))
        Spacer(Modifier.height(bottomPadding))
    }
}

@Composable
fun eventColor(event: CalendarEvent<*>): Color {
    return when {
        event is CalendarEvent.DeviceCalendarEvent -> runCatching {
            // should be turned to long then int otherwise gets stupid alpha
            if (event.color.isEmpty()) null else Color(event.color.toLong())
        }.onFailure(logException).getOrNull() ?: MaterialTheme.colorScheme.primary

        event.isHoliday || event is CalendarEvent.EquinoxCalendarEvent -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
}

fun ManagedActivityResultLauncher<Long, Void?>.viewEvent(
    event: CalendarEvent.DeviceCalendarEvent,
    context: Context
) {
    runCatching { this@viewEvent.launch(event.id) }
        .onFailure {
            Toast
                .makeText(
                    context,
                    R.string.device_does_not_support,
                    Toast.LENGTH_SHORT
                )
                .show()
        }
        .onFailure(logException)
}

fun eventTextColor(color: Int): Int = eventTextColor(Color(color)).toArgb()
fun eventTextColor(color: Color): Color = if (color.isLight) Color.Black else Color.White

@Composable
fun DayEvents(events: List<CalendarEvent<*>>, refreshCalendar: () -> Unit) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ViewEventContract()) { refreshCalendar() }
    events.forEach { event ->
        val backgroundColor by animateColor(eventColor(event))
        val eventTime =
            (event as? CalendarEvent.DeviceCalendarEvent)?.time?.let { "\n" + it }.orEmpty()
        AnimatedContent(
            (if (event.isHoliday) language.value.inParentheses.format(
                event.title, holidayString
            ) else event.title) + eventTime,
            label = "event title",
            transitionSpec = {
                (if (event is CalendarEvent.EquinoxCalendarEvent) noTransitionSpec
                else appCrossfadeSpec)()
            },
        ) { title ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(backgroundColor)
                    .combinedClickable(
                        enabled = event is CalendarEvent.DeviceCalendarEvent,
                        onClick = {
                            if (event is CalendarEvent.DeviceCalendarEvent) {
                                launcher.viewEvent(event, context)
                            }
                        },
                    )
                    .padding(8.dp)
                    .semantics {
                        this.contentDescription = if (event.isHoliday) context.getString(
                            R.string.holiday_reason, event.oneLinerTitleWithTime
                        ) else event.oneLinerTitleWithTime
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                val contentColor by animateColor(eventTextColor(backgroundColor))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    SelectionContainer {
                        Text(
                            title,
                            color = contentColor,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (event is CalendarEvent.EquinoxCalendarEvent) CompositionLocalProvider(
                        LocalLayoutDirection provides LayoutDirection.Ltr
                    ) { EquinoxCountDown(contentColor, event, backgroundColor) }
                }
                this.AnimatedVisibility(event is CalendarEvent.DeviceCalendarEvent || event is CalendarEvent.EquinoxCalendarEvent) {
                    Icon(
                        if (event is CalendarEvent.EquinoxCalendarEvent) Icons.Default.Yard
                        else Icons.AutoMirrored.Default.OpenInNew,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }
    }
}

private val countDownTimeParts = listOf(
    R.plurals.days to 1.days,
    R.plurals.hours to 1.hours,
    R.plurals.minutes to 1.minutes,
)

@Composable
private fun EquinoxCountDown(
    contentColor: Color,
    event: CalendarEvent.EquinoxCalendarEvent,
    backgroundColor: Color
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val isGradient by isGradient.collectAsState()
        val foldedCardBrush = if (isGradient) Brush.verticalGradient(
            .25f to contentColor,
            .499f to contentColor.copy(alpha = if (contentColor.isLight) .75f else .5f),
            .5f to contentColor,
        ) else Brush.verticalGradient(
            .49f to contentColor,
            .491f to Color.Transparent,
            .509f to Color.Transparent,
            .51f to contentColor,
        )

        var remainedTime = event.remainingMillis.milliseconds
        if (remainedTime < Duration.ZERO || remainedTime > 356.days) return
        countDownTimeParts.map { (pluralId, interval) ->
            val x = (remainedTime / interval).toInt()
            remainedTime -= interval * x
            x to pluralStringResource(pluralId, x, formatNumber(x))
        }.dropWhile { it.first == 0 }.forEach { (_, x) ->
            val parts = x.split(" ")
            if (parts.size == 2 && parts[0].length <= 3 && !isTalkBackEnabled) Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    val digits = parts[0].padStart(2, formatNumber(0)[0])
                    digits.forEach {
                        Text(
                            "$it",
                            style = MaterialTheme.typography.headlineSmall,
                            textAlign = TextAlign.Center,
                            color = backgroundColor,
                            modifier = Modifier
                                .background(
                                    foldedCardBrush,
                                    MaterialTheme.shapes.extraSmall,
                                )
                                .width(28.dp),
                        )
                    }
                }
                Text(
                    parts[1],
                    color = contentColor,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else Text(x, color = contentColor, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun readEvents(
    jdn: Jdn,
    viewModel: CalendarViewModel,
    deviceEvents: DeviceCalendarEventsStore,
): List<CalendarEvent<*>> {
    val context = LocalContext.current
    val events = sortEvents(eventsRepository?.getEvents(jdn, deviceEvents) ?: emptyList())

    if (mainCalendar == Calendar.SHAMSI || isAstronomicalExtraFeaturesEnabled) {
        val date = jdn.toPersianDate()
        if (jdn + 1 == Jdn(PersianDate(date.year + 1, 1, 1))) {
            val now by viewModel.now.collectAsState()
            val (rawTitle, equinoxTime) = equinoxTitle(date, jdn, context)
            val title = rawTitle.replace(": ", "\n")
            val remainedTime = equinoxTime - now
            val event = CalendarEvent.EquinoxCalendarEvent(title, false, date, remainedTime)
            return listOf(event) + events
        }
    }
    return events
}

fun sortEvents(events: List<CalendarEvent<*>>): List<CalendarEvent<*>> {
    return events.sortedBy {
        when {
            it.isHoliday -> 0L
            it !is CalendarEvent.DeviceCalendarEvent -> 1L
            else -> it.start.timeInMillis
        }
    }
}

class ViewEventContract : ActivityResultContract<Long, Void?>() {
    override fun parseResult(resultCode: Int, intent: Intent?) = null
    override fun createIntent(context: Context, input: Long): Intent {
        return Intent(Intent.ACTION_VIEW).setData(
            ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, input)
        )
    }
}
