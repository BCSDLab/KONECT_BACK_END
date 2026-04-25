# 순공 시간 도메인 가이드

## 이 도메인은 무엇을 하는가

순공 시간 도메인은 사용자의 공부 타이머, 일별 공부 시간 누적, 랭킹 캐시, 랭킹 초기화 스케줄러를 관리하는 도메인이다.

이 도메인에서 중요한 것은 단순히 초 단위 시간을 더하는 것이 아니라 아래 상태가 같은 기준으로 움직이는 것이다.

- 실행 중인 타이머(`StudyTimer`)
- 날짜별 누적 시간(`StudyTimeDaily`)
- 개인/동아리/학번 랭킹 캐시(`StudyTimeRanking`)
- 랭킹 타입(`RankingType`)
- 공부 시간 누적 이벤트(`StudyTimeAccumulatedEvent`)
- 일간/월간 랭킹 초기화 스케줄러

순공 시간 관련 작업을 할 때는 항상 "이 변경이 타이머 단일 실행, 서버/클라이언트 시간 검증, 자정 분할 누적, 랭킹 캐시 갱신, 초기화 스케줄러까지 같이 맞는가"를 먼저 확인해야 한다.

## 주요 상태

### `StudyTimer`

- 실행 중인 타이머를 나타낸다.
- 사용자당 동시에 하나만 존재할 수 있다.
- DB의 `study_timer.user_id`는 unique 제약을 가진다.
- `startedAt`은 마지막으로 누적 반영된 시각이다.
- `createdAt`은 현재 타이머 세션의 최초 시작 시각이며, 서버 기준 총 경과 시간 계산에 쓰인다.
- sync가 성공하면 `startedAt`만 현재 시각으로 갱신된다.
- stop이 성공하면 타이머 row는 삭제된다.

### `StudyTimeDaily`

- 사용자별, 날짜별 누적 공부 시간을 저장한다.
- DB의 `(user_id, study_date)`는 unique 제약을 가진다.
- 자정을 넘긴 세션은 날짜별 구간으로 나뉘어 각 날짜 row에 더해진다.
- 현재 구현은 일간 테이블을 기준으로 일간, 월간, 전체 누적 시간을 모두 계산한다.
- 마이그레이션에는 `study_time_monthly`, `study_time_total` 테이블도 있지만 현재 서비스 로직은 이 테이블들을 직접 사용하지 않는다.

### `StudyTimeRanking`

- 랭킹 조회를 빠르게 하기 위한 캐시성 테이블이다.
- 기본 키는 `(ranking_type_id, university_id, target_id)`다.
- 랭킹 타입은 `CLUB`, `STUDENT_NUMBER`, `PERSONAL` 세 가지다.
- `dailySeconds`와 `monthlySeconds`를 함께 저장한다.
- 랭킹 캐시는 공부 시간이 누적된 뒤 발행되는 이벤트 리스너에서 갱신된다.

## 기능이 실제로 어떻게 동작해야 하는가

### 타이머 시작

- 사용자당 실행 중인 타이머는 하나만 허용된다.
- 서비스는 먼저 `existsByUserId`로 실행 중 타이머를 확인한다.
- 동시에 두 요청이 들어와도 DB unique 제약과 flush 시점의 `DataIntegrityViolationException`을 통해 `ALREADY_RUNNING_STUDY_TIMER`로 정리한다.
- 타이머 시작 시점에는 공부 시간이 누적되지 않고, 이벤트도 발행되지 않는다.

### 타이머 sync

- sync는 실행 중인 타이머가 있어야 가능하다.
- 서버 기준 경과 시간은 `createdAt`부터 현재 시각까지다.
- 클라이언트가 보낸 `totalSeconds`와 서버 기준 경과 시간이 3초 이상 차이나면 실패한다.
- 시간 불일치가 발생하면 타이머를 삭제하고 `STUDY_TIMER_TIME_MISMATCH`를 던진다.
- 시간 불일치 예외는 트랜잭션 rollback 대상에서 제외되어야 한다. 그래야 잘못된 타이머 삭제가 유지된다.
- sync 성공 시 마지막 sync 이후 구간만 `StudyTimeDaily`에 누적한다.
- sync 성공 후 `StudyTimeAccumulatedEvent`를 발행하고 `startedAt`을 현재 시각으로 갱신한다.

