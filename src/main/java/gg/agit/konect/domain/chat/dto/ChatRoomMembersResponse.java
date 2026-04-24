package gg.agit.konect.domain.chat.dto;

import java.util.List;

public record ChatRoomMembersResponse(
    List<ChatRoomMemberResponse> members
) {
}
