package gg.agit.konect.integration.domain.notice;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.domain.council.model.Council;
import gg.agit.konect.domain.notice.dto.CouncilNoticeCreateRequest;
import gg.agit.konect.domain.notice.dto.CouncilNoticeUpdateRequest;
import gg.agit.konect.domain.notice.model.CouncilNotice;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.CouncilFixture;
import gg.agit.konect.support.fixture.CouncilNoticeFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class NoticeApiTest extends IntegrationTestSupport {

    private static final String NOTICES_ENDPOINT = "/councils/notices";

    private University university;
    private User user;
    private User otherUser;

    @BeforeEach
    void setUp() throws Exception {
        university = persist(UniversityFixture.create());
        user = persist(UserFixture.createUser(university, "공지유저", "2021136001"));
        otherUser = persist(UserFixture.createUser(university, "다른공지유저", "2021136002"));
        clearPersistenceContext();
        mockLoginUser(user.getId());
    }

    @Nested
    @DisplayName("GET /councils/notices - 공지 목록 조회")
    class GetNotices {

        @Test
        @DisplayName("공지 목록을 읽음 여부와 함께 조회한다")
        void getNoticesSuccess() throws Exception {
            // given
            Council council = persist(CouncilFixture.create(university));
            CouncilNotice firstNotice = persist(CouncilNoticeFixture.create(council, "첫 번째 공지", "내용1"));
            persist(CouncilNoticeFixture.create(council, "두 번째 공지", "내용2"));
            clearPersistenceContext();

            // 첫 번째 공지를 읽으면 목록에서 읽음 상태가 반영되어야 한다.
            performGet(NOTICES_ENDPOINT + "/" + firstNotice.getId())
                .andExpect(status().isOk());

            // when & then
            performGet(NOTICES_ENDPOINT + "?page=1&limit=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.councilNotices", hasSize(2)))
                .andExpect(jsonPath("$.councilNotices[0].isRead").value(false))
                .andExpect(jsonPath("$.councilNotices[1].isRead").value(true));
        }

        @Test
        @DisplayName("페이지가 1 미만이면 400을 반환한다")
        void getNoticesInvalidPageFails() throws Exception {
            // when & then
            performGet(NOTICES_ENDPOINT + "?page=0&limit=10")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_REQUEST_BODY.getCode()));
        }

        @Test
        @DisplayName("다른 대학 공지는 목록에서 제외된다")
        void getNoticesExcludesOtherUniversityNotices() throws Exception {
            // given
            Council council = persist(CouncilFixture.create(university));
            persist(CouncilNoticeFixture.create(council, "우리 대학 공지", "내용1"));

            University otherUniversity = persist(UniversityFixture.createWithName("다른대학교"));
            Council otherCouncil = persist(CouncilFixture.create(otherUniversity));
            persist(CouncilNoticeFixture.create(otherCouncil, "다른 대학 공지", "내용2"));
            clearPersistenceContext();

            // when & then
            performGet(NOTICES_ENDPOINT + "?page=1&limit=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.councilNotices", hasSize(1)))
                .andExpect(jsonPath("$.councilNotices[0].title").value("우리 대학 공지"));
        }
    }

    @Nested
    @DisplayName("GET /councils/notices/{id} - 공지 상세 조회")
    class GetNotice {

        @Test
        @DisplayName("다른 대학 공지는 403을 반환한다")
        void getNoticeForbidden() throws Exception {
            // given
            University otherUniversity = persist(UniversityFixture.createWithName("다른대학교"));
            Council otherCouncil = persist(CouncilFixture.create(otherUniversity));
            CouncilNotice notice = persist(CouncilNoticeFixture.create(otherCouncil));
            clearPersistenceContext();

            // when & then
            performGet(NOTICES_ENDPOINT + "/" + notice.getId())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.FORBIDDEN_COUNCIL_NOTICE_ACCESS.getCode()));
        }

        @Test
        @DisplayName("같은 공지를 다시 조회해도 읽음 이력은 한 번만 저장된다")
        void getNoticeDoesNotDuplicateReadHistory() throws Exception {
            // given
            Council council = persist(CouncilFixture.create(university));
            CouncilNotice notice = persist(CouncilNoticeFixture.create(council));
            clearPersistenceContext();

            // when
            performGet(NOTICES_ENDPOINT + "/" + notice.getId())
                .andExpect(status().isOk());
            performGet(NOTICES_ENDPOINT + "/" + notice.getId())
                .andExpect(status().isOk());

            // then
            Number readHistoryCount = (Number)entityManager.createNativeQuery("""
                select count(*)
                from council_notice_read_history
                where user_id = ? and council_notice_id = ?
                """)
                .setParameter(1, user.getId())
                .setParameter(2, notice.getId())
                .getSingleResult();

            org.assertj.core.api.Assertions.assertThat(readHistoryCount.longValue()).isEqualTo(1L);
        }

        @Test
        @DisplayName("한 사용자의 읽음 처리는 다른 사용자의 읽음 여부에 영향을 주지 않는다")
        void getNoticeReadHistoryIsIsolatedPerUser() throws Exception {
            // given
            Council council = persist(CouncilFixture.create(university));
            CouncilNotice notice = persist(CouncilNoticeFixture.create(council));
            clearPersistenceContext();

            performGet(NOTICES_ENDPOINT + "/" + notice.getId())
                .andExpect(status().isOk());

            mockLoginUser(otherUser.getId());

            // when & then
            performGet(NOTICES_ENDPOINT + "?page=1&limit=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.councilNotices", hasSize(1)))
                .andExpect(jsonPath("$.councilNotices[0].isRead").value(false));
        }
    }

    @Nested
    @DisplayName("POST /councils/notices - 공지 생성")
    class CreateNotice {

        @Test
        @DisplayName("공지사항을 생성한다")
        void createNoticeSuccess() throws Exception {
            // given
            insertCouncilWithIdOne(university.getId());
            clearPersistenceContext();

            CouncilNoticeCreateRequest request = new CouncilNoticeCreateRequest("생성 공지", "생성 내용");

            // when & then
            performPost(NOTICES_ENDPOINT, request)
                .andExpect(status().isOk());

            performGet(NOTICES_ENDPOINT + "?page=1&limit=10")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.councilNotices", hasSize(1)))
                .andExpect(jsonPath("$.councilNotices[0].title").value("생성 공지"));
        }

        @Test
        @DisplayName("연결된 총동아리연합회가 없으면 404를 반환한다")
        void createNoticeWithoutCouncilFails() throws Exception {
            // when & then
            performPost(NOTICES_ENDPOINT, new CouncilNoticeCreateRequest("생성 공지", "생성 내용"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.NOT_FOUND_COUNCIL.getCode()));
        }

        @Test
        @DisplayName("공지 제목이 비어 있으면 400을 반환한다")
        void createNoticeInvalidBodyFails() throws Exception {
            // given
            insertCouncilWithIdOne(university.getId());
            clearPersistenceContext();

            // when & then
            performPost(NOTICES_ENDPOINT, new CouncilNoticeCreateRequest(" ", "생성 내용"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_REQUEST_BODY.getCode()));
        }
    }

    @Nested
    @DisplayName("PUT /councils/notices/{id} - 공지 수정")
    class UpdateNotice {

        @Test
        @DisplayName("공지사항을 수정한다")
        void updateNoticeSuccess() throws Exception {
            // given
            Council council = persist(CouncilFixture.create(university));
            CouncilNotice notice = persist(CouncilNoticeFixture.create(council, "기존 제목", "기존 내용"));
            clearPersistenceContext();

            CouncilNoticeUpdateRequest request = new CouncilNoticeUpdateRequest("수정 제목", "수정 내용");

            // when & then
            performPut(NOTICES_ENDPOINT + "/" + notice.getId(), request)
                .andExpect(status().isOk());

            performGet(NOTICES_ENDPOINT + "/" + notice.getId())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정 제목"))
                .andExpect(jsonPath("$.content").value("수정 내용"));
        }

        @Test
        @DisplayName("수정 대상 공지가 없으면 404를 반환한다")
        void updateNoticeNotFound() throws Exception {
            // when & then
            performPut(NOTICES_ENDPOINT + "/99999", new CouncilNoticeUpdateRequest("수정 제목", "수정 내용"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.NOT_FOUND_COUNCIL_NOTICE.getCode()));
        }
    }

    @Nested
    @DisplayName("DELETE /councils/notices/{id} - 공지 삭제")
    class DeleteNotice {

        @Test
        @DisplayName("공지사항을 삭제한다")
        void deleteNoticeSuccess() throws Exception {
            // given
            Council council = persist(CouncilFixture.create(university));
            CouncilNotice notice = persist(CouncilNoticeFixture.create(council));
            clearPersistenceContext();

            // when & then
            performDelete(NOTICES_ENDPOINT + "/" + notice.getId())
                .andExpect(status().isNoContent());

            performGet(NOTICES_ENDPOINT + "/" + notice.getId())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.NOT_FOUND_COUNCIL_NOTICE.getCode()));
        }

        @Test
        @DisplayName("삭제 대상 공지가 없으면 404를 반환한다")
        void deleteNoticeNotFound() throws Exception {
            // when & then
            performDelete(NOTICES_ENDPOINT + "/99999")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.NOT_FOUND_COUNCIL_NOTICE.getCode()));
        }
    }

    private void insertCouncilWithIdOne(Integer universityId) {
        entityManager.createNativeQuery("""
            insert into council (
                id, name, image_url, introduce, location, personal_color,
                phone_number, email, instagram_user_name, operating_hour,
                university_id, created_at, updated_at
            ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
            """)
            .setParameter(1, 1)
            .setParameter(2, "총학생회")
            .setParameter(3, "https://example.com/council.png")
            .setParameter(4, "학생회 소개입니다.")
            .setParameter(5, "학생회관 301호")
            .setParameter(6, "#FF5733")
            .setParameter(7, "041-560-1234")
            .setParameter(8, "council@koreatech.ac.kr")
            .setParameter(9, "koreatech_council")
            .setParameter(10, "09:00 - 18:00")
            .setParameter(11, universityId)
            .executeUpdate();
    }
}