### 타이머 stop

- stop은 실행 중인 타이머가 있어야 가능하다.
- 서버/클라이언트 시간 차이가 3초 이상이면 sync와 동일하게 타이머를 삭제하고 실패한다.
- stop 성공 시 마지막 sync 이후 구간을 누적하고 `StudyTimeAccumulatedEvent`를 발행한다.
- stop 성공 후 실행 중인 타이머 row를 삭제한다.
- 응답의 `sessionSeconds`는 현재 세션의 서버 기준 총 경과 시간이다.
- 응답의 `dailySeconds`, `monthlySeconds`, `totalSeconds`는 누적 반영 후 다시 조회한 값이다.

### 자정 분할 누적

- 공부 시간이 자정을 넘기면 하나의 총합으로 저장하지 않고 날짜별로 분할한다.
- 구간 시작일이 종료일보다 이전이면 해당 날짜의 자정까지를 먼저 누적한다.
- 마지막 날짜는 실제 종료 시각까지 누적한다.
- 0초 이하 구간은 저장하지 않는다.
- 이 정책을 바꾸면 일간 조회뿐 아니라 월간/전체 합계와 랭킹 갱신 결과도 함께 바뀐다.

### 요약 조회

- 일간 공부 시간은 오늘 날짜의 `StudyTimeDaily` row를 조회한다.
- 월간 공부 시간은 이번 달 1일부터 오늘까지의 `StudyTimeDaily.totalSeconds` 합계다.
- 전체 공부 시간은 사용자의 모든 `StudyTimeDaily.totalSeconds` 합계다.
- 실행 중인 타이머의 아직 sync되지 않은 시간은 요약 조회에 포함되지 않는다.

### 랭킹 갱신

- 공부 시간이 실제로 누적된 뒤 `StudyTimeAccumulatedEvent`가 발행된다.
- 랭킹 갱신 리스너는 `AFTER_COMMIT`에 실행된다.
- 리스너는 별도 트랜잭션(`REQUIRES_NEW`)으로 랭킹 캐시를 갱신한다.
- 즉 공부 시간 누적 트랜잭션이 rollback되면 랭킹 갱신도 실행되지 않는다.
- 랭킹 갱신은 개인, 사용자가 속한 동아리, 사용자의 학번 연도 랭킹을 함께 갱신한다.

### 개인 랭킹

- 개인 랭킹의 target id는 user id다.
- target name은 사용자 이름이다.
- 랭킹 목록 응답에서 개인 이름은 마스킹된다.
- 한 글자 이름은 그대로, 두 글자 이름은 첫 글자와 `*`, 세 글자 이상은 첫 글자와 마지막 글자만 노출한다.

### 동아리 랭킹

- 동아리 랭킹은 사용자가 속한 각 동아리에 대해 갱신된다.
- target id는 club id다.
- target name은 동아리 이름이다.
- 동아리 공부 시간은 현재 동아리 회원들의 일간/월간 공부 시간 합계다.
- 사용자의 공부 시간이 누적되면 사용자가 속한 동아리들의 랭킹만 갱신된다.
- 동아리 회원 구성 변경 자체가 순공 시간 이벤트를 발행하지는 않는다. 회원 변경 후 랭킹 정합성을 요구한다면 별도 갱신 지점을 확인해야 한다.

### 학번 랭킹

- 학번 랭킹은 사용자의 `studentNumberYear` 기준으로 묶는다.
- target name은 학번 연도 문자열이다.
- 랭킹 목록 응답에서는 학번 연도의 뒤 두 자리만 노출한다.
- 학번 랭킹은 target name으로 기존 row를 찾는다.
- 새 학번 랭킹 row가 필요하면 같은 랭킹 타입과 대학 안에서 max target id를 찾아 다음 id를 부여한다.
- 따라서 학번 랭킹은 target id가 학번 값이 아니라 내부 순번이라는 점을 놓치면 안 된다.

### 랭킹 조회

