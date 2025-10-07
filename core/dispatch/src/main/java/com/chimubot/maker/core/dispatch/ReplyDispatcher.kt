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
    private val handleProvider: ReplyHandleProvider = ReplyHandleProvider.NO_OP,
    private val observer: ReplySendObserver = ReplySendObserver.NONE,
    private val sendAction: (Context, ReplyHandle, CharSequence) -> Unit = ReplySender::send,
    coroutineScope: CoroutineScope? = null,
    private val minIntervalMs: Long = 150L,
    private val maxAttempts: Int = 4
) {
    private val appContext = context.applicationContext
    private val job = SupervisorJob()
    private val scope = coroutineScope ?: CoroutineScope(Dispatchers.IO + job)
    private val channel = Channel<DispatchRequest>(Channel.UNLIMITED)

    init {
        scope.launch {
            for (request in channel) {
                process(request)
            }
        }
    }

    fun enqueue(message: OutgoingMessage, handle: ReplyHandle) {
        channel.trySend(DispatchRequest(message, handle))
    }

    fun close() {
        channel.close()
        job.cancel()
    }

    private suspend fun process(request: DispatchRequest) {
        val message = request.message
        val handle = handleProvider.currentHandleFor(message.notificationKey, message.room)
            ?: request.fallbackHandle
        if (handle == null) {
            val error = ReplySendException.MissingHandle(message.notificationKey)
            Log.w(TAG, "No reply handle for ${message.notificationKey}")
            observer.onFailure(error)
            scheduleRetryIfPossible(request.copy(fallbackHandle = null))
            delay(minIntervalMs)
            return
        }

        try {
            sendAction(appContext, handle, message.text)
            observer.onSuccess()
        } catch (error: ReplySendException.HandleExpired) {
            Log.w(TAG, "Reply handle expired for ${message.notificationKey}")
            observer.onFailure(error)
            handleProvider.invalidate(message.notificationKey)
            scheduleRetryIfPossible(request.copy(fallbackHandle = null))
        } catch (error: ReplySendException.TransportFailed) {
            Log.w(TAG, "Reply send failed for ${message.notificationKey}", error)
            observer.onFailure(error)
            scheduleRetryIfPossible(request.copy(fallbackHandle = null))
        } catch (error: Exception) {
            Log.e(TAG, "Unexpected error sending reply for ${message.notificationKey}", error)
            observer.onFailure(error)
        }

        delay(minIntervalMs)
    }

    private suspend fun scheduleRetryIfPossible(request: DispatchRequest) {
        val currentAttempt = request.message.attempt
        if (currentAttempt >= maxAttempts) {
            Log.w(TAG, "Max retry attempts reached for ${request.message.notificationKey}")
            return
        }
        val nextAttempt = currentAttempt + 1
        observer.onRetryScheduled(nextAttempt)
        val backoff = computeBackoffDelay(nextAttempt)
        delay(backoff)
        channel.trySend(
            DispatchRequest(
                message = request.message.copy(attempt = nextAttempt),
                fallbackHandle = null
            )
        )
    }

    private fun computeBackoffDelay(attempt: Int): Long {
        val base = 300L
        val multiplier = 1 shl (attempt - 1)
        val delay = base * multiplier
        return delay.coerceAtMost(1_200L)
    }

    private data class DispatchRequest(
        val message: OutgoingMessage,
        val fallbackHandle: ReplyHandle?
    )

    companion object {
        private const val TAG = "ReplyDispatcher"
    }
}
