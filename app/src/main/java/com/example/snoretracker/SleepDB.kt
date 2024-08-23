package com.example.snoretracker

import android.content.Context
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.Upsert

@Entity(tableName = "audio_categories")
data class AudioCategory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // 自动生成的主键
    val dateId: Int,
    val label: String,
    val score: Float,
    val timestamp: Long
)

@Entity(tableName = "sleep_times")
data class SleepTime(
    @PrimaryKey val dateId: Int,  // 使用 dateId 作为主键
    val startHour: Int,
    val startMinute : Int,
    val endHour: Int?,
    val endMinute: Int?
)

@Dao
interface AudioCategoryDao {

    @Insert
    suspend fun insertCategory(category: AudioCategory)

    @Query("SELECT SUM(score) FROM audio_categories WHERE label = :label AND dateId = :dateId")
    fun getTotalTimeForLabel(dateId: Int, label: String): Float?

    @Query("DELETE FROM audio_categories")
    suspend fun clearAllCategories()
}

@Dao
interface SleepTimeDao {

    @Upsert
    suspend fun upsertSleepTime(sleepTime: SleepTime)

    @Query("UPDATE sleep_times SET endHour = :endHour, endMinute = :endMinute WHERE dateId = :dateId")
    suspend fun updateEndTime(dateId: Int, endHour: Int, endMinute: Int)

    @Query("SELECT * FROM sleep_times WHERE dateId = :dateId LIMIT 1")
    suspend fun getSleepTimeByDate(dateId: Int): SleepTime?

    @Query("""
        SELECT (CASE 
                    WHEN endHour < startHour THEN endHour + 24
                    ELSE endHour
                END 
                - startHour) 
            + (endMinute - startMinute) / 60.0 AS sleepDuration
        FROM sleep_times
        WHERE dateId =:dateId
    """)
    suspend fun getSleepDurationByDate(dateId: Int): Float?

    @Query("DELETE FROM sleep_times")
    suspend fun clearAllSleepTime()
}

@Database(entities = [AudioCategory::class, SleepTime::class], version = 2)
abstract class AppDatabase : RoomDatabase() {
    abstract fun audioCategoryDao(): AudioCategoryDao
    abstract fun sleepTimeDao(): SleepTimeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun deleteDatabase(context: Context, name: String) {
            context.deleteDatabase(name)
        }
    }
}
