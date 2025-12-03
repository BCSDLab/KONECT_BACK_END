package gg.agit.konect.council.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import gg.agit.konect.council.model.CouncilSocialMedia;

public interface CouncilSocialMediaRepository extends Repository<CouncilSocialMedia, Integer> {

    List<CouncilSocialMedia> findByCouncilId(Integer councilId);
}
