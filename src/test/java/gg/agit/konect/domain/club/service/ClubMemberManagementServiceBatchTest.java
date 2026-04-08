package gg.agit.konect.domain.club.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;

import gg.agit.konect.domain.chat.service.ChatRoomMembershipService;
import gg.agit.konect.domain.club.dto.ClubPreMemberAddRequest;
import gg.agit.konect.domain.club.dto.ClubPreMemberBatchAddRequest;
import gg.agit.konect.domain.club.enums.ClubPosition;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.club.repository.ClubPreMemberRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.university.enums.Campus;
import gg.agit.konect.domain.university.model.University;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

@ExtendWith(MockitoExtension.class)
class ClubMemberManagementServiceBatchTest {

    @InjectMocks
    private ClubMemberManagementService clubMemberManagementService;

    @Mock
    private ClubRepository clubRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private ClubPreMemberRepository clubPreMemberRepository;

    @Mock
    private ClubPermissionValidator clubPermissionValidator;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRoomMembershipService chatRoomMembershipService;

    @Mock
    private PlatformTransactionManager transactionManager;

    private Club club;
    private University university;
    private Integer clubId = 1;
    private Integer requesterId = 100;

    @BeforeEach
    void setUp() {
        university = University.builder()
            .id(1)
            .koreanName("Test University")
            .campus(Campus.MAIN)
            .build();

        club = Club.builder()
            .id(clubId)
            .name("Test Club")
            .university(university)
            .build();
    }

    @Test
    @DisplayName("배치 등록 요청 시 권한 검증이 먼저 수행된다")
    void addPreMembersBatch_validatesPermissionFirst() {
        // given
        when(clubRepository.getById(clubId)).thenReturn(club);
        doThrow(CustomException.of(ApiResponseCode.FORBIDDEN_CLUB_MANAGER_ACCESS))
            .when(clubPermissionValidator).validateManagerAccess(clubId, requesterId);

        ClubPreMemberAddRequest request = new ClubPreMemberAddRequest("2022000001", "학생1", ClubPosition.MEMBER);
        ClubPreMemberBatchAddRequest batchRequest = new ClubPreMemberBatchAddRequest(List.of(request));

        // when & then
        CustomException exception = assertThrows(CustomException.class, () -> {
            clubMemberManagementService.addPreMembersBatch(clubId, requesterId, batchRequest);
        });
        assertThat(exception.getErrorCode()).isEqualTo(ApiResponseCode.FORBIDDEN_CLUB_MANAGER_ACCESS);
    }

    @Test
    @DisplayName("배치 등록 요청은 최소 1명 이상의 회원이 필요하다")
    void addPreMembersBatch_requiresAtLeastOneMember() {
        // given
        ClubPreMemberBatchAddRequest batchRequest = new ClubPreMemberBatchAddRequest(List.of());

        // when & then - Bean Validation은 컨트롤러 계층에서 처리되므로 서비스에서는 정상 동작
        // 실제로는 컨트롤러에서 @Valid로 인해 400 에러가 반환됨
        when(clubRepository.getById(clubId)).thenReturn(club);

        // 빈 리스트로 호출해도 서비스는 동작하되 결과는 빈 리스트
        var response = clubMemberManagementService.addPreMembersBatch(clubId, requesterId, batchRequest);
        assertThat(response.totalCount()).isEqualTo(0);
        assertThat(response.successCount()).isEqualTo(0);
        assertThat(response.failedCount()).isEqualTo(0);
    }
}
