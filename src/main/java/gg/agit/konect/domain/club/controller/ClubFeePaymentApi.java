package gg.agit.konect.domain.club.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import gg.agit.konect.domain.club.dto.ClubFeePaymentResponse;
import gg.agit.konect.domain.club.dto.ClubFeePaymentSubmitRequest;
import gg.agit.konect.global.auth.annotation.UserId;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "(Normal) Club - FeePayment")
@RequestMapping("/clubs")
public interface ClubFeePaymentApi {

    @Operation(summary = "Submit club fee payment.")
    @PostMapping("/{clubId}/fee-payments")
    ResponseEntity<ClubFeePaymentResponse> submitFeePayment(
        @PathVariable(name = "clubId") Integer clubId,
        @RequestBody ClubFeePaymentSubmitRequest request,
        @UserId Integer requesterId
    );

    @Operation(summary = "Approve a member's fee payment. Manager only.")
    @PostMapping("/{clubId}/fee-payments/{targetUserId}/approve")
    ResponseEntity<ClubFeePaymentResponse> approveFeePayment(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "targetUserId") Integer targetUserId,
        @UserId Integer requesterId
    );

    @Operation(summary = "Get all fee payments for a club. Manager only.")
    @GetMapping("/{clubId}/fee-payments")
    ResponseEntity<List<ClubFeePaymentResponse>> getFeePayments(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer requesterId
    );

    @Operation(summary = "Get my fee payment status.")
    @GetMapping("/{clubId}/fee-payments/me")
    ResponseEntity<ClubFeePaymentResponse> getMyFeePayment(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer requesterId
    );
}
