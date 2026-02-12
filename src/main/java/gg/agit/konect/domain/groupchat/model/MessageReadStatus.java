package gg.agit.konect.domain.groupchat.model;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "message_read_status")
@IdClass(MessageReadStatusId.class)
@NoArgsConstructor(access = PROTECTED)
public class MessageReadStatus extends BaseEntity {

    @Id
    @Column(name = "message_id", nullable = false, updatable = false)
    private Integer messageId;

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private Integer userId;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "message_id", nullable = false, insertable = false, updatable = false)
    private GroupChatMessage message;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
    private User user;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    @Builder
    private MessageReadStatus(
        Integer messageId,
        Integer userId,
        GroupChatMessage message,
        User user,
        LocalDateTime readAt
    ) {
        this.messageId = messageId;
        this.userId = userId;
        this.message = message;
        this.user = user;
        this.readAt = readAt;
    }

    public static MessageReadStatus of(GroupChatMessage message, User user) {
        return MessageReadStatus.builder()
            .messageId(message.getId())
            .userId(user.getId())
            .message(message)
            .user(user)
            .readAt(LocalDateTime.now())
            .build();
    }
}
