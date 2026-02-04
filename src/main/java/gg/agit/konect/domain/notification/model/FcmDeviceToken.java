package gg.agit.konect.domain.notification.model;

import static jakarta.persistence.FetchType.LAZY;
import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.version.enums.PlatformType;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    name = "fcm_device_token",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_fcm_device_token_token", columnNames = "token"),
        @UniqueConstraint(name = "uq_fcm_device_token_user_device", columnNames = {"user_id, device_id"})
    }
)
@NoArgsConstructor(access = PROTECTED)
public class FcmDeviceToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", length = 255, nullable = false, unique = true)
    private String token;

    @Column(name = "device_id", length = 100, nullable = false)
    private String deviceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "platform", length = 20, nullable = false)
    private PlatformType platform;

    @Builder
    private FcmDeviceToken(Integer id, User user, String token, String deviceId, PlatformType platform) {
        this.id = id;
        this.user = user;
        this.token = token;
        this.deviceId = deviceId;
        this.platform = platform;
    }

    public static FcmDeviceToken of(User user, String token, String deviceId, PlatformType platform) {
        return FcmDeviceToken.builder()
            .user(user)
            .token(token)
            .deviceId(deviceId)
            .platform(platform)
            .build();
    }

    public void updateUser(User user) {
        this.user = user;
    }

    public void updateToken(String token) {
        this.token = token;
    }
}
