package gg.agit.konect.domain.club.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import gg.agit.konect.domain.club.model.ClubRegistrationRequest;

@Repository
public interface ClubRegistrationRequestRepository extends JpaRepository<ClubRegistrationRequest, Integer> {
}
