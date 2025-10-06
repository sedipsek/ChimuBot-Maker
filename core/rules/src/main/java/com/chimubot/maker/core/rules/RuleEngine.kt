package com.chimubot.maker.core.rules

/**
 * 규칙 매칭과 전송 요청을 연결하는 최소 엔진 인터페이스.
 */
interface RuleEngine {
    fun onIncoming(notification: CapturedNotification)
}

object NoopRuleEngine : RuleEngine {
    override fun onIncoming(notification: CapturedNotification) = Unit
}

/**
 * NotificationListenerService에서 사용할 전역 엔진 레지스트리.
 * 앱 실행 시 실제 구현을 주입하고, 미초기화 상태에서는 Noop 엔진으로 동작한다.
 */
object RuleEngineRegistry {
    @Volatile
    private var delegate: RuleEngine = NoopRuleEngine

    fun install(engine: RuleEngine) {
        delegate = engine
    }

    fun current(): RuleEngine = delegate
}
