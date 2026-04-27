# 일정 도메인 가이드

## 이 도메인은 무엇을 하는가

일정 도메인은 사용자의 대학 기준 학사/행사 일정을 조회하고,
관리자가 같은 일정 모델을 생성/수정/삭제하는 도메인이다.

이 도메인에서 중요한 것은 단순 날짜 조회가 아니라
아래 상태가 같은 정책을 바라보는 것이다.

- 공통 일정 본문 (`Schedule`)
- 대학별 일정 연결 (`UniversitySchedule`)
- 로그인 사용자의 대학 (`User.university`)
- 일정 타입 (`ScheduleType`)
- 월 범위 검색과 D-Day 계산
- 관리자 일정 생성/수정/삭제 API

일정 관련 작업을 할 때는 항상 "이 변경이 대학별 일정 격리, 월 경계 포함 규칙,
검색어 정규화, D-Day 계산, 관리자 upsert/delete 정책까지 같이 맞는가"를 먼저 확인해야 한다.

## 주요 상태

### `Schedule`

- 일정의 제목, 시작 일시, 종료 일시, 타입을 저장하는 공통 엔티티다.
- `title`, `startedAt`, `endedAt`, `scheduleType`은 null일 수 없다.
- `scheduleType`은 문자열 enum으로 저장된다.
- 현재 타입은 `UNIVERSITY`, `CLUB`, `COUNCIL`, `DORM`이다.
- 생성과 수정 모두 `startedAt <= endedAt`이어야 한다.
- `startedAt`이 `endedAt`보다 늦으면 `INVALID_DATE_TIME`이다.
- `startedAt == endedAt`인 단일 시점 일정은 허용된다.
- `calculateDDay(today)`는 오늘이 시작일 전이면 남은 일수를 반환하고,
  오늘이 시작일 당일이거나 이후이면 `null`을 반환한다.

### `UniversitySchedule`

- 특정 대학에 속한 일정 연결 엔티티다.
- `@MapsId` 기반으로 `Schedule`과 같은 id를 공유한다.
- `university_schedule.id`는 `schedule.id`를 참조한다.
- 조회와 관리자 수정/삭제는 항상 로그인 사용자의 university id로
  `UniversitySchedule`을 제한해야 한다.
- 다른 대학의 일정 id가 들어오면 존재하지 않는 일정처럼 `NOT_FOUND_SCHEDULE`로 처리된다.

## 기능이 실제로 어떻게 동작해야 하는가

### 다가오는 일정 조회

- endpoint는 `GET /schedules/upcoming`이다.
- 로그인 사용자의 대학 id로 `UniversitySchedule`을 제한한다.
- 오늘 00:00 이상에 끝나는 일정만 조회한다.
- 즉 오늘 00:00에 끝나는 일정도 포함되며, 이미 시작했지만 오늘 이후까지 진행 중인 일정도 포함될 수 있다.
- 최대 3개만 반환한다.
- 정렬은 `startedAt ASC`다.
- 다른 대학의 일정은 포함되면 안 된다.
- 응답의 `dDay`는 시작일 전 일정에만 값이 있고,
  진행 중이거나 당일 시작 일정은 `null`이다.

### 월별 일정 조회

- endpoint는 `GET /schedules`다.
- `year`는 필수이며 2000 이상 2100 이하만 허용한다.
- `month`는 필수이며 1 이상 12 이하만 허용한다.
- `query`가 null이면 빈 문자열로 바뀐다.
- 조회 월의 시작은 해당 월 1일 00:00이다.
- 조회 월의 끝은 해당 월 마지막 날 `LocalTime.MAX`다.
- 일정 포함 조건은 `startedAt < monthEnd AND endedAt > monthStart`다.
- 따라서 조회 월과 조금이라도 겹치는 일정은 포함된다.
- 정렬은 `startedAt ASC`다.
- `query`가 비어 있으면 전체 월별 조회를 사용한다.
- `query`가 있으면 trim 후 lower-case로 바꾸고, 일정 제목도 lower-case로 비교한다.
- 검색은 제목 contains 조건이다.

