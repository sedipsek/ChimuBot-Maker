package com.chimubot.maker.core.dispatch

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.RemoteInput

object ReplySender {
    fun send(context: Context, handle: ReplyHandle, text: CharSequence) {
        val fillIn = Intent()
        val results = Bundle()
        val primaryKey = handle.remoteInputs.first().resultKey
        results.putCharSequence(primaryKey, text)
        RemoteInput.addResultsToIntent(handle.remoteInputs, fillIn, results)
        handle.pendingIntent.send(context, 0, fillIn)
    }
}
