package gg.agit.konect.domain.club.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ClubFeePaymentSubmitRequest(
    @Schema(description = "Payment image URL", example = "https://cdn.konect.com/fee/abc.jpg")
    String paymentImageUrl
) {
}
