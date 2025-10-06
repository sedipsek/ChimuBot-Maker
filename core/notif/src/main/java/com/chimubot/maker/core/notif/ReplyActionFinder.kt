package com.chimubot.maker.core.notif

import android.app.Notification
import com.chimubot.maker.core.dispatch.ReplyHandle

object ReplyActionFinder {
    fun find(notification: Notification): ReplyHandle? {
        val actions = notification.actions ?: return null
        for (action in actions) {
            val remoteInputs = action.remoteInputs
            if (remoteInputs != null && remoteInputs.isNotEmpty()) {
                return ReplyHandle(
                    pendingIntent = action.actionIntent,
                    remoteInputs = remoteInputs,
                    ttlMs = DEFAULT_TTL_MS
                )
            }
        }
        return null
    }

    private const val DEFAULT_TTL_MS = 60_000L
}
