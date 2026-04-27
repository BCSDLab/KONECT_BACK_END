package gg.agit.konect.unit.domain.chat.service;

import static gg.agit.konect.domain.chat.service.ChatRoomMembershipService.SYSTEM_ADMIN_ID;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CHAT_ROOM;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CLUB_MEMBER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.util.ReflectionTestUtils;

import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.service.ChatMessagePageResolver;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.support.ServiceTestSupport;
import gg.agit.konect.support.fixture.ClubFixture;
import gg.agit.konect.support.fixture.ClubMemberFixture;
import gg.agit.konect.support.fixture.UniversityFixture;
import gg.agit.konect.support.fixture.UserFixture;

class ChatMessagePageResolverTest extends ServiceTestSupport {

    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 4, 27, 10, 0);
    private static final LocalDateTime TARGET_TIME = LocalDateTime.of(2026, 4, 27, 14, 0);

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @Mock
    private ClubMemberRepository clubMemberRepository;

    @InjectMocks
    private ChatMessagePageResolver chatMessagePageResolver;

    @Test
    @DisplayName("group room 접근 권한 확인 후 messageId 페이지를 계산한다")
    void resolvePageForGroupRoomMessage() {
        User user = user(10, UserRole.USER);
        ChatRoom room = room(1, ChatType.GROUP);
        ChatMessage message = message(50, room, user, TARGET_TIME);
        stubActiveMember(room, user);
        stubTargetMessage(message);
        given(chatMessageRepository.countNewerMessagesByChatRoomId(room.getId(), message.getId(), TARGET_TIME, null))
            .willReturn(25L);

        int page = chatMessagePageResolver.resolvePageForMessage(room.getId(), message.getId(), room, user, 20);

        assertThat(page).isEqualTo(2);
    }

    @Test
    @DisplayName("group room 비회원이면 messageId 조회 전에 404를 던진다")
    void resolvePageForGroupRoomRejectsNonMemberBeforeMessageLookup() {
        User user = user(99, UserRole.USER);
        ChatRoom room = room(1, ChatType.GROUP);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId()))
            .willReturn(Optional.empty());

        assertNotFound(() -> chatMessagePageResolver.resolvePageForMessage(room.getId(), 50, room, user, 20));
        verify(chatMessageRepository, never()).findByIdWithChatRoom(50);
    }

    @Test
    @DisplayName("group room에서 나간 멤버를 404로 거부한다")
    void resolvePageForGroupRoomRejectsLeftMember() {
        User user = user(10, UserRole.USER);
        ChatRoom room = room(1, ChatType.GROUP);
        ChatRoomMember member = member(room, user);
        ReflectionTestUtils.setField(member, "leftAt", BASE_TIME.plusHours(1));
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId()))
            .willReturn(Optional.of(member));

        assertNotFound(() -> chatMessagePageResolver.resolvePageForMessage(room.getId(), 50, room, user, 20));
        verify(chatMessageRepository, never()).findByIdWithChatRoom(50);
    }

    @Test
    @DisplayName("direct room은 visibleMessageFrom 이후 메시지만 페이지 계산한다")
    void resolvePageForDirectRoomUsesVisibleMessageFrom() {
        User user = user(10, UserRole.USER);
        ChatRoom room = room(1, ChatType.DIRECT);
        LocalDateTime visibleMessageFrom = BASE_TIME.plusHours(2);
        ChatMessage message = message(50, room, user(20, UserRole.USER), BASE_TIME.plusHours(3));
        stubDirectMember(room, user, visibleMessageFrom);
        stubTargetMessage(message);
        given(chatMessageRepository.countNewerMessagesByChatRoomId(
            room.getId(), message.getId(), message.getCreatedAt(), visibleMessageFrom
        )).willReturn(0L);

        int page = chatMessagePageResolver.resolvePageForMessage(room.getId(), message.getId(), room, user, 20);

        assertThat(page).isEqualTo(1);
    }

    @Test
    @DisplayName("direct room의 visibleMessageFrom 이전 messageId를 404로 숨긴다")
    void resolvePageForDirectRoomRejectsHiddenMessage() {
        User user = user(10, UserRole.USER);
        ChatRoom room = room(1, ChatType.DIRECT);
        LocalDateTime visibleMessageFrom = BASE_TIME.plusHours(2);
        ChatMessage message = message(50, room, user(20, UserRole.USER), BASE_TIME.plusHours(1));
        stubDirectMember(room, user, visibleMessageFrom);
        stubTargetMessage(message);

        assertNotFound(() -> chatMessagePageResolver.resolvePageForMessage(room.getId(), 50, room, user, 20));
        verify(chatMessageRepository, never()).countNewerMessagesByChatRoomId(
            room.getId(), message.getId(), message.getCreatedAt(), visibleMessageFrom
        );
    }

    @Test
    @DisplayName("direct room 비회원 일반 사용자를 404로 거부한다")
    void resolvePageForDirectRoomRejectsNonMemberUser() {
        User user = user(10, UserRole.USER);
        ChatRoom room = room(1, ChatType.DIRECT);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId()))
            .willReturn(Optional.empty());

        assertNotFound(() -> chatMessagePageResolver.resolvePageForMessage(room.getId(), 50, room, user, 20));
        verify(chatMessageRepository, never()).findByIdWithChatRoom(50);
    }

    @Test
    @DisplayName("admin의 system admin 방 조회는 SYSTEM_ADMIN 가시 범위를 사용한다")
    void resolvePageForAdminSystemRoomUsesSystemAdminVisibility() {
        User admin = user(99, UserRole.ADMIN);
        ChatRoom room = room(1, ChatType.DIRECT);
        LocalDateTime visibleMessageFrom = BASE_TIME.plusHours(2);
        ChatMessage message = message(50, room, admin, BASE_TIME.plusHours(3));
        stubSystemAdminRoom(room);
        stubSystemAdminMember(room, visibleMessageFrom);
        stubTargetMessage(message);
        given(chatMessageRepository.countNewerMessagesByChatRoomId(
            room.getId(), message.getId(), message.getCreatedAt(), visibleMessageFrom
        )).willReturn(0L);

        int page = chatMessagePageResolver.resolvePageForMessage(room.getId(), message.getId(), room, admin, 20);

        assertThat(page).isEqualTo(1);
    }

    @Test
    @DisplayName("system admin 멤버가 없으면 admin 조회 가시 범위를 null로 계산한다")
    void resolvePageForAdminSystemRoomAllowsMissingSystemAdminMember() {
        User admin = user(99, UserRole.ADMIN);
        ChatRoom room = room(1, ChatType.DIRECT);
        ChatMessage message = message(50, room, admin, BASE_TIME.plusHours(3));
        stubSystemAdminRoom(room);
        given(chatRoomMemberRepository.findByChatRoomId(room.getId())).willReturn(List.of());
        stubTargetMessage(message);
        given(chatMessageRepository.countNewerMessagesByChatRoomId(
            room.getId(), message.getId(), message.getCreatedAt(), null
        )).willReturn(0L);

        int page = chatMessagePageResolver.resolvePageForMessage(room.getId(), message.getId(), room, admin, 20);

        assertThat(page).isEqualTo(1);
    }

    @Test
    @DisplayName("club group 동아리 멤버십 없음만 404로 변환한다")
    void resolvePageForClubRoomConvertsMissingClubMemberToNotFoundRoom() {
        User user = user(10, UserRole.USER);
        ChatRoom room = clubRoom(1);
        given(clubMemberRepository.getByClubIdAndUserId(room.getClub().getId(), user.getId()))
            .willThrow(CustomException.of(NOT_FOUND_CLUB_MEMBER));

        assertNotFound(() -> chatMessagePageResolver.resolvePageForMessage(room.getId(), 50, room, user, 20));
        verify(chatMessageRepository, never()).findByIdWithChatRoom(50);
    }

    @Test
    @DisplayName("club group 멤버십 조회 예외가 404가 아니면 그대로 전파한다")
    void resolvePageForClubRoomPropagatesNonMembershipException() {
        User user = user(10, UserRole.USER);
        ChatRoom room = clubRoom(1);
        CustomException exception = CustomException.of(ApiResponseCode.NOT_FOUND_USER);
        given(clubMemberRepository.getByClubIdAndUserId(room.getClub().getId(), user.getId()))
            .willThrow(exception);

        assertThatThrownBy(() -> chatMessagePageResolver.resolvePageForMessage(room.getId(), 50, room, user, 20))
            .isSameAs(exception);
        verify(chatMessageRepository, never()).findByIdWithChatRoom(50);
    }

    @Test
    @DisplayName("club group 접근 가능 사용자의 messageId 페이지를 계산한다")
    void resolvePageForClubRoomMessage() {
        User user = user(10, UserRole.USER);
        ChatRoom room = clubRoom(1);
        ChatMessage message = message(50, room, user, TARGET_TIME);
        given(clubMemberRepository.getByClubIdAndUserId(room.getClub().getId(), user.getId()))
            .willReturn(ClubMemberFixture.createMember(room.getClub(), user));
        stubTargetMessage(message);
        given(chatMessageRepository.countNewerMessagesByChatRoomId(room.getId(), message.getId(), TARGET_TIME, null))
            .willReturn(0L);

        int page = chatMessagePageResolver.resolvePageForMessage(room.getId(), message.getId(), room, user, 20);

        assertThat(page).isEqualTo(1);
    }

    @Test
    @DisplayName("다른 방의 messageId를 404로 숨긴다")
    void resolvePageForMessageRejectsMessageFromOtherRoom() {
        User user = user(10, UserRole.USER);
        ChatRoom room = room(1, ChatType.GROUP);
        ChatMessage message = message(50, room(2, ChatType.GROUP), user, TARGET_TIME);
        stubActiveMember(room, user);
        stubTargetMessage(message);

        assertNotFound(() -> chatMessagePageResolver.resolvePageForMessage(room.getId(), 50, room, user, 20));
    }

    private void stubActiveMember(ChatRoom room, User user) {
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId()))
            .willReturn(Optional.of(member(room, user)));
    }

    private void stubDirectMember(ChatRoom room, User user, LocalDateTime visibleMessageFrom) {
        ChatRoomMember member = member(room, user);
        ReflectionTestUtils.setField(member, "visibleMessageFrom", visibleMessageFrom);
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId()))
            .willReturn(Optional.of(member));
    }

    private void stubSystemAdminRoom(ChatRoom room) {
        given(chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), 99)).willReturn(Optional.empty());
        given(chatRoomMemberRepository.findRoomMemberIdsByChatRoomIds(List.of(room.getId())))
            .willReturn(List.<Object[]>of(new Object[] {room.getId(), SYSTEM_ADMIN_ID, room.getCreatedAt()}));
    }

    private void stubSystemAdminMember(ChatRoom room, LocalDateTime visibleMessageFrom) {
        ChatRoomMember member = member(room, user(SYSTEM_ADMIN_ID, UserRole.ADMIN));
        ReflectionTestUtils.setField(member, "visibleMessageFrom", visibleMessageFrom);
        given(chatRoomMemberRepository.findByChatRoomId(room.getId())).willReturn(List.of(member));
    }

    private void stubTargetMessage(ChatMessage message) {
        given(chatMessageRepository.findByIdWithChatRoom(message.getId())).willReturn(Optional.of(message));
    }

    private User user(Integer id, UserRole role) {
        return UserFixture.createUserWithId(UniversityFixture.createWithId(1), id, "사용자" + id,
            "2024" + String.format("%04d", id), role);
    }

    private ChatRoom room(Integer id, ChatType type) {
        ChatRoom room = type == ChatType.DIRECT ? ChatRoom.directOf() : ChatRoom.groupOf();
        ReflectionTestUtils.setField(room, "id", id);
        ReflectionTestUtils.setField(room, "createdAt", BASE_TIME);
        return room;
    }

    private ChatRoom clubRoom(Integer id) {
        Club club = ClubFixture.createWithId(UniversityFixture.createWithId(1), 77, "BCSD");
        ChatRoom room = ChatRoom.clubGroupOf(club);
        ReflectionTestUtils.setField(room, "id", id);
        ReflectionTestUtils.setField(room, "createdAt", BASE_TIME);
        return room;
    }

    private ChatRoomMember member(ChatRoom room, User user) {
        ChatRoomMember member = ChatRoomMember.of(room, user, BASE_TIME);
        ReflectionTestUtils.setField(member, "createdAt", BASE_TIME);
        return member;
    }

    private ChatMessage message(Integer id, ChatRoom room, User sender, LocalDateTime createdAt) {
        ChatMessage message = ChatMessage.of(room, sender, "메시지");
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", createdAt);
        return message;
    }

    private void assertNotFound(ThrowingCallable callable) {
        assertThatThrownBy(callable)
            .isInstanceOf(CustomException.class)
            .satisfies(exception ->
                assertThat(((CustomException)exception).getErrorCode()).isEqualTo(NOT_FOUND_CHAT_ROOM));
    }
}
