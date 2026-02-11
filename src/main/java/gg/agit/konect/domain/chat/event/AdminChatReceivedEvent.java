package gg.agit.konect.domain.chat.event;

public record AdminChatReceivedEvent(
    Integer senderId,
    String senderName,
    String content
) {
    public static AdminChatReceivedEvent of(Integer senderId, String senderName, String content) {
        return new AdminChatReceivedEvent(senderId, senderName, content);
    }
}
