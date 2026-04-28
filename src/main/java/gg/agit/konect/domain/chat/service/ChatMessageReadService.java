package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.domain.chat.service.ChatRoomMembershipService.SYSTEM_ADMIN_ID;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.dto.ChatMessageDetailResponse;
import gg.agit.konect.domain.chat.dto.ChatMessagePageResponse;
import gg.agit.konect.domain.chat.model.ChatMessage;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatMessageRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.user.model.User;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatMessageReadService {

    private final ChatMessageRepository chatMessageRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ChatRoomSystemAdminService chatRoomSystemAdminService;
    private final ChatDirectRoomAccessService chatDirectRoomAccessService;

    @Transactional
    public ChatMessagePageResponse getDirectChatRoomMessages(
        User user,
        ChatRoom chatRoom,
        Integer page,
        Integer limit,
        LocalDateTime readAt
    ) {
        Integer roomId = chatRoom.getId();
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
        LocalDateTime visibleMessageFrom =
            chatDirectRoomAccessService.prepareAccessAndGetVisibleMessageFrom(chatRoom, user);

        List<LocalDateTime> sortedReadBaselines = toSortedReadBaselines(members);

        return buildDirectChatRoomMessages(user, roomId, page, limit, readAt,
            visibleMessageFrom, sortedReadBaselines, null);
    }

    public ChatMessagePageResponse getAdminSystemDirectChatRoomMessages(
        User user,
        ChatRoom chatRoom,
        Integer page,
        Integer limit,
        LocalDateTime readAt
    ) {
        Integer roomId = chatRoom.getId();
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
        LocalDateTime visibleMessageFrom = resolveAdminSystemRoomVisibleMessageFrom(members);

        List<LocalDateTime> sortedReadBaselines = toAdminChatReadBaselines(members);
        Integer maskedAdminId = getMaskedAdminId(user, members);

        return buildDirectChatRoomMessages(user, roomId, page, limit, readAt,
            visibleMessageFrom, sortedReadBaselines, maskedAdminId);
    }

    public ChatMessagePageResponse getClubMessagesByRoom(
        ChatRoom room,
        Integer userId,
        Integer page,
        Integer limit
    ) {
        Integer roomId = room.getId();
        PageRequest pageable = PageRequest.of(page - 1, limit);
        long totalCount = chatMessageRepository.countByChatRoomId(roomId, null);
        Page<ChatMessage> messagePage = chatMessageRepository.findByChatRoomId(roomId, null, pageable);
        List<ChatMessage> messages = messagePage.getContent();
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
        List<LocalDateTime> sortedReadBaselines = toSortedReadBaselines(members);

        List<ChatMessageDetailResponse> responseMessages = messages.stream()
            .map(message -> {
                int unreadCount = countUnreadSince(message.getCreatedAt(), sortedReadBaselines);
                return new ChatMessageDetailResponse(
                    message.getId(),
                    message.getSender().getId(),
                    message.getSender().getName(),
                    message.getContent(),
                    message.getCreatedAt(),
                    null,
                    unreadCount,
                    message.isSentBy(userId)
                );
            })
            .toList();

        int totalPage = limit > 0 ? (int)Math.ceil((double)totalCount / (double)limit) : 0;
        return new ChatMessagePageResponse(
            totalCount,
            responseMessages.size(),
            totalPage,
            page,
            room.getClub().getId(),
            responseMessages
        );
    }

    public ChatMessagePageResponse getGroupMessagesByRoom(
        Integer roomId,
        Integer userId,
        Integer page,
        Integer limit
    ) {
        PageRequest pageable = PageRequest.of(page - 1, limit);
        long totalCount = chatMessageRepository.countByChatRoomId(roomId, null);
        Page<ChatMessage> messagePage = chatMessageRepository.findByChatRoomId(roomId, null, pageable);
        List<ChatMessage> messages = messagePage.getContent();
        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
        List<LocalDateTime> sortedReadBaselines = toSortedReadBaselines(members);

        List<ChatMessageDetailResponse> responseMessages = messages.stream()
            .map(message -> {
                int unreadCount = countUnreadSince(message.getCreatedAt(), sortedReadBaselines);
                return new ChatMessageDetailResponse(
                    message.getId(),
                    message.getSender().getId(),
                    message.getSender().getName(),
                    message.getContent(),
                    message.getCreatedAt(),
                    null,
                    unreadCount,
                    message.isSentBy(userId)
                );
            })
            .toList();

        int totalPage = limit > 0 ? (int)Math.ceil((double)totalCount / (double)limit) : 0;
        return new ChatMessagePageResponse(
            totalCount,
            responseMessages.size(),
            totalPage,
            page,
            null,
            responseMessages
        );
    }

    private ChatMessagePageResponse buildDirectChatRoomMessages(
        User user,
        Integer roomId,
        Integer page,
        Integer limit,
        LocalDateTime readAt,
        LocalDateTime visibleMessageFrom,
        List<LocalDateTime> sortedReadBaselines,
        Integer maskedAdminId
    ) {
        PageRequest pageable = PageRequest.of(page - 1, limit);
        Page<ChatMessage> messages = chatMessageRepository.findByChatRoomId(roomId, visibleMessageFrom, pageable);

        List<ChatMessageDetailResponse> responseMessages = messages.getContent().stream()
            .map(message -> {
                Integer senderId = maskedAdminId != null
                    ? resolveDirectSenderId(message, maskedAdminId)
                    : message.getSender().getId();
                boolean isMine = maskedAdminId != null
                    ? shouldDisplayAsOwnMessage(user, message, true)
                    : message.isSentBy(user.getId());
                boolean isRead = isMine || !message.getCreatedAt().isAfter(readAt);
                int unreadCount = countUnreadSince(message.getCreatedAt(), sortedReadBaselines);
                return new ChatMessageDetailResponse(
                    message.getId(),
                    senderId,
                    null,
                    message.getContent(),
                    message.getCreatedAt(),
                    isRead,
                    unreadCount,
                    isMine
                );
            })
            .toList();

        return new ChatMessagePageResponse(
            messages.getTotalElements(),
            messages.getNumberOfElements(),
            messages.getTotalPages(),
            messages.getNumber() + 1,
            null,
            responseMessages
        );
    }

    private List<LocalDateTime> toSortedReadBaselines(List<ChatRoomMember> members) {
        return members.stream()
            .map(ChatRoomMember::getLastReadAt)
            .sorted()
            .toList();
    }

    private List<LocalDateTime> toAdminChatReadBaselines(List<ChatRoomMember> members) {
        LocalDateTime adminLastReadAt = null;
        LocalDateTime userLastReadAt = null;

        for (ChatRoomMember member : members) {
            if (member.getUser().isAdmin()) {
                if (adminLastReadAt == null || member.getLastReadAt().isAfter(adminLastReadAt)) {
                    adminLastReadAt = member.getLastReadAt();
                }
            } else {
                userLastReadAt = member.getLastReadAt();
            }
        }

        List<LocalDateTime> baselines = new ArrayList<>();
        if (adminLastReadAt != null) {
            baselines.add(adminLastReadAt);
        }
        if (userLastReadAt != null) {
            baselines.add(userLastReadAt);
        }
        baselines.sort(Comparator.naturalOrder());
        return baselines;
    }

    private int countUnreadSince(LocalDateTime messageCreatedAt, List<LocalDateTime> sortedReadBaselines) {
        int left = 0;
        int right = sortedReadBaselines.size();

        while (left < right) {
            int mid = (left + right) >>> 1;
            LocalDateTime baseline = sortedReadBaselines.get(mid);

            if (baseline.isBefore(messageCreatedAt)) {
                left = mid + 1;
            } else {
                right = mid;
            }
        }

        return left;
    }

    private LocalDateTime resolveAdminSystemRoomVisibleMessageFrom(List<ChatRoomMember> members) {
        ChatRoomMember systemAdminMember = chatRoomSystemAdminService.findSystemAdminMember(members);
        return systemAdminMember != null ? systemAdminMember.getVisibleMessageFrom() : null;
    }

    private boolean shouldDisplayAsOwnMessage(
        User currentUser,
        ChatMessage message,
        boolean isAdminViewingSystemRoom
    ) {
        if (isAdminViewingSystemRoom) {
            return message.getSender().isAdmin();
        }
        return message.isSentBy(currentUser.getId());
    }

    private Integer resolveDirectSenderId(ChatMessage message, Integer maskedAdminId) {
        if (maskedAdminId != null && message.getSender().isAdmin()) {
            return maskedAdminId;
        }
        return message.getSender().getId();
    }

    private Integer getMaskedAdminId(User user, List<ChatRoomMember> members) {
        if (user.isAdmin()) {
            return null;
        }

        boolean hasSystemAdmin = members.stream()
            .map(ChatRoomMember::getUserId)
            .anyMatch(memberUserId -> memberUserId.equals(SYSTEM_ADMIN_ID));

        return hasSystemAdmin ? SYSTEM_ADMIN_ID : null;
    }
}
