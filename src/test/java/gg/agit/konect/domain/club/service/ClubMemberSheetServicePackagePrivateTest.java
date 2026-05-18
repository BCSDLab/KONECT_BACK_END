package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.ServiceTestSupport;

class ClubMemberSheetServicePackagePrivateTest extends ServiceTestSupport {

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private ClubPreMemberRepository clubPreMemberRepository;

    @Mock
    private ClubPermissionValidator clubPermissionValidator;

    @Mock
    private SheetSyncExecutor sheetSyncExecutor;

    @Mock
    private SheetHeaderMapper sheetHeaderMapper;

    @Mock
    private ClubSheetRegistrationService clubSheetRegistrationService;

    @InjectMocks
    private ClubMemberSheetService clubMemberSheetService;

    @Test
    @DisplayName("분석 결과를 받은 updateSheetId도 동아리가 없으면 권한 검증 전에 실패한다")
    void updateSheetIdWithAnalysisThrowsNotFoundClubBeforePermissionCheck() {
        // given
        Integer clubId = 1;
        Integer requesterId = 2;
        String spreadsheetId = "spreadsheet-id";
        SheetHeaderMapper.SheetAnalysisResult analysisResult = new SheetHeaderMapper.SheetAnalysisResult(
            null,
            null,
            null
        );

        given(clubRepository.existsById(clubId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> clubMemberSheetService.updateSheetId(
            clubId,
            requesterId,
            spreadsheetId,
            analysisResult
        ))
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode()).isEqualTo(
                ApiResponseCode.NOT_FOUND_CLUB));

        verifyNoInteractions(clubPermissionValidator, clubSheetRegistrationService);
    }
}
