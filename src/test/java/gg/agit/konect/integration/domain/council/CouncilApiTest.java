package gg.agit.konect.integration.domain.council;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import gg.agit.konect.domain.council.dto.CouncilCreateRequest;
import gg.agit.konect.domain.council.dto.CouncilUpdateRequest;
import gg.agit.konect.domain.council.model.Council;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.CouncilFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class CouncilApiTest extends IntegrationTestSupport {

    private static final String COUNCILS_ENDPOINT = "/councils";

    private University university;
    private User user;

    @BeforeEach
    void setUp() throws Exception {
        university = persist(UniversityFixture.create());
        user = persist(UserFixture.createUser(university, "학생회유저", "2021136001"));
        clearPersistenceContext();
        mockLoginUser(user.getId());
    }

    @Nested
    @DisplayName("GET /councils - 총동아리연합회 조회")
    class GetCouncil {

        @Test
        @DisplayName("사용자 대학의 총동아리연합회 정보를 조회한다")
        void getCouncilSuccess() throws Exception {
            // given
            Council council = persist(CouncilFixture.create(university));
            clearPersistenceContext();

            // when & then
            performGet(COUNCILS_ENDPOINT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(council.getId()))
                .andExpect(jsonPath("$.name").value("총학생회"))
                .andExpect(jsonPath("$.instagramUserName").value("koreatech_council"));
        }

        @Test
        @DisplayName("총동아리연합회가 없으면 404를 반환한다")
        void getCouncilNotFound() throws Exception {
            // when & then
            performGet(COUNCILS_ENDPOINT)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.NOT_FOUND_COUNCIL.getCode()));
        }
    }

    @Nested
    @DisplayName("POST /councils - 총동아리연합회 생성")
    class CreateCouncil {

        @Test
        @DisplayName("총동아리연합회 정보를 생성한다")
        void createCouncilSuccess() throws Exception {
            // when & then
            performPost(COUNCILS_ENDPOINT, createRequest())
                .andExpect(status().isOk());

            performGet(COUNCILS_ENDPOINT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("개화"))
                .andExpect(jsonPath("$.location").value("학생회관 2층 202호"));
        }

        @Test
        @DisplayName("이미 존재하면 409를 반환한다")
        void createCouncilDuplicateFails() throws Exception {
            // given
            persist(CouncilFixture.create(university));
            clearPersistenceContext();

            // when & then
            performPost(COUNCILS_ENDPOINT, createRequest())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.ALREADY_EXIST_COUNCIL.getCode()));
        }

        @Test
        @DisplayName("전화번호 형식이 잘못되면 400을 반환한다")
        void createCouncilInvalidPhoneFails() throws Exception {
            // given
            CouncilCreateRequest request = new CouncilCreateRequest(
                "개화",
                "https://konect.kro.kr/image.jpg",
                "총동아리연합회 소개",
                "학생회관 2층 202호",
                "#FF5733",
                "02-123-4567",
                "council@example.com",
                "평일 09:00 ~ 18:00",
                "koreatechclub"
            );

            // when & then
            performPost(COUNCILS_ENDPOINT, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_REQUEST_BODY.getCode()));
        }
    }

    @Nested
    @DisplayName("PUT /councils - 총동아리연합회 수정")
    class UpdateCouncil {

        @Test
        @DisplayName("총동아리연합회 정보를 수정한다")
        void updateCouncilSuccess() throws Exception {
            // given
            persist(CouncilFixture.create(university));
            clearPersistenceContext();

            CouncilUpdateRequest request = new CouncilUpdateRequest(
                "개화 리뉴얼",
                "https://konect.kro.kr/new-image.jpg",
                "새로운 소개",
                "학생회관 3층 301호",
                "#000000",
                "01012345678",
                "updated@example.com",
                "평일 10:00 ~ 17:00",
                "renewed_council"
            );

            // when & then
            performPut(COUNCILS_ENDPOINT, request)
                .andExpect(status().isOk());

            performGet(COUNCILS_ENDPOINT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("개화 리뉴얼"))
                .andExpect(jsonPath("$.imageUrl").value("https://konect.kro.kr/new-image.jpg"))
                .andExpect(jsonPath("$.instagramUserName").value("renewed_council"));
        }
    }

    @Nested
    @DisplayName("DELETE /councils - 총동아리연합회 삭제")
    class DeleteCouncil {

        @Test
        @DisplayName("총동아리연합회 정보를 삭제한다")
        void deleteCouncilSuccess() throws Exception {
            // given
            persist(CouncilFixture.create(university));
            clearPersistenceContext();

            // when & then
            performDelete(COUNCILS_ENDPOINT)
                .andExpect(status().isNoContent());

            performGet(COUNCILS_ENDPOINT)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.NOT_FOUND_COUNCIL.getCode()));
        }
    }

    private CouncilCreateRequest createRequest() {
        return new CouncilCreateRequest(
            "개화",
            "https://konect.kro.kr/image.jpg",
            "총동아리연합회 소개",
            "학생회관 2층 202호",
            "#FF5733",
            "01012345678",
            "council@example.com",
            "평일 09:00 ~ 18:00",
            "koreatechclub"
        );
    }
}
