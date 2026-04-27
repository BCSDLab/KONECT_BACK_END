package gg.agit.konect.domain.chat.service;

import static gg.agit.konect.domain.chat.service.ChatRoomMembershipService.SYSTEM_ADMIN_ID;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.chat.model.ChatRoomMember;
import gg.agit.konect.domain.chat.repository.ChatRoomMemberRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatRoomSystemAdminService {

    private final ChatRoomMemberRepository chatRoomMemberRepository;

    public boolean isSystemAdminRoom(Integer roomId) {
        List<Object[]> memberIds = chatRoomMemberRepository.findRoomMemberIdsByChatRoomIds(List.of(roomId));
        return memberIds.stream()
            .map(row -> (Integer)row[1])
            .anyMatch(userId -> userId.equals(SYSTEM_ADMIN_ID));
    }

    public ChatRoomMember findSystemAdminMember(List<ChatRoomMember> members) {
        return members.stream()
            .filter(member -> member.getUserId().equals(SYSTEM_ADMIN_ID))
            .findFirst()
            .orElse(null);
    }
}
