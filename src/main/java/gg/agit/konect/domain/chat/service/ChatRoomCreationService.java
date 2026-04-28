package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.domain.chat.service.ChatRoomMembershipService.SYSTEM_ADMIN_ID;
import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_CREATE_CHAT_ROOM_WITH_SELF;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_USER;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.dto.ChatRoomCreateRequest;
import gg.agit.konect.domain.chat.dto.ChatRoomResponse;
import gg.agit.konect.domain.chat.enums.ChatType;
import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatRoomCreationService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final ChatRoomSystemAdminService chatRoomSystemAdminService;

    public ChatRoomResponse createOrGetChatRoom(Integer currentUserId, ChatRoomCreateRequest request) {
        User currentUser = userRepository.getById(currentUserId);
        User targetUser = userRepository.getById(request.userId());

        if (currentUser.getId().equals(targetUser.getId())) {
            throw CustomException.of(CANNOT_CREATE_CHAT_ROOM_WITH_SELF);
        }

        if (currentUser.isAdmin() && !targetUser.isAdmin()) {
            return getOrCreateSystemAdminChatRoomForUser(targetUser, currentUser);
        }

        ChatRoom chatRoom = chatRoomRepository.findByTwoUsers(
                currentUser.getId(),
                targetUser.getId(),
                ChatType.DIRECT
            )
            .orElseGet(() -> chatRoomRepository.save(ChatRoom.directOf()));

        LocalDateTime joinedAt = Objects.requireNonNull(chatRoom.getCreatedAt(), "chatRoom.createdAt must not be null");
        ensureDirectRoomRequester(chatRoom, currentUser, joinedAt);
        ensureRoomMember(chatRoom, targetUser, joinedAt);

        return ChatRoomResponse.from(chatRoom);
    }

    public ChatRoomResponse createOrGetAdminChatRoom(Integer currentUserId) {
        User adminUser = userRepository.findFirstByRoleAndDeletedAtIsNullOrderByIdAsc(UserRole.ADMIN)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_USER));

        return createOrGetChatRoom(currentUserId, new ChatRoomCreateRequest(adminUser.getId()));
    }

    public ChatRoomResponse createGroupChatRoom(Integer currentUserId, ChatRoomCreateRequest.Group request) {
        User creator = userRepository.getById(currentUserId);

        List<Integer> distinctUserIds = request.userIds().stream()
            .distinct()
            .filter(id -> !id.equals(currentUserId))
            .toList();

        if (distinctUserIds.isEmpty()) {
            throw CustomException.of(CANNOT_CREATE_CHAT_ROOM_WITH_SELF);
        }

        List<User> invitees = userRepository.findAllByIdIn(distinctUserIds);
        if (invitees.size() != distinctUserIds.size()) {
            throw CustomException.of(NOT_FOUND_USER);
        }

        ChatRoom chatRoom = chatRoomRepository.save(ChatRoom.groupOf());
        LocalDateTime joinedAt = Objects.requireNonNull(
            chatRoom.getCreatedAt(), "chatRoom.createdAt must not be null"
        );

        List<ChatRoomMember> members = new ArrayList<>();
        members.add(ChatRoomMember.ofOwner(chatRoom, creator, joinedAt));
        invitees.forEach(user -> members.add(ChatRoomMember.of(chatRoom, user, joinedAt)));
        chatRoomMemberRepository.saveAll(members);

        return ChatRoomResponse.from(chatRoom);
    }

    private ChatRoomResponse getOrCreateSystemAdminChatRoomForUser(User targetUser, User adminUser) {
        ChatRoom chatRoom = chatRoomRepository.findByTwoUsers(SYSTEM_ADMIN_ID, targetUser.getId(), ChatType.DIRECT)
            .orElseGet(() -> {
                ChatRoom newRoom = chatRoomRepository.save(ChatRoom.directOf());
                User systemAdmin = userRepository.getById(SYSTEM_ADMIN_ID);
                LocalDateTime joinedAt = Objects.requireNonNull(
                    newRoom.getCreatedAt(), "chatRoom.createdAt must not be null"
                );
                ensureRoomMember(newRoom, systemAdmin, joinedAt);
                ensureRoomMember(newRoom, targetUser, joinedAt);
                return newRoom;
            });

        LocalDateTime joinedAt = Objects.requireNonNull(
            chatRoom.getCreatedAt(), "chatRoom.createdAt must not be null"
        );
        ensureDirectRoomRequester(chatRoom, adminUser, joinedAt);

        return ChatRoomResponse.from(chatRoom);
    }

    private void ensureRoomMember(ChatRoom room, User user, LocalDateTime joinedAt) {
        chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId())
            .ifPresentOrElse(member -> {
                LocalDateTime lastReadAt = member.getLastReadAt();
                if (lastReadAt == null || lastReadAt.isBefore(joinedAt)) {
                    member.updateLastReadAt(joinedAt);
                }
            }, () -> chatRoomMemberRepository.save(ChatRoomMember.of(room, user, joinedAt)));
    }

    private void ensureDirectRoomRequester(ChatRoom room, User user, LocalDateTime joinedAt) {
        if (shouldSkipSystemAdminMembership(room, user)) {
            return;
        }

        chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId())
            .ifPresentOrElse(member -> {
                if (member.hasLeft()) {
                    member.reopenDirectRoom(LocalDateTime.now());
                    return;
                }

                LocalDateTime lastReadAt = member.getLastReadAt();
                if (lastReadAt == null || lastReadAt.isBefore(joinedAt)) {
                    member.updateLastReadAt(joinedAt);
                }
            }, () -> chatRoomMemberRepository.save(ChatRoomMember.of(room, user, joinedAt)));
    }

    private boolean shouldSkipSystemAdminMembership(ChatRoom room, User user) {
        // 문의방은 SYSTEM_ADMIN + 일반 사용자 2인 구조를 전제로 재사용(findByTwoUsers)되므로,
        // 생성/재오픈 경로에서도 일반 ADMIN을 멤버로 추가하면 안 된다.
        return user.isAdmin() && chatRoomSystemAdminService.isSystemAdminRoom(room.getId());
    }
}
