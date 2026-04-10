# 서비스 계층 단위 테스트 가이드

## 왜 이 문서가 필요한가

현재 저장소에는 `ServiceTestSupport` 기반의 서비스 단위 테스트가 이미 여러 개 존재하지만,
병렬 실행 안전성, 확장 가능한 테스트 구조, 엣지 케이스 체크리스트가 한곳에 정리돼 있지 않습니다.
이 문서는 기존 테스트 패턴을 리뷰한 결과를 바탕으로 **새 서비스 테스트를 같은 기준으로 추가**할 수 있도록 만든 기준서입니다.

## 리뷰 요약

### 현재 강점
- `src/test/java/gg/agit/konect/support/ServiceTestSupport.java` 를 통해 Spring Context 없이 Mockito 중심 테스트로 고정되어 있다.
- `RefreshTokenServiceTest`, `SignupTokenServiceTest` 처럼 **정상 흐름 + 입력 검증 + 직렬화/만료/서명 오류**를 함께 검증하는 패턴이 이미 있다.
- `NotificationInboxServiceTest`, `GoogleSheetPermissionServiceTest` 처럼 **부분 실패 허용**, **페이지네이션**, **동시성에 가까운 재시도 상황**을 회귀 테스트로 고정한 사례가 있다.

### 현재 보완 포인트
- 서비스별로 테스트 naming / fixture 사용 / edge-case 범위가 조금씩 달라 신규 테스트 작성 시 기준이 분산되어 있다.
- 병렬 실행 안전성을 위한 규칙(공유 상태 금지, 시간/외부 I/O 제어, 테스트 간 격리)이 코드에는 녹아 있지만 문서화돼 있지 않았다.
- 단순 happy path를 넘어 어떤 논리 경계까지 반드시 커버해야 하는지 공통 checklist가 없었다.

## 기본 원칙

1. **서비스 테스트는 Spring 없이 실행한다.**
   - 기본 베이스 클래스는 `ServiceTestSupport` 를 사용한다.
   - `@SpringBootTest` 대신 `@Mock`, `@InjectMocks` 조합으로 의존성을 고립한다.

2. **병렬 실행에 안전해야 한다.**
   - 테스트 간 공유되는 mutable static 상태를 두지 않는다.
   - 실제 네트워크, Redis, Google API, 시간 의존 로직은 mock/stub/helper 로 고정한다.
   - `sleep`, 실제 스레드 경합, 순서 의존 assertions 를 피한다.

3. **테스트는 공개 행위(public behavior)를 기준으로 작성한다.**
   - private 메서드가 아니라 서비스의 공개 메서드와 그 결과/부수효과를 검증한다.
   - 구현 디테일보다 "무슨 입력에서 어떤 결과/예외/상호작용이 나와야 하는가"를 우선한다.

4. **성공/실패/경계 조건을 함께 묶어 회귀를 막는다.**
   - 정상 흐름 1개만으로 끝내지 않는다.
   - 입력 누락, 잘못된 상태, 외부 시스템 오류, 부분 성공, 중복 호출, 직렬화 오류까지 점검한다.

5. **테스트 구조는 AAA(Arrange-Act-Assert)로 통일한다.**
   - `// given`, `// when`, `// then` 블록을 유지한다.
   - `@DisplayName` 은 시나리오와 기대 결과가 드러나도록 작성한다.

## 권장 파일 구조

- 서비스 테스트 위치: `src/test/java/.../<domain>/service/*ServiceTest.java`
- 공통 베이스: `src/test/java/gg/agit/konect/support/ServiceTestSupport.java`
- 범용 fixture: `src/test/java/gg/agit/konect/support/fixture/*`
- 특정 외부 API helper: 테스트와 같은 패키지에 두고 재사용한다.
  - 예: `GoogleApiTestUtils`

## 신규 서비스 테스트 작성 절차

1. **주요 공개 메서드별 시나리오 목록을 먼저 적는다.**
   - 성공
   - 입력 검증 실패
   - 저장/조회 결과 없음
   - 외부 의존성 예외
   - 부분 성공 / 재시도 / 중복 호출

2. **테스트 이름을 동작 중심으로 정한다.**
   - 예: `issueStoresSerializedClaimsWithTtl`
   - 예: `sendSseBatchContinuesWhenOneSendFails`

3. **fixture 는 최소 필드만 채운다.**
   - 실제 비즈니스 분기에 필요한 값만 넣고 나머지는 fixture/helper 에 위임한다.
   - 테스트 안에서 객체 그래프를 과도하게 조립하지 않는다.

4. **검증은 결과 + 상호작용을 분리해서 본다.**
   - 반환값/예외/assertion
   - 저장소/외부 클라이언트 호출 여부 (`verify`, `never`, `times`)

5. **에러 코드는 타입이 아니라 의미까지 검증한다.**
   - `CustomException` 만 보는 대신 `ApiResponseCode` 까지 확인한다.

## 반드시 확인할 엣지 케이스 체크리스트

### 입력/상태 검증
- null / blank / empty collection
- 잘못된 enum / token / identifier
- 필수 설정값 누락

### 저장/조회 경계
- 조회 결과 없음
- 일부 데이터만 조회됨
- 중복 데이터 또는 이미 처리된 상태
- 정렬/집계/카운트 값이 경계 조건에서 틀어지지 않는지

### 외부 의존성/인프라 경계
- 외부 API 인증 실패
- 권한 부족 / 4xx / 5xx
- 재시도 중 중간 성공 또는 이미 반영된 상태
- 직렬화/역직렬화 실패

### 병렬/회귀 관점
- 이전 호출의 정리 동작이 새 상태를 지우지 않는지
- 일부 실패가 전체 실패로 전파되면 안 되는 시나리오인지
- 호출 순서가 바뀌어도 결과가 안정적인지
- 테스트가 실행 순서에 의존하지 않는지

## 현재 저장소의 대표 예시

- `RefreshTokenServiceTest`
  - 서명 불일치, 만료 토큰, 잘못된 token type, 설정 누락까지 검증한다.
- `SignupTokenServiceTest`
  - Redis 저장 TTL, 직렬화 포맷, 원자적 consume, blank token 방어를 검증한다.
- `NotificationInboxServiceTest`
  - 일부 전송 실패가 있어도 전체 배치를 중단하지 않는지 검증한다.
- `NotificationInboxSseServiceTest`
  - 재구독 이후 이전 emitter 정리가 현재 구독을 지우지 않는 회귀를 검증한다.
- `GoogleSheetPermissionServiceTest`
  - 페이지네이션, 권한 승격, 동시성에 가까운 중복 부여 상황을 검증한다.

## 권장 검증 명령

```bash
./gradlew compileJava compileTestJava
./gradlew test --tests 'gg.agit.konect.domain.*.service.*Test'
./gradlew checkstyleMain
```

도메인별로 빠르게 확인할 때는 클래스 단위로 좁혀서 실행합니다.

```bash
./gradlew test --tests 'gg.agit.konect.domain.user.service.RefreshTokenServiceTest'
```

## 리뷰 결론

이 저장소의 서비스 단위 테스트 기반은 이미 좋은 편입니다.
앞으로는 이 문서의 체크리스트를 기준으로 **happy path + 논리 경계 + 외부 실패 + 회귀 시나리오**를 함께 고정하면,
병렬 실행이 가능한 빠른 단위 테스트를 유지하면서도 서비스 로직 회귀를 더 안정적으로 막을 수 있습니다.
