package gg.agit.konect.club.repository;

import java.util.List;

import org.springframework.data.repository.Repository;

import gg.agit.konect.club.model.ClubFeePayment;
import gg.agit.konect.club.model.ClubMember;

public interface ClubFeePaymentRepository extends Repository<ClubFeePayment, Integer> {

    List<ClubFeePayment> findAllByClubMemberIn(List<ClubMember> clubMembers);
}
