package com.chimubot.maker.core.dispatch

import android.app.PendingIntent
import androidx.core.app.RemoteInput

/**
 * Direct Reply PendingIntent 및 RemoteInput 메타데이터.
 */
data class ReplyHandle(
    val pendingIntent: PendingIntent,
    val remoteInputs: Array<RemoteInput>,
    val ttlMs: Long
)
