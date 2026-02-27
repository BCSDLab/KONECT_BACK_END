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

    @Query("""
        SELECT u
        FROM User u
        WHERE u.email = :email
        AND u.provider = :provider
        AND u.deletedAt IS NULL
        """)
    Optional<User> findByEmailAndProvider(@Param("email") String email, @Param("provider") Provider provider);

    Optional<User> findFirstByEmailAndProviderAndDeletedAtIsNotNullOrderByDeletedAtDesc(
        @Param("email") String email,
        @Param("provider") Provider provider
    );

    @Query("""
        SELECT u
        FROM User u
        WHERE u.providerId = :providerId
        AND u.provider = :provider
        AND u.deletedAt IS NULL
        """)
    Optional<User> findByProviderIdAndProvider(
        @Param("providerId") String providerId,
        @Param("provider") Provider provider
    );

    Optional<User> findFirstByProviderIdAndProviderAndDeletedAtIsNotNullOrderByDeletedAtDesc(
        @Param("providerId") String providerId,
        @Param("provider") Provider provider
    );

    @Query("""
        SELECT (COUNT(u) > 0)
        FROM User u
        WHERE u.providerId = :providerId
        AND u.provider = :provider
        AND u.deletedAt IS NULL
        """)
    boolean existsByProviderIdAndProvider(@Param("providerId") String providerId, @Param("provider") Provider provider);

    @Query("""
        SELECT u
        FROM User u
        WHERE u.id = :id
        AND u.deletedAt IS NULL
        """)
    Optional<User> findById(@Param("id") Integer id);

    Optional<User> findFirstByRoleOrderByIdAsc(UserRole role);

    default User getById(Integer id) {
        return findById(id).orElseThrow(() ->
            CustomException.of(ApiResponseCode.NOT_FOUND_USER));
    }

    default User getByProviderIdAndProvider(String providerId, Provider provider) {
        return findByProviderIdAndProvider(providerId, provider)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_USER));
    }

    @Query("""
        SELECT (COUNT(u) > 0)
        FROM User u
        WHERE u.university.id = :universityId
        AND u.studentNumber = :studentNumber
        AND u.id <> :id
        AND u.deletedAt IS NULL
        """)
    boolean existsByUniversityIdAndStudentNumberAndIdNot(
        @Param("universityId") Integer universityId,
        @Param("studentNumber") String studentNumber,
        @Param("id") Integer id
    );

    @Query("""
        SELECT (COUNT(u) > 0)
        FROM User u
        WHERE u.university.id = :universityId
        AND u.studentNumber = :studentNumber
        AND u.deletedAt IS NULL
        """)
    boolean existsByUniversityIdAndStudentNumber(
        @Param("universityId") Integer universityId,
        @Param("studentNumber") String studentNumber
    );

    @Query("""
        SELECT u
        FROM User u
        WHERE u.university.id = :universityId
        AND u.studentNumber = :studentNumber
        AND u.deletedAt IS NULL
        """)
    Optional<User> findByUniversityIdAndStudentNumber(
        @Param("universityId") Integer universityId,
        @Param("studentNumber") String studentNumber
    );

    @Query("""
        SELECT u
        FROM User u
        WHERE u.university.id = :universityId
        AND u.studentNumber LIKE CONCAT(:year, '%')
        AND u.deletedAt IS NULL
        """)
    List<User> findUserIdsByUniversityAndStudentYear(
        @Param("universityId") Integer universityId,
        @Param("year") String year
    );

    @Query("""
        SELECT (COUNT(u) > 0)
        FROM User u
        WHERE u.phoneNumber = :phoneNumber
        AND u.id <> :id
        AND u.deletedAt IS NULL
        """)
    boolean existsByPhoneNumberAndIdNot(@Param("phoneNumber") String phoneNumber, @Param("id") Integer id);

    User save(User user);

    @Query("""
        SELECT u
        FROM User u
        WHERE u.id IN :ids
        AND u.deletedAt IS NULL
        """)
    List<User> findAllByIdIn(@Param("ids") List<Integer> ids);
}
