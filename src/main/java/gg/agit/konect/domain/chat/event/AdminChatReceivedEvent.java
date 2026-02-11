package gg.agit.konect.domain.chat.event;

public record AdminChatReceivedEvent(
    Integer userId,
    String senderName,
    String content
) {
    public static AdminChatReceivedEvent of(Integer userId, String senderName, String content) {
        return new AdminChatReceivedEvent(userId, senderName, content);
    }
}
