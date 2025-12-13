package gg.agit.konect.user.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.security.enums.Provider;
import gg.agit.konect.user.model.User;

public interface UserRepository extends Repository<User, Integer> {

    Optional<User> findByEmailAndProvider(String email, Provider provider);

    Optional<User> findById(Integer id);

    boolean existsByUniversityIdAndStudentNumberAndIdNot(Integer universityId, String studentNumber, Integer id);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Integer id);

    default User getById(Integer id) {
        return findById(id).orElseThrow(() ->
            CustomException.of(ApiResponseCode.NOT_FOUND_USER));
    }

    User save(User user);
}
