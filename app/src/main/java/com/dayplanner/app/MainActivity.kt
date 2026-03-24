package com.dayplanner.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: TimelineAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var tvDate: TextView
    private lateinit var tvDayName: TextView
    private lateinit var btnPrevDay: View
    private lateinit var btnNextDay: View
    private lateinit var fabAdd: FloatingActionButton

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())

    private var currentDate: Calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = AppDatabase.getDatabase(this)
        AlarmScheduler.createNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100)
        }

        tvDate = findViewById(R.id.tvDate)
        tvDayName = findViewById(R.id.tvDayName)
        btnPrevDay = findViewById(R.id.btnPrevDay)
        btnNextDay = findViewById(R.id.btnNextDay)
        fabAdd = findViewById(R.id.fabAdd)
        recyclerView = findViewById(R.id.recyclerView)

        adapter = TimelineAdapter(
            onEventClick = { event ->
                val intent = Intent(this, AddEventActivity::class.java)
                intent.putExtra("event_id", event.id)
                startActivity(intent)
            },
            onEventDelete = { event ->
                lifecycleScope.launch {
                    AlarmScheduler.cancelAlarm(this@MainActivity, event.id)
                    db.eventDao().deleteEvent(event)
                }
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        btnPrevDay.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_YEAR, -1)
            loadDay()
        }

        btnNextDay.setOnClickListener {
            currentDate.add(Calendar.DAY_OF_YEAR, 1)
            loadDay()
        }

        fabAdd.setOnClickListener {
            val intent = Intent(this, AddEventActivity::class.java)
            intent.putExtra("date", dateFormat.format(currentDate.time))
            startActivity(intent)
        }

        loadDay()
    }

    override fun onResume() {
        super.onResume()
        loadDay()
    }

    private fun loadDay() {
        val dateStr = dateFormat.format(currentDate.time)
        tvDate.text = displayFormat.format(currentDate.time)
        tvDayName.text = dayFormat.format(currentDate.time)

        lifecycleScope.launch {
            db.eventDao().getEventsForDate(dateStr).collectLatest { events ->
                adapter.setEvents(events)
                // Scroll to current hour
                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                recyclerView.scrollToPosition(hour)
            }
        }
    }
}
