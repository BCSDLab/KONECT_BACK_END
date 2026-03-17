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

    @Operation(
        summary = "회비 납부 접수",
        description = "회원이 회비를 납부했음을 접수합니다. 납부 증빙 이미지 URL을 함께 제출할 수 있습니다."
    )
    @PostMapping("/{clubId}/fee-payments")
    ResponseEntity<ClubFeePaymentResponse> submitFeePayment(
        @PathVariable(name = "clubId") Integer clubId,
        @RequestBody ClubFeePaymentSubmitRequest request,
        @UserId Integer requesterId
    );

    @Operation(
        summary = "회비 납부 승인 (운영진 전용)",
        description = "운영진이 특정 회원의 회비 납부를 승인합니다. "
            + "승인 즉시 구글 스프레드시트의 해당 회원 납부 여부(FeePaid) 컬럼이 자동으로 업데이트됩니다."
    )
    @PostMapping("/{clubId}/fee-payments/{targetUserId}/approve")
    ResponseEntity<ClubFeePaymentResponse> approveFeePayment(
        @PathVariable(name = "clubId") Integer clubId,
        @PathVariable(name = "targetUserId") Integer targetUserId,
        @UserId Integer requesterId
    );

    @Operation(
        summary = "전체 회비 납부 목록 조회 (운영진 전용)",
        description = "동아리 전체 회원의 회비 납부 현황을 조회합니다. 납부 여부, 납부일, 증빙 이미지 URL을 확인할 수 있습니다."
    )
    @GetMapping("/{clubId}/fee-payments")
    ResponseEntity<List<ClubFeePaymentResponse>> getFeePayments(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer requesterId
    );

    @Operation(
        summary = "내 회비 납부 상태 조회",
        description = "로그인한 회원 본인의 회비 납부 상태를 조회합니다."
    )
    @GetMapping("/{clubId}/fee-payments/me")
    ResponseEntity<ClubFeePaymentResponse> getMyFeePayment(
        @PathVariable(name = "clubId") Integer clubId,
        @UserId Integer requesterId
    );
}
