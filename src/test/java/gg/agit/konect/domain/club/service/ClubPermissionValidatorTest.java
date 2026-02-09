package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClubPermissionValidator 단위 테스트")
class ClubPermissionValidatorTest {

    private static final int CLUB_ID = 10;
    private static final int USER_ID = 20;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @InjectMocks
    private ClubPermissionValidator clubPermissionValidator;

    @Nested
    @DisplayName("validatePresidentAccess 테스트")
    class ValidatePresidentAccessTests {

        @Test
        @DisplayName("회장 권한이 있으면 예외가 발생하지 않는다")
        void validatePresidentAccessWithAuthorityDoesNotThrow() {
            // Given
            when(clubMemberRepository.existsByClubIdAndUserIdAndPositionIn(any(), any(), any())).thenReturn(true);

            // When & Then
            clubPermissionValidator.validatePresidentAccess(CLUB_ID, USER_ID);
        }

        @Test
        @DisplayName("회장 권한이 없으면 FORBIDDEN_CLUB_MANAGER_ACCESS 예외가 발생한다")
        void validatePresidentAccessWithoutAuthorityThrowsCustomException() {
            // Given
            when(clubMemberRepository.existsByClubIdAndUserIdAndPositionIn(any(), any(), any())).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> clubPermissionValidator.validatePresidentAccess(CLUB_ID, USER_ID))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(ex.getErrorCode())
                    .isEqualTo(ApiResponseCode.FORBIDDEN_CLUB_MANAGER_ACCESS));
        }
    }

    @Nested
    @DisplayName("validateLeaderAccess 테스트")
    class ValidateLeaderAccessTests {

        @Test
        @DisplayName("리더 권한이 있으면 예외가 발생하지 않는다")
        void validateLeaderAccessWithAuthorityDoesNotThrow() {
            // Given
            when(clubMemberRepository.existsByClubIdAndUserIdAndPositionIn(any(), any(), any())).thenReturn(true);

            // When & Then
            clubPermissionValidator.validateLeaderAccess(CLUB_ID, USER_ID);
        }

        @Test
        @DisplayName("리더 권한이 없으면 FORBIDDEN_CLUB_MANAGER_ACCESS 예외가 발생한다")
        void validateLeaderAccessWithoutAuthorityThrowsCustomException() {
            // Given
            when(clubMemberRepository.existsByClubIdAndUserIdAndPositionIn(any(), any(), any())).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> clubPermissionValidator.validateLeaderAccess(CLUB_ID, USER_ID))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(ex.getErrorCode())
                    .isEqualTo(ApiResponseCode.FORBIDDEN_CLUB_MANAGER_ACCESS));
        }
    }

    @Nested
    @DisplayName("validateManagerAccess 테스트")
    class ValidateManagerAccessTests {

        @Test
        @DisplayName("운영진 권한이 있으면 예외가 발생하지 않는다")
        void validateManagerAccessWithAuthorityDoesNotThrow() {
            // Given
            when(clubMemberRepository.existsByClubIdAndUserIdAndPositionIn(any(), any(), any())).thenReturn(true);

            // When & Then
            clubPermissionValidator.validateManagerAccess(CLUB_ID, USER_ID);
        }

        @Test
        @DisplayName("운영진 권한이 없으면 FORBIDDEN_CLUB_MANAGER_ACCESS 예외가 발생한다")
        void validateManagerAccessWithoutAuthorityThrowsCustomException() {
            // Given
            when(clubMemberRepository.existsByClubIdAndUserIdAndPositionIn(any(), any(), any())).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> clubPermissionValidator.validateManagerAccess(CLUB_ID, USER_ID))
                .isInstanceOfSatisfying(CustomException.class, ex -> assertThat(ex.getErrorCode())
                    .isEqualTo(ApiResponseCode.FORBIDDEN_CLUB_MANAGER_ACCESS));
        }
    }
}
