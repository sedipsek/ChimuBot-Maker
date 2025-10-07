package com.chimubot.maker.core.notif

import android.content.Context
import com.chimubot.maker.core.dispatch.ReplyDispatcher
import com.chimubot.maker.core.rules.RuleEngineRegistry
import com.chimubot.maker.core.rules.SimpleLoggingRuleEngine
import com.chimubot.maker.core.state.ReplyHandleCache
import com.chimubot.maker.core.state.ReplySendTelemetry

object NotificationServiceInitializer {
    @Volatile
    private var initialized = false
    private var dispatcher: ReplyDispatcher? = null

    fun ensure(context: Context) {
        if (!initialized) {
            synchronized(this) {
                if (!initialized) {
                    val createdDispatcher = ReplyDispatcher(
                        context = context,
                        handleProvider = ReplyHandleCache,
                        observer = ReplySendTelemetry
                    )
                    dispatcher = createdDispatcher
                    NotificationTargetRegistry.install(KakaoTalkOnlyFilter)
                    RuleEngineRegistry.install(SimpleLoggingRuleEngine(createdDispatcher))
                    initialized = true
                }
            }
        }
    }

    fun dispatcher(): ReplyDispatcher {
        return dispatcher ?: error("ReplyDispatcher not initialized")
    }
}
