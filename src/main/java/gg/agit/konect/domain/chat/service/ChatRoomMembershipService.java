package gg.agit.konect.domain.chat.service;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.club.model.ClubMember;
import gg.agit.konect.domain.user.model.User;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomMembershipService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;

    @Transactional
    public void addClubMember(ClubMember clubMember) {
        LocalDateTime baseline = Objects.requireNonNull(clubMember.getCreatedAt(), "clubMember.createdAt must not be null");
        ChatRoom room = chatRoomRepository.findByClubId(clubMember.getClub().getId())
            .orElseGet(() -> chatRoomRepository.save(ChatRoom.groupOf(clubMember.getClub())));
        ensureMember(room, clubMember.getUser(), baseline);
    }

    @Transactional
    public void addDirectMembers(ChatRoom room, User firstUser, User secondUser, LocalDateTime joinedAt) {
        LocalDateTime baseline = Objects.requireNonNull(joinedAt, "joinedAt must not be null");
        ensureMember(room, firstUser, baseline);
        ensureMember(room, secondUser, baseline);
    }

    private void ensureMember(ChatRoom room, User user, LocalDateTime baseline) {
        chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId())
            .ifPresentOrElse(member -> {
                LocalDateTime lastReadAt = member.getLastReadAt();
                if (lastReadAt == null || lastReadAt.isBefore(baseline)) {
                    member.updateLastReadAt(baseline);
                }
            }, () -> chatRoomMemberRepository.save(ChatRoomMember.of(room, user, baseline)));
    }

    @Transactional
    public void removeClubMember(Integer clubId, Integer userId) {
        chatRoomRepository.findByClubId(clubId)
            .ifPresent(room -> chatRoomMemberRepository.deleteByChatRoomIdAndUserId(room.getId(), userId));
    }
}
