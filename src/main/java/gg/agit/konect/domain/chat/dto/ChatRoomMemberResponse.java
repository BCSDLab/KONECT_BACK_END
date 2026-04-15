package gg.agit.konect.domain.chat.dto;

import java.time.LocalDateTime;

public record ChatRoomMemberResponse(
    Integer userId,
    String name,
    String profileImageUrl,
    boolean isOwner,
    LocalDateTime joinedAt
) {
}
