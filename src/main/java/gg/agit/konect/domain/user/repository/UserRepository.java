package gg.agit.konect.domain.user.repository;

import java.util.Optional;

import org.springframework.data.repository.Repository;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.User;

public interface UserRepository extends Repository<User, Integer> {

    Optional<User> findByEmailAndProvider(String email, Provider provider);

    Optional<User> findById(Integer id);

    default User getById(Integer id) {
        return findById(id).orElseThrow(() ->
            CustomException.of(ApiResponseCode.NOT_FOUND_USER));
    }

    boolean existsByUniversityIdAndStudentNumberAndIdNot(Integer universityId, String studentNumber, Integer id);

    boolean existsByUniversityIdAndStudentNumber(Integer universityId, String studentNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Integer id);

    User save(User user);

    void delete(User user);
}
