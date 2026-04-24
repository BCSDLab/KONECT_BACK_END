# 유저 도메인 가이드

## 이 도메인은 무엇을 하는가

유저 도메인은 OAuth 로그인 이후의 회원가입, 계정 연동, 토큰 재발급, 활동 시각 갱신, 회원 탈퇴와 복구 유예기간을 관리하는 도메인이다.

이 도메인에서 중요한 것은 단순한 사용자 CRUD가 아니라 아래 상태가 서로 같은 정책을 바라보는 것이다.

- 실제 회원(`User`)
- 회원가입 전 임시 사용자(`UnRegisteredUser`)
- OAuth 제공자별 계정 연결(`UserOAuthAccount`)
- 쿠키 기반 signup token과 refresh token
- 탈퇴 상태(`deletedAt`)와 7일 복구 유예기간
- 동아리 사전 회원(`ClubPreMember`) 흡수
- 동아리 회장 탈퇴 제한
- 가입 환영 메시지용 direct 채팅방과 마지막 메시지 메타데이터
- Apple / Google Drive refresh token

유저 관련 작업을 할 때는 항상 "이 변경이 OAuth 식별자, 탈퇴/복구 정책, 동아리 사전 회원 전환, 채팅/알림 후속 효과까지 같이 맞는가"를 먼저 확인해야 한다.

## 사용자 상태

### `UnRegisteredUser`

- OAuth 로그인은 아직 `User`를 만들지 않고 임시 사용자인 `UnRegisteredUser`를 만든다.
- signup token은 이 임시 사용자 정보를 기반으로 추가 정보 입력 화면과 최종 회원가입을 이어준다.
- Google, Naver, Kakao 흐름은 이메일과 provider 조합으로 임시 사용자를 찾거나 만든다.
- Apple 흐름은 providerId가 더 중요한 식별자이며, 이메일이 없는 신규 Apple 사용자는 가입을 진행할 수 없다.
- Apple은 최초 로그인 시 받은 이름과 refresh token을 임시 사용자 또는 OAuth 계정에 보존할 수 있다.

### `User`

- 실제 회원은 대학, 이메일, 이름, 학번, 마케팅 동의, 기본 프로필 이미지를 가진다.
- 기본 역할은 `USER`다.
- `ADMIN`은 별도 역할이며, 동아리 도메인의 회장/부회장/운영진 권한과 같은 개념이 아니다.
- 탈퇴는 row 삭제가 아니라 `deletedAt`을 채우는 soft delete다.
- `UserRepository.getById()`는 `deletedAt IS NULL`인 사용자만 찾는다.
- 탈퇴한 사용자의 이름은 `User.getName()`에서 `탈퇴한 사용자`로 표시된다.

### `UserOAuthAccount`

- OAuth 계정은 `User`와 provider별 외부 계정을 연결한다.
- 한 사용자는 provider별로 하나의 OAuth 계정만 가질 수 있다.
- providerId와 oauthEmail은 활성 사용자 기준으로 다른 사용자와 충돌하면 안 된다.
- providerId가 없는 상태로 primary OAuth 계정이 만들어질 수 있지만, 일반 계정 연동(`linkOAuthAccount`)은 providerId가 필요하다.
- Apple refresh token과 Google Drive refresh token은 모두 `UserOAuthAccount`에 저장되지만, 쓰임과 생명주기가 다르다.

## 기능이 실제로 어떻게 동작해야 하는가

### OAuth 로그인과 회원가입 전 상태

- OAuth 로그인에서 이미 가입된 활성 사용자가 있으면 임시 사용자를 새로 만들지 않는다.
- 가입되지 않은 사용자라면 `UnRegisteredUser`를 만들고 signup token으로 회원가입을 이어간다.
- signup token은 Redis에 `auth:signup:{token}` 형태로 저장되며 TTL은 10분이다.
- 회원가입 사전 입력 조회는 signup token을 읽기만 한다.
- 실제 회원가입은 signup token을 consume해서 한 번만 사용할 수 있게 한다.
- signup token 값은 이메일, provider, providerId, 이름을 직렬화한 값이다.
- 구분자(`|`)가 들어간 값이나 provider가 깨진 값은 유효하지 않은 signup token으로 처리된다.

### 회원가입 완료

