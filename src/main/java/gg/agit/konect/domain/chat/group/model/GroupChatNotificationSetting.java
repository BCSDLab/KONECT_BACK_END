package gg.agit.konect.domain.chat.group.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
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
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "group_chat_notification_setting",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"room_id", "user_id"})
    }
)
@NoArgsConstructor(access = PROTECTED)
public class GroupChatNotificationSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private GroupChatRoom room;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_muted", nullable = false)
    private Boolean isMuted;

    @Builder
    private GroupChatNotificationSetting(
        Integer id,
        GroupChatRoom room,
        User user,
        Boolean isMuted
    ) {
        this.id = id;
        this.room = room;
        this.user = user;
        this.isMuted = isMuted != null ? isMuted : false;
    }

    public static GroupChatNotificationSetting of(GroupChatRoom room, User user, Boolean isMuted) {
        return GroupChatNotificationSetting.builder()
            .room(room)
            .user(user)
            .isMuted(isMuted)
            .build();
    }

    public void toggleMute() {
        this.isMuted = !this.isMuted;
    }
}
