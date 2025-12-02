package gg.agit.konect.club.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import gg.agit.konect.club.model.ClubRepresentative;
import gg.agit.konect.club.model.ClubMemberId;

public interface ClubRepresentativeRepository extends Repository<ClubRepresentative, ClubMemberId> {

    List<ClubRepresentative> findByClubId(Integer clubId);
}
