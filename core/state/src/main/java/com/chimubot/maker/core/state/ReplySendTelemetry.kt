package com.chimubot.maker.core.state

import com.chimubot.maker.core.dispatch.ReplySendObserver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Reply 전송 성공/실패/재시도 이력을 추적해 UI에서 시각화할 수 있도록 한다.
 */
object ReplySendTelemetry : ReplySendObserver {

    private val _metrics = MutableStateFlow(ReplySendMetrics())
    val metrics: StateFlow<ReplySendMetrics> = _metrics.asStateFlow()

    override fun onSuccess() {
        val now = System.currentTimeMillis()
        _metrics.update {
            it.copy(
                successCount = it.successCount + 1,
                lastUpdatedAt = now
            )
        }
    }

    override fun onFailure(error: Throwable) {
        val now = System.currentTimeMillis()
        val message = error.message ?: error::class.java.simpleName
        _metrics.update {
            it.copy(
                failureCount = it.failureCount + 1,
                lastErrorMessage = message,
                lastFailureAt = now,
                lastUpdatedAt = now
            )
        }
    }

    override fun onRetryScheduled(attempt: Int) {
        val now = System.currentTimeMillis()
        _metrics.update {
            it.copy(
                retryCount = it.retryCount + 1,
                lastRetryAttempt = attempt,
                lastUpdatedAt = now
            )
        }
    }
}

/**
 * Reply 전송 상태 요약.
 */
data class ReplySendMetrics(
    val successCount: Long = 0,
    val failureCount: Long = 0,
    val retryCount: Long = 0,
    val lastErrorMessage: String? = null,
    val lastFailureAt: Long? = null,
    val lastRetryAttempt: Int = 0,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)
