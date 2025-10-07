package com.chimubot.maker.core.dispatch

/**
 * Reply 전송 결과/재시도 이벤트를 관찰하기 위한 훅. Telemetry 모듈이 구현한다.
 */
interface ReplySendObserver {
    fun onSuccess()
    fun onFailure(error: Throwable)
    fun onRetryScheduled(attempt: Int)

    companion object {
        val NONE: ReplySendObserver = object : ReplySendObserver {
            override fun onSuccess() = Unit
            override fun onFailure(error: Throwable) = Unit
            override fun onRetryScheduled(attempt: Int) = Unit
        }
    }
}