- 회원가입은 아래 순서의 부수 효과를 함께 가진다.
  - signup token claims에서 이메일, provider, providerId를 가져온다.
  - `UnRegisteredUser`를 찾는다.
  - 대학을 검증한다.
  - `User`를 생성한다.
  - primary OAuth 계정을 연결한다.
  - 같은 대학/학번/이름의 사전 동아리 회원을 실제 동아리 회원으로 전환한다.
  - 운영자 계정이 있으면 환영 direct 메시지를 보낸다.
  - 임시 사용자를 삭제한다.
  - `UserRegisteredEvent`를 발행한다.
  - refresh token 쿠키와 access token 헤더를 내려준다.
- Apple 회원가입은 providerId가 비어 있으면 실패한다.
- 이미 같은 providerId 또는 같은 provider/oauthEmail로 가입된 활성 사용자가 있으면 다시 가입할 수 없다.
- 회원가입 성공 후 signup token 쿠키는 제거되어야 한다.

### 사전 동아리 회원 흡수

- 회원가입 시 같은 대학, 같은 학번, 같은 이름의 `ClubPreMember`를 모두 찾는다.
- 매칭된 사전 회원은 각 동아리의 실제 `ClubMember`로 전환된다.
- 전환된 회원은 동아리 그룹 채팅방 멤버십에도 추가된다.
- 사전 회원 직책이 `PRESIDENT`면 기존 회장을 먼저 제거하고 새 가입자를 회장으로 올린다.
- 기존 회장을 제거할 때도 동아리 채팅방 멤버십을 함께 제거한다.
- 전환이 끝난 `ClubPreMember`는 삭제된다.
- 즉 회원가입 로직을 바꿀 때는 동아리 회원 상태와 club group 채팅방 멤버십까지 같이 확인해야 한다.

### 가입 환영 메시지

- 회원가입 후 가장 작은 id의 활성 admin 사용자를 운영자로 골라 환영 메시지를 보낸다.
- 운영자가 없으면 환영 메시지는 생략된다.
- 운영자와 신규 사용자가 같은 사용자가 되면 안 된다.
- direct 채팅방이 이미 있으면 재사용하고, 없으면 새로 만든다.
- direct 멤버십을 보장한 뒤 운영자 메시지를 저장한다.
- 저장된 메시지는 `chat_room.last_message_*`에도 최신 메시지 조건으로 동기화한다.
- 환영 메시지 실패는 회원가입 전체를 실패시키지 않고 warning 로그로 남긴다.
- 이 로직은 채팅 도메인의 마지막 메시지 메타데이터 정책과 맞아야 한다.

### 로그인, refresh token, 활동 시각

- refresh token은 JWT이며 TTL은 30일이다.
- refresh token에는 issuer, 만료 시각, jti, user id, `token_type=refresh`가 들어간다.
- refresh token 검증은 서명, issuer, 만료, token type, user id claim을 모두 확인한다.
- refresh 요청은 기존 refresh token을 검증한 뒤 새 refresh token으로 rotate한다.
- refresh 성공 시 `lastLoginAt`과 `lastActivityAt`을 함께 현재 시각으로 갱신한다.
- 일반 활동 시각 갱신은 userId가 null이면 아무 것도 하지 않는다.
- `updateLastActivityAt`은 사용자가 이미 탈퇴했거나 없으면 조용히 건너뛴다.

### OAuth 계정 연동

- 연동 상태 조회는 모든 `Provider`에 대해 linked 여부를 반환한다.
- 일반 OAuth 계정 연동은 provider 문자열을 대문자 enum으로 해석한다.
- 지원하지 않는 provider는 `UNSUPPORTED_PROVIDER`다.
- 연동 요청은 provider별 verifier로 토큰을 검증한 뒤 저장해야 한다.
- 다른 활성 사용자가 같은 providerId 또는 provider/oauthEmail을 이미 쓰고 있으면 연동할 수 없다.
- 같은 사용자에게 이미 같은 provider 계정이 있으면 기존 계정을 갱신한다.
- 기존 계정의 providerId가 비어 있는 경우에는 새 providerId를 채울 수 있다.
- 기존 계정에 다른 providerId가 이미 있으면 충돌로 막아야 한다.
- Apple 연동은 Apple refresh token이 있으면 기존 계정에 갱신한다.

### 탈퇴와 복구 유예기간

