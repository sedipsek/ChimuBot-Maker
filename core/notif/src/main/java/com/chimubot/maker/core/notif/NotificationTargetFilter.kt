package com.chimubot.maker.core.notif

interface NotificationTargetFilter {
    fun shouldProcess(packageName: String): Boolean
}

object AllowAllTargetFilter : NotificationTargetFilter {
    override fun shouldProcess(packageName: String): Boolean = true
}

object KakaoTalkOnlyFilter : NotificationTargetFilter {
    override fun shouldProcess(packageName: String): Boolean = packageName == KAKAO_TALK_PACKAGE

    private const val KAKAO_TALK_PACKAGE = "com.kakao.talk"
}

object NotificationTargetRegistry {
    @Volatile
    private var delegate: NotificationTargetFilter = AllowAllTargetFilter

    fun install(filter: NotificationTargetFilter) {
        delegate = filter
    }

    fun current(): NotificationTargetFilter = delegate
}
