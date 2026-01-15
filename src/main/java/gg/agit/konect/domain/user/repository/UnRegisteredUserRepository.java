package gg.agit.konect.domain.user.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.UnRegisteredUser;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface UnRegisteredUserRepository extends Repository<UnRegisteredUser, Integer> {

    Optional<UnRegisteredUser> findByEmailAndProvider(String email, Provider provider);

    Optional<UnRegisteredUser> findByProviderIdAndProvider(String providerId, Provider provider);

    boolean existsByProviderIdAndProvider(String providerId, Provider provider);

    default UnRegisteredUser getByEmailAndProvider(String email, Provider provider) {
        return findByEmailAndProvider(email, provider)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_UNREGISTERED_USER));
    }

    default UnRegisteredUser getByProviderIdAndProvider(String providerId, Provider provider) {
        return findByProviderIdAndProvider(providerId, provider)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_UNREGISTERED_USER));
    }

    void save(UnRegisteredUser user);

    void delete(UnRegisteredUser user);
}
