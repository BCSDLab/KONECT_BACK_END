package gg.agit.konect.domain.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.model.UserOAuthAccount;

public interface UserOAuthAccountRepository extends Repository<UserOAuthAccount, Integer> {

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
        WHERE uoa.user.id = :userId
        """)
    List<UserOAuthAccount> findAllByUserId(@Param("userId") Integer userId);

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

    UserOAuthAccount save(UserOAuthAccount userOAuthAccount);
}
