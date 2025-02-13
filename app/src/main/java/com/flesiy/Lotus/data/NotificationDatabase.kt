package com.flesiy.Lotus.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.time.ZoneId

enum class RepeatInterval {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    SPECIFIC_DAYS,
    NONE
}

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: Long,
    val noteId: Long,
    val title: String,
    val description: String,
    val triggerTimeMillis: Long,
    val repeatInterval: RepeatInterval?,
    val isEnabled: Boolean,
    val selectedDays: String? = null // Дни недели в формате "1,2,3" где 1 = Понедельник
)

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications WHERE noteId = :noteId")
    fun getNotificationsForNote(noteId: Long): Flow<List<NotificationEntity>>

    @Query("SELECT * FROM notifications")
    fun getAllNotifications(): Flow<List<NotificationEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: NotificationEntity)

    @Delete
    suspend fun deleteNotification(notification: NotificationEntity)

    @Query("UPDATE notifications SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateNotificationEnabled(id: Long, isEnabled: Boolean)

    @Query("DELETE FROM notifications WHERE noteId = :noteId")
    suspend fun deleteNotificationsForNote(noteId: Long)
}

@Database(entities = [NotificationEntity::class], version = 2)
@TypeConverters(Converters::class)
abstract class NotificationDatabase : RoomDatabase() {
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: NotificationDatabase? = null

        fun getDatabase(context: Context): NotificationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NotificationDatabase::class.java,
                    "notification_database"
                )
                .addMigrations(MIGRATION_1_2)
                .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE notifications ADD COLUMN selectedDays TEXT"
                )
            }
        }
    }
}

class Converters {
    @TypeConverter
    fun fromRepeatInterval(value: RepeatInterval?): String? {
        return value?.name
    }

    @TypeConverter
    fun toRepeatInterval(value: String?): RepeatInterval? {
        return value?.let { RepeatInterval.valueOf(it) }
    }

    @TypeConverter
    fun fromSelectedDays(value: String?): List<Int>? {
        return value?.split(",")?.map { it.toInt() }
    }

    @TypeConverter
    fun toSelectedDays(value: List<Int>?): String? {
        return value?.joinToString(",")
    }
} 