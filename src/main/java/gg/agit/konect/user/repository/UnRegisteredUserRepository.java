package gg.agit.konect.user.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.security.enums.Provider;
import gg.agit.konect.user.model.UnRegisteredUser;

public interface UnRegisteredUserRepository extends Repository<UnRegisteredUser, Integer> {

    Optional<UnRegisteredUser> findByEmailAndProvider(String email, Provider provider);

    void save(UnRegisteredUser user);
}
