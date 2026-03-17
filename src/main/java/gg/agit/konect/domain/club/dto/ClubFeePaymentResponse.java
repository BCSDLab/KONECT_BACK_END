package gg.agit.konect.domain.club.dto;

import java.time.LocalDateTime;

import gg.agit.konect.domain.club.model.ClubFeePayment;
import io.swagger.v3.oas.annotations.media.Schema;

public record ClubFeePaymentResponse(
    @Schema(description = "User ID", example = "1")
    Integer userId,

    @Schema(description = "Name", example = "John")
    String userName,

    @Schema(description = "Student number", example = "2021136089")
    String studentNumber,

    @Schema(description = "Payment status", example = "true")
    boolean isPaid,

    @Schema(description = "Approved at")
    LocalDateTime approvedAt,

    @Schema(description = "Payment image URL")
    String paymentImageUrl
) {
    public static ClubFeePaymentResponse from(ClubFeePayment payment) {
        return new ClubFeePaymentResponse(
            payment.getUser().getId(),
            payment.getUser().getName(),
            payment.getUser().getStudentNumber(),
            payment.isPaid(),
            payment.getApprovedAt(),
            payment.getPaymentImageUrl()
        );
    }
}
