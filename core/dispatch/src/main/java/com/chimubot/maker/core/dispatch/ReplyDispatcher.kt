package com.chimubot.maker.core.dispatch

import android.content.Context
import android.util.Log
import com.chimubot.maker.core.rules.OutgoingMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ReplyDispatcher(
    context: Context,
    private val sendAction: (Context, ReplyHandle, CharSequence) -> Unit = ReplySender::send,
    coroutineScope: CoroutineScope? = null,
    private val minIntervalMs: Long = 150L
) {
    private val appContext = context.applicationContext
    private val job = SupervisorJob()
    private val scope = coroutineScope ?: CoroutineScope(Dispatchers.IO + job)
    private val channel = Channel<Pair<OutgoingMessage, ReplyHandle>>(Channel.UNLIMITED)

    init {
        scope.launch {
            for ((message, handle) in channel) {
                runCatching {
                    sendAction(appContext, handle, message.text)
                    delay(minIntervalMs)
                }.onFailure { error ->
                    Log.w(TAG, "Reply send failed for ${message.notificationKey}", error)
                    delay(minIntervalMs)
                }
            }
        }
    }

    fun enqueue(message: OutgoingMessage, handle: ReplyHandle) {
        channel.trySend(message to handle)
    }

    fun close() {
        channel.close()
        job.cancel()
    }

    companion object {
        private const val TAG = "ReplyDispatcher"
    }
}
