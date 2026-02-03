package gg.agit.konect.domain.club.repository;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import gg.agit.konect.domain.club.model.ClubPreMember;

// TODO. 초기 회원 처리 완료 후 제거 예정
public interface ClubPreMemberRepository extends Repository<ClubPreMember, Integer> {

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
}
