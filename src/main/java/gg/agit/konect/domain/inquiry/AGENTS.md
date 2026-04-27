# 문의 도메인 가이드

## 이 도메인은 무엇을 하는가

문의 도메인은 사용자가 어드민에게 보내는 일반 문의를 접수하고,
트랜잭션 commit 이후 Slack 알림으로 전달하는 도메인이다.

이 도메인에서 중요한 것은 문의 내용을 DB에 저장하는 것이 아니라
아래 경계가 같은 정책을 바라보는 것이다.

- 공개 문의 API (`POST /inquiries`)
- 문의 요청 DTO (`InquiryRequest`)
- 문의 제출 이벤트 (`InquirySubmittedEvent`)
- Slack 문의 알림 리스너 (`InquirySlackListener`)
- Slack 메시지 포맷과 event webhook (`SlackNotificationService.notifyInquiry`)

문의 관련 작업을 할 때는 항상 "이 변경이 공개 API 검증, 이벤트 발행,
commit 이후 Slack 알림, 채팅 문의방 정책과의 분리까지 같이 맞는가"를 먼저 확인해야 한다.

## 주요 상태

### `InquiryRequest`

- 문의 API의 요청 DTO다.
- `content`는 blank일 수 없다.
- 현재 요청 DTO에는 최대 길이 제한이 없다.
- `@NotBlank` 외의 trimming, sanitizing, masking 정책은 없다.

### `InquirySubmittedEvent`

- 문의 내용을 후속 알림으로 넘기는 이벤트다.
- 현재 이벤트 payload는 `content` 하나뿐이다.
- 사용자 id, 이메일, 요청 시각, 저장 id는 포함하지 않는다.
- 서비스는 전달받은 content를 그대로 이벤트에 담는다.

### Slack 알림

- `InquirySlackListener`는 `InquirySubmittedEvent`를 `AFTER_COMMIT`에 처리한다.
- 리스너는 `slackTaskExecutor`로 비동기 실행된다.
- Slack 메시지는 `SlackMessageTemplate.INQUIRY` 형식으로 만든다.
- Slack 전송 대상은 event webhook (`slackProperties.webhooks().event()`)이다.

## 기능이 실제로 어떻게 동작해야 하는가

### 문의 전송

- endpoint는 `POST /inquiries`다.
- `@PublicApi`이므로 로그인 없이 호출할 수 있다.
- 요청 body는 JSON이어야 하고 `content`를 포함해야 한다.
- `content`가 blank면 400 응답이며 `INVALID_REQUEST_BODY`다.
- 요청 body가 없거나 JSON 형식이 아니면 400 응답이며 `INVALID_JSON_FORMAT`이다.
- 성공 응답은 200 OK이고 body는 없다.
- 컨트롤러는 `request.content()`를 그대로 서비스에 넘긴다.

### 이벤트 발행

- `InquiryService.submitInquiry`는 별도 저장 없이 `InquirySubmittedEvent`를 발행한다.
- 이벤트 content는 서비스에 들어온 content와 같아야 한다.
- 현재 서비스는 문의 내용을 trim하거나 마스킹하지 않는다.
- 문의 접수를 영속 데이터로 남기는 정책을 추가하려면 이벤트 payload와 저장 실패/알림 실패 정책을 함께 재정의해야 한다.

### Slack 후속 처리

- Slack 알림은 원 트랜잭션이 commit된 뒤 실행되어야 한다.
- 원 트랜잭션이 rollback되면 Slack 알림도 실행되면 안 된다.
- 리스너는 이벤트 content를 그대로 `SlackNotificationService.notifyInquiry`에 위임한다.
- Slack 전송 실패를 문의 API 응답 정책으로 바꾸려면 비동기 리스너, 예외 처리, 재시도 정책을 함께 확인해야 한다.

## 절대 놓치면 안 되는 정책

- 문의 API는 공개 API다. 인증 사용자 전제를 넣으면 안 된다.
- 문의 내용은 현재 DB에 저장하지 않는다.
- `content` blank만 막고, 최대 길이 제한은 없다.
- 서비스는 content를 그대로 이벤트로 발행한다.
- Slack 알림은 `AFTER_COMMIT` 이후 실행된다.
- 이 도메인은 채팅 도메인의 `SYSTEM_ADMIN` 문의방 정책과 다르다.
- 채팅 문의방 생성, 채팅방 reopen, 마지막 메시지 갱신 정책을 이 도메인에 섞으면 안 된다.
- 사용자 입력이 Slack으로 전달되므로, 로그나 에러 응답에 content를 불필요하게 노출하지 않는다.

