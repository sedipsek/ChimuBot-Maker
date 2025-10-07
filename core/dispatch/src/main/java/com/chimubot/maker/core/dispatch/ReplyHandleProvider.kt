package com.chimubot.maker.core.dispatch

/**
 * Reply PendingIntent를 조회/무효화하는 계약. core/state 모듈에서 구현한다.
 */
interface ReplyHandleProvider {
    fun currentHandleFor(notificationKey: String, room: String?): ReplyHandle?
    fun invalidate(notificationKey: String)

    companion object {
        val NO_OP: ReplyHandleProvider = object : ReplyHandleProvider {
            override fun currentHandleFor(notificationKey: String, room: String?): ReplyHandle? = null
            override fun invalidate(notificationKey: String) = Unit
        }
    }
}
