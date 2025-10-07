package com.chimubot.maker.core.state

import android.os.SystemClock
import com.chimubot.maker.core.dispatch.ReplyHandle
import com.chimubot.maker.core.dispatch.ReplyHandleProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Reply PendingIntent를 메모리에 유지해 동일 알림 또는 동일 대화방에서 재사용할 수 있도록 한다.
 * NotificationListener에서 새로운 알림이 도착할 때마다 갱신되며 TTL이 지나면 자동으로 제거된다.
 */
object ReplyHandleCache : ReplyHandleProvider {

    private data class CacheEntry(
        val handle: ReplyHandle,
        val room: String?,
        val expiresAtElapsed: Long
    )

    private val lock = Any()
    private val entriesByKey = mutableMapOf<String, CacheEntry>()
    private val _metrics = MutableStateFlow(ReplyHandleMetrics())
    val metrics: StateFlow<ReplyHandleMetrics> = _metrics.asStateFlow()

    override fun currentHandleFor(notificationKey: String, room: String?): ReplyHandle? {
        val now = SystemClock.elapsedRealtime()
        synchronized(lock) {
            val changed = pruneExpiredLocked(now)
            if (changed) {
                publishMetricsLocked()
            }
            entriesByKey[notificationKey]?.let { entry ->
                if (entry.expiresAtElapsed > now) {
                    return entry.handle
                }
            }
            if (room != null) {
                entriesByKey.values.firstOrNull { entry ->
                    entry.room == room && entry.expiresAtElapsed > now
                }?.let { return it.handle }
            }
        }
        return null
    }

    override fun invalidate(notificationKey: String) {
        synchronized(lock) {
            val removed = entriesByKey.remove(notificationKey)
            if (removed != null) {
                publishMetricsLocked()
            }
        }
    }

    fun upsert(notificationKey: String, room: String?, handle: ReplyHandle) {
        val expiresAt = SystemClock.elapsedRealtime() + handle.ttlMs
        synchronized(lock) {
            val changed = pruneExpiredLocked(SystemClock.elapsedRealtime())
            if (room != null) {
                val iterator = entriesByKey.entries.iterator()
                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (entry.value.room == room) {
                        iterator.remove()
                    }
                }
            }
            entriesByKey[notificationKey] = CacheEntry(handle, room, expiresAt)
            if (changed) {
                publishMetricsLocked()
            }
            publishMetricsLocked()
        }
    }

    fun pruneExpired() {
        synchronized(lock) {
            val changed = pruneExpiredLocked(SystemClock.elapsedRealtime())
            if (changed) {
                publishMetricsLocked()
            }
        }
    }

    private fun pruneExpiredLocked(now: Long): Boolean {
        var changed = false
        val iterator = entriesByKey.entries.iterator()
        while (iterator.hasNext()) {
            val entry = iterator.next()
            if (entry.value.expiresAtElapsed <= now) {
                iterator.remove()
                changed = true
            }
        }
        return changed
    }

    private fun publishMetricsLocked() {
        val activeCount = entriesByKey.size
        val timestamp = System.currentTimeMillis()
        _metrics.update {
            it.copy(
                activeCount = activeCount,
                lastUpdatedAt = timestamp
            )
        }
    }
}

/**
 * Reply 핸들 캐시 현황.
 */
data class ReplyHandleMetrics(
    val activeCount: Int = 0,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)