### 일정 응답

- 응답 루트는 `schedules` 배열이다.
- 각 일정은 `title`, `startedAt`, `endedAt`, `dDay`, `scheduleCategory`를 포함한다.
- `startedAt`, `endedAt` JSON 형식은 `yyyy.MM.dd HH:mm`이다.
- `scheduleCategory`는 `ScheduleType.name()`이다.
- `dDay`는 요청 기준 날짜가 아니라 서버의 `LocalDate.now()` 기준이다.

### 관리자 일정 생성

- endpoint는 `POST /admin/schedules`다.
- `ADMIN` 권한이 필요하다.
- 요청의 `title`은 blank일 수 없다.
- `startedAt`, `endedAt`, `scheduleType`은 null일 수 없다.
- 날짜 범위는 `Schedule` 엔티티에서 검증한다.
- 생성 시 먼저 `Schedule`을 저장하고,
  저장된 schedule과 로그인 사용자의 대학으로 `UniversitySchedule`을 저장한다.
- 생성 성공 응답은 200 OK이고 body는 없다.

### 관리자 일정 일괄 생성/수정

- endpoint는 `PUT /admin/schedules/batch`다.
- `ADMIN` 권한이 필요하다.
- 요청의 `schedules`는 비어 있을 수 없다.
- 각 item의 `scheduleId`가 null이면 새 일정을 생성한다.
- 각 item의 `scheduleId`가 있으면 로그인 사용자 대학에 속한
  `UniversitySchedule`을 찾아 기존 `Schedule`을 수정한다.
- 수정 대상이 없거나 다른 대학 일정이면 `NOT_FOUND_SCHEDULE`이다.
- 생성과 수정은 한 transaction에서 순차 처리된다.
- 항목 중 하나라도 검증이나 수정 대상 조회에 실패하면 전체 요청이 실패해야 한다.
- 일정 50개 규모의 batch도 현재 테스트에서 고정한다.

### 관리자 일정 삭제

- endpoint는 `DELETE /admin/schedules/{scheduleId}`다.
- `ADMIN` 권한이 필요하다.
- 로그인 사용자 대학에 속한 `UniversitySchedule`만 삭제할 수 있다.
- 다른 대학 일정이거나 이미 삭제된 일정이면 `NOT_FOUND_SCHEDULE`이다.
- 삭제는 `UniversitySchedule`을 먼저 삭제하고 연결된 `Schedule`도 삭제한다.
- 삭제 성공 응답은 200 OK이고 body는 없다.

## 절대 놓치면 안 되는 정책

- 일반 조회와 관리자 수정/삭제 모두 로그인 사용자의 대학 기준으로 격리되어야 한다.
- 월별 일정은 시작일이 해당 월에 있는 일정만 보는 것이 아니라
  월 범위와 겹치는 일정을 본다.
- 월 경계 조건은 `startedAt < monthEnd AND endedAt > monthStart`다.
- 다가오는 일정은 종료 시각이 오늘 00:00 이후인 일정이다.
- D-Day는 시작일 전일 때만 내려가고, 당일/진행 중 일정은 `null`이다.
- 검색어는 trim + lower-case 처리 후 제목 contains로 비교한다.
- 관리자 batch upsert는 일부 성공을 허용하지 않는 하나의 transaction이다.
- `UniversitySchedule`은 `Schedule`과 id를 공유하므로,
  schedule id와 university schedule id를 같은 값으로 다룬다.
- 다른 대학 일정 수정/삭제 시 권한 오류가 아니라 `NOT_FOUND_SCHEDULE`로 숨긴다.

## 수정 시 함께 확인해야 하는 것

### 조회 조건을 바꿀 때

