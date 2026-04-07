package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import gg.agit.konect.domain.chat.service.ChatRoomMembershipService;
import gg.agit.konect.domain.club.dto.SheetImportConfirmRequest;
import gg.agit.konect.domain.club.dto.SheetImportPreviewResponse;
import gg.agit.konect.domain.club.dto.SheetImportResponse;
import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.SheetColumnMapping;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class SheetImportServiceTest extends ServiceTestSupport {

    private static final Integer CLUB_ID = 1;
    private static final Integer REQUESTER_ID = 2;
    private static final String SPREADSHEET_ID = "sheet-id";
    private static final String SPREADSHEET_URL =
        "https://docs.google.com/spreadsheets/d/" + SPREADSHEET_ID + "/edit";

    @Mock
    private Sheets googleSheetsService;

    @Mock
    private Sheets.Spreadsheets spreadsheets;

    @Mock
    private Sheets.Spreadsheets.Values values;

    @Mock
    private Sheets.Spreadsheets.Values.Get getRequest;

    @Mock
    private SheetHeaderMapper sheetHeaderMapper;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubPreMemberRepository clubPreMemberRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomMembershipService chatRoomMembershipService;

    @Mock
    private ClubPermissionValidator clubPermissionValidator;

    @Mock
    private PlatformTransactionManager transactionManager;

    @InjectMocks
    private SheetImportService sheetImportService;

    @Test
    void previewPreMembersFromSheetReturnsDirectAndPreMembers() throws IOException {
        Club club = ClubFixture.create(UniversityFixture.create());
        User directUser = UserFixture.createUser(club.getUniversity(), "Alex Kim", "2021232948");

        given(clubRepository.getById(CLUB_ID)).willReturn(club);
        given(sheetHeaderMapper.analyzeAllSheets(SPREADSHEET_ID)).willReturn(
            new SheetHeaderMapper.SheetAnalysisResult(SheetColumnMapping.defaultMapping(), null, null)
        );
        given(clubMemberRepository.findStudentNumbersByClubId(CLUB_ID)).willReturn(Set.of());
        given(clubPreMemberRepository.findStudentNumberAndNameByClubId(CLUB_ID))
            .willReturn(List.<ClubPreMemberRepository.PreMemberKey>of());
        given(clubMemberRepository.findUserIdsByClubId(CLUB_ID)).willReturn(List.of());
        given(transactionManager.getTransaction(any())).willReturn(new SimpleTransactionStatus());
        given(userRepository.findAllByUniversityIdAndStudentNumberIn(
            eq(club.getUniversity().getId()),
            anySet()
        )).willReturn(List.of(directUser));

        given(googleSheetsService.spreadsheets()).willReturn(spreadsheets);
        given(spreadsheets.values()).willReturn(values);
        given(values.get(SPREADSHEET_ID, "A2:Z")).willReturn(getRequest);
        given(getRequest.setValueRenderOption("FORMATTED_VALUE")).willReturn(getRequest);
        given(getRequest.execute()).willReturn(new ValueRange().setValues(List.of(
            List.of("Alex Kim", "2021232948", "", "010-1234-5678", ClubPosition.MANAGER.name()),
            List.of("Dana Lee", "2021232949", "", "010-9999-8888", ClubPosition.MEMBER.name())
        )));

        SheetImportPreviewResponse response = sheetImportService.previewPreMembersFromSheet(
            CLUB_ID,
            REQUESTER_ID,
            SPREADSHEET_URL
        );

        assertThat(response.previewCount()).isEqualTo(2);
        assertThat(response.autoRegisteredCount()).isEqualTo(1);
        assertThat(response.preRegisteredCount()).isEqualTo(1);
        assertThat(response.members())
            .extracting(SheetImportPreviewResponse.PreviewMember::studentNumber)
            .containsExactly("2021232948", "2021232949");
        assertThat(response.members())
            .extracting(SheetImportPreviewResponse.PreviewMember::isDirectMember)
            .containsExactly(true, false);
        assertThat(response.members())
            .extracting(SheetImportPreviewResponse.PreviewMember::enabled)
            .containsExactly(true, true);
    }

    @Test
    void confirmImportPreMembersImportsOnlyEnabledMembers() {
        Club club = ClubFixture.create(UniversityFixture.create());
        User directUser = UserFixture.createUser(club.getUniversity(), "Alex Kim", "2021232948");

        given(clubRepository.getById(CLUB_ID)).willReturn(club);
        given(clubMemberRepository.findStudentNumbersByClubId(CLUB_ID)).willReturn(Set.of());
        given(clubPreMemberRepository.findStudentNumberAndNameByClubId(CLUB_ID))
            .willReturn(List.<ClubPreMemberRepository.PreMemberKey>of());
        given(clubMemberRepository.findUserIdsByClubId(CLUB_ID)).willReturn(List.of());
        given(userRepository.findAllByUniversityIdAndStudentNumberIn(
            eq(club.getUniversity().getId()),
            anySet()
        )).willReturn(List.of(directUser));
        given(clubMemberRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        SheetImportResponse response = sheetImportService.confirmImportPreMembers(
            CLUB_ID,
            REQUESTER_ID,
            List.of(
                new SheetImportConfirmRequest.ConfirmMember(
                    "2021232948",
                    "Alex Kim",
                    ClubPosition.MANAGER,
                    true
                ),
                new SheetImportConfirmRequest.ConfirmMember(
                    "2021232949",
                    "Dana Lee",
                    ClubPosition.MEMBER,
                    false
                ),
                new SheetImportConfirmRequest.ConfirmMember(
                    "2021232950",
                    "Chris Park",
                    ClubPosition.MEMBER,
                    true
                )
            )
        );

        assertThat(response.importedCount()).isEqualTo(1);
        assertThat(response.autoRegisteredCount()).isEqualTo(1);
        verifyNoInteractions(googleSheetsService);
    }
}
