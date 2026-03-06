package gg.agit.konect.domain.user.model;

import static jakarta.persistence.FetchType.LAZY;
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
    name = "user_oauth_account",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_user_oauth_account_provider_provider_id",
            columnNames = {"provider", "provider_id"}
        ),
        @UniqueConstraint(
            name = "uq_user_oauth_account_user_provider",
            columnNames = {"user_id", "provider"}
        )
    }
)
@NoArgsConstructor(access = PROTECTED)
public class UserOAuthAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Integer id;

    @ManyToOne(fetch = LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", length = 20, nullable = false)
    private Provider provider;

    @Column(name = "provider_id", length = 255)
    private String providerId;

    @Column(name = "oauth_email", length = 100)
    private String oauthEmail;

    @Column(name = "apple_refresh_token", length = 1024)
    private String appleRefreshToken;

    @Builder
    private UserOAuthAccount(User user, Provider provider, String providerId, String oauthEmail,
        String appleRefreshToken) {
        this.user = user;
        this.provider = provider;
        this.providerId = providerId;
        this.oauthEmail = oauthEmail;
        this.appleRefreshToken = appleRefreshToken;
    }

    public static UserOAuthAccount of(User user, Provider provider, String providerId, String oauthEmail,
        String appleRefreshToken) {
        return UserOAuthAccount.builder()
            .user(user)
            .provider(provider)
            .providerId(providerId)
            .oauthEmail(oauthEmail)
            .appleRefreshToken(appleRefreshToken)
            .build();
    }

    public void updateOauthEmail(String oauthEmail) {
        this.oauthEmail = oauthEmail;
    }

    public void updateAppleRefreshToken(String appleRefreshToken) {
        this.appleRefreshToken = appleRefreshToken;
    }
}
