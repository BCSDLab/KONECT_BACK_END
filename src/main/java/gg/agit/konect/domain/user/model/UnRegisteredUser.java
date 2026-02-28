package gg.agit.konect.domain.user.model;

import static jakarta.persistence.GenerationType.IDENTITY;
import static lombok.AccessLevel.PROTECTED;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.global.model.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
    name = "unregistered_user",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_unregistered_user_email_provider",
            columnNames = {"email", "provider"}
        ),
        @UniqueConstraint(
            name = "uq_unregistered_user_provider_provider_id",
            columnNames = {"provider", "provider_id"}
        )
    }
)
@NoArgsConstructor(access = PROTECTED)
public class UnRegisteredUser extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false, unique = true)
    private Integer id;

    @Column(name = "email", length = 100, nullable = false)
    private String email;

    @Column(name = "provider", length = 20)
    @Enumerated(EnumType.STRING)
    private Provider provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "name", length = 30)
    private String name;

    @Column(name = "apple_refresh_token", length = 1024)
    private String appleRefreshToken;

    @Builder
    private UnRegisteredUser(Integer id, String email, Provider provider, String providerId, String name) {
        this.id = id;
        this.email = email;
        this.provider = provider;
        this.providerId = providerId;
        this.name = name;
    }

    public void updateAppleRefreshToken(String appleRefreshToken) {
        this.appleRefreshToken = appleRefreshToken;
    }

    public void updateName(String name) {
        this.name = name;
    }
}
