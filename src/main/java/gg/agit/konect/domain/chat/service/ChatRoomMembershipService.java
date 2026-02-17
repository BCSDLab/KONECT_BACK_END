package gg.agit.konect.domain.chat.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.model.ChatRoom;
import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import gg.agit.konect.domain.chat.repository.ChatRoomRepository;
import gg.agit.konect.domain.club.model.Club;
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
        addClubMember(clubMember.getClub(), clubMember.getUser(), clubMember.getCreatedAt());
    }

    @Transactional
    public void addDirectMembers(ChatRoom room, User firstUser, User secondUser, LocalDateTime joinedAt) {
        LocalDateTime baseline = joinedAt != null ? joinedAt : LocalDateTime.now();
        ensureMember(room, firstUser, baseline);
        ensureMember(room, secondUser, baseline);
    }

    @Transactional
    public void addClubMember(Club club, User user, LocalDateTime joinedAt) {
        ChatRoom room = chatRoomRepository.findByClubId(club.getId())
            .orElseGet(() -> chatRoomRepository.save(ChatRoom.groupOf(club)));

        LocalDateTime baseline = joinedAt != null ? joinedAt : LocalDateTime.now();

        ensureMember(room, user, baseline);
    }

    private void ensureMember(ChatRoom room, User user, LocalDateTime baseline) {
        chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), user.getId())
            .ifPresentOrElse(member -> {
                if (member.getLastReadAt().isBefore(baseline)) {
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
