package com.chimubot.maker.core.notif

import android.app.Notification
import android.service.notification.StatusBarNotification
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.chimubot.maker.core.rules.CapturedNotification

object NotificationParser {
    fun parse(sbn: StatusBarNotification): CapturedNotification? {
        val notification = sbn.notification
        val replyHandle = ReplyActionFinder.find(notification)
        val extras = notification.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val conversationTitle = extras?.getCharSequence(Notification.EXTRA_CONVERSATION_TITLE)?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val isGroupConversation = extras?.getBoolean(NotificationCompat.EXTRA_IS_GROUP_CONVERSATION) ?: false
        val (resolvedText, resolvedSender) = resolveLatestMessage(extras)

        return CapturedNotification(
            key = sbn.key,
            packageName = sbn.packageName,
            postedAt = sbn.postTime,
            room = conversationTitle ?: title,
            sender = resolvedSender,
            text = resolvedText ?: text,
            isGroup = isGroupConversation,
            replyHandle = replyHandle
        )
    }

    private fun resolveLatestMessage(extras: Bundle?): Pair<String?, String?> {
        val messages = extras?.getParcelableArray(NotificationCompat.EXTRA_MESSAGES) ?: return null to null
        val last = messages.lastOrNull() as? Bundle ?: return null to null
        val body = last.getCharSequence(NotificationCompat.EXTRA_TEXT)?.toString()
        val sender = last.getCharSequence(NotificationCompat.EXTRA_SENDER)?.toString()
        return body to sender
    }
}
