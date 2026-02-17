package gg.agit.konect.domain.club.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.club.model.ClubPreMember;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;

// TODO. 초기 회원 처리 완료 후 제거 예정
public interface ClubPreMemberRepository extends Repository<ClubPreMember, Integer> {

    @Query("""
        SELECT cpm
        FROM ClubPreMember cpm
        WHERE cpm.club.id = :clubId
        ORDER BY
            CASE cpm.clubPosition
                WHEN gg.agit.konect.domain.club.enums.ClubPosition.PRESIDENT THEN 0
                WHEN gg.agit.konect.domain.club.enums.ClubPosition.VICE_PRESIDENT THEN 1
                WHEN gg.agit.konect.domain.club.enums.ClubPosition.MANAGER THEN 2
                WHEN gg.agit.konect.domain.club.enums.ClubPosition.MEMBER THEN 3
            END ASC,
            cpm.name ASC
        """)
    List<ClubPreMember> findAllByClubId(@Param("clubId") Integer clubId);

    @Query("""
        SELECT cpm
        FROM ClubPreMember cpm
        WHERE cpm.id = :preMemberId
        AND cpm.club.id = :clubId
        """)
    Optional<ClubPreMember> findByIdAndClubId(
        @Param("preMemberId") Integer preMemberId,
        @Param("clubId") Integer clubId
    );

    default ClubPreMember getByIdAndClubId(Integer preMemberId, Integer clubId) {
        return findByIdAndClubId(preMemberId, clubId)
            .orElseThrow(() -> CustomException.of(ApiResponseCode.NOT_FOUND_CLUB_PRE_MEMBER));
    }

    @Query("""
        SELECT cpm
        FROM ClubPreMember cpm
        JOIN FETCH cpm.club c
        WHERE c.university.id = :universityId
        AND cpm.studentNumber = :studentNumber
        AND cpm.name = :name
        """)
    List<ClubPreMember> findAllByUniversityIdAndStudentNumberAndName(
        @Param("universityId") Integer universityId,
        @Param("studentNumber") String studentNumber,
        @Param("name") String name
    );

    void deleteAll(Iterable<ClubPreMember> preMembers);

    boolean existsByClubIdAndStudentNumberAndName(Integer clubId, String studentNumber, String name);

    void deleteByClubIdAndStudentNumber(Integer clubId, String studentNumber);

    void delete(ClubPreMember preMember);

    ClubPreMember save(ClubPreMember preMember);
}
