# 공지 도메인 가이드

## 이 도메인은 무엇을 하는가

공지 도메인은 총동아리연합회 공지사항의 목록/상세 조회, 생성/수정/삭제,
사용자별 읽음 이력을 관리하는 도메인이다.

이 도메인에서 중요한 것은 단순 게시글 CRUD가 아니라
아래 상태가 같은 정책을 바라보는 것이다.

- 총동아리연합회 공지 (`CouncilNotice`)
- 사용자별 공지 읽음 이력 (`CouncilNoticeReadHistory`)
- 사용자의 대학과 해당 대학의 총동아리연합회 (`User.university`, `Council`)
- 마이페이지의 읽지 않은 공지 수 (`UserInfoResponse.unreadCouncilNoticeCount`)

공지 관련 작업을 할 때는 항상 "이 변경이 대학별 공지 격리, 상세 조회 시 읽음 처리,
중복 읽음 이력 방지, 사용자 정보의 unread count까지 같이 맞는가"를 먼저 확인해야 한다.

## 주요 상태

### `CouncilNotice`

- 총동아리연합회 공지사항이다.
- `title`과 `content`는 null일 수 없다.
- `title`은 요청 DTO 기준 최대 255자다.
- `content`는 `TEXT` 컬럼이다.
- 각 공지는 하나의 `Council`에 속한다.
- DB 마이그레이션 기준으로 `council_notice.council_id`는 `council.id`를 참조한다.

### `CouncilNoticeReadHistory`

- 사용자별 공지 읽음 기록이다.
- `(user_id, council_notice_id)` unique 제약을 가진다.
- 같은 사용자가 같은 공지를 여러 번 조회해도 읽음 이력은 하나만 유지해야 한다.
- 읽음 여부는 notice row의 상태가 아니라 read history 존재 여부로 판단한다.

### `Council`

- 목록 조회는 로그인 사용자의 대학으로 `Council`을 찾은 뒤, 그 council의 공지만 조회한다.
- 현재 생성 API는 로그인 사용자 대학이 아니라 `councilRepository.getById(1)`로
  council id 1을 고정 사용한다.
- 생성 정책을 바꿀 때는 기존 테스트의 `insertCouncilWithIdOne` 전제와
  운영 데이터의 기본 council 전제를 함께 확인해야 한다.

## 기능이 실제로 어떻게 동작해야 하는가

### 공지 목록 조회

- endpoint는 `GET /councils/notices`다.
- `page` 기본값은 1이고, 1 이상이어야 한다.
- `limit` 기본값은 10이고, 1 이상이어야 한다.
- 서비스는 `PageRequest.of(page - 1, limit, createdAt DESC)`로 조회한다.
- 로그인 사용자의 대학으로 council을 찾고, 해당 council id의 공지만 조회한다.
- 다른 대학 council의 공지는 목록에 포함되면 안 된다.
- 목록 응답의 `isRead`는 현재 페이지에 포함된 공지 id 중 read history가 있는지로 계산한다.
- 목록 응답은 `totalCount`, `currentCount`, `totalPage`, `currentPage`, `councilNotices`를 포함한다.
- 목록의 공지 날짜는 `createdAt.toLocalDate()`이며 JSON 형식은 `yyyy.MM.dd`다.

### 공지 상세 조회와 읽음 처리

- endpoint는 `GET /councils/notices/{id}`다.
- 공지가 없으면 `NOT_FOUND_COUNCIL_NOTICE`다.
- 공지의 council university와 로그인 사용자 university가 다르면 `FORBIDDEN_COUNCIL_NOTICE_ACCESS`다.
- 상세 조회는 read transaction이 아니라 쓰기 transaction이다.
- 상세 조회는 읽음 이력을 만들 수 있기 때문이다.
- 같은 대학 공지를 상세 조회하면 read history가 없을 때만 `CouncilNoticeReadHistory`를 저장한다.
- 이미 read history가 있으면 새로 저장하지 않는다.
- 상세 응답은 `id`, `title`, `content`, `createdAt`, `updatedAt`를 반환한다.
- 상세 응답의 날짜/시간 JSON 형식은 `yyyy.MM.dd HH:mm:ss`다.

### 공지 생성

- endpoint는 `POST /councils/notices`다.
- 요청의 `title`과 `content`는 blank일 수 없다.
- `title`은 최대 255자다.
- 현재 서비스는 `councilRepository.getById(1)`로 가져온 council에 공지를 생성한다.
- council id 1이 없으면 `NOT_FOUND_COUNCIL`이다.
- 생성 성공 응답은 200 OK이고 body는 없다.
- 생성 흐름을 로그인 사용자 대학 기준으로 바꾸려면
  기존 id 1 전제와 API 권한 정책을 함께 재정의해야 한다.

### 공지 수정

- endpoint는 `PUT /councils/notices/{id}`다.
- 공지가 없으면 `NOT_FOUND_COUNCIL_NOTICE`다.
- 요청의 `title`과 `content`는 blank일 수 없다.
- `title`은 최대 255자다.
- 수정은 기존 엔티티의 `title`, `content`만 교체한다.
- 공지의 council, 생성 시각, 읽음 이력은 수정하지 않는다.
- 수정 성공 응답은 200 OK이고 body는 없다.

### 공지 삭제

- endpoint는 `DELETE /councils/notices/{id}`다.
- 공지가 없으면 `NOT_FOUND_COUNCIL_NOTICE`다.
- 삭제는 `deleteById`로 공지 row를 삭제한다.
- 삭제 성공 응답은 204 No Content다.

