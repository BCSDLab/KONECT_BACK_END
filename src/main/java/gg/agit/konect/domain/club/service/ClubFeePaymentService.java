package gg.agit.konect.domain.club.service;

import static gg.agit.konect.global.code.ApiResponseCode.NOT_FOUND_FEE_PAYMENT;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.club.dto.ClubFeePaymentResponse;
import gg.agit.konect.domain.club.event.ClubFeePaymentApprovedEvent;
import gg.agit.konect.domain.club.model.Club;
import gg.agit.konect.domain.club.model.ClubFeePayment;
import gg.agit.konect.domain.club.repository.ClubFeePaymentRepository;
import gg.agit.konect.domain.club.repository.ClubRepository;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClubFeePaymentService {

    private final ClubRepository clubRepository;
    private final ClubFeePaymentRepository clubFeePaymentRepository;
    private final UserRepository userRepository;
    private final ClubPermissionValidator clubPermissionValidator;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public ClubFeePaymentResponse submitFeePayment(
        Integer clubId,
        Integer userId,
        String paymentImageUrl
    ) {
        Club club = clubRepository.getById(clubId);
        User user = userRepository.getById(userId);

        clubFeePaymentRepository.findByClubIdAndUserId(clubId, userId)
            .ifPresent(p -> {
                throw CustomException.of(
                    gg.agit.konect.global.code.ApiResponseCode.ALREADY_FEE_PAYMENT_SUBMITTED
                );
            });

        ClubFeePayment payment = ClubFeePayment.of(club, user, paymentImageUrl);
        return ClubFeePaymentResponse.from(clubFeePaymentRepository.save(payment));
    }

    @Transactional
    public ClubFeePaymentResponse approveFeePayment(
        Integer clubId,
        Integer targetUserId,
        Integer requesterId
    ) {
        clubPermissionValidator.validateManagerAccess(clubId, requesterId);
        User approver = userRepository.getById(requesterId);
        ClubFeePayment payment = clubFeePaymentRepository.getByClubIdAndUserId(
            clubId, targetUserId
        );

        if (payment.isPaid()) {
            throw CustomException.of(
                gg.agit.konect.global.code.ApiResponseCode.ALREADY_FEE_PAYMENT_APPROVED
            );
        }

        payment.approve(approver);
        applicationEventPublisher.publishEvent(ClubFeePaymentApprovedEvent.of(clubId));

        return ClubFeePaymentResponse.from(payment);
    }

    public List<ClubFeePaymentResponse> getFeePayments(Integer clubId, Integer requesterId) {
        clubRepository.getById(clubId);
        clubPermissionValidator.validateManagerAccess(clubId, requesterId);

        return clubFeePaymentRepository.findAllByClubId(clubId).stream()
            .map(ClubFeePaymentResponse::from)
            .toList();
    }

    public ClubFeePaymentResponse getMyFeePayment(Integer clubId, Integer userId) {
        clubRepository.getById(clubId);
        ClubFeePayment payment = clubFeePaymentRepository.findByClubIdAndUserId(clubId, userId)
            .orElseThrow(() -> CustomException.of(NOT_FOUND_FEE_PAYMENT));
        return ClubFeePaymentResponse.from(payment);
    }
}
