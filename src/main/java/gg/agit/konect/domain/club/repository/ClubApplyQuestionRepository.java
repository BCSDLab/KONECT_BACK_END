package gg.agit.konect.domain.club.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.club.model.ClubApplyQuestion;

public interface ClubApplyQuestionRepository extends Repository<ClubApplyQuestion, Integer> {

    List<ClubApplyQuestion> findAllByClubIdOrderByIdAsc(Integer clubId);

    ClubApplyQuestion save(ClubApplyQuestion question);

    List<ClubApplyQuestion> saveAll(Iterable<ClubApplyQuestion> questions);

    void deleteAll(Iterable<ClubApplyQuestion> questions);
}
