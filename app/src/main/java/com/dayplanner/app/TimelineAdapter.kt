package com.dayplanner.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class TimelineAdapter(
    private val onEventClick: (Event) -> Unit,
    private val onEventDelete: (Event) -> Unit
) : RecyclerView.Adapter<TimelineAdapter.HourViewHolder>() {

    private val hours = (0..23).toList()
    private var events: List<Event> = emptyList()

    fun setEvents(newEvents: List<Event>) {
        events = newEvents
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_hour, parent, false)
        return HourViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourViewHolder, position: Int) {
        val hour = hours[position]
        val hourEvents = events.filter { it.startHour == hour }
        holder.bind(hour, hourEvents)
    }

    override fun getItemCount() = hours.size

    inner class HourViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvHour: TextView = itemView.findViewById(R.id.tvHour)
        private val eventsContainer: LinearLayout = itemView.findViewById(R.id.eventsContainer)
        private val hourLine: View = itemView.findViewById(R.id.hourLine)

        fun bind(hour: Int, hourEvents: List<Event>) {
            val displayHour = when {
                hour == 0 -> "12 AM"
                hour < 12 -> "$hour AM"
                hour == 12 -> "12 PM"
                else -> "${hour - 12} PM"
            }
            tvHour.text = displayHour

            // Highlight current hour
            val now = java.util.Calendar.getInstance()
            if (now.get(java.util.Calendar.HOUR_OF_DAY) == hour) {
                hourLine.setBackgroundColor(0xFF6200EE.toInt())
                tvHour.setTextColor(0xFF6200EE.toInt())
            } else {
                hourLine.setBackgroundColor(0xFFE0E0E0.toInt())
                tvHour.setTextColor(0xFF757575.toInt())
            }

            eventsContainer.removeAllViews()
            for (event in hourEvents) {
                val eventView = LayoutInflater.from(itemView.context)
                    .inflate(R.layout.item_event, eventsContainer, false)

                val tvTitle = eventView.findViewById<TextView>(R.id.tvEventTitle)
                val tvTime = eventView.findViewById<TextView>(R.id.tvEventTime)
                val tvAlarm = eventView.findViewById<TextView>(R.id.tvAlarmBadge)
                val colorBar = eventView.findViewById<View>(R.id.colorBar)

                tvTitle.text = event.title

                val startStr = formatTime(event.startHour, event.startMinute)
                val endStr = formatTime(event.endHour, event.endMinute)
                tvTime.text = "$startStr – $endStr"

                tvAlarm.visibility = if (event.alarmEnabled) View.VISIBLE else View.GONE

                colorBar.setBackgroundColor(event.color)
                eventView.setBackgroundColor(event.color and 0x00FFFFFF or 0x22000000)

                eventView.setOnClickListener { onEventClick(event) }
                eventView.setOnLongClickListener {
                    showDeleteDialog(event)
                    true
                }

                eventsContainer.addView(eventView)
            }
        }

        private fun formatTime(hour: Int, minute: Int): String {
            val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            val ampm = if (hour < 12) "AM" else "PM"
            val m = minute.toString().padStart(2, '0')
            return "$h:$m $ampm"
        }

        private fun showDeleteDialog(event: Event) {
            androidx.appcompat.app.AlertDialog.Builder(itemView.context)
                .setTitle("Delete Event")
                .setMessage("Delete \"${event.title}\"?")
                .setPositiveButton("Delete") { _, _ -> onEventDelete(event) }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }
}