### 읽지 않은 공지 수

- 사용자 정보 조회는 `CouncilNoticeReadRepository.countUnreadNoticesByUserId(userId)`를 사용한다.
- 이 쿼리는 read history가 없는 `CouncilNotice` 수를 센다.
- 현재 쿼리는 사용자의 대학 council로 범위를 제한하지 않는다.
- unread count 정책을 대학별로 바꾸려면 `UserService.getUserInfo`, repository query,
  notice 목록/상세 테스트를 함께 확인해야 한다.

## 절대 놓치면 안 되는 정책

- 공지 목록은 로그인 사용자 대학의 council 기준으로 격리되어야 한다.
- 공지 상세 조회는 다른 대학 공지를 읽을 수 없어야 한다.
- 다른 대학 공지를 상세 조회할 때 read history를 만들면 안 된다.
- read history는 `(userId, councilNoticeId)` 기준으로 중복 저장되면 안 된다.
- 목록의 `isRead`는 현재 페이지의 공지 id와 사용자별 read history를 매칭해서 만든다.
- 상세 조회만 읽음 처리한다. 목록 조회 자체는 읽음 이력을 만들지 않는다.
- 생성 API는 현재 council id 1 전제를 가진다.
- unread count는 현재 구현상 전체 `CouncilNotice` 기준이라는 점을 알고 수정해야 한다.

## 수정 시 함께 확인해야 하는 것

### 목록 조회를 바꿀 때

- 로그인 사용자의 대학 조회
- `CouncilRepository.getByUniversity`
- `CouncilNoticeRepository.findByCouncilId`
- `createdAt DESC` 정렬
- page가 1-based로 들어와 `page - 1`로 변환되는 지점
- 현재 페이지 공지 id만 read history 조회에 쓰는지

### 상세 조회와 읽음 처리를 바꿀 때

- `CouncilNoticeRepository.getById`
- 공지 council university와 사용자 university 비교
- `FORBIDDEN_COUNCIL_NOTICE_ACCESS`
- `existsByUserIdAndCouncilNoticeId` 선검사
- `(user_id, council_notice_id)` unique 제약
- 같은 공지 반복 조회 시 read history 중복 저장 방지

### 생성/수정/삭제를 바꿀 때

- 요청 DTO의 `@NotBlank`, `@Size(max = 255)`
- 현재 생성 경로의 council id 1 고정 전제
- 수정 시 `title`, `content`만 바꾸는 정책
- 삭제 후 읽음 이력을 함께 정리해야 하는지에 대한 DB/JPA 정책
- API 응답 status (생성/수정 200, 삭제 204)

### 유저 도메인과 함께 바꿀 때

- `UserService.getUserInfo`의 `unreadCouncilNoticeCount`
- `countUnreadNoticesByUserId` 쿼리의 대학 범위 여부
- 사용자 대학 변경 가능성이 생길 경우 read history와 unread count의 의미
- 유저 삭제 시 read history cascade 삭제

## 주요 클래스와 책임

### `NoticeService`

- 공지 목록/상세/생성/수정/삭제 정책이 모여 있는 중심 서비스다.
- 목록에서는 사용자 대학의 council 공지만 조회한다.
- 상세에서는 다른 대학 접근을 막고 읽음 이력을 생성한다.

### `CouncilNoticeRepository`

- 공지 조회와 저장/삭제를 담당한다.
- `getById`는 공지가 없으면 `NOT_FOUND_COUNCIL_NOTICE`를 던진다.

### `CouncilNoticeReadRepository`

- 읽음 이력 존재 여부, 현재 페이지 공지의 읽음 이력 목록, unread count를 담당한다.
- `countUnreadNoticesByUserId`는 마이페이지 unread count와 연결된다.

### `CouncilNoticeResponse`

- 공지 상세 응답 DTO다.
- 날짜/시간은 `yyyy.MM.dd HH:mm:ss` 형식으로 내려간다.

### `CouncilNoticesResponse`

- 공지 목록 응답 DTO다.
- 페이지 메타데이터와 목록 아이템의 `isRead`를 함께 내려준다.
- 목록 아이템 날짜는 `yyyy.MM.dd` 형식이다.

## 테스트 전략

이미 통합 테스트는 아래 정책을 일부 고정한다.

- 공지 목록 조회와 `isRead` 반영
- 공지 목록 조회가 read history를 만들지 않는 정책
- page가 1 미만이면 400 응답
- 다른 대학 공지 목록 제외
- 다른 대학 공지 상세 조회 403
- 같은 공지 반복 상세 조회 시 read history 중복 방지
- 사용자별 read history 격리
- 공지 생성, 수정, 삭제
- 생성 시 council id 1이 없으면 404
- 생성 요청 title blank 검증
- 수정/삭제 대상 공지가 없으면 404

이 도메인의 정책을 바꾸거나 가이드 claim을 강화한다면
추가로 아래 회귀 테스트를 보강하는 것이 좋다.

- `limit`이 1 미만이면 400을 반환하는 테스트
- 생성/수정 요청의 title 255자 초과 검증 테스트
- 공지 삭제 시 read history 정리 정책을 명확히 고정하는 테스트
- `unreadCouncilNoticeCount`가 현재 전체 공지 기준인지,
  대학별 공지 기준인지 명확히 고정하는 테스트

검증할 때는 최소한 아래 테스트를 실행한다.

```bash
CI=true ./gradlew test --tests 'gg.agit.konect.integration.domain.notice.*'
```
