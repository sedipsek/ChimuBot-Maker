# ChimuBot Maker

ChimuBot Maker는 Android NotificationListenerService 기반의 자동 응답 엔진을 구현하기 위한 설계 문서와 레퍼런스 구조를 정리한 리포지터리입니다. 루트 권한 없이 알림을 수신하고, 규칙 기반으로 자동 답장을 전송하며, 동일 알림의 Reply `PendingIntent`를 재사용하여 여러 차례 응답할 수 있도록 하는 것을 목표로 합니다.

## 주요 목표
- **루트 불필요:** 시스템 권한 없이 사용자 허용을 통한 알림 접근.
- **알림 → 규칙 → 답장 파이프라인:** 수신된 알림을 파싱하여 규칙에 매칭하고 자동 응답을 전송.
- **Reply `PendingIntent` 재사용:** 동일 알림이 살아있는 동안 여러 번 자동 응답.
- **선택 기능:** 응답 결과 추적, 전송 큐/속도 제어, 간단한 텔레메트리.

## 비스코프
- 다른 앱 DB 직접 접근, 숨겨진 Binder 호출, 패치/리패키징, 내부 API 리플렉션 등 비공식 경로는 사용하지 않습니다.
- 미디어를 완전 무인으로 전송하지 않으며, 필요 시 공유 Intent와 접근성 보조를 결합합니다.

## 시스템 구성
```
app/
 ├─ core/
 │   ├─ notif/
 │   │   ├─ NotificationParser.kt
 │   │   ├─ ReplyActionFinder.kt
 │   │   └─ ReplySender.kt
 │   ├─ engine/
 │   │   ├─ RuleEngine.kt
 │   │   ├─ SendQueue.kt
 │   │   └─ RateLimiter.kt
 │   ├─ store/
 │   │   ├─ Entities.kt
 │   │   └─ Dao.kt
 │   └─ sys/
 │       ├─ NotifListener.kt
 │       └─ ForegroundSvc.kt
 ├─ features/
 │   ├─ scripts/
 │   └─ sharing/
 └─ ui/
     ├─ SettingsActivity.kt
     └─ RulesActivity.kt
```

## 핵심 동작 요약
1. **알림 수신:** `NotificationListenerService`로 알림/제거/랭킹 변동 콜백 처리.
2. **Reply 액션 추출:** `Notification.Action` 중 `RemoteInput`이 있는 Reply 액션을 찾음.
3. **자동 전송:** `RemoteInput.addResultsToIntent()` 후 `PendingIntent.send()`로 반복 응답.
4. **규칙 매칭:** `RuleEngine`이 조건을 평가하여 `SendQueue`에 메시지를 enqueue.
5. **전송 제어:** `RateLimiter`와 재시도 정책으로 안정성 확보.

## 권한 및 정책
- `BIND_NOTIFICATION_LISTENER_SERVICE`: 서비스 선언 후 사용자 설정에서 허용 유도.
- Android 13+: 자체 알림 표시 시 `POST_NOTIFICATIONS` 런타임 권한 요청.
- (선택) 포그라운드 서비스 사용 시 Android 14+에서 타입 선언 및 정책 준수.
- 배터리 최적화 예외는 필요한 경우에만 사용자 동의를 받고 요청.

## 구현 체크리스트
- 매니페스트에 알림 리스너 서비스 선언 및 온보딩 화면 제공.
- 알림 extras(`EXTRA_TITLE`, `EXTRA_TEXT` 등) 파싱과 Reply 핸들 캐싱.
- `PendingIntent.CanceledException` 처리 및 알림 재동기화 로직 구현.
- 잠금화면, Doze/App Standby, 제조사별 백그라운드 제약 대응.
- 프라이버시 및 Play 정책 준수: 접근성 서비스 사용 시 고지/동의 필수.

## 테스트 시나리오
- Reply 액션 탐지 및 반복 전송 검증.
- 알림 교체/종료 시 핸들 갱신.
- 잠금 상태 전송 및 재시도.
- Doze/배터리 최적화 환경에서 지연/안정성 확인.
- 제조사별 커스텀 OS에서 서비스 유지 여부 확인.

## 추가 자료
- Android Developers: NotificationListenerService, RemoteInput, PendingIntent.
- Microsoft Learn: `ACTION_NOTIFICATION_LISTENER_SETTINGS` 진입.
- Google Play 정책: 접근성 서비스, 포그라운드 서비스 신고 요건.
