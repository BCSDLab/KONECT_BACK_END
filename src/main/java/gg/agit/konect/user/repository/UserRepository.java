package gg.agit.konect.user.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.user.model.User;

public interface UserRepository extends Repository<User, Integer> {

    Optional<User> findByEmail(String email);

    User save(User user);
}
