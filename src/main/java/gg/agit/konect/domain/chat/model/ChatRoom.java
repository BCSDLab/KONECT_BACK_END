package gg.agit.konect.domain.chat.model;

import static gg.agit.konect.global.code.ApiResponseCode.CANNOT_CREATE_CHAT_ROOM_WITH_SELF;
import static gg.agit.konect.global.code.ApiResponseCode.FORBIDDEN_CHAT_ROOM_ACCESS;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "chat_room")
@NoArgsConstructor(access = PROTECTED)
public class ChatRoom extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @Column(name = "last_message_content", length = 1000)
    private String lastMessageContent;

    @Column(name = "last_message_sent_at")
    private LocalDateTime lastMessageSentAt;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "receiver_id")
    private User receiver;

    @Builder
    private ChatRoom(Integer id, User sender, User receiver) {
        this.id = id;
        this.sender = sender;
        this.receiver = receiver;
    }

    public static ChatRoom of(User sender, User receiver) {
        validateIsNotSameParticipant(sender, receiver);
        return ChatRoom.builder()
            .sender(sender)
            .receiver(receiver)
            .build();
    }

    public static void validateIsNotSameParticipant(User sender, User receiver) {
        if (sender.getId().equals(receiver.getId())) {
            throw CustomException.of(CANNOT_CREATE_CHAT_ROOM_WITH_SELF);
        }
    }

    public void validateIsParticipant(Integer userId) {
        if (!isParticipant(userId)) {
            throw CustomException.of(FORBIDDEN_CHAT_ROOM_ACCESS);
        }
    }

    public boolean isParticipant(Integer userId) {
        return sender.getId().equals(userId) || receiver.getId().equals(userId);
    }

    public User getChatPartner(User currentUser) {
        return sender.getId().equals(currentUser.getId()) ? receiver : sender;
    }

    public User getNonAdminUser() {
        if (sender.getRole() != UserRole.ADMIN) {
            return sender;
        }
        return receiver;
    }

    public void updateLastMessage(String lastMessageContent, LocalDateTime lastMessageSentAt) {
        this.lastMessageContent = lastMessageContent;
        this.lastMessageSentAt = lastMessageSentAt;
    }
}
