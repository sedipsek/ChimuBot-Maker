package com.chimubot.maker.core.notif

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.chimubot.maker.core.rules.RuleEngineRegistry
import com.chimubot.maker.core.state.ReplyHandleCache

class ChimuNotificationListener : NotificationListenerService() {

    override fun onCreate() {
        super.onCreate()
        NotificationServiceInitializer.ensure(applicationContext)
        Log.i(TAG, "Notification listener created")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!NotificationTargetRegistry.current().shouldProcess(sbn.packageName)) {
            Log.d(TAG, "Package filtered: ${sbn.packageName}")
            return
        }
        ReplyHandleCache.pruneExpired()
        val captured = NotificationParser.parse(sbn)
        if (captured == null) {
            Log.d(TAG, "Failed to parse notification: ${sbn.key}")
            return
        }
        val replyHandle = captured.replyHandle
        if (replyHandle != null) {
            ReplyHandleCache.upsert(captured.key, captured.room, replyHandle)
        }
        NotificationLogRepository.record(captured)
        RuleEngineRegistry.current().onIncoming(captured)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        Log.d(TAG, "Notification removed: ${sbn.key}")
        ReplyHandleCache.invalidate(sbn.key)
    }

    companion object {
        private const val TAG = "ChimuNotifListener"
    }
}