- 회원 탈퇴는 사용자 row를 삭제하지 않고 `deletedAt`을 현재 시각으로 채운다.
- 회장인 사용자는 탈퇴할 수 없다.
- 여러 동아리 중 하나라도 회장이면 탈퇴할 수 없다.
- 부회장, 운영진, 일반 회원은 탈퇴할 수 있다.
- 탈퇴 시 연결된 Apple OAuth 계정의 refresh token은 즉시 revoke를 시도한다.
- 탈퇴 후에는 `UserWithdrawnEvent`가 발행된다.
- 탈퇴 API는 refresh token과 signup token 쿠키를 함께 제거한다.
- 탈퇴한 사용자는 일반 `getById` 경로에서 더 이상 활성 사용자로 조회되지 않는다.

### 탈퇴 계정 복구와 OAuth 정리

- 탈퇴 사용자는 7일 복구 유예기간을 가진다.
- OAuth 연동/회원가입 과정에서 같은 providerId 또는 provider/oauthEmail의 탈퇴 계정이 발견되면 복구 또는 정리를 먼저 시도한다.
- stage 프로필이 아니고 7일 이내 탈퇴라면 기존 사용자를 복구한다.
- stage 프로필이거나 7일이 지난 탈퇴 계정이면 기존 OAuth 계정 연결을 삭제하고 새 연결이 가능하게 한다.
- 00:10 스케줄러는 7일이 지난 탈퇴 사용자의 OAuth 계정 연결을 삭제한다.
- Apple token revoke 스케줄러는 매일 00:00에 7일이 지난 탈퇴 Apple 계정의 refresh token을 revoke하고, 성공 시 저장된 refresh token을 비운다.
- 복구 유예기간 정책을 바꿀 때는 즉시 탈퇴 처리, OAuth 충돌 처리, 스케줄러 삭제/토큰 폐기 시점을 함께 맞춰야 한다.

### Google Drive OAuth

- Google Drive OAuth는 로그인용 Google OAuth와 같은 `UserOAuthAccount`의 `googleDriveRefreshToken` 필드를 쓴다.
- Drive OAuth state는 Redis에 10분 TTL로 저장되고 callback에서 한 번만 consume된다.
- Drive refresh token은 Google provider 계정이 있어야 저장할 수 있다.
- 재동의 과정에서 새 refresh token이 내려오지 않아도 기존 refresh token이 있으면 유지한다.
- 기존 refresh token도 없고 새 refresh token도 없으면 Drive 인증 실패다.
- 동아리 시트 기능은 이 refresh token 존재 여부에 영향을 받는다.

## 절대 놓치면 안 되는 정책

- `User`의 탈퇴는 hard delete가 아니라 `deletedAt` soft delete다.
- 활성 사용자 조회는 대부분 `deletedAt IS NULL` 기준이다.
- `User.getName()`은 탈퇴 사용자 이름을 원문 그대로 노출하지 않는다.
- 회원가입 token은 읽기와 consume의 의미가 다르다. 실제 가입에서는 반드시 consume해야 한다.
- Apple은 providerId가 핵심 식별자다. Apple 가입에서 providerId가 없으면 정상 가입으로 보면 안 된다.
- providerId 없는 primary 계정 생성은 허용되지만, 일반 OAuth 계정 연동에는 providerId가 필요하다.
- OAuth providerId와 oauthEmail 중 하나만 봐서 중복을 판단하면 안 된다.
- 탈퇴 계정의 OAuth 연결은 7일 복구 유예기간과 stage 프로필 예외를 함께 본다.
- 회장 사용자는 탈퇴할 수 없다. 동아리 하나라도 회장이면 막아야 한다.
- 사전 동아리 회원 흡수는 이름까지 일치해야 한다.
- 사전 회원이 회장이면 기존 회장과 채팅방 멤버십도 함께 교체된다.
- 회원가입 환영 메시지 실패는 회원가입 실패로 전파하지 않는다.
- refresh token은 access token과 다른 `token_type=refresh` claim을 가져야 한다.
- refresh 성공은 토큰 재발급뿐 아니라 로그인 시각 갱신이다.
- Google Drive refresh token은 별도 OAuth state 흐름으로 저장되며, 로그인용 OAuth 계정과 같은 row를 쓴다.

## 수정 시 함께 확인해야 하는 것

### 회원가입 로직을 바꿀 때

