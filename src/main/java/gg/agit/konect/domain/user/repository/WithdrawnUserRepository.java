package gg.agit.konect.domain.user.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.user.model.WithdrawnUser;

public interface WithdrawnUserRepository extends Repository<WithdrawnUser, Integer> {

    WithdrawnUser save(WithdrawnUser withdrawnUser);

    List<WithdrawnUser> findByWithdrawnAtBefore(LocalDateTime dateTime);

    void deleteAll(List<WithdrawnUser> withdrawnUsers);
}
