package com.flesiy.Lotus.services

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.flesiy.Lotus.R
import com.flesiy.Lotus.MainActivity
import com.flesiy.Lotus.data.NotificationEntity
import com.flesiy.Lotus.data.NotificationDatabase
import com.flesiy.Lotus.data.RepeatInterval
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "NotificationService"

class NotificationService(private val context: Context) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val notificationDao = NotificationDatabase.getDatabase(context).notificationDao()

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминания Lotus",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Канал для напоминаний о заметках"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Создан канал уведомлений: ${channel.id}")
        }
    }

    fun scheduleNotification(notification: NotificationEntity) {
        Log.d(TAG, "Планирование уведомления: id=${notification.id}, title=${notification.title}, triggerTime=${notification.triggerTimeMillis}")
        
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(EXTRA_NOTIFICATION_ID, notification.id)
            putExtra(EXTRA_NOTIFICATION_TITLE, notification.title)
            putExtra(EXTRA_NOTIFICATION_DESCRIPTION, notification.description)
            putExtra(EXTRA_NOTE_ID, notification.noteId)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notification.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTime = notification.triggerTimeMillis

        if (notification.isEnabled) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    if (alarmManager.canScheduleExactAlarms()) {
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                        Log.d(TAG, "Уведомление запланировано на: ${LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(triggerTime),
                            ZoneId.systemDefault()
                        )}")
                    } else {
                        Log.e(TAG, "Нет разрешения на точные уведомления")
                        // Используем неточное планирование как запасной вариант
                        alarmManager.setAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            triggerTime,
                            pendingIntent
                        )
                    }
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                    )
                    Log.d(TAG, "Уведомление запланировано на: ${LocalDateTime.ofInstant(
                        java.time.Instant.ofEpochMilli(triggerTime),
                        ZoneId.systemDefault()
                    )}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при планировании уведомления", e)
            }
        } else {
            alarmManager.cancel(pendingIntent)
            Log.d(TAG, "Уведомление отменено: id=${notification.id}")
        }
    }

    fun cancelNotification(notificationId: Long) {
        Log.d(TAG, "Отмена уведомления: id=$notificationId")
        val intent = Intent(context, NotificationReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        notificationManager.cancel(notificationId.toInt())
    }

    companion object {
        const val CHANNEL_ID = "lotus_notifications"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
        const val EXTRA_NOTIFICATION_TITLE = "notification_title"
        const val EXTRA_NOTIFICATION_DESCRIPTION = "notification_description"
        const val EXTRA_NOTE_ID = "note_id"
        const val ACTION_DISMISS = "com.flesiy.Lotus.DISMISS_NOTIFICATION"
    }
}

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            NotificationService.ACTION_DISMISS -> {
                // Обработка нажатия на кнопку "Принято"
                val notificationId = intent.getLongExtra(NotificationService.EXTRA_NOTIFICATION_ID, 0)
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.cancel(notificationId.toInt())
                
                // Обработка повторяющихся уведомлений или удаление одноразовых
                handleNotificationDismissal(context, notificationId)
            }
            else -> {
                Log.d(TAG, "Получено уведомление")
                val notificationId = intent.getLongExtra(NotificationService.EXTRA_NOTIFICATION_ID, 0)
                val title = intent.getStringExtra(NotificationService.EXTRA_NOTIFICATION_TITLE) ?: ""
                val description = intent.getStringExtra(NotificationService.EXTRA_NOTIFICATION_DESCRIPTION) ?: ""
                val noteId = intent.getLongExtra(NotificationService.EXTRA_NOTE_ID, 0)

                Log.d(TAG, "Данные уведомления: id=$notificationId, title=$title, noteId=$noteId")

                // Создаем Intent для открытия заметки
                val openNoteIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("note_id", noteId)
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId.toInt(),
                    openNoteIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Создаем Intent для кнопки "Принято"
                val dismissIntent = Intent(context, NotificationReceiver::class.java).apply {
                    action = NotificationService.ACTION_DISMISS
                    putExtra(NotificationService.EXTRA_NOTIFICATION_ID, notificationId)
                }

                val dismissPendingIntent = PendingIntent.getBroadcast(
                    context,
                    notificationId.toInt() + 1000, // Используем другой ID чтобы избежать конфликтов
                    dismissIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, NotificationService.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(title)
                    .setContentText(description)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setOngoing(true) // Делаем уведомление неубираемым
                    .addAction(R.drawable.ic_notification, "Принято", dismissPendingIntent)
                    .build()

                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.notify(notificationId.toInt(), notification)
                Log.d(TAG, "Уведомление показано")
            }
        }
    }

    private fun handleNotificationDismissal(context: Context, notificationId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val notificationDao = NotificationDatabase.getDatabase(context).notificationDao()
                val notifications = notificationDao.getAllNotifications().first()
                val notification = notifications.find { it.id == notificationId }
                
                notification?.let {
                    if (it.repeatInterval != null && it.repeatInterval != RepeatInterval.NONE) {
                        Log.d(TAG, "Перепланирование повторяющегося уведомления: interval=${it.repeatInterval}")
                        val nextTriggerTime = calculateNextTriggerTime(
                            LocalDateTime.ofInstant(
                                java.time.Instant.ofEpochMilli(it.triggerTimeMillis),
                                ZoneId.systemDefault()
                            ),
                            it.repeatInterval,
                            it.selectedDays?.split(",")?.map { day -> day.toInt() }
                        )
                        
                        // Проверяем, что следующее время действительно в будущем
                        val now = LocalDateTime.now()
                        val finalTriggerTime = if (nextTriggerTime.isBefore(now)) {
                            // Если время в прошлом, пересчитываем от текущего момента
                            calculateNextTriggerTime(now, it.repeatInterval, 
                                it.selectedDays?.split(",")?.map { day -> day.toInt() })
                        } else {
                            nextTriggerTime
                        }
                        
                        val updatedNotification = it.copy(
                            triggerTimeMillis = finalTriggerTime.atZone(ZoneId.systemDefault())
                                .toInstant().toEpochMilli()
                        )
                        
                        notificationDao.insertNotification(updatedNotification)
                        NotificationService(context).scheduleNotification(updatedNotification)
                        Log.d(TAG, "Уведомление перепланировано на: $finalTriggerTime")
                    } else {
                        // Удаляем одноразовое уведомление после срабатывания
                        Log.d(TAG, "Удаление одноразового уведомления после срабатывания: id=$notificationId")
                        notificationDao.deleteNotification(it)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при обработке уведомления", e)
            }
        }
    }

    private fun calculateNextTriggerTime(
        currentTime: LocalDateTime,
        repeatInterval: RepeatInterval,
        selectedDays: List<Int>? = null
    ): LocalDateTime {
        val now = LocalDateTime.now()
        
        return when (repeatInterval) {
            RepeatInterval.DAILY -> {
                var nextTime = currentTime
                // Если время уже прошло сегодня, переносим на завтра
                if (nextTime.isBefore(now)) {
                    nextTime = nextTime.plusDays(1)
                }
                nextTime
            }
            RepeatInterval.WEEKLY -> {
                var nextTime = currentTime
                // Если время уже прошло на этой неделе, переносим на следующую
                while (nextTime.isBefore(now)) {
                    nextTime = nextTime.plusWeeks(1)
                }
                nextTime
            }
            RepeatInterval.MONTHLY -> {
                var nextTime = currentTime
                // Если время уже прошло в этом месяце, переносим на следующий
                while (nextTime.isBefore(now)) {
                    nextTime = nextTime.plusMonths(1)
                }
                nextTime
            }
            RepeatInterval.YEARLY -> {
                var nextTime = currentTime
                // Если время уже прошло в этом году, переносим на следующий
                while (nextTime.isBefore(now)) {
                    nextTime = nextTime.plusYears(1)
                }
                nextTime
            }
            RepeatInterval.SPECIFIC_DAYS -> {
                if (selectedDays.isNullOrEmpty()) return currentTime

                val now = LocalDateTime.now()
                
                // Если выбранная дата в будущем (не сегодня), используем её как есть
                if (currentTime.toLocalDate().isAfter(now.toLocalDate())) {
                    Log.d(TAG, "🕒 Выбрана будущая дата, используем её: $currentTime")
                    return currentTime
                }
                
                // Получаем текущий день недели (1 = Понедельник, 7 = Воскресенье)
                val currentDayOfWeek = now.dayOfWeek.value
                val currentTimeOfDay = now.toLocalTime()
                val selectedTimeOfDay = currentTime.toLocalTime()
                
                // Сортируем выбранные дни
                val sortedDays = selectedDays.sorted()
                
                // Проверяем, можем ли запустить сегодня
                if (sortedDays.contains(currentDayOfWeek)) {
                    if (selectedTimeOfDay.isAfter(currentTimeOfDay)) {
                        // Если выбранное время сегодня ещё не наступило, используем его
                        val nextTriggerTime = LocalDateTime.of(now.toLocalDate(), selectedTimeOfDay)
                        Log.d(TAG, "🕒 Уведомление запланировано на сегодня: $nextTriggerTime")
                        return nextTriggerTime
                    }
                }
                
                // Если сегодня уже поздно или сегодня не выбранный день,
                // ищем следующий подходящий день
                val nextDay = sortedDays.find { it > currentDayOfWeek }
                val daysToAdd = if (nextDay != null) {
                    // Нашли день на этой неделе
                    (nextDay - currentDayOfWeek).toLong()
                } else {
                    // Переходим к первому дню следующей недели
                    val firstDayNextWeek = sortedDays.first()
                    (7 - currentDayOfWeek + firstDayNextWeek).toLong()
                }
                
                val nextTriggerTime = LocalDateTime.of(
                    now.toLocalDate().plusDays(daysToAdd),
                    selectedTimeOfDay
                )
                
                Log.d(TAG, "🕒 Итоговое время уведомления: $nextTriggerTime")
                nextTriggerTime
            }
            RepeatInterval.NONE -> currentTime
        }
    }
} 