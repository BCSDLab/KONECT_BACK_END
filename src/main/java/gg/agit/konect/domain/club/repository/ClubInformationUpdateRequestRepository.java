package gg.agit.konect.domain.club.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import gg.agit.konect.domain.club.model.ClubInformationUpdateRequestEntity;

public interface ClubInformationUpdateRequestRepository
    extends JpaRepository<ClubInformationUpdateRequestEntity, Integer> {
}
