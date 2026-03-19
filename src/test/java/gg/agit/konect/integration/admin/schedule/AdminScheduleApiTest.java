package gg.agit.konect.integration.admin.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.admin.schedule.dto.AdminScheduleCreateRequest;
import gg.agit.konect.admin.schedule.dto.AdminScheduleUpsertItemRequest;
import gg.agit.konect.admin.schedule.dto.AdminScheduleUpsertRequest;
import gg.agit.konect.domain.schedule.model.Schedule;
import gg.agit.konect.domain.schedule.model.ScheduleType;
import gg.agit.konect.domain.schedule.model.UniversitySchedule;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.ScheduleFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class AdminScheduleApiTest extends IntegrationTestSupport {

    private static final String BASE_URL = "/admin/schedules";

    private University university;
    private User admin;

    @BeforeEach
    void setUp() {
        university = persist(UniversityFixture.create());
        admin = persist(UserFixture.createAdmin(university));
    }

    @Nested
    @DisplayName("POST /admin/schedules - 일정 생성")
    class CreateSchedule {

        @Test
        @DisplayName("관리자가 일정 생성에 성공한다")
        void createScheduleSuccess() throws Exception {
            // given
            LocalDateTime startedAt = LocalDateTime.now().plusDays(1);
            LocalDateTime endedAt = startedAt.plusDays(7);

            var request = new AdminScheduleCreateRequest(
                "동계방학",
                startedAt,
                endedAt,
                ScheduleType.UNIVERSITY
            );

            mockLoginUser(admin.getId());
            clearPersistenceContext();

            // when & then
            performPost(BASE_URL, request)
                .andExpect(status().isOk());

            clearPersistenceContext();

            // 데이터 저장 검증
            List<UniversitySchedule> saved = entityManager.createQuery(
                    "SELECT us FROM UniversitySchedule us WHERE us.university.id = :universityId",
                    UniversitySchedule.class)
                .setParameter("universityId", university.getId())
                .getResultList();

            assertThat(saved).hasSize(1);
            assertThat(saved.get(0).getSchedule().getTitle()).isEqualTo("동계방학");
            assertThat(saved.get(0).getSchedule().getScheduleType()).isEqualTo(ScheduleType.UNIVERSITY);
        }

        @Test
        @DisplayName("제목이 없으면 400 에러를 반환한다")
        void createScheduleFailWithoutTitle() throws Exception {
            // given
            LocalDateTime startedAt = LocalDateTime.now().plusDays(1);
            LocalDateTime endedAt = startedAt.plusDays(7);

            var request = new AdminScheduleCreateRequest(
                null,
                startedAt,
                endedAt,
                ScheduleType.UNIVERSITY
            );

            mockLoginUser(admin.getId());

            // when & then
            performPost(BASE_URL, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_REQUEST_BODY.getCode()));
        }

        @Test
        @DisplayName("시작일시가 종료일시보다 늦으면 400 에러를 반환한다")
        void createScheduleFailWithInvalidDateRange() throws Exception {
            // given
            LocalDateTime startedAt = LocalDateTime.now().plusDays(10);
            LocalDateTime endedAt = LocalDateTime.now().plusDays(1);

            var request = new AdminScheduleCreateRequest(
                "동계방학",
                startedAt,
                endedAt,
                ScheduleType.UNIVERSITY
            );

            mockLoginUser(admin.getId());

            // when & then
            performPost(BASE_URL, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_DATE_TIME.getCode()));
        }

        @Test
        @DisplayName("일정 종류가 없으면 400 에러를 반환한다")
        void createScheduleFailWithoutScheduleType() throws Exception {
            // given
            LocalDateTime startedAt = LocalDateTime.now().plusDays(1);
            LocalDateTime endedAt = startedAt.plusDays(7);

            var request = new AdminScheduleCreateRequest(
                "동계방학",
                startedAt,
                endedAt,
                null
            );

            mockLoginUser(admin.getId());

            // when & then
            performPost(BASE_URL, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_REQUEST_BODY.getCode()));
        }

        @Test
        @DisplayName("시작일시가 null이면 400 에러를 반환한다")
        void createScheduleFailWithNullStartedAt() throws Exception {
            // given
            LocalDateTime endedAt = LocalDateTime.now().plusDays(7);

            var request = new AdminScheduleCreateRequest(
                "동계방학",
                null,
                endedAt,
                ScheduleType.UNIVERSITY
            );

            mockLoginUser(admin.getId());

            // when & then
            performPost(BASE_URL, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_REQUEST_BODY.getCode()));
        }

        @Test
        @DisplayName("종료일시가 null이면 400 에러를 반환한다")
        void createScheduleFailWithNullEndedAt() throws Exception {
            // given
            LocalDateTime startedAt = LocalDateTime.now().plusDays(1);

            var request = new AdminScheduleCreateRequest(
                "동계방학",
                startedAt,
                null,
                ScheduleType.UNIVERSITY
            );

            mockLoginUser(admin.getId());

            // when & then
            performPost(BASE_URL, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_REQUEST_BODY.getCode()));
        }

        @Test
        @DisplayName("빈 문자열 제목으로는 일정을 생성할 수 없다")
        void createScheduleFailWithBlankTitle() throws Exception {
            // given
            LocalDateTime startedAt = LocalDateTime.now().plusDays(1);
            LocalDateTime endedAt = startedAt.plusDays(7);

            var request = new AdminScheduleCreateRequest(
                "   ",
                startedAt,
                endedAt,
                ScheduleType.UNIVERSITY
            );

            mockLoginUser(admin.getId());

            // when & then
            performPost(BASE_URL, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_REQUEST_BODY.getCode()));
        }

        @Test
        @DisplayName("시작일시와 종료일시가 같아도 일정을 생성할 수 있다")
        void createScheduleSuccessWithSameStartAndEnd() throws Exception {
            // given
            LocalDateTime sameDateTime = LocalDateTime.now().plusDays(1);

            var request = new AdminScheduleCreateRequest(
                "단일 일정",
                sameDateTime,
                sameDateTime,
                ScheduleType.UNIVERSITY
            );

            mockLoginUser(admin.getId());
            clearPersistenceContext();

            // when & then
            performPost(BASE_URL, request)
                .andExpect(status().isOk());

            clearPersistenceContext();

            List<UniversitySchedule> saved = entityManager.createQuery(
                    "SELECT us FROM UniversitySchedule us WHERE us.university.id = :universityId",
                    UniversitySchedule.class)
                .setParameter("universityId", university.getId())
                .getResultList();

            assertThat(saved).hasSize(1);
        }
    }

    @Nested
    @DisplayName("PUT /admin/schedules/batch - 일정 일괄 생성/수정")
    class UpsertSchedules {

        @Test
        @DisplayName("새로운 일정을 일괄 생성한다")
        void upsertSchedulesCreateSuccess() throws Exception {
            // given
            LocalDateTime startedAt = LocalDateTime.now().plusDays(1);
            LocalDateTime endedAt = startedAt.plusDays(7);

            var item1 = new AdminScheduleUpsertItemRequest(
                null,
                "동계방학",
                startedAt,
                endedAt,
                ScheduleType.UNIVERSITY
            );

            var item2 = new AdminScheduleUpsertItemRequest(
                null,
                "하계방학",
                startedAt.plusMonths(6),
                startedAt.plusMonths(6).plusDays(7),
                ScheduleType.UNIVERSITY
            );

            var request = new AdminScheduleUpsertRequest(List.of(item1, item2));

            mockLoginUser(admin.getId());
            clearPersistenceContext();

            // when & then
            performPut(BASE_URL + "/batch", request)
                .andExpect(status().isOk());

            clearPersistenceContext();

            List<UniversitySchedule> saved = entityManager.createQuery(
                    "SELECT us FROM UniversitySchedule us WHERE us.university.id = :universityId",
                    UniversitySchedule.class)
                .setParameter("universityId", university.getId())
                .getResultList();

            assertThat(saved).hasSize(2);
        }

        @Test
        @DisplayName("기존 일정을 수정한다")
        void upsertSchedulesUpdateSuccess() throws Exception {
            // given
            Schedule schedule = persist(ScheduleFixture.createUniversity(
                "기존 일정",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(5)
            ));
            UniversitySchedule universitySchedule = persist(
                ScheduleFixture.createUniversitySchedule(schedule, university)
            );
            clearPersistenceContext();

            String newTitle = "수정된 일정";
            LocalDateTime newStartedAt = LocalDateTime.now().plusDays(2);
            LocalDateTime newEndedAt = newStartedAt.plusDays(3);

            var item = new AdminScheduleUpsertItemRequest(
                universitySchedule.getId(),
                newTitle,
                newStartedAt,
                newEndedAt,
                ScheduleType.CLUB
            );

            var request = new AdminScheduleUpsertRequest(List.of(item));

            mockLoginUser(admin.getId());

            // when & then
            performPut(BASE_URL + "/batch", request)
                .andExpect(status().isOk());

            clearPersistenceContext();

            UniversitySchedule updated = entityManager.find(UniversitySchedule.class, universitySchedule.getId());
            assertThat(updated.getSchedule().getTitle()).isEqualTo(newTitle);
            assertThat(updated.getSchedule().getScheduleType()).isEqualTo(ScheduleType.CLUB);
        }

        @Test
        @DisplayName("생성과 수정을 동시에 수행한다")
        void upsertSchedulesMixedSuccess() throws Exception {
            // given
            Schedule existingSchedule = persist(ScheduleFixture.createUniversity(
                "기존 일정",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(5)
            ));
            UniversitySchedule existingUniversitySchedule = persist(
                ScheduleFixture.createUniversitySchedule(existingSchedule, university)
            );
            clearPersistenceContext();

            var updateItem = new AdminScheduleUpsertItemRequest(
                existingUniversitySchedule.getId(),
                "수정된 일정",
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(6),
                ScheduleType.UNIVERSITY
            );

            var createItem = new AdminScheduleUpsertItemRequest(
                null,
                "새 일정",
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(15),
                ScheduleType.DORM
            );

            var request = new AdminScheduleUpsertRequest(List.of(updateItem, createItem));

            mockLoginUser(admin.getId());

            // when & then
            performPut(BASE_URL + "/batch", request)
                .andExpect(status().isOk());

            clearPersistenceContext();

            List<UniversitySchedule> allSchedules = entityManager.createQuery(
                    "SELECT us FROM UniversitySchedule us WHERE us.university.id = :universityId",
                    UniversitySchedule.class)
                .setParameter("universityId", university.getId())
                .getResultList();

            assertThat(allSchedules).hasSize(2);
        }

        @Test
        @DisplayName("빈 목록이면 400 에러를 반환한다")
        void upsertSchedulesFailWithEmptyList() throws Exception {
            // given
            var request = new AdminScheduleUpsertRequest(List.of());

            mockLoginUser(admin.getId());

            // when & then
            performPut(BASE_URL + "/batch", request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_REQUEST_BODY.getCode()));
        }

        @Test
        @DisplayName("수정 시 존재하지 않는 일정 ID면 404 에러를 반환한다")
        void upsertSchedulesFailWithNonExistentId() throws Exception {
            // given
            int nonExistentId = 99999;

            var item = new AdminScheduleUpsertItemRequest(
                nonExistentId,
                "수정된 일정",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(7),
                ScheduleType.UNIVERSITY
            );

            var request = new AdminScheduleUpsertRequest(List.of(item));

            mockLoginUser(admin.getId());

            // when & then
            performPut(BASE_URL + "/batch", request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.NOT_FOUND_SCHEDULE.getCode()));
        }

        @Test
        @DisplayName("다른 대학의 일정은 수정할 수 없다")
        void upsertSchedulesFailOtherUniversity() throws Exception {
            // given
            University otherUniversity = persist(UniversityFixture.createWithName("다른대학교"));
            Schedule otherSchedule = persist(ScheduleFixture.createUniversity(
                "다른 대학 일정",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(5)
            ));
            UniversitySchedule otherUniversitySchedule = persist(
                ScheduleFixture.createUniversitySchedule(otherSchedule, otherUniversity)
            );
            clearPersistenceContext();

            var item = new AdminScheduleUpsertItemRequest(
                otherUniversitySchedule.getId(),
                "수정 시도",
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(6),
                ScheduleType.UNIVERSITY
            );

            var request = new AdminScheduleUpsertRequest(List.of(item));

            mockLoginUser(admin.getId());

            // when & then
            performPut(BASE_URL + "/batch", request)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.NOT_FOUND_SCHEDULE.getCode()));
        }

        @Test
        @DisplayName("수정 시 잘못된 날짜 범위면 400 에러를 반환한다")
        void upsertSchedulesFailWithInvalidDateRange() throws Exception {
            // given
            Schedule schedule = persist(ScheduleFixture.createUniversity(
                "기존 일정",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(5)
            ));
            UniversitySchedule universitySchedule = persist(
                ScheduleFixture.createUniversitySchedule(schedule, university)
            );
            clearPersistenceContext();

            var item = new AdminScheduleUpsertItemRequest(
                universitySchedule.getId(),
                "수정된 일정",
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(1),
                ScheduleType.UNIVERSITY
            );

            var request = new AdminScheduleUpsertRequest(List.of(item));

            mockLoginUser(admin.getId());

            // when & then
            performPut(BASE_URL + "/batch", request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_DATE_TIME.getCode()));
        }

        @Test
        @DisplayName("항목 중 하나라도 검증 실패하면 전체 요청이 실패한다")
        void upsertSchedulesFailWithOneInvalidItem() throws Exception {
            // given
            var validItem = new AdminScheduleUpsertItemRequest(
                null,
                "유효한 일정",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(7),
                ScheduleType.UNIVERSITY
            );

            var invalidItem = new AdminScheduleUpsertItemRequest(
                null,
                "잘못된 일정",
                LocalDateTime.now().plusDays(10),
                LocalDateTime.now().plusDays(1),
                ScheduleType.UNIVERSITY
            );

            var request = new AdminScheduleUpsertRequest(List.of(validItem, invalidItem));

            mockLoginUser(admin.getId());

            // when & then
            performPut(BASE_URL + "/batch", request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_DATE_TIME.getCode()));

            clearPersistenceContext();

            List<UniversitySchedule> saved = entityManager.createQuery(
                    "SELECT us FROM UniversitySchedule us WHERE us.university.id = :universityId",
                    UniversitySchedule.class)
                .setParameter("universityId", university.getId())
                .getResultList();
            assertThat(saved).isEmpty();
        }

        @Test
        @DisplayName("대량 일정을 일괄 처리할 수 있다")
        void upsertSchedulesLargeBatch() throws Exception {
            // given
            LocalDateTime baseDate = LocalDateTime.now().plusDays(1);

            var items = IntStream.range(0, 50)
                .mapToObj(i -> new AdminScheduleUpsertItemRequest(
                    null,
                    "일정 " + i,
                    baseDate.plusDays(i),
                    baseDate.plusDays(i + 1),
                    ScheduleType.UNIVERSITY
                ))
                .toList();

            var request = new AdminScheduleUpsertRequest(items);

            mockLoginUser(admin.getId());
            clearPersistenceContext();

            // when & then
            performPut(BASE_URL + "/batch", request)
                .andExpect(status().isOk());

            clearPersistenceContext();

            List<UniversitySchedule> saved = entityManager.createQuery(
                    "SELECT us FROM UniversitySchedule us WHERE us.university.id = :universityId",
                    UniversitySchedule.class)
                .setParameter("universityId", university.getId())
                .getResultList();

            assertThat(saved).hasSize(50);
        }
    }

    @Nested
    @DisplayName("DELETE /admin/schedules/{scheduleId} - 일정 삭제")
    class DeleteSchedule {

        @Test
        @DisplayName("일정 삭제에 성공한다")
        void deleteScheduleSuccess() throws Exception {
            // given
            Schedule schedule = persist(ScheduleFixture.createUniversity(
                "삭제될 일정",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(5)
            ));
            UniversitySchedule universitySchedule = persist(
                ScheduleFixture.createUniversitySchedule(schedule, university)
            );
            clearPersistenceContext();

            mockLoginUser(admin.getId());

            // when & then
            performDelete(BASE_URL + "/" + universitySchedule.getId())
                .andExpect(status().isOk());

            clearPersistenceContext();

            UniversitySchedule deleted = entityManager.find(UniversitySchedule.class, universitySchedule.getId());
            Schedule deletedSchedule = entityManager.find(Schedule.class, schedule.getId());

            assertThat(deleted).isNull();
            assertThat(deletedSchedule).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 일정이면 404 에러를 반환한다")
        void deleteScheduleFailNotFound() throws Exception {
            // given
            int nonExistentId = 99999;

            mockLoginUser(admin.getId());

            // when & then
            performDelete(BASE_URL + "/" + nonExistentId)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.NOT_FOUND_SCHEDULE.getCode()));
        }

        @Test
        @DisplayName("다른 대학의 일정은 삭제할 수 없다")
        void deleteScheduleFailOtherUniversity() throws Exception {
            // given
            University otherUniversity = persist(UniversityFixture.createWithName("다른대학교"));
            Schedule otherSchedule = persist(ScheduleFixture.createUniversity(
                "다른 대학 일정",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(5)
            ));
            UniversitySchedule otherUniversitySchedule = persist(
                ScheduleFixture.createUniversitySchedule(otherSchedule, otherUniversity)
            );
            clearPersistenceContext();

            mockLoginUser(admin.getId());

            // when & then
            performDelete(BASE_URL + "/" + otherUniversitySchedule.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.NOT_FOUND_SCHEDULE.getCode()));
        }

        @Test
        @DisplayName("이미 삭제된 일정을 다시 삭제하면 404 에러를 반환한다")
        void deleteScheduleFailAlreadyDeleted() throws Exception {
            // given
            Schedule schedule = persist(ScheduleFixture.createUniversity(
                "삭제될 일정",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(5)
            ));
            UniversitySchedule universitySchedule = persist(
                ScheduleFixture.createUniversitySchedule(schedule, university)
            );
            clearPersistenceContext();

            mockLoginUser(admin.getId());

            // when - 첫 삭제 성공
            performDelete(BASE_URL + "/" + universitySchedule.getId())
                .andExpect(status().isOk());

            clearPersistenceContext();

            // then - 재삭제 시 404
            performDelete(BASE_URL + "/" + universitySchedule.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.NOT_FOUND_SCHEDULE.getCode()));
        }

        @Test
        @DisplayName("음수 ID로 삭제 요청하면 404 에러를 반환한다")
        void deleteScheduleFailWithNegativeId() throws Exception {
            // given
            mockLoginUser(admin.getId());

            // when & then
            performDelete(BASE_URL + "/-1")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.NOT_FOUND_SCHEDULE.getCode()));
        }
    }

    @Nested
    @DisplayName("관리자 권한 검증")
    class AdminAuthorization {

        @Test
        @DisplayName("일반 사용자는 일정 생성 권한이 없다")
        void nonAdminCannotCreateSchedule() throws Exception {
            // given
            User normalUser = persist(UserFixture.createUser(university, "일반유저", "2021136002"));
            clearPersistenceContext();

            LocalDateTime startedAt = LocalDateTime.now().plusDays(1);
            LocalDateTime endedAt = startedAt.plusDays(7);

            var request = new AdminScheduleCreateRequest(
                "동계방학",
                startedAt,
                endedAt,
                ScheduleType.UNIVERSITY
            );

            mockLoginUser(normalUser.getId());
            given(authorizationInterceptor.preHandle(any(), any(), any()))
                .willThrow(CustomException.of(ApiResponseCode.FORBIDDEN_ROLE_ACCESS));

            // when & then
            performPost(BASE_URL, request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.FORBIDDEN_ROLE_ACCESS.getCode()));
        }

        @Test
        @DisplayName("일반 사용자는 일정 삭제 권한이 없다")
        void nonAdminCannotDeleteSchedule() throws Exception {
            // given
            User normalUser = persist(UserFixture.createUser(university, "일반유저", "2021136003"));
            Schedule schedule = persist(ScheduleFixture.createUniversity(
                "일정",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(5)
            ));
            UniversitySchedule universitySchedule = persist(
                ScheduleFixture.createUniversitySchedule(schedule, university)
            );
            clearPersistenceContext();

            mockLoginUser(normalUser.getId());
            given(authorizationInterceptor.preHandle(any(), any(), any()))
                .willThrow(CustomException.of(ApiResponseCode.FORBIDDEN_ROLE_ACCESS));

            // when & then
            performDelete(BASE_URL + "/" + universitySchedule.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.FORBIDDEN_ROLE_ACCESS.getCode()));
        }

        @Test
        @DisplayName("일반 사용자는 배치 수정 권한이 없다")
        void nonAdminCannotUpsertSchedules() throws Exception {
            // given
            User normalUser = persist(UserFixture.createUser(university, "일반유저", "2021136004"));
            clearPersistenceContext();

            var item = new AdminScheduleUpsertItemRequest(
                null,
                "동계방학",
                LocalDateTime.now().plusDays(1),
                LocalDateTime.now().plusDays(7),
                ScheduleType.UNIVERSITY
            );

            var request = new AdminScheduleUpsertRequest(List.of(item));

            mockLoginUser(normalUser.getId());
            given(authorizationInterceptor.preHandle(any(), any(), any()))
                .willThrow(CustomException.of(ApiResponseCode.FORBIDDEN_ROLE_ACCESS));

            // when & then
            performPut(BASE_URL + "/batch", request)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.FORBIDDEN_ROLE_ACCESS.getCode()));
        }
    }
}