- signup token read/consume 구분
- `UnRegisteredUser` 조회 기준
- providerId와 oauthEmail 중복 검증
- Apple providerId 필수 정책
- primary OAuth 계정 생성
- 사전 동아리 회원 흡수
- 동아리 채팅방 멤버십 추가
- 환영 direct 메시지와 마지막 메시지 동기화
- `UserRegisteredEvent` 발행
- signup token 쿠키 제거와 refresh token 설정

### OAuth 계정 연동을 바꿀 때

- provider별 verifier 검증
- providerId 필수 여부
- 같은 사용자 기존 provider 계정 갱신 규칙
- 다른 활성 사용자와의 providerId/oauthEmail 충돌
- 탈퇴 계정 복구 또는 OAuth 계정 정리
- Apple refresh token 저장
- Google Drive refresh token과의 row 공유

### 탈퇴/복구 정책을 바꿀 때

- 회장 탈퇴 제한
- `deletedAt` 기반 활성 사용자 필터
- Apple token revoke 시점
- `UserWithdrawnEvent` 발행
- refresh/signup 쿠키 제거
- 7일 복구 유예기간
- stage 프로필 예외
- 탈퇴 OAuth 계정 삭제 스케줄러
- 탈퇴 사용자 이름 노출 정책

### 활동 시각과 토큰을 바꿀 때

- refresh token TTL
- issuer와 secret 검증
- `token_type=refresh` 검증
- rotate 후 새 refresh token 쿠키 설정
- `lastLoginAt`과 `lastActivityAt` 갱신 범위
- 탈퇴 사용자의 활동 시각 갱신 처리

### 동아리/채팅 연동을 바꿀 때

- `ClubPreMember` 매칭 기준
- 회장 사전 회원의 기존 회장 교체
- `ClubMember` 저장과 club group 멤버십 추가
- 기존 회장 제거 시 club group 멤버십 제거
- 환영 direct 채팅방 재사용 기준
- `chat_room.last_message_*` 최신 메시지 조건부 갱신

## 주요 클래스와 책임

### `UserService`

- 회원가입, 사용자 정보 조회, 회원 탈퇴가 모이는 중심 서비스다.
- 동아리 사전 회원 흡수, 환영 메시지, 이벤트 발행처럼 다른 도메인과 만나는 지점이 많다.

### `UserOAuthAccountService`

- OAuth 계정 연동, 연동 상태 조회, 탈퇴 계정 복구/정리, OAuth 계정 cleanup을 담당한다.
- providerId와 oauthEmail 충돌 정책을 바꿀 때 가장 먼저 봐야 한다.

### `SignupTokenService`

- 회원가입 token 발급, 조회, consume, 직렬화/역직렬화를 담당한다.
- token TTL과 claim 구조를 바꾸면 회원가입 prefill과 실제 가입 흐름을 같이 확인해야 한다.

### `RefreshTokenService`

- refresh token 발급, 검증, rotate를 담당한다.
- JWT secret, issuer, claim 정책을 바꾸면 인증 전역에 영향을 준다.

### `UserActivityService`

- 로그인 시각과 활동 시각 갱신을 담당한다.
- null userId와 탈퇴 사용자 처리 방식이 다르므로 단순 공통화하면 안 된다.

### `UserSchedulerService` / `UserSchedulerTxService`

- 7일 유예기간이 지난 Apple token revoke를 담당한다.
- 외부 Apple revoke 성공 후 저장된 refresh token을 비우는 순서를 유지해야 한다.

### `UserOAuthAccountCleanupScheduler`

- 7일 유예기간이 지난 탈퇴 사용자의 OAuth 계정 연결 삭제를 담당한다.
- 복구 유예기간과 동일한 기준을 써야 한다.

### `GoogleDriveOAuthService`

- Google Drive 권한 위임용 OAuth state와 refresh token 저장을 담당한다.
- 동아리 구글 시트 기능과 직접 연결된다.

### `UserRepository`

- 활성 사용자 기준 조회를 담당한다.
- `getById()`는 탈퇴 사용자를 반환하지 않는다.

### `UserOAuthAccountRepository`

- OAuth providerId, oauthEmail, provider별 사용자 계정 조회를 담당한다.
- 활성 사용자만 찾는 조회와 탈퇴 사용자까지 포함하는 계정 조회가 섞여 있으므로 쿼리 조건을 바꿀 때 주의해야 한다.
