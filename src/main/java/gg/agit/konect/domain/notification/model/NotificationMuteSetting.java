package gg.agit.konect.domain.notification.model;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.notification.enums.NotificationTargetType;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "notification_mute_setting",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"target_type", "target_id", "user_id"})
    }
)
@NoArgsConstructor(access = PROTECTED)
public class NotificationMuteSetting extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @Enumerated(STRING)
    @Column(name = "target_type", nullable = false, length = 50)
    private NotificationTargetType targetType;

    @Column(name = "target_id")
    private Integer targetId;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_muted", nullable = false)
    private Boolean isMuted;

    private NotificationMuteSetting(
        Integer id,
        NotificationTargetType targetType,
        Integer targetId,
        User user,
        Boolean isMuted
    ) {
        this.id = id;
        this.targetType = targetType;
        this.targetId = targetId;
        this.user = user;
        this.isMuted = isMuted != null ? isMuted : false;
    }

    public static NotificationMuteSetting of(
        NotificationTargetType targetType,
        Integer targetId,
        User user,
        Boolean isMuted
    ) {
        return new NotificationMuteSetting(null, targetType, targetId, user, isMuted);
    }

    public void toggleMute() {
        this.isMuted = !this.isMuted;
    }
}
