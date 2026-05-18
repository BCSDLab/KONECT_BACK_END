package gg.agit.konect.domain.chat.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import gg.agit.konect.domain.chat.dto.ChatRoomSummaryResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatRoomSummaryService {

    private final ChatRoomSettingsService chatRoomSettingsService;

    public List<ChatRoomSummaryResponse> summarizeChatRooms(
        Integer userId,
        List<ChatRoomSummaryResponse> directRooms,
        List<ChatRoomSummaryResponse> clubRooms,
        List<ChatRoomSummaryResponse> groupRooms
    ) {
        List<ChatRoomSummaryResponse> rooms = new ArrayList<>();
        rooms.addAll(directRooms);
        rooms.addAll(clubRooms);
        rooms.addAll(groupRooms);

        rooms = new ArrayList<>(chatRoomSettingsService.applyUserSettings(rooms, userId));
        rooms.sort(Comparator
            .comparing(
                (ChatRoomSummaryResponse room) ->
                    room.lastSentAt() != null ? room.lastSentAt() : room.createdAt(),
                Comparator.reverseOrder()
            ));

        return rooms;
    }

    public List<ChatRoomSummaryResponse> summarizeSearchableRooms(
        Integer userId,
        List<ChatRoomSummaryResponse> directRooms,
        List<ChatRoomSummaryResponse> clubRooms
    ) {
        List<ChatRoomSummaryResponse> rooms = new ArrayList<>();
        rooms.addAll(directRooms);
        rooms.addAll(clubRooms);

        rooms = new ArrayList<>(chatRoomSettingsService.applyUserSettings(rooms, userId));
        rooms.sort(
            Comparator.comparing(ChatRoomSummaryResponse::lastSentAt,
                    Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ChatRoomSummaryResponse::roomId)
        );

        return rooms;
    }

    public Map<Integer, String> getDefaultRoomNameMap(
        List<ChatRoomSummaryResponse> directRooms,
        List<ChatRoomSummaryResponse> clubRooms
    ) {
        Map<Integer, String> defaultRoomNameMap = new HashMap<>();
        directRooms.forEach(room -> defaultRoomNameMap.put(room.roomId(), room.roomName()));
        clubRooms.forEach(room -> defaultRoomNameMap.put(room.roomId(), room.roomName()));
        return defaultRoomNameMap;
    }
}
