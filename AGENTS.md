# Repository Agent Instructions

이 리포지터리의 모든 변경 사항은 다음 지침을 따른다.

1. **문서 중심:** README 및 추가 문서는 한국어를 기본으로 작성하되, 코드 스니펫과 API 이름은 원문(영문)을 유지한다.
2. **구조 준수:** `README.md`에 기재된 디렉터리 구조와 모듈 역할을 변경할 경우, 변경 이유와 새로운 구조를 README에 명확히 반영한다.
3. **정책 강조:** 알림 접근 권한, 포그라운드 서비스, 접근성 서비스 등 정책/권한 관련 항목을 수정할 때는 관련 근거(공식 문서 링크 등)를 업데이트한다.
4. **테스트 기록:** 자동 응답 로직이나 큐/레이트 리미터 관련 변경 시에는 테스트 섹션에 해당 시나리오를 추가하고 결과를 명시한다.
5. **PR 요약:** Pull Request 설명에는 주요 목표(루트 불필요, 규칙 매칭, Reply PendingIntent 재사용)를 항상 포함한다.

---

# 치무봇 메이커(ChimuBot Maker) — 봇 제작 방식 설계 문서

## 1. 개요
치무봇 메이커는 **Node-RED 스타일의 비주얼 플로우 편집기**와 **메신저봇 R 호환 JS 런타임**을 결합한 하이브리드 자동응답 봇 제작 플랫폼이다.  
사용자는 GUI에서 트리거-조건-액션 노드를 시각적으로 연결하여 규칙을 구성하며, 내부 엔진은 이를 JSON 플로우 DSL로 직렬화하여 실행 그래프로 변환한다.

---

## 2. 핵심 구성
### 2.1 플로우 구조
```
[Trigger: MessageReceived]
      ↓
[Condition: RegexMatch]
      ↓
[Script Node (JS Block)]
      ↓
[Action: Reply]
      ↓
[Action: Webhook]
```

### 2.2 계층별 역할
| 계층 | 역할 | 관련 모듈 |
|------|------|-----------|
| **GUI (FlowCanvas)** | 노드 생성·연결·편집, 속성 패널 및 코드 에디터 제공 | `features/scripts` (Flow Builder UI) |
| **RuleEngine** | JSON 플로우 DSL 파싱 → 실행 그래프 구성 | `core/rules` |
| **Execution Runtime** | 노드 타입별 핸들러 실행, JS 샌드박스 실행 포함 | `core/rules`, `core/dispatch`, `core/state` |
| **Compat Layer** | 메신저봇 R 스크립트 API (`response()`, `replier.reply()` 등) 호환 | `core/rules/compat` |
| **Storage** | 룰 및 플로우 데이터 저장 (Room 또는 DataStore) | `data/store`, `data/prefs` |

---

## 3. 하이브리드 모델
각 노드에는 옵션으로 **JS Script Block**을 내장할 수 있다.
사용자가 GUI 노드 속성에서 직접 코드를 작성하면 엔진은 이를 QuickJS 샌드박스에서 실행한다.
QuickJS 런타임은 메신저봇 R API2 사양에서 정의한 전역 객체(`Api`, `Log`, `Utils`, `FileStream` 등)를 동일한 시그니처로 바인딩해야 하며, 레퍼런스는 Dark Tornado와 KBOT Docs의 API2 문서를 기준으로 관리한다.

예시 코드:
```
if (msg.includes("주문")) {
  replier.reply("주문 확인했습니다 😎");
  webhook("https://api/order", {room, msg});
}
```

→ Node-RED의 Function 노드 + 메신저봇 R의 스크립트 함수를 통합한 형태.

---

## 4. 메신저봇 R 호환성
- 기존 메신저봇 R 스크립트(`response(room, msg, sender, isGroupChat, replier, ...)`)를 그대로 실행 가능.
- **ChimuCompatLayer.kt**에서 다음 API를 바인딩:
  - `replier.reply(text)` → `core/dispatch/ReplySender.send()`로 위임
  - `Api`, `Log`, `FileStream`, `Utils`, `Device`, `Utils.getRoomList()` 등 API2에서 필수로 요구하는 객체/메서드를 Stub 또는 실제 동작과 연결
- GUI에서 `.js` 스크립트를 직접 불러와 Script Node로 삽입 가능하며, 노드 속성 패널에서 API2 함수 호출을 템플릿으로 제안한다.
- API2에서 새롭게 추가된 이벤트(`onCreate`, `onCommand`, `onNotificationPosted` 등)가 필요할 경우 FlowCanvas 노드 타입으로 선행 정의하고, Execution Runtime에서 해당 이벤트를 트리거로 매핑한다.

---

## 5. JSON 플로우 DSL 예시
```
{
  "nodes": [
    {"id":"n1","type":"trigger.message","params":{"app":"com.kakao.talk"}},
    {"id":"n2","type":"condition.regex","params":{"pattern":".*주문.*"}},
    {"id":"n3","type":"script.js","params":{"code":"replier.reply('주문완료');"}},
    {"id":"n4","type":"action.reply","params":{"text":"감사합니다!"}}
  ],
  "edges": [
    {"from":"n1","to":"n2"},
    {"from":"n2","to":"n3"},
    {"from":"n3","to":"n4"}
  ]
}
```

`RuleEngine.buildFrom(json)` → ExecutionGraph 생성 → 각 노드 핸들러 실행.

---

