package gg.agit.konect.integration.domain.club;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import gg.agit.konect.domain.club.dto.ClubRegistrationRequest;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.model.ClubRegistrationRequestEntity;
import gg.agit.konect.domain.club.repository.ClubRegistrationRequestRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.support.IntegrationTestSupport;

class ClubRegistrationRequestApiTest extends IntegrationTestSupport {

    private static final String CLUB_REGISTRATION_REQUESTS_ENDPOINT = "/clubs/registration-requests";

    @Autowired
    private ClubRegistrationRequestRepository clubRegistrationRequestRepository;

    @Nested
    @DisplayName("POST /clubs/registration-requests - 신규 동아리 등록 요청")
    class SubmitClubRegistrationRequest {

        @Test
        @DisplayName("비로그인 사용자가 신규 동아리 등록 요청을 보낸다")
        void submitClubRegistrationRequestSuccess() throws Exception {
            // given
            ClubRegistrationRequest request = createRequest();

            // when & then
            performPost(CLUB_REGISTRATION_REQUESTS_ENDPOINT, request)
                .andExpect(status().isOk());

            clearPersistenceContext();
            List<ClubRegistrationRequestEntity> requests = clubRegistrationRequestRepository.findAll();
            org.assertj.core.api.Assertions.assertThat(requests).hasSize(1);
            ClubRegistrationRequestEntity savedRequest = requests.getFirst();
            org.assertj.core.api.Assertions.assertThat(savedRequest.getUniversityName()).isEqualTo("한국기술교육대학교");
            org.assertj.core.api.Assertions.assertThat(savedRequest.getClubName()).isEqualTo("BCSD Lab");
            org.assertj.core.api.Assertions.assertThat(savedRequest.getClubCategory()).isEqualTo(ClubCategory.ACADEMIC);
            org.assertj.core.api.Assertions.assertThat(savedRequest.getTopic()).isEqualTo("개발");
            org.assertj.core.api.Assertions.assertThat(savedRequest.getEmoji()).isEqualTo("💻");
            org.assertj.core.api.Assertions.assertThat(savedRequest.getDescription()).isEqualTo("즐겁게 서비스 만드는 동아리");
            org.assertj.core.api.Assertions.assertThat(savedRequest.getMediaUrls())
                .containsExactly("https://example.com/club-1.png", "https://example.com/club-2.mp4");
            org.assertj.core.api.Assertions.assertThat(savedRequest.getIntroduce())
                .isEqualTo("BCSD Lab은 IT 서비스 개발 동아리입니다.");
        }

        @Test
        @DisplayName("한 줄 소개가 30자를 초과하면 400을 반환한다")
        void submitClubRegistrationRequestWithTooLongDescriptionFails() throws Exception {
            // given
            ClubRegistrationRequest request = new ClubRegistrationRequest(
                "한국기술교육대학교",
                "BCSD Lab",
                ClubCategory.ACADEMIC,
                "개발",
                "💻",
                "가".repeat(31),
                List.of("https://example.com/club-1.png"),
                "BCSD Lab은 IT 서비스 개발 동아리입니다."
            );

            // when & then
            performPost(CLUB_REGISTRATION_REQUESTS_ENDPOINT, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_REQUEST_BODY.getCode()));
        }

        @Test
        @DisplayName("사진 및 영상 URL이 5개를 초과하면 400을 반환한다")
        void submitClubRegistrationRequestWithTooManyMediaUrlsFails() throws Exception {
            // given
            ClubRegistrationRequest request = new ClubRegistrationRequest(
                "한국기술교육대학교",
                "BCSD Lab",
                ClubCategory.ACADEMIC,
                "개발",
                "💻",
                "즐겁게 서비스 만드는 동아리",
                List.of(
                    "https://example.com/club-1.png",
                    "https://example.com/club-2.png",
                    "https://example.com/club-3.png",
                    "https://example.com/club-4.png",
                    "https://example.com/club-5.png",
                    "https://example.com/club-6.png"
                ),
                "BCSD Lab은 IT 서비스 개발 동아리입니다."
            );

            // when & then
            performPost(CLUB_REGISTRATION_REQUESTS_ENDPOINT, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_REQUEST_BODY.getCode()));
        }

        @Test
        @DisplayName("동아리 소개가 2000자를 초과하면 400을 반환한다")
        void submitClubRegistrationRequestWithTooLongIntroduceFails() throws Exception {
            // given
            ClubRegistrationRequest request = new ClubRegistrationRequest(
                "한국기술교육대학교",
                "BCSD Lab",
                ClubCategory.ACADEMIC,
                "개발",
                "💻",
                "즐겁게 서비스 만드는 동아리",
                List.of("https://example.com/club-1.png"),
                "가".repeat(2001)
            );

            // when & then
            performPost(CLUB_REGISTRATION_REQUESTS_ENDPOINT, request)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ApiResponseCode.INVALID_REQUEST_BODY.getCode()));
        }

        private ClubRegistrationRequest createRequest() {
            return new ClubRegistrationRequest(
                "한국기술교육대학교",
                "BCSD Lab",
                ClubCategory.ACADEMIC,
                "개발",
                "💻",
                "즐겁게 서비스 만드는 동아리",
                List.of("https://example.com/club-1.png", "https://example.com/club-2.mp4"),
                "BCSD Lab은 IT 서비스 개발 동아리입니다."
            );
        }
    }
}
