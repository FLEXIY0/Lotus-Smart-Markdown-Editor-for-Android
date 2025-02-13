package com.flesiy.Lotus.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flesiy.Lotus.data.NotificationDatabase
import com.flesiy.Lotus.data.NotificationEntity
import com.flesiy.Lotus.data.RepeatInterval
import com.flesiy.Lotus.services.NotificationService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId

data class NoteNotification(
    val id: Long = System.currentTimeMillis(),
    val noteId: Long,
    val title: String,
    val description: String,
    val triggerTime: LocalDateTime,
    val repeatInterval: RepeatInterval? = null,
    val isEnabled: Boolean = true,
    val selectedDays: List<Int> = emptyList()
)

class NotificationViewModel(application: Application) : AndroidViewModel(application) {
    private val notificationDao = NotificationDatabase.getDatabase(application).notificationDao()
    private val notificationService = NotificationService(application)

    private val _notifications = MutableStateFlow<List<NoteNotification>>(emptyList())
    val notifications: StateFlow<List<NoteNotification>> = _notifications

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            notificationDao.getAllNotifications().collect { entities ->
                _notifications.value = entities.map { entity ->
                    NoteNotification(
                        id = entity.id,
                        noteId = entity.noteId,
                        title = entity.title,
                        description = entity.description,
                        triggerTime = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(entity.triggerTimeMillis),
                            ZoneId.systemDefault()
                        ),
                        repeatInterval = entity.repeatInterval,
                        isEnabled = entity.isEnabled,
                        selectedDays = entity.selectedDays?.split(",")?.map { it.toInt() } ?: emptyList()
                    )
                }
            }
        }
    }

    fun addNotification(notification: NoteNotification) {
        viewModelScope.launch {
            val entity = NotificationEntity(
                id = notification.id,
                noteId = notification.noteId,
                title = notification.title,
                description = notification.description,
                triggerTimeMillis = notification.triggerTime.atZone(ZoneId.systemDefault())
                    .toInstant().toEpochMilli(),
                repeatInterval = notification.repeatInterval,
                isEnabled = notification.isEnabled,
                selectedDays = if (notification.repeatInterval == RepeatInterval.SPECIFIC_DAYS) 
                    notification.selectedDays.joinToString(",") 
                else null
            )
            notificationDao.insertNotification(entity)
            notificationService.scheduleNotification(entity)
        }
    }

    fun deleteNotification(notification: NoteNotification) {
        viewModelScope.launch {
            val entity = NotificationEntity(
                id = notification.id,
                noteId = notification.noteId,
                title = notification.title,
                description = notification.description,
                triggerTimeMillis = notification.triggerTime.atZone(ZoneId.systemDefault())
                    .toInstant().toEpochMilli(),
                repeatInterval = notification.repeatInterval,
                isEnabled = notification.isEnabled
            )
            notificationDao.deleteNotification(entity)
            notificationService.cancelNotification(notification.id)
        }
    }

    fun toggleNotification(notification: NoteNotification) {
        viewModelScope.launch {
            val updatedNotification = notification.copy(isEnabled = !notification.isEnabled)
            addNotification(updatedNotification)
        }
    }

    fun getNotificationsForNote(noteId: Long) {
        viewModelScope.launch {
            notificationDao.getNotificationsForNote(noteId).collect { entities ->
                _notifications.value = entities.map { entity ->
                    NoteNotification(
                        id = entity.id,
                        noteId = entity.noteId,
                        title = entity.title,
                        description = entity.description,
                        triggerTime = LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(entity.triggerTimeMillis),
                            ZoneId.systemDefault()
                        ),
                        repeatInterval = entity.repeatInterval,
                        isEnabled = entity.isEnabled,
                        selectedDays = entity.selectedDays?.split(",")?.map { it.toInt() } ?: emptyList()
                    )
                }
            }
        }
    }
} 