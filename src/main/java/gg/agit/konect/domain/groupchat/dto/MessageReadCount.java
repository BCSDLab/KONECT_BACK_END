package gg.agit.konect.domain.groupchat.dto;

public record MessageReadCount(
    Integer messageId,
    Long readCount
) {
}
