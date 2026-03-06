package gg.agit.konect.domain.user.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.model.UserOAuthAccount;

public interface UserOAuthAccountRepository extends JpaRepository<UserOAuthAccount, Integer> {

    @Query("""
        SELECT uoa.user
        FROM UserOAuthAccount uoa
        WHERE uoa.provider = :provider
        AND uoa.providerId = :providerId
        AND uoa.user.deletedAt IS NULL
        """)
    Optional<User> findUserByProviderAndProviderId(
        @Param("provider") Provider provider,
        @Param("providerId") String providerId
    );

    @Query("""
        SELECT uoa
        FROM UserOAuthAccount uoa
        JOIN FETCH uoa.user user
        WHERE uoa.provider = :provider
        AND uoa.providerId = :providerId
        """)
    Optional<UserOAuthAccount> findAccountByProviderAndProviderId(
        @Param("provider") Provider provider,
        @Param("providerId") String providerId
    );

    @Query("""
        SELECT uoa
        FROM UserOAuthAccount uoa
        WHERE uoa.user.id = :userId
        ORDER BY uoa.id ASC
        """)
    List<UserOAuthAccount> findAllByUserId(@Param("userId") Integer userId);

    @Query("""
        SELECT uoa.user
        FROM UserOAuthAccount uoa
        WHERE uoa.oauthEmail = :oauthEmail
        AND uoa.provider = :provider
        AND uoa.user.deletedAt IS NULL
        """)
    Optional<User> findUserByOauthEmailAndProvider(
        @Param("oauthEmail") String oauthEmail,
        @Param("provider") Provider provider
    );

    @Query("""
        SELECT uoa
        FROM UserOAuthAccount uoa
        WHERE uoa.provider = :provider
        AND uoa.providerId = :providerId
        """)
    Optional<UserOAuthAccount> findByProviderAndProviderId(
        @Param("provider") Provider provider,
        @Param("providerId") String providerId
    );

    @Query("""
        SELECT uoa
        FROM UserOAuthAccount uoa
        WHERE uoa.user.id = :userId
        AND uoa.provider = :provider
        """)
    Optional<UserOAuthAccount> findByUserIdAndProvider(
        @Param("userId") Integer userId,
        @Param("provider") Provider provider
    );

    void delete(UserOAuthAccount userOAuthAccount);

    @Modifying
    @Query("""
        DELETE
        FROM UserOAuthAccount uoa
        WHERE uoa.user.deletedAt IS NOT NULL
        AND uoa.user.deletedAt <= :expiredAt
        """)
    int deleteAllByWithdrawnUsersBefore(@Param("expiredAt") LocalDateTime expiredAt);

    @Query("""
        SELECT (COUNT(uoa) > 0)
        FROM UserOAuthAccount uoa
        WHERE uoa.provider = :provider
        AND uoa.providerId = :providerId
        AND uoa.user.deletedAt IS NULL
        """)
    boolean existsByProviderAndProviderId(
        @Param("provider") Provider provider,
        @Param("providerId") String providerId
    );

}
