# 알림 도메인 가이드

## 이 도메인은 무엇을 하는가

알림 도메인은 Expo push token 관리, 채팅 푸시 알림, 동아리 지원 관련 인앱 알림, SSE 실시간 전달, 알림함 읽음 상태를 관리하는 도메인이다.

이 도메인에서 중요한 것은 단순히 외부 push API를 호출하는 것이 아니라 아래 상태와 후속 효과가 같은 정책을 바라보는 것이다.

- 사용자별 Expo device token (`NotificationDeviceToken`)
- 인앱 알림함 (`NotificationInbox`)
- 채팅방별 mute 설정 (`NotificationMuteSetting`)
- 현재 채팅방 접속 상태 (`ChatPresenceService`)
- SSE emitter 연결 상태 (`NotificationInboxSseService`)
- Expo push 발송과 재시도 (`ExpoPushClient`)
- 동아리 지원 이벤트 리스너 (`ClubApplicationNotificationListener`)

알림 관련 작업을 할 때는 항상 "이 변경이 push token, 인앱 알림 저장, SSE 전달, 채팅방 접속/뮤트 필터, 이벤트 commit 이후 발송까지 같이 맞는가"를 먼저 확인해야 한다.

## 주요 상태

### `NotificationDeviceToken`

- 사용자별 Expo push token을 저장한다.
- DB의 `notification_device_token.user_id`는 unique 제약을 가진다.
- 현재 구현 기준으로 한 사용자에게 활성 token row는 하나만 존재한다.
- token 문자열은 `ExponentPushToken[...]` 또는 `ExpoPushToken[...]` 형식만 허용한다.
- token 조회는 row가 없으면 `NOT_FOUND_NOTIFICATION_TOKEN`으로 실패한다.
- token 삭제는 요청한 userId와 token이 정확히 일치할 때만 삭제하고, 없으면 조용히 끝난다.

### `NotificationInbox`

- 인앱 알림함에 보이는 알림이다.
- 생성 시 `isRead`는 항상 `false`다.
- 알림 타입, 제목, 본문, 이동 path를 함께 저장한다.
- 단건 읽음 처리는 `notificationId + userId`로 찾은 본인 알림만 읽음 처리한다.
- 전체 읽음 처리는 채팅 관련 타입을 제외한 알림만 대상으로 한다.

### `NotificationMuteSetting`

- 현재 mute 대상 타입은 `CHAT_ROOM`뿐이다.
- unique 기준은 `(target_type, target_id, user_id)`다.
- `isMuted`가 null로 들어오면 false로 저장된다.
- `toggleMute()`는 현재 값을 반전한다.
- 채팅 알림 발송에서는 `isMuted = true`인 사용자만 제외한다.

### `NotificationInboxSseService`

- 사용자별 SSE emitter를 메모리 맵에 보관한다.
- 같은 사용자가 다시 구독하면 기존 emitter를 완료하고 새 emitter로 교체한다.
- 구독 성공 시 `connect` 이벤트로 `connected` 데이터를 보낸다.
- timeout, completion, error가 발생하면 현재 emitter와 일치할 때만 맵에서 제거한다.
- 알림 전송 실패가 `IOException` 또는 `IllegalStateException`이면 emitter를 제거한다.

## 기능이 실제로 어떻게 동작해야 하는가

### Push token 등록과 삭제

- token 등록은 먼저 활성 사용자를 조회한다.
- Expo token 형식이 아니면 `INVALID_NOTIFICATION_TOKEN`이다.
- 기존 token row가 있으면 새 token 값으로 갱신한다.
- 기존 token row가 없으면 새 `NotificationDeviceToken`을 저장한다.
- 삭제는 `userId + token`으로 찾은 row만 삭제한다.
- 삭제 대상이 없으면 예외를 던지지 않는다.

### 단일 채팅 푸시 알림

- `sendChatNotification`은 비동기(`notificationTaskExecutor`)로 실행된다.
- 수신자가 해당 채팅방에 접속 중이면 푸시를 보내지 않는다.
- 해당 채팅방을 mute한 사용자에게는 푸시를 보내지 않는다.
- 수신자의 push token이 없으면 푸시를 보내지 않는다.
- 메시지 본문 preview는 Unicode code point 기준 최대 30자다.
- 30자를 넘으면 앞 30 code point 뒤에 `...`를 붙인다.
- 메시지가 null이면 빈 문자열 preview를 쓴다.
- push payload의 path는 `chats/{roomId}`다.
- 이 흐름에서 발생한 예외는 잡아서 로그로 남기고 호출 흐름으로 전파하지 않는다.

### 그룹 채팅 푸시 알림

