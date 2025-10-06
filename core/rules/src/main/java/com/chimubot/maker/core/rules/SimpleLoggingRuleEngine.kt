package com.chimubot.maker.core.rules

import android.util.Log
import com.chimubot.maker.core.dispatch.ReplyDispatcher
import com.chimubot.maker.core.notif.CapturedNotification

/**
 * 1단계 프로토타입: 카카오톡 알림 본문에 고정 키워드가 포함되면 자동으로 응답을 큐에 넣는다.
 */
class SimpleLoggingRuleEngine(
    private val dispatcher: ReplyDispatcher,
    private val triggerKeyword: String = "자동응답"
) : RuleEngine {

    override fun onIncoming(notification: CapturedNotification) {
        if (notification.replyHandle == null) {
            Log.d(TAG, "Reply handle missing: ${notification.key}")
            return
        }
        val body = notification.text.orEmpty()
        if (!body.contains(triggerKeyword)) {
            Log.d(TAG, "No trigger keyword in ${notification.key}")
            return
        }
        dispatcher.enqueue(
            OutgoingMessage(
                notificationKey = notification.key,
                text = "[자동응답] ${triggerKeyword} 감지"
            ),
            notification.replyHandle
        )
    }

    companion object {
        private const val TAG = "SimpleRuleEngine"
    }
}
