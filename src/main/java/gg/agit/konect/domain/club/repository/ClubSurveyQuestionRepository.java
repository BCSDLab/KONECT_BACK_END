package gg.agit.konect.domain.club.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import gg.agit.konect.domain.club.model.ClubSurveyQuestion;

public interface ClubSurveyQuestionRepository extends Repository<ClubSurveyQuestion, Integer> {

    List<ClubSurveyQuestion> findAllByClubId(Integer clubId);
}
