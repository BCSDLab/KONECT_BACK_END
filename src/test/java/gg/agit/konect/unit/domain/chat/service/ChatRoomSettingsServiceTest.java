package gg.agit.konect.unit.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.chat.dto.ChatRoomSummaryResponse;
import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.service.ChatRoomSettingsService;
import gg.agit.konect.domain.notification.enums.NotificationTargetType;
import gg.agit.konect.domain.notification.model.NotificationMuteSetting;
import gg.agit.konect.domain.notification.repository.NotificationMuteSettingRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.support.ServiceTestSupport;

class ChatRoomSettingsServiceTest extends ServiceTestSupport {

    @Mock
    private NotificationMuteSettingRepository notificationMuteSettingRepository;

    @Mock
    private ChatRoomMemberRepository chatRoomMemberRepository;

    @InjectMocks
    private ChatRoomSettingsService chatRoomSettingsService;

    @Test
    @DisplayName("applyUserSettings는 커스텀 방 이름과 뮤트 설정을 목록 응답에 합성한다")
    void applyUserSettingsAppliesCustomNameAndMute() {
        // given
        Integer userId = 10;
        ChatRoomSummaryResponse room = createRoomSummary(1, "기본 이름");
        ChatRoomMember member = mock(ChatRoomMember.class);
        given(member.getChatRoomId()).willReturn(room.roomId());
        given(member.getCustomRoomName()).willReturn("내 방 이름");
        given(notificationMuteSettingRepository.findByTargetTypeAndTargetIdsAndUserId(
            NotificationTargetType.CHAT_ROOM,
            List.of(room.roomId()),
            userId
        )).willReturn(List.of(NotificationMuteSetting.of(
            NotificationTargetType.CHAT_ROOM,
            room.roomId(),
            mock(User.class),
            true
        )));
        given(chatRoomMemberRepository.findByChatRoomIdsAndUserId(List.of(room.roomId()), userId))
            .willReturn(List.of(member));

        // when
        List<ChatRoomSummaryResponse> result = chatRoomSettingsService.applyUserSettings(List.of(room), userId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).roomName()).isEqualTo("내 방 이름");
        assertThat(result.get(0).isMuted()).isTrue();
        assertThat(result.get(0).lastMessage()).isEqualTo(room.lastMessage());
    }

    @Test
    @DisplayName("applyUserSettings는 빈 목록이면 설정 조회를 생략한다")
    void applyUserSettingsSkipsLookupForEmptyRooms() {
        // when
        List<ChatRoomSummaryResponse> result = chatRoomSettingsService.applyUserSettings(List.of(), 10);

        // then
        assertThat(result).isEmpty();
        verifyNoInteractions(notificationMuteSettingRepository, chatRoomMemberRepository);
    }

    private ChatRoomSummaryResponse createRoomSummary(Integer roomId, String roomName) {
        return new ChatRoomSummaryResponse(
            roomId,
            ChatType.DIRECT,
            roomName,
            "https://example.com/image.png",
            "마지막 메시지",
            LocalDateTime.of(2026, 4, 27, 11, 0),
            LocalDateTime.of(2026, 4, 27, 10, 0),
            3,
            false
        );
    }
}