- 랭킹은 로그인한 사용자의 대학 기준으로만 조회된다.
- `type`은 `CLUB`, `STUDENT_NUMBER`, `PERSONAL`만 허용한다.
- `type`은 대소문자를 구분하지 않고 조회하지만, 요청 검증의 허용 값은 세 타입으로 제한된다.
- `page` 기본값은 1, `limit` 기본값은 20, `sort` 기본값은 `MONTHLY`다.
- `limit`은 1 이상 100 이하만 허용한다.
- 일간 정렬은 `dailySeconds DESC`, `monthlySeconds DESC`, `targetId ASC` 순서다.
- 월간 정렬은 `monthlySeconds DESC`, `dailySeconds DESC`, `targetId ASC` 순서다.
- 페이지 응답의 rank는 페이지 시작 번호 기준으로 계산된다.
- 내 랭킹 조회는 동아리 랭킹 목록, 학번 랭킹, 개인 랭킹을 함께 반환한다.
- 내 동아리 랭킹은 존재하는 랭킹 row만 반환하고 rank 오름차순으로 정렬한다.
- 학번/개인 랭킹 row가 아직 없으면 해당 응답 필드는 `null`일 수 있다.

### 랭킹 초기화 스케줄러

- 매일 00:00에 모든 랭킹의 `dailySeconds`를 0으로 초기화한다.
- 매월 1일 00:00에 모든 랭킹의 `monthlySeconds`를 0으로 초기화한다.
- 초기화 대상은 `study_time_ranking` 캐시 테이블이다.
- `StudyTimeDaily` 원본 누적 데이터는 초기화하지 않는다.
- 스케줄러는 예외를 잡아 로그로 남기며, 예외를 외부로 다시 던지지 않는다.

## 절대 놓치면 안 되는 정책

- 실행 중 타이머는 사용자당 하나뿐이다. 서비스 선검사와 DB unique 제약을 함께 봐야 한다.
- `startedAt`과 `createdAt`은 역할이 다르다. `startedAt`은 마지막 누적 지점, `createdAt`은 세션 전체 경과 시간 기준이다.
- 서버/클라이언트 시간 차이가 3초 이상이면 타이머를 삭제하고 실패한다.
- 시간 불일치 실패에서 타이머 삭제가 rollback되면 안 된다.
- sync/stop은 마지막 sync 이후 구간만 누적한다.
- 자정을 넘긴 세션은 날짜별로 분할 누적해야 한다.
- 요약 조회는 저장된 `StudyTimeDaily`만 본다. 실행 중 타이머의 미반영 시간은 포함하지 않는다.
- 랭킹 갱신은 공부 시간 누적 트랜잭션 commit 이후 별도 트랜잭션에서 실행된다.
- 랭킹 캐시는 원본 누적 데이터가 아니다. 일간/월간 초기화는 랭킹 캐시에만 적용된다.
- 개인 이름과 학번 연도는 랭킹 목록 응답에서 노출 정책이 다르다.
- 학번 랭킹 target id는 학번 자체가 아니라 내부 순번이다.
- 동아리 회원 변경은 순공 시간 랭킹 갱신 이벤트를 자동으로 만들지 않는다.

## 수정 시 함께 확인해야 하는 것

### 타이머 시작/종료 정책을 바꿀 때

- `study_timer.user_id` unique 제약
- 중복 시작 시 `ALREADY_RUNNING_STUDY_TIMER`
- `createdAt` 기준 서버 경과 시간 계산
- `startedAt` 기준 마지막 누적 구간 계산
- 시간 불일치 시 타이머 삭제 유지
- sync/stop 성공 시 `StudyTimeAccumulatedEvent` 발행

### 시간 누적 로직을 바꿀 때

- 자정 분할 누적
- `StudyTimeDaily`의 `(user_id, study_date)` unique 제약
- 일간/월간/전체 조회 쿼리
- 0초 이하 구간 무시
- 랭킹 갱신에 쓰이는 일간/월간 집계 기준

### 랭킹 정책을 바꿀 때

- `RankingType` seed 값 (`CLUB`, `STUDENT_NUMBER`, `PERSONAL`)
- 대학별 랭킹 격리
- 일간/월간 정렬 tie-breaker
- 개인 이름 마스킹
- 학번 연도 뒤 두 자리 표시
- 학번 랭킹 target id 생성 규칙
- 내 랭킹 조회에서 null 허용 필드

