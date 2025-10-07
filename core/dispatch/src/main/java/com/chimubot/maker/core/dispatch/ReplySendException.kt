package com.chimubot.maker.core.dispatch

import android.app.PendingIntent

sealed class ReplySendException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class HandleExpired(cause: PendingIntent.CanceledException) :
        ReplySendException("Reply handle expired", cause)

    class TransportFailed(cause: Throwable) : ReplySendException("Reply send failed", cause)

    class MissingHandle(notificationKey: String) :
        ReplySendException("Missing reply handle for $notificationKey")
}
