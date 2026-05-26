package gg.agit.konect.integration.domain.club;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import gg.agit.konect.domain.club.dto.ClubInformationUpdateRequestDto;
import gg.agit.konect.domain.club.dto.ClubRegistrationRequestDto;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.website.model.WebClub;
import gg.agit.konect.domain.website.model.WebUniversity;
import gg.agit.konect.support.IntegrationTestSupport;
import gg.agit.konect.support.fixture.WebClubFixture;
import gg.agit.konect.support.fixture.WebUniversityFixture;

class ClubRegistrationRequestApiTest extends IntegrationTestSupport {

    @Test
    @DisplayName("비로그인 사용자도 동아리 등록 요청을 보낼 수 있다")
    void registerClubWithoutLogin() throws Exception {
        // given
        ClubRegistrationRequestDto request = new ClubRegistrationRequestDto(
            "한국기술교육대학교",
            "BCSD Lab",
            ClubCategory.ACADEMIC,
            "코딩",
            "💻",
            "코딩 동아리입니다.",
            "상세한 동아리 소개 내용입니다.",
            List.of("https://example.com/image1.jpg")
        );

        // when & then
        performPost("/clubs/registration-requests", request)
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("사진 및 영상이 없어도 동아리 등록 요청을 보낼 수 있다")
    void registerClubWithoutImages() throws Exception {
        // given
        ClubRegistrationRequestDto request = new ClubRegistrationRequestDto(
            "한국기술교육대학교",
            "BCSD Lab",
            ClubCategory.ACADEMIC,
            "코딩",
            "💻",
            "코딩 동아리입니다.",
            "상세한 동아리 소개 내용입니다.",
            null
        );

        // when & then
        performPost("/clubs/registration-requests", request)
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("필수값이 없으면 400을 반환한다")
    void registerClubWithMissingFields() throws Exception {
        // given
        ClubRegistrationRequestDto request = new ClubRegistrationRequestDto(
            "",
            "BCSD Lab",
            ClubCategory.ACADEMIC,
            "코딩",
            "💻",
            "코딩 동아리입니다.",
            "상세한 동아리 소개 내용입니다.",
            List.of()
        );

        // when & then
        performPost("/clubs/registration-requests", request)
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이미지가 5개를 초과하면 400을 반환한다")
    void registerClubWithTooManyImages() throws Exception {
        // given
        ClubRegistrationRequestDto request = new ClubRegistrationRequestDto(
            "한국기술교육대학교",
            "BCSD Lab",
            ClubCategory.ACADEMIC,
            "코딩",
            "💻",
            "코딩 동아리입니다.",
            "상세한 동아리 소개 내용입니다.",
            List.of(
                "https://example.com/image1.jpg",
                "https://example.com/image2.jpg",
                "https://example.com/image3.jpg",
                "https://example.com/image4.jpg",
                "https://example.com/image5.jpg",
                "https://example.com/image6.jpg"
            )
        );

        // when & then
        performPost("/clubs/registration-requests", request)
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("동아리 소개가 2000자를 초과하면 400을 반환한다")
    void registerClubWithLongIntroduction() throws Exception {
        // given
        String longIntroduction = "a".repeat(2001);
        ClubRegistrationRequestDto request = new ClubRegistrationRequestDto(
            "한국기술교육대학교",
            "BCSD Lab",
            ClubCategory.ACADEMIC,
            "코딩",
            "💻",
            "코딩 동아리입니다.",
            longIntroduction,
            List.of()
        );

        // when & then
        performPost("/clubs/registration-requests", request)
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("비로그인 사용자도 기존 동아리 정보 수정 요청을 보낼 수 있다")
    void requestClubInformationUpdateWithoutLogin() throws Exception {
        // given
        WebUniversity university = persist(WebUniversityFixture.create());
        WebClub club = persist(WebClubFixture.create(university));
        ClubInformationUpdateRequestDto request = createInformationUpdateRequest();

        // when & then
        performPost("/clubs/" + club.getId() + "/information-update-requests", request)
            .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("존재하지 않는 동아리에 대한 정보 수정 요청은 404를 반환한다")
    void requestClubInformationUpdateWithUnknownClub() throws Exception {
        // given
        ClubInformationUpdateRequestDto request = createInformationUpdateRequest();

        // when & then
        performPost("/clubs/999999/information-update-requests", request)
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("동아리 정보 수정 요청 필수값이 없으면 400을 반환한다")
    void requestClubInformationUpdateWithMissingFields() throws Exception {
        // given
        WebUniversity university = persist(WebUniversityFixture.create());
        WebClub club = persist(WebClubFixture.create(university));
        ClubInformationUpdateRequestDto request = new ClubInformationUpdateRequestDto(
            "",
            ClubCategory.ACADEMIC,
            "수정 소개",
            "https://example.com/logo.png",
            "학생회관 102호",
            "수정 상세 소개입니다."
        );

        // when & then
        performPost("/clubs/" + club.getId() + "/information-update-requests", request)
            .andExpect(status().isBadRequest());
    }

    private ClubInformationUpdateRequestDto createInformationUpdateRequest() {
        return new ClubInformationUpdateRequestDto(
            "BCSD Lab",
            ClubCategory.ACADEMIC,
            "수정 소개",
            "https://example.com/logo.png",
            "학생회관 102호",
            "수정 상세 소개입니다."
        );
    }
}
