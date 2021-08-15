package com.byagowi.persiancalendar.ui.calendar.dialogs

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.core.text.buildSpannedString
import androidx.core.text.color
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.databinding.MonthOverviewItemBinding
import com.byagowi.persiancalendar.entities.Jdn
import com.byagowi.persiancalendar.ui.utils.copyToClipboard
import com.byagowi.persiancalendar.ui.utils.layoutInflater
import com.byagowi.persiancalendar.ui.utils.resolveColor
import com.byagowi.persiancalendar.utils.dayTitleSummary
import com.byagowi.persiancalendar.utils.getEvents
import com.byagowi.persiancalendar.utils.getEventsTitle
import com.byagowi.persiancalendar.utils.getMonthLength
import com.byagowi.persiancalendar.utils.isHighTextContrastEnabled
import com.byagowi.persiancalendar.utils.mainCalendar
import com.byagowi.persiancalendar.utils.readMonthDeviceEvents
import com.google.android.material.bottomsheet.BottomSheetDialog
import io.github.persiancalendar.calendar.AbstractDate

fun showMonthOverviewDialog(context: Context, date: AbstractDate) {
    val baseJdn = Jdn(date)
    val deviceEvents = context.readMonthDeviceEvents(baseJdn)
    val colorTextHoliday = context.resolveColor(R.attr.colorTextHoliday)
    val events = (0 until mainCalendar.getMonthLength(date.year, date.month)).mapNotNull {
        val jdn = baseJdn + it
        val events = getEvents(jdn, deviceEvents)
        val holidays = getEventsTitle(
            events, holiday = true, compact = false, showDeviceCalendarEvents = false,
            insertRLM = false, addIsHoliday = isHighTextContrastEnabled
        )
        val nonHolidays = getEventsTitle(
            events, holiday = false, compact = false, showDeviceCalendarEvents = true,
            insertRLM = false, addIsHoliday = false
        )
        if (holidays.isEmpty() && nonHolidays.isEmpty()) null
        else dayTitleSummary(jdn, jdn.toCalendar(mainCalendar)) to buildSpannedString {
            if (holidays.isNotEmpty()) color(colorTextHoliday) { append(holidays) }
            if (nonHolidays.isNotEmpty()) {
                if (holidays.isNotEmpty()) appendLine()
                append(nonHolidays)
            }
        }
    }.takeIf { it.isNotEmpty() } ?: listOf(context.getString(R.string.warn_if_events_not_set) to "")

    BottomSheetDialog(context, R.style.BottomSheetDialog).also { dialog ->
        dialog.setContentView(
            RecyclerView(context).also {
                it.layoutManager = LinearLayoutManager(context)
                it.adapter = MonthOverviewItemAdapter(events)
            }
        )
    }.show()
}

private class MonthOverviewItemAdapter(private val rows: List<Pair<String, CharSequence>>) :
    RecyclerView.Adapter<MonthOverviewItemAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ViewHolder(
        MonthOverviewItemBinding.inflate(parent.context.layoutInflater, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(position)

    override fun getItemCount(): Int = rows.size

    inner class ViewHolder(private val binding: MonthOverviewItemBinding) :
        RecyclerView.ViewHolder(binding.root), View.OnClickListener {

        init {
            binding.root.setOnClickListener(this)
        }

        fun bind(position: Int) = rows[position].let { (title, body) ->
            binding.title.text = title
            binding.body.text = body
        }

        override fun onClick(v: View?) = v?.context.copyToClipboard(rows[bindingAdapterPosition]
            .let { (title, body) -> listOf(title, body).joinToString("\n") })
    }
}
