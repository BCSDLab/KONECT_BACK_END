package gg.agit.konect.domain.club.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.club.model.ClubApplyAnswer;

public interface ClubApplyAnswerRepository extends Repository<ClubApplyAnswer, Integer> {

    List<ClubApplyAnswer> saveAll(Iterable<ClubApplyAnswer> answers);
}