- `sendGroupChatNotification`도 비동기(`notificationTaskExecutor`)로 실행된다.
- 수신자 목록에서 발신자를 먼저 제외한다.
- 남은 수신자가 없으면 푸시를 보내지 않는다.
- 채팅방에 접속 중인 사용자와 mute 사용자를 제외한다.
- 최종 대상이 없으면 푸시를 보내지 않는다.
- 최종 대상 사용자들의 token을 조회하고, token별 Expo batch message를 만든다.
- title은 동아리 이름, body는 `senderName + ": " + preview` 형식이다.
- payload path는 `chats/{roomId}`다.
- batch message가 비어 있지 않을 때만 Expo batch 발송을 호출한다.
- 이 흐름에서 발생한 예외는 잡아서 로그로 남기고 호출 흐름으로 전파하지 않는다.

### 동아리 지원 알림

- 동아리 지원 알림은 push만 보내는 것이 아니라 인앱 알림 저장, SSE, push를 함께 수행한다.
- 지원 제출 알림:
  - type: `CLUB_APPLICATION_SUBMITTED`
  - title: 동아리 이름
  - body: `{applicantName}님이 동아리 가입을 신청했어요.`
  - path: `mypage/manager/{clubId}/applications/{applicationId}`
- 지원 승인 알림:
  - type: `CLUB_APPLICATION_APPROVED`
  - title: 동아리 이름
  - body: `동아리 지원이 승인되었어요.`
  - path: `clubs/{clubId}`
- 지원 거절 알림:
  - type: `CLUB_APPLICATION_REJECTED`
  - title: 동아리 이름
  - body: `동아리 지원이 거절되었어요.`
  - path: `clubs/{clubId}`
- 각 알림은 `NotificationInbox`를 저장한 뒤 저장된 값을 `NotificationInboxResponse`로 바꿔 SSE로 보낸다.
- push token이 없으면 인앱 알림과 SSE는 유지하고 push만 생략한다.

### 동아리 이벤트 리스너

- `ClubApplicationNotificationListener`는 동아리 지원 이벤트를 `AFTER_COMMIT`에 처리한다.
- 원 트랜잭션이 rollback되면 알림 후속 작업도 실행되지 않는다.
- 승인/거절 이벤트는 단일 수신자에게 알림을 보낸다.
- 제출 이벤트는 `receiverIds`의 각 사용자에게 개별 알림을 보낸다.
- 이벤트 리스너를 바꿀 때는 동아리 도메인의 이벤트 발행 시점과 트랜잭션 경계를 함께 확인해야 한다.

### 인앱 알림 목록과 읽음 처리

- 목록 조회는 page 기본값 1, page size 20이다.
- 정렬은 `createdAt DESC, id DESC`다.
- 목록 조회와 미읽음 개수, 전체 읽음 처리는 채팅 관련 타입을 제외한다.
- 제외되는 채팅 관련 타입은 `CHAT_MESSAGE`, `GROUP_CHAT_MESSAGE`, `UNREAD_CHAT_COUNT`다.
- 단건 읽음은 본인 알림만 가능하고, 다른 사용자의 알림 id는 찾지 못한 알림으로 처리된다.
- 전체 읽음은 채팅 관련 타입을 제외한 미읽음 알림만 읽음 처리한다.

### SSE 구독과 전송

- SSE timeout은 30분이다.
- 구독 직후 `connect` 이벤트를 보낸다.
- 한 사용자에게는 최신 emitter 하나만 유지한다.
- 이전 emitter의 completion callback이 늦게 실행되더라도 새 emitter를 제거하면 안 된다.
- 알림 전송 시 emitter가 없으면 조용히 종료한다.
- 전송 중 emitter가 이미 완료되어 있거나 IO 오류가 나면 맵에서 제거한다.
- SSE 실패는 인앱 알림 저장이나 push 발송 정책과 분리해서 생각해야 한다.

### Expo push client

- Expo push endpoint는 `https://exp.host/--/api/v2/push/send`다.
- 단건 사용자 발송도 token 목록을 message 목록으로 변환해서 Expo API를 호출한다.
- batch 발송은 100개씩 나눠 보낸다.
- HTTP 상태가 2xx가 아니거나 응답 body/data가 없으면 실패로 본다.
- Expo ticket의 status가 `ok`가 아니면 해당 token 실패를 error 로그로 남긴다.
- `@Retryable(maxAttempts = 2)`로 재시도한다.
- HTTP 오류, 연결 오류, 비정상 응답, 기타 RestClient 오류는 recover 메서드에서 로그로 남긴다.

## 절대 놓치면 안 되는 정책

- 사용자별 device token은 하나만 유지한다. 새 token 등록은 row 추가가 아니라 기존 row 갱신일 수 있다.
- token 형식 검증은 Expo token 문자열 형식 기준이다.
- 채팅 push는 사용자가 이미 채팅방에 있거나 mute한 경우 보내면 안 된다.
- 그룹 채팅 push는 발신자를 수신자에서 제외해야 한다.
- 채팅 preview는 Java 문자열 길이가 아니라 Unicode code point 기준 30자를 사용한다.
- 채팅 알림 실패는 메시지 저장이나 채팅 흐름을 실패시키면 안 된다.
- 동아리 지원 알림은 인앱 저장, SSE, push가 함께 움직인다.
- 동아리 지원 이벤트는 commit 이후에만 알림으로 이어져야 한다.
- 인앱 알림 목록/미읽음/전체 읽음은 채팅 관련 타입을 제외한다.
- SSE는 사용자당 최신 연결 하나만 유지한다.
- Expo push 실패 ticket은 전체 요청 성공 여부와 별도로 token별 실패 로그를 확인해야 한다.

