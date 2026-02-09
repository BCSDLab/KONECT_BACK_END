package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gg.agit.konect.domain.club.dto.ClubRecruitmentResponse;
import gg.agit.konect.domain.club.dto.ClubRecruitmentUpsertRequest;
import gg.agit.konect.domain.club.dto.ClubRecruitmentUpsertRequest.InnerClubRecruitmentImageRequest;
import gg.agit.konect.domain.club.enums.ClubCategory;
import gg.agit.konect.domain.club.enums.RecruitmentStatus;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubRecruitment;
import gg.agit.konect.domain.club.model.ClubRecruitmentImage;
import gg.agit.konect.domain.club.repository.ClubApplyRepository;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRecruitmentRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.university.enums.Campus;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClubRecruitmentService 단위 테스트")
class ClubRecruitmentServiceTest {

    private static final int CLUB_ID = 31;
    private static final int USER_ID = 41;
    private static final int UNIVERSITY_ID = 1;
    private static final int EXISTING_RECRUITMENT_ID = 99;
    private static final LocalDate START_DATE = LocalDate.of(2026, 3, 1);
    private static final LocalDate END_DATE = LocalDate.of(2026, 3, 31);

    @Mock
    private ClubRepository clubRepository;
    @Mock
    private ClubRecruitmentRepository clubRecruitmentRepository;
    @Mock
    private ClubMemberRepository clubMemberRepository;
    @Mock
    private ClubApplyRepository clubApplyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ClubPermissionValidator clubPermissionValidator;

    @InjectMocks
    private ClubRecruitmentService clubRecruitmentService;

    @Nested
    @DisplayName("getRecruitment 테스트")
    class GetRecruitmentTests {

        @Test
        @DisplayName("멤버면 지원 여부를 true로 반환하고 지원조회는 수행하지 않는다")
        void getRecruitmentForMemberReturnsAppliedTrueWithoutApplyLookup() {
            // Given
            Club club = createClub(CLUB_ID);
            User user = createUser(USER_ID, "member");
            ClubRecruitment recruitment = createRecruitment(null, club, true, null, null, "상시모집");
            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(userRepository.getById(USER_ID)).thenReturn(user);
            when(clubRecruitmentRepository.getByClubId(CLUB_ID)).thenReturn(recruitment);
            when(clubMemberRepository.existsByClubIdAndUserId(CLUB_ID, USER_ID)).thenReturn(true);

            // When
            ClubRecruitmentResponse response = clubRecruitmentService.getRecruitment(CLUB_ID, USER_ID);

            // Then
            assertThat(response.isApplied()).isTrue();
            assertThat(response.status()).isEqualTo(RecruitmentStatus.ONGOING);
            verify(clubApplyRepository, never()).existsByClubIdAndUserId(CLUB_ID, USER_ID);
        }

        @Test
        @DisplayName("비멤버지만 지원했으면 지원 여부를 true로 반환한다")
        void getRecruitmentForAppliedNonMemberReturnsAppliedTrue() {
            // Given
            Club club = createClub(CLUB_ID);
            User user = createUser(USER_ID, "applicant");
            ClubRecruitment recruitment = createRecruitment(null, club, false, START_DATE, END_DATE, "기간모집");
            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(userRepository.getById(USER_ID)).thenReturn(user);
            when(clubRecruitmentRepository.getByClubId(CLUB_ID)).thenReturn(recruitment);
            when(clubMemberRepository.existsByClubIdAndUserId(CLUB_ID, USER_ID)).thenReturn(false);
            when(clubApplyRepository.existsByClubIdAndUserId(CLUB_ID, USER_ID)).thenReturn(true);

            // When
            ClubRecruitmentResponse response = clubRecruitmentService.getRecruitment(CLUB_ID, USER_ID);

            // Then
            assertThat(response.isApplied()).isTrue();
        }

        @Test
        @DisplayName("비멤버이고 지원도 안했으면 지원 여부를 false로 반환한다")
        void getRecruitmentForNonAppliedUserReturnsAppliedFalse() {
            // Given
            Club club = createClub(CLUB_ID);
            User user = createUser(USER_ID, "visitor");
            ClubRecruitment recruitment = createRecruitment(null, club, false, START_DATE, END_DATE, "기간모집");
            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(userRepository.getById(USER_ID)).thenReturn(user);
            when(clubRecruitmentRepository.getByClubId(CLUB_ID)).thenReturn(recruitment);
            when(clubMemberRepository.existsByClubIdAndUserId(CLUB_ID, USER_ID)).thenReturn(false);
            when(clubApplyRepository.existsByClubIdAndUserId(CLUB_ID, USER_ID)).thenReturn(false);

            // When
            ClubRecruitmentResponse response = clubRecruitmentService.getRecruitment(CLUB_ID, USER_ID);

            // Then
            assertThat(response.isApplied()).isFalse();
        }
    }

    @Nested
    @DisplayName("upsertRecruitment 테스트")
    class UpsertRecruitmentTests {

