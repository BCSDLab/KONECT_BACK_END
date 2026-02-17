package gg.agit.konect.domain.chat.group.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonFormat;

import gg.agit.konect.domain.chat.direct.model.ChatMessage;
import gg.agit.konect.domain.chat.direct.model.ChatRoom;
import io.swagger.v3.oas.annotations.media.Schema;

public record GroupChatRoomsResponse(
    @Schema(description = "단체 채팅방 리스트", requiredMode = REQUIRED)
    List<InnerGroupChatRoomResponse> groupChatRooms
) {
    public record InnerGroupChatRoomResponse(
        @Schema(description = "단체 채팅방 ID", example = "1", requiredMode = REQUIRED)
        Integer groupChatRoomId,

        @Schema(description = "동아리 이름", example = "BCSD Lab", requiredMode = REQUIRED)
        String clubName,

        @Schema(description = "동아리 이미지 URL", example = "https://bcsdlab.com/static/img/logo.d89d9cc.png", requiredMode = REQUIRED)
        String clubImageUrl,

        @Schema(description = "마지막 메시지", example = "안녕하세요!", requiredMode = NOT_REQUIRED)
        String lastMessage,

        @Schema(description = "마지막 메시지 전송 시간", example = "2025.12.19 23:21", requiredMode = NOT_REQUIRED)
        @JsonFormat(pattern = "yyyy.MM.dd HH:mm")
        LocalDateTime lastSentAt,

        @Schema(description = "읽지 않은 메시지 개수", example = "12", requiredMode = REQUIRED)
        Integer unreadMessageCount
    ) {
        public static InnerGroupChatRoomResponse from(
            ChatRoom room,
            ChatMessage lastMessage,
            Map<Integer, Integer> unreadCountMap
        ) {
            return new InnerGroupChatRoomResponse(
                room.getId(),
                room.getClub().getName(),
                room.getClub().getImageUrl(),
                lastMessage != null ? lastMessage.getContent() : null,
                lastMessage != null ? lastMessage.getCreatedAt() : null,
                unreadCountMap.getOrDefault(room.getId(), 0)
            );
        }
    }

    public static GroupChatRoomsResponse from(
        List<ChatRoom> rooms,
        Map<Integer, ChatMessage> lastMessageMap,
        Map<Integer, Integer> unreadCountMap
    ) {
        return new GroupChatRoomsResponse(
            rooms.stream()
                .map(room -> InnerGroupChatRoomResponse.from(
                    room,
                    lastMessageMap.get(room.getId()),
                    unreadCountMap
                ))
                .toList()
        );
    }
}
