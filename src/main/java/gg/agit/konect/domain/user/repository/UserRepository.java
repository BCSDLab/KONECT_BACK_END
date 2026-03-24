package gg.agit.konect.domain.user.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    Optional<User> findFirstByRoleAndDeletedAtIsNullOrderByIdAsc(UserRole role);

    default User getById(Integer id) {
        return findById(id).orElseThrow(() ->
            CustomException.of(ApiResponseCode.NOT_FOUND_USER));
    }

    // 학번 유니크 제약 제거로 동일 대학+학번 유저가 복수 존재할 수 있어 List로 반환
    @Query("""
        SELECT u
        FROM User u
        WHERE u.university.id = :universityId
        AND u.studentNumber = :studentNumber
        AND u.deletedAt IS NULL
        """)
    List<User> findAllByUniversityIdAndStudentNumber(
        @Param("universityId") Integer universityId,
        @Param("studentNumber") String studentNumber
    );

    @Query("""
        SELECT u
        FROM User u
        WHERE u.university.id = :universityId
        AND u.studentNumber IN :studentNumbers
        AND u.deletedAt IS NULL
        """)
    List<User> findAllByUniversityIdAndStudentNumberIn(
        @Param("universityId") Integer universityId,
        @Param("studentNumbers") Set<String> studentNumbers
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
