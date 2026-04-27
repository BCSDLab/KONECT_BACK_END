package gg.agit.konect.domain.chat.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import gg.agit.konect.domain.chat.dto.ChatMessageMatchResult;
import gg.agit.konect.domain.chat.dto.ChatMessageMatchesResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomMatchesResponse;
import gg.agit.konect.domain.chat.dto.ChatRoomSummaryResponse;
import gg.agit.konect.domain.chat.dto.ChatSearchResponse;
import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatMessageQueryRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ChatSearchService {

    private final ChatMessageQueryRepository chatMessageQueryRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public ChatSearchResponse search(
        Integer userId,
        String keyword,
        List<ChatRoomSummaryResponse> accessibleRooms,
        Map<Integer, String> defaultRoomNameMap,
        Integer page,
        Integer limit
    ) {
        String normalizedKeyword = normalizeKeyword(keyword);
        ChatRoomMatchesResponse roomMatches = searchRoomsByName(
            accessibleRooms,
            defaultRoomNameMap,
            normalizedKeyword,
            page,
            limit
        );
        ChatMessageMatchesResponse messageMatches = searchByMessageContent(
            userId,
            accessibleRooms,
            normalizedKeyword,
            page,
            limit
        );

        return new ChatSearchResponse(roomMatches, messageMatches);
    }

    private ChatRoomMatchesResponse searchRoomsByName(
        List<ChatRoomSummaryResponse> accessibleRooms,
        Map<Integer, String> defaultRoomNameMap,
        String keyword,
        Integer page,
        Integer limit
    ) {
        List<ChatRoomSummaryResponse> matchedRooms = accessibleRooms.stream()
            .filter(room -> matchesRoomName(room, keyword, defaultRoomNameMap))
            .toList();

        return ChatRoomMatchesResponse.from(toPage(matchedRooms, page, limit));
    }

    private ChatMessageMatchesResponse searchByMessageContent(
        Integer userId,
        List<ChatRoomSummaryResponse> accessibleRooms,
        String keyword,
        Integer page,
        Integer limit
    ) {
        if (accessibleRooms.isEmpty() || keyword.isBlank()) {
            return ChatMessageMatchesResponse.from(emptyPage(page, limit));
        }

        Map<Integer, ChatRoomSummaryResponse> roomMap = accessibleRooms.stream()
            .collect(Collectors.toMap(ChatRoomSummaryResponse::roomId, room -> room));
        List<Integer> roomIds = accessibleRooms.stream()
            .map(ChatRoomSummaryResponse::roomId)
            .toList();
        List<Integer> directRoomIds = accessibleRooms.stream()
            .filter(room -> room.chatType() == ChatType.DIRECT)
            .map(ChatRoomSummaryResponse::roomId)
            .toList();
        Map<Integer, LocalDateTime> visibleMessageFromMap = getVisibleMessageFromMap(directRoomIds, userId);

        List<ChatMessageMatchResult> matchedMessages = chatMessageQueryRepository
            .searchLatestMatchingMessagesByChatRoomIds(roomIds, keyword)
            .stream()
            .filter(message -> isVisibleMessageMatch(message, roomMap, visibleMessageFromMap))
            .map(message -> ChatMessageMatchResult.from(roomMap.get(message.getChatRoom().getId()), message))
            .toList();

        return ChatMessageMatchesResponse.from(toPage(matchedMessages, page, limit));
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim();
    }

    private boolean matchesRoomName(
        ChatRoomSummaryResponse room,
        String keyword,
        Map<Integer, String> defaultRoomNameMap
    ) {
        return containsKeyword(room.roomName(), keyword)
            || containsKeyword(defaultRoomNameMap.get(room.roomId()), keyword);
    }

    private boolean containsKeyword(String text, String keyword) {
        return text != null
            && !keyword.isBlank()
            && text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private Map<Integer, LocalDateTime> getVisibleMessageFromMap(List<Integer> roomIds, Integer userId) {
        if (roomIds.isEmpty()) {
            return Map.of();
        }

        Map<Integer, LocalDateTime> visibleMessageFromMap = new HashMap<>();
        for (ChatRoomMember roomMember : chatRoomMemberRepository.findByChatRoomIdsAndUserId(roomIds, userId)) {
            visibleMessageFromMap.put(roomMember.getChatRoomId(), roomMember.getVisibleMessageFrom());
        }
        return visibleMessageFromMap;
    }

    private boolean isVisibleMessageMatch(
        ChatMessage message,
        Map<Integer, ChatRoomSummaryResponse> roomMap,
        Map<Integer, LocalDateTime> visibleMessageFromMap
    ) {
        ChatRoomSummaryResponse room = roomMap.get(message.getChatRoom().getId());
        if (room == null || room.chatType() != ChatType.DIRECT) {
            return true;
        }

        LocalDateTime visibleMessageFrom = visibleMessageFromMap.get(room.roomId());
        return visibleMessageFrom == null || message.getCreatedAt().isAfter(visibleMessageFrom);
    }

    private <T> Page<T> toPage(List<T> items, Integer page, Integer limit) {
        PageRequest pageable = PageRequest.of(page - 1, limit);
        long offset = (long)(page - 1) * limit;
        if (offset >= items.size()) {
            return new PageImpl<>(List.of(), pageable, items.size());
        }

        int fromIndex = (int)offset;
        int toIndex = Math.min(fromIndex + limit, items.size());
        return new PageImpl<>(items.subList(fromIndex, toIndex), pageable, items.size());
    }

    private Page<ChatMessageMatchResult> emptyPage(Integer page, Integer limit) {
        return new PageImpl<>(List.of(), PageRequest.of(page - 1, limit), 0);
    }
}
