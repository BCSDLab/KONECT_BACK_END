package gg.agit.konect.domain.user.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.UnRegisteredUser;

public interface UnRegisteredUserRepository extends Repository<UnRegisteredUser, Integer> {

    Optional<UnRegisteredUser> findByEmailAndProvider(String email, Provider provider);

    void save(UnRegisteredUser user);

    void delete(UnRegisteredUser user);
}
