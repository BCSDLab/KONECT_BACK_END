package gg.agit.konect.domain.club.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import gg.agit.konect.domain.club.model.ClubRegistrationRequestEntity;

public interface ClubRegistrationRequestRepository extends JpaRepository<ClubRegistrationRequestEntity, Integer> {
}