        @Test
        @DisplayName("기존 모집공고가 없으면 신규 생성 후 저장한다")
        void upsertRecruitmentWithoutExistingRecruitmentCreatesAndSaves() {
            // Given
            Club club = createClub(CLUB_ID);
            ClubRecruitmentUpsertRequest request = createRequest(false, START_DATE, END_DATE, "신규공고", "img1", "img2");
            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(userRepository.getById(USER_ID)).thenReturn(createUser(USER_ID, "manager"));
            when(clubRecruitmentRepository.findByClubId(CLUB_ID)).thenReturn(Optional.empty());

            // When
            clubRecruitmentService.upsertRecruitment(CLUB_ID, USER_ID, request);

            // Then
            ArgumentCaptor<ClubRecruitment> captor = ArgumentCaptor.forClass(ClubRecruitment.class);
            verify(clubRecruitmentRepository).save(captor.capture());
            ClubRecruitment saved = captor.getValue();
            assertThat(saved.getImages()).hasSize(2);
            assertThat(saved.getImages().get(0).getDisplayOrder()).isEqualTo(0);
            assertThat(saved.getImages().get(1).getDisplayOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("기존 모집공고가 있으면 업데이트하고 저장은 호출하지 않는다")
        void upsertRecruitmentWithExistingRecruitmentUpdatesWithoutSaveCall() {
            // Given
            Club club = createClub(CLUB_ID);
            ClubRecruitment existing = createRecruitment(
                EXISTING_RECRUITMENT_ID,
                club,
                false,
                START_DATE,
                END_DATE,
                "기존공고"
            );
            existing.addImage(ClubRecruitmentImage.of("old1", 0, existing));
            existing.addImage(ClubRecruitmentImage.of("old2", 1, existing));
            ClubRecruitmentUpsertRequest request = createRequest(
                true,
                null,
                null,
                "갱신공고",
                "new1"
            );
            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(userRepository.getById(USER_ID)).thenReturn(createUser(USER_ID, "manager"));
            when(clubRecruitmentRepository.findByClubId(CLUB_ID)).thenReturn(Optional.of(existing));

            // When
            clubRecruitmentService.upsertRecruitment(CLUB_ID, USER_ID, request);

            // Then
            assertThat(existing.getIsAlwaysRecruiting()).isTrue();
            assertThat(existing.getContent()).isEqualTo("갱신공고");
            assertThat(existing.getImages()).hasSize(1);
            assertThat(existing.getImages().get(0).getUrl()).isEqualTo("new1");
            verify(clubRecruitmentRepository, never()).save(any());
        }

        @Test
        @DisplayName("권한이 없으면 validator 예외를 전파하고 upsert를 중단한다")
        void upsertRecruitmentWithoutManagerAccessPropagatesException() {
            // Given
            ClubRecruitmentUpsertRequest request = createRequest(false, START_DATE, END_DATE, "공고", "img");
            CustomException forbidden = CustomException.of(ApiResponseCode.FORBIDDEN_CLUB_MANAGER_ACCESS);
            when(clubRepository.getById(CLUB_ID)).thenReturn(createClub(CLUB_ID));
            when(userRepository.getById(USER_ID)).thenReturn(createUser(USER_ID, "user"));
            doThrow(forbidden)
                .when(clubPermissionValidator)
                .validateManagerAccess(CLUB_ID, USER_ID);

            // When & Then
            assertThatThrownBy(() -> clubRecruitmentService.upsertRecruitment(CLUB_ID, USER_ID, request))
                .isSameAs(forbidden);
            verify(clubRecruitmentRepository, never()).findByClubId(CLUB_ID);
        }

        @Test
        @DisplayName("상시모집에서 날짜를 전달하면 INVALID_RECRUITMENT_DATE_NOT_ALLOWED 예외가 발생한다")
        void upsertRecruitmentWithAlwaysRecruitingAndDatesThrowsCustomException() {
            // Given
            Club club = createClub(CLUB_ID);
            ClubRecruitmentUpsertRequest request = createRequest(
                true,
                START_DATE,
                END_DATE,
                "공고",
                "img"
            );
            when(clubRepository.getById(CLUB_ID)).thenReturn(club);
            when(userRepository.getById(USER_ID)).thenReturn(createUser(USER_ID, "manager"));
            when(clubRecruitmentRepository.findByClubId(CLUB_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> clubRecruitmentService.upsertRecruitment(CLUB_ID, USER_ID, request))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(ex.getErrorCode())
                    .isEqualTo(ApiResponseCode.INVALID_RECRUITMENT_DATE_NOT_ALLOWED));
            verify(clubRecruitmentRepository, never()).save(any());
        }
    }

    private Club createClub(Integer clubId) {
        return Club.builder()
            .id(clubId)
            .name("BCSD")
            .description("desc")
            .introduce("intro")
            .imageUrl("https://img")
            .location("room")
            .clubCategory(ClubCategory.ACADEMIC)
            .university(University.builder().id(UNIVERSITY_ID).koreanName("경북대").campus(Campus.MAIN).build())
            .build();
    }

    private User createUser(Integer userId, String name) {
        return User.builder()
            .id(userId)
            .name(name)
            .email(name + "@konect.gg")
            .studentNumber("2020123456")
            .provider(Provider.GOOGLE)
            .providerId("pid-" + userId)
            .university(University.builder().id(UNIVERSITY_ID).koreanName("경북대").campus(Campus.MAIN).build())
            .build();
    }

    private ClubRecruitment createRecruitment(
        Integer recruitmentId,
        Club club,
        boolean alwaysRecruiting,
        LocalDate startDate,
        LocalDate endDate,
        String content
    ) {
        return ClubRecruitment.builder()
            .id(recruitmentId)
            .club(club)
            .isAlwaysRecruiting(alwaysRecruiting)
            .startDate(startDate)
            .endDate(endDate)
            .content(content)
            .build();
    }

    private ClubRecruitmentUpsertRequest createRequest(
        boolean alwaysRecruiting,
        LocalDate startDate,
        LocalDate endDate,
        String content,
        String... imageUrls
    ) {
        List<InnerClubRecruitmentImageRequest> images = java.util.Arrays.stream(imageUrls)
            .map(InnerClubRecruitmentImageRequest::new)
            .toList();
        return new ClubRecruitmentUpsertRequest(startDate, endDate, alwaysRecruiting, content, images);
    }
}
