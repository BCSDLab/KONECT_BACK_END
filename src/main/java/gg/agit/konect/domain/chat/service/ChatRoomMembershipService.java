package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS;
import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_CHAT_ROOM;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.club.repository.ClubMemberRepository;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomMembershipService {

    private static final int SYSTEM_ADMIN_ID = 1;

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final ClubMemberRepository clubMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public void addClubMember(ClubMember clubMember) {
        LocalDateTime baseline = Objects.requireNonNull(clubMember.getCreatedAt());
        ChatRoom room = findOrCreateClubRoom(clubMember.getClub());
        ensureMember(room, clubMember.getUser(), baseline);
    }

    @Transactional
    public void addDirectMembers(ChatRoom room, User firstUser, User secondUser, LocalDateTime joinedAt) {
        LocalDateTime baseline = Objects.requireNonNull(joinedAt);
        ensureMember(room, firstUser, baseline);
        ensureMember(room, secondUser, baseline);
    }

    @Transactional
    public void removeClubMember(Integer clubId, Integer userId) {
        chatRoomRepository.findByClubId(clubId)
            .ifPresent(room -> chatRoomMemberRepository.deleteByChatRoomIdAndUserId(room.getId(), userId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateLastReadAt(Integer roomId, Integer userId, LocalDateTime readAt) {
        chatRoomMemberRepository.updateLastReadAtIfOlder(roomId, userId, readAt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateDirectRoomLastReadAt(Integer roomId, Integer userId, LocalDateTime readAt) {
        User user = userRepository.getById(userId);
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));

        ensureDirectRoomMemberExists(room, user, readAt);

        if (user.getRole() == UserRole.ADMIN) {
            List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
            boolean isSystemAdmin = members.stream()
                .anyMatch(member -> Objects.equals(member.getUserId(), SYSTEM_ADMIN_ID));

            if (isSystemAdmin) {
                for (ChatRoomMember member : members) {
                    if (member.getUser().getRole() == UserRole.ADMIN) {
                        chatRoomMemberRepository.updateLastReadAtIfOlder(roomId, member.getUserId(), readAt);
                    }
                }
                return;
            }
        }

        chatRoomMemberRepository.updateLastReadAtIfOlder(roomId, userId, readAt);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureClubRoomMember(Integer roomId, Integer userId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));
        if (!room.isGroupRoom() || room.getClub() == null) {
            throw CustomException.of(NOT_FOUND_CHAT_ROOM);
        }
        ClubMember member = clubMemberRepository.getByClubIdAndUserId(room.getClub().getId(), userId);
        ensureMember(room, member.getUser(), member.getCreatedAt());
    }

    private ChatRoom findOrCreateClubRoom(Club club) {
        return chatRoomRepository.findByClubId(club.getId())
            .orElseGet(() -> {
                try {
                    return chatRoomRepository.save(ChatRoom.clubGroupOf(club));
                } catch (DataIntegrityViolationException e) {
                    if (!isDuplicateKeyException(e)) {
                        throw e;
                    }
                    log.debug("클럽 채팅방 동시 생성 감지, 재조회: clubId={}", club.getId());
                    return chatRoomRepository.findByClubId(club.getId())
                        .orElseThrow(() -> CustomException.of(NOT_FOUND_CHAT_ROOM));
                }
            });
    }

    private void ensureMember(ChatRoom room, User user, LocalDateTime baseline) {
        chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId())
            .ifPresentOrElse(member -> {
                LocalDateTime lastReadAt = member.getLastReadAt();
                if (lastReadAt == null || lastReadAt.isBefore(baseline)) {
                    member.updateLastReadAt(baseline);
                }
            }, () -> saveRoomMemberIgnoringDuplicate(room, user, baseline));
    }

    private void saveRoomMemberIgnoringDuplicate(ChatRoom room, User user, LocalDateTime baseline) {
        try {
            chatRoomMemberRepository.save(ChatRoomMember.of(room, user, baseline));
        } catch (DataIntegrityViolationException e) {
            if (!isDuplicateKeyException(e)) {
                throw e;
            }
            log.debug("채팅방 멤버 동시 생성 감지, 무시: roomId={}, userId={}", room.getId(), user.getId());
        }
    }

    private void ensureDirectRoomMemberExists(ChatRoom room, User user, LocalDateTime readAt) {
        boolean exists = chatRoomMemberRepository.existsByChatRoomIdAndUserId(room.getId(), user.getId());
        if (exists) {
            return;
        }

        if (user.getRole() == UserRole.ADMIN && isSystemAdminRoom(room.getId())) {
            saveRoomMemberIgnoringDuplicate(room, user, readAt);
            return;
        }

        throw CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS);
    }

    private boolean isSystemAdminRoom(Integer roomId) {
        List<Object[]> memberIds = chatRoomMemberRepository.findRoomMemberIdsByChatRoomIds(List.of(roomId));
        return memberIds.stream()
            .map(row -> (Integer)row[1])
            .anyMatch(userId -> userId.equals(SYSTEM_ADMIN_ID));
    }

    private boolean isDuplicateKeyException(DataIntegrityViolationException e) {
        if (e instanceof DuplicateKeyException) {
            return true;
        }
        Throwable rootCause = e.getRootCause();
        if (rootCause == null) {
            return false;
        }
        String message = rootCause.getMessage();
        return message != null && (message.contains("Duplicate") || message.contains("duplicate key"));
    }
}
