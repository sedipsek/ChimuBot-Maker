package com.chimubot.maker.core.notif

import com.chimubot.maker.core.rules.CapturedNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * 최근에 파싱된 알림을 UI에 노출하기 위한 간단한 인메모리 저장소.
 */
object NotificationLogRepository {

    private const val MAX_LOG_SIZE = 50

    private val _items = MutableStateFlow<List<NotificationLogItem>>(emptyList())
    val items: StateFlow<List<NotificationLogItem>> = _items.asStateFlow()

    fun record(notification: CapturedNotification) {
        val entry = NotificationLogItem(
            key = notification.key,
            packageName = notification.packageName,
            room = notification.room,
            sender = notification.sender,
            body = notification.text,
            postedAt = notification.postedAt
        )
        _items.update { current ->
            val withoutSameKey = current.filterNot { it.key == entry.key }
            (listOf(entry) + withoutSameKey).take(MAX_LOG_SIZE)
        }
    }
}

/**
 * UI가 본문/파싱 결과를 렌더링하기 위한 데이터 표현.
 */
data class NotificationLogItem(
    val key: String,
    val packageName: String,
    val room: String?,
    val sender: String?,
    val body: String?,
    val postedAt: Long
)