## 수정 시 함께 확인해야 하는 것

### Push token 정책을 바꿀 때

- `notification_device_token.user_id` unique 제약
- Expo token 정규식
- 기존 token 갱신과 신규 저장 분기
- 삭제 요청의 userId/token 일치 기준
- 탈퇴 사용자 token 조회 제외 여부

### 채팅 알림을 바꿀 때

- `ChatPresenceService` 접속 상태 필터
- `NotificationMuteSetting`의 `CHAT_ROOM` mute 필터
- 발신자 제외 정책
- Unicode code point 기준 preview 길이
- payload path (`chats/{roomId}`)
- 예외를 삼키고 로그로 남기는 비동기 경계

### 동아리 지원 알림을 바꿀 때

- 동아리 이벤트 발행 시점
- `AFTER_COMMIT` 리스너 유지 여부
- inbox type/title/body/path
- SSE 전송 대상과 응답 DTO
- push token이 없을 때 인앱 알림을 유지하는 정책

### 인앱 알림함을 바꿀 때

- 채팅 관련 타입 제외 집합
- page size 20
- `createdAt DESC, id DESC` 정렬
- 단건 읽음의 userId 소유권 조건
- 전체 읽음에서 채팅 관련 타입 제외

### SSE를 바꿀 때

- 사용자별 emitter 교체 정책
- completion/timeout/error callback의 조건부 제거
- 최초 connect 이벤트
- send 실패 시 emitter 제거
- 이미 완료된 emitter 처리

### Expo push client를 바꿀 때

- batch size 100
- channel id `default_notifications`
- 2xx 상태와 body/data 검증
- ticket별 실패 로그
- retry/recover 메서드 시그니처

## 주요 클래스와 책임

### `NotificationService`

- push token 등록/삭제, 채팅 push, 동아리 지원 알림 발송을 담당한다.
- presence, mute, token 조회, inbox 저장, SSE, Expo push가 만나는 중심 서비스다.

### `NotificationInboxService`

- 인앱 알림 저장, 목록 조회, 미읽음 개수, 읽음 처리를 담당한다.
- 채팅 관련 알림을 알림함 목록/카운트/전체 읽음에서 제외하는 정책이 있다.

### `NotificationInboxSseService`

- 사용자별 SSE emitter 생명주기를 관리한다.
- 같은 사용자 재구독, connect 이벤트, 실패 시 emitter 제거 정책을 바꾸면 이 클래스를 먼저 확인해야 한다.

### `ExpoPushClient`

- Expo push API 호출, batch 분할, retry/recover, ticket 실패 로그를 담당한다.
- 외부 API 실패를 비즈니스 알림 흐름과 어떻게 분리할지 판단하는 지점이다.

### `ClubApplicationNotificationListener`

- 동아리 지원 이벤트를 commit 이후 알림 발송으로 연결한다.
- 동아리 도메인의 이벤트 payload가 바뀌면 이 리스너와 알림 path/body를 같이 확인해야 한다.

## 테스트 전략

이미 단위 테스트는 아래 정책을 일부 고정한다.

- token 등록/갱신/삭제와 잘못된 Expo token 거부
- `ExpoPushToken[...]`과 `ExponentPushToken[...]` 형식 허용
- 채팅방 접속 중 사용자와 mute 사용자 push 제외
- 그룹 채팅의 발신자, 접속 중, mute 사용자 필터
- Unicode emoji를 포함한 preview 30 code point 처리
- 동아리 지원 알림의 inbox, SSE, push 호출
- push token이 없어도 동아리 지원 인앱 알림과 SSE를 유지하는 정책
- SSE 재구독과 완료된 emitter 정리
- 인앱 알림 save/saveAll/read 처리

이 도메인의 정책을 바꾸거나 가이드 claim을 강화한다면 추가로 아래 회귀 테스트를 보강하는 것이 좋다.

- 동아리 지원 이벤트가 rollback되면 알림이 생성되지 않는 `AFTER_COMMIT` 통합 테스트
- 인앱 알림 목록/미읽음/전체 읽음에서 채팅 관련 타입이 제외되는 repository 통합 테스트
- Expo push ticket 일부 실패가 전체 예외로 전파되지 않고 로그만 남는 테스트
- 그룹 채팅 token 수와 대상 사용자 수가 다를 때 현재 정책을 명확히 고정하는 테스트

검증할 때는 최소한 아래 테스트를 실행한다.

```bash
CI=true ./gradlew test --tests 'gg.agit.konect.unit.domain.notification.*' --tests 'gg.agit.konect.integration.domain.notification.*'
```
