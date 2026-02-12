package gg.agit.konect.domain.groupchat.model;

import static jakarta.persistence.FetchType.LAZY;
import static lombok.AccessLevel.PROTECTED;

import java.time.LocalDateTime;

import gg.agit.konect.domain.user.model.User;
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
@Table(name = "group_chat_read_status")
@IdClass(GroupChatReadStatusId.class)
@NoArgsConstructor(access = PROTECTED)
public class GroupChatReadStatus {

    @Id
    @Column(name = "room_id", nullable = false, updatable = false)
    private Integer roomId;

    @Id
    @Column(name = "user_id", nullable = false, updatable = false)
    private Integer userId;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "room_id", nullable = false, insertable = false, updatable = false)
    private GroupChatRoom room;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false, insertable = false, updatable = false)
    private User user;

    @Column(name = "last_read_at", nullable = false)
    private LocalDateTime lastReadAt;

    @Builder
    private GroupChatReadStatus(
        Integer roomId,
        Integer userId,
        GroupChatRoom room,
        User user,
        LocalDateTime lastReadAt
    ) {
        this.roomId = roomId;
        this.userId = userId;
        this.room = room;
        this.user = user;
        this.lastReadAt = lastReadAt;
    }

    public static GroupChatReadStatus of(GroupChatRoom room, User user, LocalDateTime lastReadAt) {
        return GroupChatReadStatus.builder()
            .roomId(room.getId())
            .userId(user.getId())
            .room(room)
            .user(user)
            .lastReadAt(lastReadAt)
            .build();
    }

    public void updateLastReadAt(LocalDateTime lastReadAt) {
        if (this.lastReadAt.isBefore(lastReadAt)) {
            this.lastReadAt = lastReadAt;
        }
    }
}