## 수정 시 함께 확인해야 하는 것

### API 검증을 바꿀 때

- `InquiryRequest.content`의 Bean Validation
- `InquiryApi.submitInquiry`의 `@Valid @RequestBody`
- blank content 400 응답 코드
- body 누락 또는 JSON 형식 오류 응답 코드
- 공개 API 유지 여부

### 이벤트 payload를 바꿀 때

- `InquirySubmittedEvent`
- `InquiryService.submitInquiry`
- `InquirySlackListener.handleInquirySubmitted`
- Slack 메시지 템플릿의 인자 순서
- 새 payload가 개인정보를 포함할 경우 로그와 Slack 노출 범위

### Slack 알림 정책을 바꿀 때

- `@TransactionalEventListener(phase = AFTER_COMMIT)` 유지 여부
- `@Async("slackTaskExecutor")` 유지 여부
- `SlackNotificationService.notifyInquiry`
- `SlackMessageTemplate.INQUIRY`
- event webhook과 error webhook 중 어느 채널을 써야 하는지
- Slack 실패가 API 성공 여부에 영향을 줘야 하는지

### 채팅 문의 정책과 함께 바꿀 때

- 채팅 도메인의 `SYSTEM_ADMIN` 직접 문의방 정책
- 채팅방 reopen 정책
- 채팅방 마지막 메시지 메타데이터 갱신 정책
- Slack 문의 알림과 채팅 문의방 생성의 사용자 경험 차이

## 주요 클래스와 책임

### `InquiryApi`

- 문의 API 스펙과 공개 API 여부를 정의한다.
- 요청 DTO 검증은 이 인터페이스의 `@Valid @RequestBody` 조합에 걸려 있다.

### `InquiryController`

- HTTP 요청을 서비스 호출로 넘기고 성공 시 200 OK를 반환한다.
- 별도 응답 body를 만들지 않는다.

### `InquiryService`

- 문의 접수의 도메인 경계다.
- 현재 책임은 문의 이벤트 발행뿐이다.

### `InquirySubmittedEvent`

- 문의 내용을 Slack 알림으로 넘기는 이벤트 payload다.
- 현재 content 외 상태를 갖지 않는다.

### `InquirySlackListener`

- 문의 이벤트를 commit 이후 비동기로 처리한다.
- Slack 알림 서비스에 content를 위임한다.

### `SlackNotificationService`

- 문의 Slack 메시지를 템플릿으로 포맷하고 event webhook으로 전송한다.

## 테스트 전략

이미 통합 테스트는 아래 정책을 고정한다.

- `POST /inquiries` 성공 시 200 OK
- blank content는 400과 `INVALID_REQUEST_BODY`
- 요청 body 누락은 400과 `INVALID_JSON_FORMAT`

단위 테스트는 아래 정책을 고정한다.

- `InquiryService.submitInquiry`가 content를 그대로 `InquirySubmittedEvent`로 발행한다.
- `InquirySlackListener`가 이벤트 content를 Slack 알림 서비스에 그대로 위임한다.

이 도메인의 정책을 바꾸거나 가이드 claim을 강화한다면
추가로 아래 회귀 테스트를 보강하는 것이 좋다.

- content 최대 길이 제한을 추가할 경우 경계값 테스트
- content trimming 또는 sanitizing 정책을 추가할 경우 원문/가공 결과 테스트
- Slack 실패가 문의 API 응답에 영향을 주도록 바꿀 경우 실패 전파 테스트
- 이벤트 payload에 사용자 정보를 추가할 경우 인증/비인증 호출 정책 테스트

검증할 때는 최소한 아래 테스트를 실행한다.

```bash
CI=true ./gradlew test --tests 'gg.agit.konect.integration.domain.inquiry.*' --tests 'gg.agit.konect.unit.domain.inquiry.*' --tests 'gg.agit.konect.unit.infrastructure.slack.listener.InquirySlackListenerTest'
```
