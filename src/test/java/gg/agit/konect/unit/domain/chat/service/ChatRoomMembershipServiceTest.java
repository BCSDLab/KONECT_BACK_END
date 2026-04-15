package gg.agit.konect.unit.domain.chat.service;

import static gg.agit.konect.domain.chat.service.ChatRoomMembershipService.SYSTEM_ADMIN_ID;
import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CHAT_ROOM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.chat.dto.ChatRoomMemberResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomMembersResponse;
import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.chat.service.ChatRoomMembershipService;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class ChatRoomMembershipServiceTest extends ServiceTestSupport {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ChatRoomMembershipService chatRoomMembershipService;

    @Test
    @DisplayName("채팅방 멤버 목록 조회 성공")
    void getChatRoomMembers_success() {
        // given
        Integer chatRoomId = 1;
        Integer currentUserId = 100;

        User user1 = createUser(currentUserId, "User1", UserRole.USER);
        User user2 = createUser(200, "User2", UserRole.USER);
        ChatRoom chatRoom = createRoom(chatRoomId, ChatType.GROUP, LocalDateTime.now());
        ChatRoomMember member1 = createRoomMember(chatRoom, user1, true, LocalDateTime.now());
        ChatRoomMember member2 = createRoomMember(chatRoom, user2, false, LocalDateTime.now());

        given(userRepository.findById(currentUserId)).willReturn(Optional.of(user1));
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(chatRoomMemberRepository.existsActiveByChatRoomIdAndUserId(chatRoomId, currentUserId)).willReturn(true);
        given(chatRoomMemberRepository.findActiveMembersByChatRoomId(chatRoomId)).willReturn(List.of(member1, member2));

        // when
        ChatRoomMembersResponse response = chatRoomMembershipService.getChatRoomMembers(chatRoomId, currentUserId);

        // then
        assertThat(response.members()).hasSize(2);

        ChatRoomMemberResponse firstMember = response.members().get(0);
        assertThat(firstMember.userId()).isEqualTo(100);
        assertThat(firstMember.name()).isEqualTo("User1");
        assertThat(firstMember.isOwner()).isTrue();

        ChatRoomMemberResponse secondMember = response.members().get(1);
        assertThat(secondMember.userId()).isEqualTo(200);
        assertThat(secondMember.name()).isEqualTo("User2");
        assertThat(secondMember.isOwner()).isFalse();
    }

    @Test
    @DisplayName("비멤버가 조회 시도 시 FORBIDDEN 예외 발생")
    void getChatRoomMembers_forbiddenWhenNotMember() {
        // given
        Integer chatRoomId = 1;
        Integer currentUserId = 100;
        User user = createUser(currentUserId, "User1", UserRole.USER);
        ChatRoom chatRoom = createRoom(chatRoomId, ChatType.GROUP, LocalDateTime.now());

        given(userRepository.findById(currentUserId)).willReturn(Optional.of(user));
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(chatRoomMemberRepository.existsActiveByChatRoomIdAndUserId(chatRoomId, currentUserId)).willReturn(false);

        // when & then
        assertErrorCode(
            () -> chatRoomMembershipService.getChatRoomMembers(chatRoomId, currentUserId),
            FORBIDDEN_CHAT_ROOM_ACCESS
        );
    }

    @Test
    @DisplayName("나간 멤버가 조회 시도 시 FORBIDDEN 예외 발생")
    void getChatRoomMembers_forbiddenWhenLeftMember() {
        // given
        Integer chatRoomId = 1;
        Integer currentUserId = 100;
        User user = createUser(currentUserId, "User1", UserRole.USER);
        ChatRoom chatRoom = createRoom(chatRoomId, ChatType.GROUP, LocalDateTime.now());

        given(userRepository.findById(currentUserId)).willReturn(Optional.of(user));
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(chatRoomMemberRepository.existsActiveByChatRoomIdAndUserId(chatRoomId, currentUserId)).willReturn(false);

        // when & then
        assertErrorCode(
            () -> chatRoomMembershipService.getChatRoomMembers(chatRoomId, currentUserId),
            FORBIDDEN_CHAT_ROOM_ACCESS
        );
    }

    @Test
    @DisplayName("나간 멤버는 조회되지 않음")
    void getChatRoomMembers_excludesLeftMembers() {
        // given
        Integer chatRoomId = 1;
        Integer currentUserId = 100;

        User user1 = createUser(currentUserId, "User1", UserRole.USER);
        User user2 = createUser(200, "User2", UserRole.USER);
        ChatRoom chatRoom = createRoom(chatRoomId, ChatType.GROUP, LocalDateTime.now());
        ChatRoomMember member1 = createRoomMember(chatRoom, user1, false, LocalDateTime.now());

        given(userRepository.findById(currentUserId)).willReturn(Optional.of(user1));
        given(chatRoomRepository.findById(chatRoomId)).willReturn(Optional.of(chatRoom));
        given(chatRoomMemberRepository.existsActiveByChatRoomIdAndUserId(chatRoomId, currentUserId)).willReturn(true);
        given(chatRoomMemberRepository.findActiveMembersByChatRoomId(chatRoomId)).willReturn(List.of(member1));

        // when
        ChatRoomMembersResponse response = chatRoomMembershipService.getChatRoomMembers(chatRoomId, currentUserId);

        // then
        assertThat(response.members()).hasSize(1);
        assertThat(response.members().get(0).userId()).isEqualTo(100);
        assertThat(response.members().get(0).name()).isEqualTo("User1");
    }

    @Test
    @DisplayName("addClubMember는 기존 클럽 채팅방이 없으면 생성하고 멤버를 저장한다")
    void addClubMemberCreatesClubRoomAndSavesMember() {
        // given
        Club club = createClub(10);
        User user = createUser(20, "동아리원", UserRole.USER);
        ClubMember clubMember = createClubMember(club, user, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoom savedRoom = createRoom(30, ChatType.CLUB_GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findByClubId(club.getId())).willReturn(Optional.empty());
        given(chatRoomRepository.save(any(ChatRoom.class))).willReturn(savedRoom);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(savedRoom.getId(), user.getId()))
            .willReturn(Optional.empty());

        // when
        chatRoomMembershipService.addClubMember(clubMember);

        // then
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("addClubMember는 채팅방 동시 생성 중복 예외가 나면 재조회한 방에 멤버를 추가한다")
    void addClubMemberReloadsRoomWhenClubRoomCreationHitsDuplicate() {
        // given
        Club club = createClub(10);
        User user = createUser(20, "동아리원", UserRole.USER);
        ClubMember clubMember = createClubMember(club, user, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoom existingRoom = createRoom(31, ChatType.CLUB_GROUP, LocalDateTime.of(2026, 4, 11, 10, 1));

        given(chatRoomRepository.findByClubId(club.getId()))
            .willReturn(Optional.empty())
            .willReturn(Optional.of(existingRoom));
        given(chatRoomRepository.save(any(ChatRoom.class)))
            .willThrow(new DuplicateKeyException("duplicate room"));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(existingRoom.getId(), user.getId()))
            .willReturn(Optional.empty());

        // when
        chatRoomMembershipService.addClubMember(clubMember);

        // then
        verify(chatRoomRepository).save(any(ChatRoom.class));
        verify(chatRoomRepository, times(2)).findByClubId(club.getId());
        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("addClubMember는 이미 멤버가 있으면 더 최신 createdAt일 때만 lastReadAt을 갱신한다")
    void addClubMemberUpdatesLastReadAtOnlyWhenBaselineIsNewer() {
        // given
        Club club = createClub(10);
        User user = createUser(20, "동아리원", UserRole.USER);
        ChatRoom room = createRoom(30, ChatType.CLUB_GROUP, LocalDateTime.of(2026, 4, 11, 9, 0));
        ClubMember clubMember = createClubMember(club, user, LocalDateTime.of(2026, 4, 11, 11, 0));
        ChatRoomMember existingMember = createRoomMember(room, user, false, LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findByClubId(club.getId())).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId()))
            .willReturn(Optional.of(existingMember));

        // when
        chatRoomMembershipService.addClubMember(clubMember);

        // then
        assertThat(existingMember.getLastReadAt()).isEqualTo(clubMember.getCreatedAt());
        verify(chatRoomMemberRepository, never()).save(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("addClubMember는 이미 더 최신 lastReadAt이 있으면 저장하지 않고 유지한다")
    void addClubMemberKeepsLastReadAtWhenExistingMemberIsNewer() {
        // given
        Club club = createClub(10);
        User user = createUser(20, "동아리원", UserRole.USER);
        ChatRoom room = createRoom(30, ChatType.CLUB_GROUP, LocalDateTime.of(2026, 4, 11, 9, 0));
        ClubMember clubMember = createClubMember(club, user, LocalDateTime.of(2026, 4, 11, 10, 0));
        ChatRoomMember existingMember = createRoomMember(room, user, false, LocalDateTime.of(2026, 4, 11, 11, 0));

        given(chatRoomRepository.findByClubId(club.getId())).willReturn(Optional.of(room));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId()))
            .willReturn(Optional.of(existingMember));

        // when
        chatRoomMembershipService.addClubMember(clubMember);

        // then
        assertThat(existingMember.getLastReadAt()).isEqualTo(LocalDateTime.of(2026, 4, 11, 11, 0));
        verify(chatRoomMemberRepository, never()).save(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("addDirectMembers는 joinedAt이 null이면 즉시 실패한다")
    void addDirectMembersFailsWhenJoinedAtIsNull() {
        // given
        ChatRoom room = createRoom(10, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        User firstUser = createUser(20, "첫번째", UserRole.USER);
        User secondUser = createUser(30, "두번째", UserRole.USER);

        // when & then
        assertThatThrownBy(() -> chatRoomMembershipService.addDirectMembers(room, firstUser, secondUser, null))
            .isInstanceOf(NullPointerException.class);
        verify(chatRoomMemberRepository, never()).save(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("addDirectMembers는 멤버 저장 중 중복 예외가 나면 무시하고 나머지 멤버를 계속 처리한다")
    void addDirectMembersIgnoresDuplicateMemberSave() {
        // given
        ChatRoom room = createRoom(10, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        User firstUser = createUser(20, "첫번째", UserRole.USER);
        User secondUser = createUser(30, "두번째", UserRole.USER);
        LocalDateTime joinedAt = LocalDateTime.of(2026, 4, 11, 10, 5);

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), firstUser.getId()))
            .willReturn(Optional.empty());
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), secondUser.getId()))
            .willReturn(Optional.empty());
        given(chatRoomMemberRepository.save(any(ChatRoomMember.class)))
            .willThrow(new DuplicateKeyException("duplicate member"))
            .willReturn(createRoomMember(room, secondUser, false, joinedAt));

        // when
        chatRoomMembershipService.addDirectMembers(room, firstUser, secondUser, joinedAt);

        // then
        verify(chatRoomMemberRepository, times(2)).save(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("removeClubMember는 클럽 채팅방이 있을 때만 멤버를 삭제한다")
    void removeClubMemberDeletesOnlyWhenClubRoomExists() {
        // given
        ChatRoom room = createRoom(10, ChatType.CLUB_GROUP, LocalDateTime.of(2026, 4, 11, 10, 0));
        given(chatRoomRepository.findByClubId(1)).willReturn(Optional.of(room));

        // when
        chatRoomMembershipService.removeClubMember(1, 20);

        // then
        verify(chatRoomMemberRepository).deleteByChatRoomIdAndUserId(room.getId(), 20);
    }

    @Test
    @DisplayName("updateDirectRoomLastReadAt는 system-admin room에서 admin이 읽으면 시스템 관리자 멤버만 갱신한다")
    void updateDirectRoomLastReadAtUpdatesSystemAdminForAdminReader() {
        // given
        Integer roomId = 10;
        User admin = createUser(99, "관리자", UserRole.ADMIN);
        ChatRoom room = createRoom(roomId, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        LocalDateTime readAt = LocalDateTime.of(2026, 4, 11, 10, 5);
        given(chatRoomMemberRepository.findRoomMemberIdsByChatRoomIds(List.of(roomId)))
            .willReturn(List.<Object[]>of(new Object[] {roomId, SYSTEM_ADMIN_ID, readAt}));

        // when
        chatRoomMembershipService.updateDirectRoomLastReadAt(roomId, admin, readAt, room);

        // then
        verify(chatRoomMemberRepository).updateLastReadAtIfOlder(roomId, SYSTEM_ADMIN_ID, readAt);
        verify(chatRoomMemberRepository, never()).existsByChatRoomIdAndUserId(roomId, admin.getId());
        verify(chatRoomMemberRepository, never()).updateLastReadAtIfOlder(roomId, admin.getId(), readAt);
    }

    @Test
    @DisplayName("updateDirectRoomLastReadAt는 일반 멤버가 직접 채팅방에 속하면 본인 lastReadAt을 갱신한다")
    void updateDirectRoomLastReadAtUpdatesReaderWhenMemberExists() {
        // given
        Integer roomId = 10;
        User user = createUser(20, "사용자", UserRole.USER);
        ChatRoom room = createRoom(roomId, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        LocalDateTime readAt = LocalDateTime.of(2026, 4, 11, 10, 5);
        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(roomId, user.getId())).willReturn(true);

        // when
        chatRoomMembershipService.updateDirectRoomLastReadAt(roomId, user, readAt, room);

        // then
        verify(chatRoomMemberRepository).updateLastReadAtIfOlder(roomId, user.getId(), readAt);
    }

    @Test
    @DisplayName("updateDirectRoomLastReadAt는 일반 사용자가 멤버가 아니면 접근을 거부한다")
    void updateDirectRoomLastReadAtRejectsNonMemberUser() {
        // given
        Integer roomId = 10;
        User user = createUser(20, "사용자", UserRole.USER);
        ChatRoom room = createRoom(roomId, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        LocalDateTime readAt = LocalDateTime.of(2026, 4, 11, 10, 5);
        given(chatRoomMemberRepository.existsByChatRoomIdAndUserId(roomId, user.getId())).willReturn(false);

        // when & then
        assertErrorCode(
            () -> chatRoomMembershipService.updateDirectRoomLastReadAt(roomId, user, readAt, room),
            FORBIDDEN_CHAT_ROOM_ACCESS
        );
        verify(chatRoomMemberRepository, never()).updateLastReadAtIfOlder(roomId, user.getId(), readAt);
    }

    @Test
    @DisplayName("updateDirectRoomLastReadAt는 admin이 system-admin room 비멤버여도 본인 멤버를 생성하지 않는다")
    void updateDirectRoomLastReadAtSkipsAdminMembershipCreationInSystemAdminRoom() {
        // given
        Integer roomId = 10;
        User admin = createUser(99, "관리자", UserRole.ADMIN);
        ChatRoom room = createRoom(roomId, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        LocalDateTime readAt = LocalDateTime.of(2026, 4, 11, 10, 5);
        given(chatRoomMemberRepository.findRoomMemberIdsByChatRoomIds(List.of(roomId)))
            .willReturn(List.<Object[]>of(new Object[] {roomId, SYSTEM_ADMIN_ID, readAt}));

        // when
        chatRoomMembershipService.updateDirectRoomLastReadAt(roomId, admin, readAt, room);

        // then
        verify(chatRoomMemberRepository, never()).save(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("ensureClubRoomMember는 group room이 아니거나 club이 없으면 채팅방을 찾을 수 없다고 본다")
    void ensureClubRoomMemberRejectsNonClubGroupRoom() {
        // given
        ChatRoom directRoom = createRoom(10, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        given(chatRoomRepository.findById(directRoom.getId())).willReturn(Optional.of(directRoom));

        // when & then
        assertErrorCode(() -> chatRoomMembershipService.ensureClubRoomMember(directRoom.getId(), 20),
            NOT_FOUND_CHAT_ROOM);
        verify(clubMemberRepository, never()).getByClubIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("ensureClubRoomMember는 club 멤버 createdAt 기준으로 채팅방 멤버를 보장한다")
    void ensureClubRoomMemberCreatesOrUpdatesMemberFromClubMemberBaseline() {
        // given
        Club club = createClub(10);
        ChatRoom room = createRoom(30, ChatType.CLUB_GROUP, LocalDateTime.of(2026, 4, 11, 9, 0));
        ReflectionTestUtils.setField(room, "club", club);
        User user = createUser(20, "동아리원", UserRole.USER);
        ClubMember clubMember = createClubMember(club, user, LocalDateTime.of(2026, 4, 11, 10, 0));

        given(chatRoomRepository.findById(room.getId())).willReturn(Optional.of(room));
        given(clubMemberRepository.getByClubIdAndUserId(club.getId(), user.getId())).willReturn(clubMember);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId()))
            .willReturn(Optional.empty());

        // when
        chatRoomMembershipService.ensureClubRoomMember(room.getId(), user.getId());

        // then
        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));
    }

    @Test
    @DisplayName("updateLastReadAt는 저장된 값이 더 오래된 경우에만 갱신 쿼리를 위임한다")
    void updateLastReadAtDelegatesConditionalUpdate() {
        // when
        LocalDateTime readAt = LocalDateTime.of(2026, 4, 11, 10, 0);
        chatRoomMembershipService.updateLastReadAt(10, 20, readAt);

        // then
        verify(chatRoomMemberRepository).updateLastReadAtIfOlder(10, 20, readAt);
    }

    @Test
    @DisplayName("중복이 아닌 DataIntegrityViolationException은 삼키지 않고 다시 던진다")
    void addClubMemberRethrowsNonDuplicateIntegrityViolation() {
        // given
        Club club = createClub(10);
        User user = createUser(20, "동아리원", UserRole.USER);
        ClubMember clubMember = createClubMember(club, user, LocalDateTime.of(2026, 4, 11, 10, 0));
        DataIntegrityViolationException exception = new DataIntegrityViolationException("other constraint");

        given(chatRoomRepository.findByClubId(club.getId())).willReturn(Optional.empty());
        given(chatRoomRepository.save(any(ChatRoom.class))).willThrow(exception);

        // when & then
        assertThatThrownBy(() -> chatRoomMembershipService.addClubMember(clubMember))
            .isSameAs(exception);
    }

    @Test
    @DisplayName("root cause 메시지에 duplicate key가 있으면 채팅방 멤버 저장 중복도 무시한다")
    void addDirectMembersIgnoresDuplicateByRootCauseMessage() {
        // given
        ChatRoom room = createRoom(10, ChatType.DIRECT, LocalDateTime.of(2026, 4, 11, 10, 0));
        User firstUser = createUser(20, "첫번째", UserRole.USER);
        User secondUser = createUser(30, "두번째", UserRole.USER);
        LocalDateTime joinedAt = LocalDateTime.of(2026, 4, 11, 10, 5);
        DataIntegrityViolationException duplicateLikeException = new DataIntegrityViolationException(
            "constraint violated",
            new RuntimeException("duplicate key value violates unique constraint")
        );

        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), firstUser.getId()))
            .willReturn(Optional.empty());
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), secondUser.getId()))
            .willReturn(Optional.empty());
        given(chatRoomMemberRepository.save(any(ChatRoomMember.class)))
            .willThrow(duplicateLikeException)
            .willReturn(createRoomMember(room, secondUser, false, joinedAt));

        // when
        chatRoomMembershipService.addDirectMembers(room, firstUser, secondUser, joinedAt);

        // then
        verify(chatRoomMemberRepository, times(2)).save(any(ChatRoomMember.class));
    }

    private Club createClub(Integer clubId) {
        return ClubFixture.createWithId(UniversityFixture.createWithId(1), clubId, "BCSD");
    }

    private User createUser(Integer userId, String name, UserRole role) {
        return UserFixture.createUserWithId(userId, name, role);
    }

    private ClubMember createClubMember(Club club, User user, LocalDateTime createdAt) {
        ClubMember clubMember = ClubMemberFixture.createMember(club, user);
        ReflectionTestUtils.setField(clubMember, "createdAt", createdAt);
        return clubMember;
    }

    private ChatRoom createRoom(Integer id, ChatType type, LocalDateTime createdAt) {
        ChatRoom room = switch (type) {
            case DIRECT -> ChatRoom.directOf();
            case GROUP -> ChatRoom.groupOf();
            case CLUB_GROUP -> ChatRoom.clubGroupOf(createClub(77));
            default -> throw new IllegalArgumentException("Unsupported ChatType: " + type);
        };
        ReflectionTestUtils.setField(room, "id", id);
        ReflectionTestUtils.setField(room, "createdAt", createdAt);
        return room;
    }

    private ChatRoomMember createRoomMember(ChatRoom room, User user, boolean isOwner, LocalDateTime lastReadAt) {
        ChatRoomMember member = isOwner
            ? ChatRoomMember.ofOwner(room, user, lastReadAt)
            : ChatRoomMember.of(room, user, lastReadAt);
        ReflectionTestUtils.setField(member, "createdAt", lastReadAt);
        return member;
    }

    private void assertErrorCode(ThrowingCallable callable, ApiResponseCode errorCode) {
        assertThatThrownBy(callable)
            .isInstanceOf(CustomException.class)
            .satisfies(exception -> assertThat(((CustomException)exception).getErrorCode()).isEqualTo(errorCode));
    }
}
