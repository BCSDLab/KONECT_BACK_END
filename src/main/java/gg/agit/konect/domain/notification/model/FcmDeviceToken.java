package gg.agit.konect.domain.notification.model;

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
    name = "fcm_device_token",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_fcm_device_token_token", columnNames = "token")
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

    @Builder
    private FcmDeviceToken(Integer id, User user, String token) {
        this.id = id;
        this.user = user;
        this.token = token;
    }

    public static FcmDeviceToken of(User user, String token) {
        return FcmDeviceToken.builder()
            .user(user)
            .token(token)
            .build();
    }

    public void updateUser(User user) {
        this.user = user;
    }
}
