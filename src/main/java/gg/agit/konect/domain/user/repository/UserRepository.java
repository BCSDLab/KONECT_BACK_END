package gg.agit.konect.domain.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;

public interface UserRepository extends Repository<User, Integer> {

    Optional<User> findByEmailAndProvider(String email, Provider provider);

    Optional<User> findByProviderIdAndProvider(String providerId, Provider provider);

    boolean existsByProviderIdAndProvider(String providerId, Provider provider);

    Optional<User> findById(Integer id);

    Optional<User> findFirstByRoleOrderByIdAsc(UserRole role);

    default User getById(Integer id) {
        return findById(id).orElseThrow(() ->
            CustomException.of(ApiResponseCode.NOT_FOUND_USER));
    }

    default User getByProviderIdAndProvider(String providerId, Provider provider) {
        return findByProviderIdAndProvider(providerId, provider)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_USER));
    }

    boolean existsByUniversityIdAndStudentNumberAndIdNot(Integer universityId, String studentNumber, Integer id);

    boolean existsByUniversityIdAndStudentNumber(Integer universityId, String studentNumber);

    Optional<User> findByUniversityIdAndStudentNumber(Integer universityId, String studentNumber);

    @Query("""
        SELECT u
        FROM User u
        WHERE u.university.id = :universityId
        AND u.studentNumber LIKE CONCAT(:year, '%')
        """)
    List<User> findUserIdsByUniversityAndStudentYear(
        @Param("universityId") Integer universityId,
        @Param("year") String year
    );

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Integer id);

    User save(User user);

    void delete(User user);

    void flush();

    List<User> findAllByIdIn(List<Integer> ids);
}