## 6. 런타임 상호작용
- **JS 엔진:** QuickJS (경량 임베디드 JS)  
- **Sandbox:** 전역 객체 격리(`room`, `msg`, `replier` 등 바인딩)  
- **Replier Bridge:** 메신저봇 API → Notification Reply 액션 직접 연결  
- **Context Storage:** 노드 간 변수 공유(`context.set/get`) 지원 (`core/state` 연동)  

---

## 7. GUI 모듈 설계
| 모듈 | 설명 | 배치 |
|------|------|------|
| **FlowCanvas** | 노드 드래그·연결, 연결선 렌더링 (Compose Canvas or ReactFlow) | `features/scripts/flowbuilder` |
| **NodePropertyPanel** | 노드별 파라미터 편집 (정규식, 메시지 등) | `features/scripts/flowbuilder` |
| **ScriptEditorDialog** | Script Node 용 코드 에디터 (Monaco / CodeMirror), API2 함수 인텔리센스/자동완성 제공 | `features/scripts/editor` |
| **SimulationPanel** | 입력 이벤트 테스트 + 로그 출력 | `features/diagnostics`와 연계 |
| **FlowManager** | JSON 로드/세이브/내보내기 | `data/store` ↔ `features/scripts` |
| **RuntimeConnector** | RuleEngine와 실시간 상태 동기화 | `core/rules` ↔ `features/scripts` |

---

## 8. 기술 스택
| 영역 | 기술 |
|------|------|
| **언어/플랫폼** | Kotlin + Jetpack Compose |
| **Persistence** | Room 또는 Proto DataStore |
| **JS Runtime** | QuickJS 또는 Rhino (개발 단계에서 선택), API2 표준 전역 객체 바인딩 |
| **네트워킹** | OkHttp / Retrofit |
| **로깅** | Timber + 내장 LogView |
| **멀티모듈** | `core/`(notif, rules, dispatch, state), `data/`(store, telemetry, prefs), `features/`(scripts, sharing, diagnostics), `runtime/compat`(메신저봇 API 브리지) |

---

## 9. 개발 단계 권장 순서
1. **Core 엔진 프로토타입** – NotificationListener + ReplySender + RuleEngine (`core/notif`, `core/rules`, `core/dispatch`).
2. **플로우 DSL → 실행 그래프** 변환기 구현 (`core/rules`와 `data/store`).
3. **QuickJS 통합 및 Compat Layer** 완성 (`runtime/compat`, `core/rules/compat`).
4. **Node-RED 형식 GUI (FlowCanvas + Property Panel)** – `features/scripts/flowbuilder`.
5. **시뮬레이터 및 런타임 연동 테스트** – `features/diagnostics` + `core/rules`.
6. **규칙 패키지 배포/공유 기능** 추가 – `features/scripts` + `data/store`.

---

## 10. 요약 비전
> **ChimuBot Maker = Node-RED + 메신저봇 R + Notification Automation**
> GUI로 규칙을 만들고, JS 로직을 혼합하며, 메신저봇 R 스크립트까지 실행하는 루트리스 자동응답 플랫폼.

---

## 11. 1단계 코드 구현 지침
- `core/notif`은 **KakaoTalk 알림 전용 필터**를 기본값으로 유지한다. 다른 메신저를 다룰 때는 새로운 `NotificationTargetFilter`를 추가하고 레지스트리에 명시적으로 교체한다.
- `core/dispatch`의 `ReplyDispatcher`는 코루틴 기반 싱글 컨슈머를 유지하며, rate-limit 상수는 80~200ms 범위 내에서만 수정한다. 재시도 정책을 확장할 때는 `RuleEngine`에 실패 신호를 전달할 수 있는 콜백을 추가한다.
- `core/rules`는 `RuleEngineRegistry`를 통해 전역 인스턴스를 노출한다. NotificationListener에서 직접 구현 클래스를 참조하지 말고, 레지스트리에 주입하도록 유지한다.
- Kotlin JVM 타겟은 17로 고정하며, 새로운 모듈을 추가할 때 동일한 `compileOptions`/`kotlinOptions` 설정을 복제한다.
- 테스트나 샘플 코드를 위해 `SimpleLoggingRuleEngine`을 수정할 경우, README의 “1단계 프로토타입 구현 현황”을 갱신해 동작이 어떻게 바뀌었는지 기록한다.

## 12. 2단계 Reply 전송 고도화 지침
- `core/state/ReplyHandleCache`는 Reply `PendingIntent`의 TTL을 `SystemClock.elapsedRealtime()` 기준으로 관리한다. 알림이 교체되거나 제거되면 반드시 `invalidate(key)`를 호출하고, UI와 동기화되는 `metrics` 흐름을 최신 상태로 유지한다.
- `core/state/ReplySendTelemetry`는 `ReplySendObserver` 인터페이스를 구현해 성공/실패/재시도 이벤트를 `StateFlow`로 노출한다. UI는 이 흐름을 직접 구독하므로 필드를 제거하거나 이름을 바꿀 때는 README와 레이아웃 문서를 동시에 수정한다.
- `ReplyDispatcher`는 `ReplyHandleProvider`를 통해 핸들을 조회하고, 실패 시 `handleProvider.invalidate()`로 캐시를 무효화한다. 백오프는 300ms부터 시작해 두 배씩 증가하되 1.2s를 상한으로 유지한다.
- UI 모듈(`NotificationLogActivity`)은 Stage 2 Telemetry를 시각화해야 하므로, 전송 관련 변경 시 상단 카드가 새로운 메트릭을 반영하도록 업데이트한다.
