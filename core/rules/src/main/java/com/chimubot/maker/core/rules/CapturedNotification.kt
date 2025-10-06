package com.chimubot.maker.core.rules

import com.chimubot.maker.core.dispatch.ReplyHandle

/**
 * NotificationListenerService에서 추출한 알림 정보의 표준 표현.
 */
data class CapturedNotification(
    val key: String,
    val packageName: String,
    val postedAt: Long,
    val room: String?,
    val sender: String?,
    val text: String?,
    val isGroup: Boolean,
    val replyHandle: ReplyHandle?
)
