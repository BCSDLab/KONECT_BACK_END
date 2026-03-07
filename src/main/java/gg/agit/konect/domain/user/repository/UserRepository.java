package gg.agit.konect.domain.user.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.user.enums.UserRole;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

public interface UserRepository extends Repository<User, Integer> {

    @Query("""
        SELECT u
        FROM User u
        WHERE u.id = :id
        AND u.deletedAt IS NULL
        """)
    Optional<User> findById(@Param("id") Integer id);

    @Query("""
        SELECT u
        FROM User u
        WHERE u.role = :role
        AND u.deletedAt IS NULL
        ORDER BY u.id ASC
        """)
    Optional<User> findFirstByRoleOrderByIdAsc(@Param("role") UserRole role);

    default User getById(Integer id) {
        return findById(id).orElseThrow(() ->
            CustomException.of(ApiResponseCode.NOT_FOUND_USER));
    }

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

    User save(User user);

    @Query("""
        SELECT u
        FROM User u
        WHERE u.id IN :ids
        AND u.deletedAt IS NULL
        """)
    List<User> findAllByIdIn(@Param("ids") List<Integer> ids);
}
