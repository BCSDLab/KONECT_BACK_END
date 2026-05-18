package gg.agit.konect.unit.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import gg.agit.konect.domain.chat.dto.ChatRoomSummaryResponse;
import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.service.ChatRoomSettingsService;
import gg.agit.konect.domain.chat.service.ChatRoomSummaryService;
import gg.agit.konect.support.ServiceTestSupport;

class ChatRoomSummaryServiceTest extends ServiceTestSupport {

    @Mock
    private ChatRoomSettingsService chatRoomSettingsService;

    @InjectMocks
    private ChatRoomSummaryService chatRoomSummaryService;

    @Test
    @DisplayName("summarizeChatRooms는 사용자 설정을 적용한 뒤 최신 대화 순으로 정렬한다")
    void summarizeChatRoomsAppliesSettingsAndSortsByRecentActivity() {
        // given
        Integer userId = 10;
        ChatRoomSummaryResponse olderRoom = createRoom(1, ChatType.DIRECT, "오래된 방",
            LocalDateTime.of(2026, 4, 27, 9, 0), LocalDateTime.of(2026, 4, 27, 8, 0));
        ChatRoomSummaryResponse emptyNewRoom = createRoom(2, ChatType.GROUP, "새 빈 방",
            null, LocalDateTime.of(2026, 4, 27, 11, 0));
        ChatRoomSummaryResponse newestRoom = createRoom(3, ChatType.CLUB_GROUP, "최신 방",
            LocalDateTime.of(2026, 4, 27, 12, 0), LocalDateTime.of(2026, 4, 27, 7, 0));
        List<ChatRoomSummaryResponse> combinedRooms = List.of(olderRoom, newestRoom, emptyNewRoom);

        given(chatRoomSettingsService.applyUserSettings(combinedRooms, userId))
            .willReturn(combinedRooms);

        // when
        List<ChatRoomSummaryResponse> result = chatRoomSummaryService.summarizeChatRooms(
            userId,
            List.of(olderRoom),
            List.of(newestRoom),
            List.of(emptyNewRoom)
        );

        // then
        assertThat(result).extracting(ChatRoomSummaryResponse::roomId)
            .containsExactly(3, 2, 1);
    }

    @Test
    @DisplayName("getDefaultRoomNameMap은 검색용 기본 방 이름을 보존한다")
    void getDefaultRoomNameMapKeepsOriginalRoomNames() {
        // given
        ChatRoomSummaryResponse directRoom = createRoom(1, ChatType.DIRECT, "상대방",
            LocalDateTime.of(2026, 4, 27, 9, 0), LocalDateTime.of(2026, 4, 27, 8, 0));
        ChatRoomSummaryResponse clubRoom = createRoom(2, ChatType.CLUB_GROUP, "동아리",
            LocalDateTime.of(2026, 4, 27, 10, 0), LocalDateTime.of(2026, 4, 27, 8, 0));

        // when
        Map<Integer, String> result = chatRoomSummaryService.getDefaultRoomNameMap(
            List.of(directRoom),
            List.of(clubRoom)
        );

        // then
        assertThat(result).containsEntry(1, "상대방")
            .containsEntry(2, "동아리");
    }

    private ChatRoomSummaryResponse createRoom(
        Integer roomId,
        ChatType chatType,
        String roomName,
        LocalDateTime lastSentAt,
        LocalDateTime createdAt
    ) {
        return new ChatRoomSummaryResponse(
            roomId,
            chatType,
            roomName,
            null,
            null,
            lastSentAt,
            createdAt,
            0,
            false
        );
    }
}
