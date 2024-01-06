package com.byagowi.persiancalendar.ui.calendar.calendarpager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.entities.CalendarEvent
import com.byagowi.persiancalendar.entities.EventsStore
import com.byagowi.persiancalendar.entities.Jdn
import com.byagowi.persiancalendar.entities.Language
import com.byagowi.persiancalendar.global.eventsRepository
import com.byagowi.persiancalendar.global.isShowDeviceCalendarEvents
import com.byagowi.persiancalendar.global.isShowWeekOfYearEnabled
import com.byagowi.persiancalendar.global.isTalkBackEnabled
import com.byagowi.persiancalendar.global.mainCalendar
import com.byagowi.persiancalendar.global.mainCalendarDigits
import com.byagowi.persiancalendar.ui.calendar.AddEvent
import com.byagowi.persiancalendar.ui.calendar.CalendarViewModel
import com.byagowi.persiancalendar.ui.theme.AppDayPainterColors
import com.byagowi.persiancalendar.ui.utils.AppBlendAlpha
import com.byagowi.persiancalendar.utils.applyWeekStartOffsetToWeekDay
import com.byagowi.persiancalendar.utils.formatNumber
import com.byagowi.persiancalendar.utils.getA11yDaySummary
import com.byagowi.persiancalendar.utils.getInitialOfWeekDay
import com.byagowi.persiancalendar.utils.getShiftWorkTitle
import com.byagowi.persiancalendar.utils.getWeekDayName
import com.byagowi.persiancalendar.utils.readMonthDeviceEvents
import com.byagowi.persiancalendar.utils.revertWeekStartOffsetFromWeekDay
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Month(viewModel: CalendarViewModel, offset: Int, width: Dp, height: Dp) {
    val today by viewModel.today.collectAsState()
    val monthStartDate = mainCalendar.getMonthStartFromMonthsDistance(today, offset)
    val monthStartJdn = Jdn(monthStartDate)

    val columnsCount = if (isShowWeekOfYearEnabled) 8 else 7
    val rowsCount = 7

    val startingDayOfWeek = applyWeekStartOffsetToWeekDay(monthStartJdn.dayOfWeek)
    val monthLength = mainCalendar.getMonthLength(monthStartDate.year, monthStartDate.month)
    val monthRange = 0..<monthLength
    val startOfYearJdn = Jdn(mainCalendar, monthStartDate.year, 1, 1)
    val weekOfYearStart = monthStartJdn.getWeekOfYear(startOfYearJdn)

    val widthPixels = with(LocalDensity.current) { width.toPx() }
    val heightPixels = with(LocalDensity.current) { height.toPx() }
    val cellWidthPx = widthPixels / columnsCount
    val cellHeightPx = heightPixels / rowsCount
    val oneDpInPx = with(LocalDensity.current) { 1.dp.toPx() }

    SelectionIndicator(
        viewModel = viewModel,
        monthStartJdn = monthStartJdn,
        monthRange = monthRange,
        width = width,
        height = height,
        startingDayOfWeek = startingDayOfWeek,
        widthPixels = widthPixels,
        cellWidthPx = cellWidthPx,
        cellHeightPx = cellHeightPx,
        oneDpInPx = oneDpInPx,
    )

    val refreshToken by viewModel.refreshToken.collectAsState()
    val context = LocalContext.current
    val isShowDeviceCalendarEvents by isShowDeviceCalendarEvents.collectAsState()
    val monthDeviceEvents = remember(refreshToken, isShowDeviceCalendarEvents) {
        if (isShowDeviceCalendarEvents) context.readMonthDeviceEvents(monthStartJdn)
        else EventsStore.empty()
    }

    val cellSize = DpSize(width / columnsCount, height / rowsCount)
    val diameter = min(cellSize.height, cellSize.width)
    val dayPainterColors = AppDayPainterColors()
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val dayPainter = remember(height, width, refreshToken, dayPainterColors) {
        DayPainter(context.resources, cellWidthPx, cellHeightPx, isRtl, dayPainterColors)
    }
    val textMeasurer = rememberTextMeasurer()
    val mainCalendarDigitsIsArabic = mainCalendarDigits === Language.ARABIC_DIGITS
    val daysTextSize = diameter * (if (mainCalendarDigitsIsArabic) 18 else 25) / 40
    val daysStyle = LocalTextStyle.current.copy(
        fontSize = with(LocalDensity.current) { daysTextSize.toSp() },
    )
    val contentColor = LocalContentColor.current

    // Slight fix for the particular font we use for native digits in Persian and so
    val dayOffsetY = if (mainCalendarDigits === Language.ARABIC_DIGITS) 0f else min(
        cellWidthPx, cellHeightPx
    ) * 1 / 40

    val addEvent = AddEvent(viewModel)
    val selectedDay by viewModel.selectedDay.collectAsState()
    val isHighlighted by viewModel.isHighlighted.collectAsState()
    FixedSizeTableLayout(Modifier.size(width, height), columnsCount, rowsCount) {
        if (isShowWeekOfYearEnabled) Spacer(Modifier)
        (0..<7).forEach { column ->
            Box(Modifier.size(cellSize), contentAlignment = Alignment.Center) {
                val weekDayPosition = revertWeekStartOffsetFromWeekDay(column)
                val description = stringResource(
                    R.string.week_days_name_column, getWeekDayName(weekDayPosition)
                )
                Text(
                    getInitialOfWeekDay(weekDayPosition),
                    fontSize = with(LocalDensity.current) { (diameter * .5f).toSp() },
                    modifier = Modifier
                        .alpha(AppBlendAlpha)
                        .semantics { this.contentDescription = description },
                )
            }
        }
        monthRange.forEach { dayOffset ->
            if (isShowWeekOfYearEnabled && (dayOffset == 0 || (dayOffset + startingDayOfWeek) % 7 == 0)) {
                Box(Modifier.size(cellSize), contentAlignment = Alignment.Center) {
                    val weekNumber = formatNumber(weekOfYearStart + dayOffset / 8)
                    val description = stringResource(R.string.nth_week_of_year, weekNumber)
                    Text(
                        weekNumber,
                        fontSize = with(LocalDensity.current) { (diameter * .35f).toSp() },
                        modifier = Modifier
                            .alpha(AppBlendAlpha)
                            .semantics { this.contentDescription = description },
                    )
                }
            }
            if (dayOffset == 0) repeat(startingDayOfWeek) { Spacer(Modifier) }
            val day = monthStartJdn + dayOffset
            val isToday = day == today
            Canvas(
                modifier = Modifier
                    .size(cellSize)
                    .combinedClickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { viewModel.changeSelectedDay(day) },
                        onClickLabel = if (isTalkBackEnabled) getA11yDaySummary(
                            context.resources,
                            day,
                            isToday,
                            EventsStore.empty(),
                            withZodiac = isToday,
                            withOtherCalendars = false,
                            withTitle = true
                        ) else (dayOffset + 1).toString(),
                        onLongClickLabel = stringResource(R.string.add_event),
                        onLongClick = {
                            viewModel.changeSelectedDay(day)
                            addEvent()
                        },
                    ),
            ) {
                val events = eventsRepository?.getEvents(day, monthDeviceEvents) ?: emptyList()
                val hasEvents = events.any { it !is CalendarEvent.DeviceCalendarEvent }
                val hasAppointments = events.any { it is CalendarEvent.DeviceCalendarEvent }
                val shiftWorkTitle = getShiftWorkTitle(day, true)
                val isSelected = isHighlighted && selectedDay == day
                dayPainter.setDayOfMonthItem(
                    false,
                    isSelected,
                    hasEvents,
                    hasAppointments,
                    false,
                    day,
                    "",
                    shiftWorkTitle,
                )
                drawIntoCanvas {
                    if (isToday) drawCircle(
                        Color(dayPainterColors.colorCurrentDay),
                        radius = size.minDimension / 2 - oneDpInPx / 2,
                        style = Stroke(width = oneDpInPx)
                    )
                    val textLayoutResult = textMeasurer.measure(
                        text = formatNumber(dayOffset + 1, mainCalendarDigits),
                        style = daysStyle,
                    )
                    val isHoliday = events.any { it.isHoliday }
                    drawText(
                        textLayoutResult,
                        color = when {
                            isSelected -> Color(dayPainterColors.colorTextDaySelected)
                            isHoliday || day.isWeekEnd() -> Color(dayPainterColors.colorHolidays)
                            else -> contentColor
                        },
                        topLeft = Offset(
                            x = center.x - textLayoutResult.size.width / 2,
                            y = center.y - textLayoutResult.size.height / 2 + dayOffsetY,
                        ),
                    )
                    dayPainter.drawDay(it.nativeCanvas)
                }
            }
        }
    }
}

@Composable
private fun FixedSizeTableLayout(
    modifier: Modifier,
    columnsCount: Int,
    rowsCount: Int,
    content: @Composable () -> Unit,
) {
    Layout(content, modifier) { measurables, constraints ->
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight
        val cellWidthPx = widthPx / columnsCount.toFloat()
        val cellHeightPx = heightPx / rowsCount.toFloat()
        val cellsConstraints =
            Constraints.fixed(cellWidthPx.roundToInt(), cellHeightPx.roundToInt())
        layout(widthPx, heightPx) {
            measurables.forEachIndexed { cellIndex, measurable ->
                val row = cellIndex / columnsCount
                val column = cellIndex % columnsCount
                measurable.measure(cellsConstraints).placeRelative(
                    (column * cellWidthPx).roundToInt(),
                    (row * cellHeightPx).roundToInt(),
                )
            }
        }
    }
}
