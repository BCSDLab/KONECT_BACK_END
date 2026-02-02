package gg.agit.konect.domain.chat.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static java.lang.Boolean.FALSE;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chat_message")
@NoArgsConstructor(access = PROTECTED)
public class ChatMessage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @NotNull
    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = FALSE;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "chat_room_id", nullable = false)
    private ChatRoom chatRoom;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "receiver_id", nullable = false)
    private User receiver;

    @Builder
    private ChatMessage(
        Integer id,
        String content,
        Boolean isRead,
        ChatRoom chatRoom,
        User sender,
        User receiver
    ) {
        this.id = id;
        this.content = content;
        this.isRead = isRead;
        this.chatRoom = chatRoom;
        this.sender = sender;
        this.receiver = receiver;
    }

    public static ChatMessage of(ChatRoom chatRoom, User sender, User receiver, String content) {
        return ChatMessage.builder()
            .content(content)
            .chatRoom(chatRoom)
            .sender(sender)
            .receiver(receiver)
            .isRead(FALSE)
            .build();
    }

    public boolean isSentBy(Integer userId) {
        return sender.getId().equals(userId);
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
