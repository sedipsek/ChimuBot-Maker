package com.chimubot.maker.core.rules

/**
 * 규칙 매칭 결과로 Reply 큐에 들어가는 메시지 페이로드.
 */
data class OutgoingMessage(
    val notificationKey: String,
    val text: String,
    val room: String? = null,
    val attempt: Int = 0,
    val scheduledAt: Long = System.currentTimeMillis()
)
