package gg.agit.konect.domain.chat.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.chat.dto.ChatRoomSummaryResponse;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.notification.enums.NotificationTargetType;
import gg.agit.konect.domain.notification.model.NotificationMuteSetting;
import gg.agit.konect.domain.notification.repository.NotificationMuteSettingRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatRoomSettingsService {

    private final NotificationMuteSettingRepository notificationMuteSettingRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public List<ChatRoomSummaryResponse> applyUserSettings(
        List<ChatRoomSummaryResponse> rooms,
        Integer userId
    ) {
        List<Integer> roomIds = rooms.stream()
            .map(ChatRoomSummaryResponse::roomId)
            .toList();
        Map<Integer, Boolean> muteMap = getMuteMap(roomIds, userId);
        Map<Integer, String> customRoomNameMap = getCustomRoomNameMap(roomIds, userId);

        return rooms.stream()
            .map(room -> applyRoomSettings(room, muteMap, customRoomNameMap))
            .toList();
    }

    private ChatRoomSummaryResponse applyRoomSettings(
        ChatRoomSummaryResponse room,
        Map<Integer, Boolean> muteMap,
        Map<Integer, String> customRoomNameMap
    ) {
        return new ChatRoomSummaryResponse(
            room.roomId(),
            room.chatType(),
            customRoomNameMap.getOrDefault(room.roomId(), room.roomName()),
            room.roomImageUrl(),
            room.lastMessage(),
            room.lastSentAt(),
            room.createdAt(),
            room.unreadCount(),
            muteMap.getOrDefault(room.roomId(), false)
        );
    }

    private Map<Integer, Boolean> getMuteMap(List<Integer> roomIds, Integer userId) {
        if (roomIds.isEmpty()) {
            return Map.of();
        }

        List<NotificationMuteSetting> settings = notificationMuteSettingRepository
            .findByTargetTypeAndTargetIdsAndUserId(NotificationTargetType.CHAT_ROOM, roomIds, userId);

        Map<Integer, Boolean> muteMap = new HashMap<>();
        for (NotificationMuteSetting setting : settings) {
            Integer targetId = setting.getTargetId();
            if (targetId != null) {
                muteMap.put(targetId, setting.getIsMuted());
            }
        }

        return muteMap;
    }

    private Map<Integer, String> getCustomRoomNameMap(List<Integer> roomIds, Integer userId) {
        if (roomIds.isEmpty()) {
            return Map.of();
        }

        return chatRoomMemberRepository.findByChatRoomIdsAndUserId(roomIds, userId).stream()
            .filter(member -> StringUtils.hasText(member.getCustomRoomName()))
            .collect(Collectors.toMap(ChatRoomMember::getChatRoomId, ChatRoomMember::getCustomRoomName));
    }
}
