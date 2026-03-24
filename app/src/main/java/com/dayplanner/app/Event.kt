package com.dayplanner.app

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val date: String,      // Format: "yyyy-MM-dd"
    val startHour: Int,    // 0-23
    val startMinute: Int,  // 0 or 30
    val endHour: Int,
    val endMinute: Int,
    val color: Int,
    val alarmEnabled: Boolean = true
)

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE date = :date ORDER BY startHour, startMinute")
    fun getEventsForDate(date: String): Flow<List<Event>>

    @Query("SELECT * FROM events ORDER BY date, startHour, startMinute")
    suspend fun getAllEvents(): List<Event>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: Event): Long

    @Delete
    suspend fun deleteEvent(event: Event)

    @Update
    suspend fun updateEvent(event: Event)

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getEventById(id: Int): Event?
}

@Database(entities = [Event::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dayplanner_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