### 스케줄러를 바꿀 때

- 매일 00:00 일간 랭킹 초기화
- 매월 1일 00:00 월간 랭킹 초기화
- 원본 `StudyTimeDaily`를 초기화하지 않는 정책
- `scheduler.studytime` 로거 설정
- 예외를 잡아 스케줄러 실행 흐름을 유지하는 정책

### 동아리/유저 도메인과 함께 바꿀 때

- 동아리 회원 목록 기반 동아리 랭킹 합산
- 회원 탈퇴 또는 동아리 탈퇴 후 랭킹 캐시 정합성
- 사용자 이름 변경 시 개인 랭킹 target name 갱신 여부
- 학번 변경 가능성이 생길 경우 학번 랭킹 target name과 target id 정합성
- 사용자 대학 변경 가능성이 생길 경우 대학별 랭킹 격리

## 주요 클래스와 책임

### `StudyTimerService`

- 타이머 시작, sync, stop을 담당한다.
- 시간 불일치 검증, 날짜별 누적, 이벤트 발행이 모여 있는 중심 서비스다.

### `StudyTimeQueryService`

- 일간, 월간, 전체 누적 공부 시간 조회를 담당한다.
- 현재 구현은 `StudyTimeDaily` 합계만 사용한다.

### `StudyTimeRankingUpdateService`

- 공부 시간 누적 이후 개인/동아리/학번 랭킹 캐시를 갱신한다.
- 동아리 회원 합산과 학번 연도 합산 정책을 바꿀 때 가장 먼저 봐야 한다.

### `StudyTimeRankingService`

- 랭킹 목록 조회와 내 랭킹 조회를 담당한다.
- 정렬 기준, rank 계산, 이름/학번 노출 정책이 응답으로 나가는 지점이다.

### `StudyTimeRankingUpdateListener`

- `StudyTimeAccumulatedEvent`를 commit 이후 받아 랭킹 갱신을 실행한다.
- 트랜잭션 전파 방식을 바꾸면 누적 성공과 랭킹 갱신의 결합도가 달라진다.

### `StudyTimeSchedulerService` / `StudyTimeScheduler`

- 일간/월간 랭킹 캐시 초기화를 담당한다.
- 원본 누적 데이터가 아니라 랭킹 캐시만 초기화한다는 점을 유지해야 한다.

## 테스트 전략

현재 `StudyTimeApiTest`는 비어 있으므로 API 통합 흐름은 아직 충분히 고정되어 있지 않다. 다만 핵심 단위 정책은 `gg.agit.konect.unit.domain.studytime` 아래에서 먼저 고정한다.

이미 고정한 회귀 테스트는 아래와 같다.

- 중복 타이머 시작은 `ALREADY_RUNNING_STUDY_TIMER`로 실패한다.
- 시간 불일치 sync/stop은 타이머를 삭제하고 공부 시간/랭킹 후속 효과를 만들지 않는다.
- 개인 랭킹 이름과 학번 랭킹 이름은 노출 정책에 맞게 마스킹된다.
- 랭킹 초기화 스케줄러는 `StudyTimeRanking`의 일간/월간 캐시만 초기화한다.

이 도메인의 정책을 바꾸거나 가이드 claim을 강화한다면 추가로 아래 회귀 테스트를 보강하는 것이 좋다.

- 자정을 넘긴 stop은 두 날짜의 `StudyTimeDaily`로 분할 누적한다.
- sync 후 stop은 이미 sync된 구간을 다시 더하지 않는다.
- 공부 시간 누적 commit 이후 개인/동아리/학번 랭킹 캐시가 갱신된다.
- 랭킹 목록은 일간/월간 정렬 tie-breaker와 이름/학번 노출 정책을 지킨다.

검증할 때는 최소한 아래 테스트를 실행한다.

```bash
CI=true ./gradlew test --tests 'gg.agit.konect.unit.domain.studytime.*'
```

API 통합 테스트를 추가한 뒤에는 `gg.agit.konect.integration.domain.studytime.*` 필터도 별도로 실행 가능하게 유지해야 한다.
