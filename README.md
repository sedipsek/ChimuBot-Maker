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
대화 상태 추적과 텔레메트리 요구가 늘어남에 따라 `core` 모듈을 세분화하고, 영속 계층(`data/`)과 진단 기능(`features/diagnostics`)을 명시적으로 분리했습니다. 아래 구조는 알림 파이프라인과 운영 도구의 책임을 명확히 합니다.
```
app/
 ├─ core/
 │   ├─ notif/              # 알림 파싱·Reply 핸들 추출
 │   ├─ rules/              # 규칙 DSL, 매칭 엔진, 샌드박스 실행
 │   ├─ dispatch/           # SendQueue, RateLimiter, Retry 정책
 │   ├─ state/              # 알림/대화 캐시, TTL 관리
 │   └─ sys/                # NotificationListenerService, ForegroundSvc
 ├─ data/
 │   ├─ store/              # Room/ProtoDatastore, DAO, 마이그레이션
 │   ├─ telemetry/          # 전송 로그, 실패 통계 수집
 │   └─ prefs/              # 사용자 설정, 기능 토글
 ├─ features/
 │   ├─ scripts/            # 규칙 작성 UI, 스크립트 배포
 │   ├─ sharing/            # 공유 Intent + 접근성 보조
 │   └─ diagnostics/        # 알림 흐름 모니터링, 재연도구
 └─ ui/
     ├─ onboarding/         # 권한 온보딩, 배터리 최적화 안내
     ├─ settings/           # 전역 설정, 규칙 관리 접근
     └─ rules/              # 규칙 편집/테스트 화면
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

## 기능 로드맵
알림 파이프라인을 기반으로 실제 메신저 자동 응답 봇을 완성하기 위해 다음 단계별 기능을 계획합니다. 각 항목은 `core/`, `data/`, `features/`, `ui/` 모듈의 책임 분리에 맞춰 진행하며, 선행 조건과 산출물을 명확히 정의합니다.

### 1단계: 카카오톡 알림 파싱 기반 구축
- **목표:** 카카오톡의 채팅 알림 구조를 안정적으로 해석하여 `CapturedNotification` 모델에 담습니다.
- **주요 작업:**
  - `core/notif/NotificationParser` 확장: MessagingStyle 기반 메시지 배열, `EXTRA_CONVERSATION_TITLE`에서 room id 후보 추출.
  - `data/store`에 카카오톡 전용 파싱 결과 스키마 추가(방 식별자, 마지막 발신자, 멀티 라인 메시지 등).
  - Diagnostics 모듈에 원시 알림 JSON 덤프 뷰 제공(개발 모드 한정, 개인정보 마스킹).
- **선행 조건:** 알림 접근 권한 확보 UX 완료, 기본 Reply 액션 추출 기능 동작.
- **완료 기준:** 실제 카카오톡 알림 수신 시 룸 정보·메시지 본문·Reply 핸들이 로그/데이터 스토어에서 확인 가능.

### 2단계: Reply 전송 루틴 고도화
- **목표:** 카카오톡 Reply PendingIntent를 재사용해 다중 자동 응답을 안정적으로 전송합니다.
- **주요 작업:**
  - `core/dispatch/ReplySender`에 카카오톡 특화 예외 처리(알림 교체, CanceledException 백오프) 추가.
  - `core/state`에서 알림 수명/룸별 Reply 핸들 캐시 TTL 관리.
  - Telemetry에 전송 성공/실패 카운터 및 재시도 이력 기록.
- **선행 조건:** 카카오톡 알림 파싱이 룸 식별까지 지원.
- **완료 기준:** 동일 알림으로 3회 이상 자동 답장 테스트를 통과하고, 실패 시 재시도 로직이 동작.

### 3단계: 테스트 알림 생성기
- **목표:** 카카오톡 알림을 모방한 테스트 알림을 생성해 파서/전송 파이프라인을 앱 내에서 재현합니다.
- **주요 작업:**
  - `features/diagnostics`에 테스트 알림 생성 Activity 추가, MessagingStyle 구성 요소 선택 UI 제공.
  - `ui/onboarding` 또는 개발자 설정에 테스트 알림 트리거 버튼 배치.
  - 생성된 알림이 실제 카카오톡 알림과 동일한 extras/pendingIntent 인터페이스를 가지도록 시나리오 스크립트 정의.
- **선행 조건:** Reply 전송 루틴이 안정화되어 실제 알림에서 검증 완료.
- **완료 기준:** 테스트 알림으로 파서→규칙→전송 전체 플로우를 로컬에서 검증 가능, 실 단말 연결 없이 회귀 테스트 수행.

### 4단계: 봇 작동 관리 UI
- **목표:** 봇을 전체 On/Off 하거나 특정 방(room id) 단위로 제어할 수 있는 관리 기능을 제공합니다.
- **주요 작업:**
  - `ui/settings`에 글로벌 토글과 룸 리스트 관리 화면 추가, `data/prefs`에 상태 저장.
  - `core/state`에서 최근 대화 목록을 유지하고, 룸별 마지막 메시지 미리보기를 제공.
  - `features/scripts`의 RuleEngine 진입점에서 룸 필터(허용/차단 목록) 적용.
- **선행 조건:** 룸 id 추출 및 최근 대화 캐시가 작동.
- **완료 기준:** 사용자가 UI에서 룸을 선택해 봇 활성화 여부를 변경하면 즉시 규칙 평가에 반영.

### 5단계: 봇 제작 기능(초기 버전)
- **목표:** 사용자가 규칙 기반 봇을 생성/편집할 수 있는 제작 도구의 1차 버전을 제공합니다.
- **주요 작업(초안):**
  - 규칙 템플릿 갤러리와 간단한 스크립트 편집기(`features/scripts`) 구현.
  - `RuleEngine`이 사용자 정의 파라미터(키워드, 응답 텍스트, 시간 제한 등)를 수용하도록 DSL 확장.
  - 제작한 봇의 동작을 시뮬레이션하는 미리보기(테스트 알림과 연동) 제공.
- **선행 조건:** 테스트 알림 생성기와 룸 기반 봇 제어가 안정화.
- **완료 기준:** 최소 1개 이상의 커스텀 봇을 생성해 특정 룸에 배포하고, 자동 응답 결과를 텔레메트리에서 확인 가능.

### 장기 과제
- 전송 큐 레이트 리미터 자동 튜닝(트래픽 기반 동적 조절).
- 텔레메트리 시각화 대시보드 및 로그 내보내기.
- 접근성 보조를 통한 미디어 공유 워크플로(정책 검토 후).

## 추가 자료
- Android Developers: NotificationListenerService, RemoteInput, PendingIntent.
- Microsoft Learn: `ACTION_NOTIFICATION_LISTENER_SETTINGS` 진입.
- Google Play 정책: 접근성 서비스, 포그라운드 서비스 신고 요건.