- `ScheduleRepository.findUpcomingSchedules`
- `ScheduleRepository.findSchedulesByMonth`
- `ScheduleQueryRepository.findSchedulesByMonthAndQuery`
- 대학별 `UniversitySchedule` 조인 조건
- 진행 중 일정의 upcoming 포함 여부
- 월 경계에 걸친 일정 포함 여부
- `startedAt ASC` 정렬

### 검색 정책을 바꿀 때

- `ScheduleCondition`의 null query 기본값
- query trim 처리
- query lower-case 처리
- 제목 lower-case contains 조건
- 빈 문자열 query와 공백-only query의 동작

### 날짜와 D-Day를 바꿀 때

- `Schedule.validateDateTimeRange`
- `startedAt == endedAt` 허용 여부
- `Schedule.calculateDDay`
- `SchedulesResponse.InnerScheduleResponse.from`
- 서버 `LocalDate.now()` 기준 사용 여부
- 응답 날짜 형식 `yyyy.MM.dd HH:mm`

### 관리자 생성/수정/삭제를 바꿀 때

- `@Auth(roles = {UserRole.ADMIN})`
- 요청 DTO의 `@NotBlank`, `@NotNull`, `@NotEmpty`
- `AdminScheduleService.createUniversitySchedule`
- batch upsert의 transaction 경계
- 다른 대학 일정 수정/삭제 차단
- `UniversitySchedule` 삭제와 `Schedule` 삭제 순서

## 주요 클래스와 책임

### `ScheduleService`

- 일반 사용자 일정 조회를 담당한다.
- 로그인 사용자 대학 기준으로 upcoming/monthly 일정을 조회하고 응답 DTO로 변환한다.

### `ScheduleRepository`

- query가 없는 upcoming/monthly 조회를 담당한다.
- 대학별 일정 격리와 월 범위 겹침 조건이 들어 있다.

### `ScheduleQueryRepository`

- query가 있는 월별 검색을 담당한다.
- QueryDSL로 대학, 월 범위, 제목 contains 조건을 조합한다.

### `AdminScheduleService`

- 관리자 일정 생성, batch upsert, 삭제를 담당한다.
- 같은 `Schedule`/`UniversitySchedule` 모델을 쓰므로 일반 조회 정책과 함께 봐야 한다.

### `Schedule`

- 일정 날짜 범위 검증과 D-Day 계산을 담당한다.
- 날짜 정책을 바꾸면 조회 응답과 관리자 생성/수정 검증이 함께 바뀐다.

### `UniversitySchedule`

- 일정과 대학을 연결한다.
- `Schedule`과 같은 id를 공유하는 구조이므로
  id 의미를 바꾸면 조회와 관리자 API가 같이 깨진다.

## 테스트 전략

이미 통합 테스트는 아래 정책을 일부 고정한다.

- 다가오는 일정 최대 3개 조회
- 종료된 일정 upcoming 제외
- 다른 대학 일정 조회 제외
- 진행 중인 일정의 `dDay` null과 시작 전 일정의 `dDay` 계산
- 특정 월 일정 조회
- query 검색
- query의 대소문자 무시와 trim 처리
- 월을 걸치는 일정 조회
- 관리자 일정 생성과 validation 실패
- 시작/종료가 같은 일정 생성 허용
- 관리자 batch 생성/수정/혼합 처리
- batch 요청에서 하나라도 실패하면 전체 rollback
- 다른 대학 일정 수정/삭제 차단
- 관리자 일정 삭제 시 `Schedule`과 `UniversitySchedule` 함께 삭제
- 일반 사용자 관리자 API 접근 차단

이 도메인의 정책을 바꾸거나 가이드 claim을 강화한다면
추가로 아래 회귀 테스트를 보강하는 것이 좋다.

- `year`, `month` 범위 검증 테스트
- 공백-only query가 현재 전체 조회와 같은지 명확히 고정하는 테스트

검증할 때는 최소한 아래 테스트를 실행한다.

```bash
CI=true ./gradlew test \
  --tests 'gg.agit.konect.integration.domain.schedule.*' \
  --tests 'gg.agit.konect.integration.admin.schedule.*'
```
