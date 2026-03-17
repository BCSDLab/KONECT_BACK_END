package gg.agit.konect.domain.club.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import gg.agit.konect.domain.club.dto.ClubFeePaymentResponse;
import gg.agit.konect.domain.club.dto.ClubFeePaymentSubmitRequest;
import gg.agit.konect.domain.club.service.ClubFeePaymentService;
import gg.agit.konect.global.auth.annotation.UserId;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/clubs")
public class ClubFeePaymentController implements ClubFeePaymentApi {

    private final ClubFeePaymentService clubFeePaymentService;

    @Override
    public ResponseEntity<ClubFeePaymentResponse> submitFeePayment(
        @PathVariable(name = "clubId") Integer clubId,
        @RequestBody ClubFeePaymentSubmitRequest request,
        @UserId Integer requesterId
    ) {
        ClubFeePaymentResponse response = clubFeePaymentService.submitFeePayment(
            clubId, requesterId, request.paymentImageUrl()
        );
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<ClubFeePaymentResponse> approveFeePayment(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "targetUserId") Integer targetUserId,
        @UserId Integer requesterId
    ) {
        ClubFeePaymentResponse response = clubFeePaymentService.approveFeePayment(
            clubId, targetUserId, requesterId
        );
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<List<ClubFeePaymentResponse>> getFeePayments(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer requesterId
    ) {
        return ResponseEntity.ok(clubFeePaymentService.getFeePayments(clubId, requesterId));
    }

    @Override
    public ResponseEntity<ClubFeePaymentResponse> getMyFeePayment(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer requesterId
    ) {
        return ResponseEntity.ok(clubFeePaymentService.getMyFeePayment(clubId, requesterId));
    }
}
