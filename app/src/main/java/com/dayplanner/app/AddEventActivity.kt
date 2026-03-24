package com.dayplanner.app

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddEventActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayDateFormat = SimpleDateFormat("EEE, MMM d yyyy", Locale.getDefault())

    private lateinit var etTitle: EditText
    private lateinit var etDescription: EditText
    private lateinit var btnDate: Button
    private lateinit var btnStartTime: Button
    private lateinit var btnEndTime: Button
    private lateinit var switchAlarm: Switch
    private lateinit var btnSave: Button
    private lateinit var btnCancel: Button
    private lateinit var tvToolbarTitle: TextView

    private val colorOptions = listOf(
        0xFF6200EE.toInt(), // Purple
        0xFF03DAC5.toInt(), // Teal
        0xFFE91E63.toInt(), // Pink
        0xFF2196F3.toInt(), // Blue
        0xFF4CAF50.toInt(), // Green
        0xFFFF9800.toInt(), // Orange
        0xFFF44336.toInt(), // Red
        0xFF795548.toInt()  // Brown
    )
    private var selectedColor = colorOptions[0]
    private var selectedDate: Calendar = Calendar.getInstance()
    private var startHour = 9
    private var startMinute = 0
    private var endHour = 10
    private var endMinute = 0
    private var editingEventId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_event)

        db = AppDatabase.getDatabase(this)

        etTitle = findViewById(R.id.etTitle)
        etDescription = findViewById(R.id.etDescription)
        btnDate = findViewById(R.id.btnDate)
        btnStartTime = findViewById(R.id.btnStartTime)
        btnEndTime = findViewById(R.id.btnEndTime)
        switchAlarm = findViewById(R.id.switchAlarm)
        btnSave = findViewById(R.id.btnSave)
        btnCancel = findViewById(R.id.btnCancel)
        tvToolbarTitle = findViewById(R.id.tvToolbarTitle)

        // Load existing event if editing
        val eventId = intent.getIntExtra("event_id", -1)
        if (eventId != -1) {
            editingEventId = eventId
            tvToolbarTitle.text = "Edit Event"
            btnSave.text = "Update Event"
            loadEvent(eventId)
        } else {
            tvToolbarTitle.text = "New Event"
            val dateStr = intent.getStringExtra("date")
            if (dateStr != null) {
                selectedDate.time = dateFormat.parse(dateStr) ?: Date()
            }
            updateDateButton()
            updateTimeButtons()
        }

        setupColorPicker()

        btnDate.setOnClickListener { showDatePicker() }
        btnStartTime.setOnClickListener { showTimePicker(isStart = true) }
        btnEndTime.setOnClickListener { showTimePicker(isStart = false) }

        btnSave.setOnClickListener { saveEvent() }
        btnCancel.setOnClickListener { finish() }
    }

    private fun loadEvent(id: Int) {
        lifecycleScope.launch {
            val event = db.eventDao().getEventById(id) ?: return@launch
            etTitle.setText(event.title)
            etDescription.setText(event.description)
            selectedDate.time = dateFormat.parse(event.date) ?: Date()
            startHour = event.startHour
            startMinute = event.startMinute
            endHour = event.endHour
            endMinute = event.endMinute
            selectedColor = event.color
            switchAlarm.isChecked = event.alarmEnabled
            updateDateButton()
            updateTimeButtons()
            updateColorSelection()
        }
    }

    private fun setupColorPicker() {
        val colorContainer = findViewById<LinearLayout>(R.id.colorContainer)
        colorContainer.removeAllViews()
        colorOptions.forEachIndexed { index, color ->
            val btn = Button(this).apply {
                val size = resources.getDimensionPixelSize(android.R.dimen.app_icon_size)
                layoutParams = LinearLayout.LayoutParams(size, size).also {
                    it.marginEnd = 12
                }
                setBackgroundColor(color)
                text = ""
                tag = color
                setOnClickListener {
                    selectedColor = color
                    updateColorSelection()
                }
            }
            colorContainer.addView(btn)
        }
        updateColorSelection()
    }

    private fun updateColorSelection() {
        val colorContainer = findViewById<LinearLayout>(R.id.colorContainer)
        for (i in 0 until colorContainer.childCount) {
            val btn = colorContainer.getChildAt(i) as? Button
            btn?.alpha = if (btn?.tag == selectedColor) 1.0f else 0.4f
            btn?.scaleX = if (btn?.tag == selectedColor) 1.2f else 1.0f
            btn?.scaleY = if (btn?.tag == selectedColor) 1.2f else 1.0f
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate.set(year, month, day)
                updateDateButton()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(isStart: Boolean) {
        val initHour = if (isStart) startHour else endHour
        val initMin = if (isStart) startMinute else endMinute
        TimePickerDialog(this, { _, hour, minute ->
            if (isStart) {
                startHour = hour
                startMinute = minute
                // Auto-set end time 1 hour later
                if (editingEventId == null) {
                    endHour = (hour + 1) % 24
                    endMinute = minute
                }
            } else {
                endHour = hour
                endMinute = minute
            }
            updateTimeButtons()
        }, initHour, initMin, false).show()
    }

    private fun updateDateButton() {
        btnDate.text = displayDateFormat.format(selectedDate.time)
    }

    private fun updateTimeButtons() {
        btnStartTime.text = "Start: ${formatTime(startHour, startMinute)}"
        btnEndTime.text = "End: ${formatTime(endHour, endMinute)}"
    }

    private fun formatTime(hour: Int, minute: Int): String {
        val h = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
        val ampm = if (hour < 12) "AM" else "PM"
        val m = minute.toString().padStart(2, '0')
        return "$h:$m $ampm"
    }

    private fun saveEvent() {
        val title = etTitle.text.toString().trim()
        if (title.isEmpty()) {
            etTitle.error = "Please enter a title"
            return
        }

        val event = Event(
            id = editingEventId ?: 0,
            title = title,
            description = etDescription.text.toString().trim(),
            date = dateFormat.format(selectedDate.time),
            startHour = startHour,
            startMinute = startMinute,
            endHour = endHour,
            endMinute = endMinute,
            color = selectedColor,
            alarmEnabled = switchAlarm.isChecked
        )

        lifecycleScope.launch {
            if (editingEventId != null) {
                AlarmScheduler.cancelAlarm(this@AddEventActivity, editingEventId!!)
                db.eventDao().updateEvent(event)
                AlarmScheduler.scheduleAlarm(this@AddEventActivity, event)
                Toast.makeText(this@AddEventActivity, "Event updated!", Toast.LENGTH_SHORT).show()
            } else {
                val newId = db.eventDao().insertEvent(event)
                val savedEvent = event.copy(id = newId.toInt())
                AlarmScheduler.scheduleAlarm(this@AddEventActivity, savedEvent)
                Toast.makeText(this@AddEventActivity, "Event saved!", Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}
