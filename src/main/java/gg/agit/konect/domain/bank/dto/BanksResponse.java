package gg.agit.konect.domain.bank.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import gg.agit.konect.domain.bank.model.Bank;
import io.swagger.v3.oas.annotations.media.Schema;

public record BanksResponse(
    @Schema(description = "은행 리스트", requiredMode = REQUIRED)
    List<BankResponse> banks
) {
    public record BankResponse(
        @Schema(description = "은행 고유 ID", example = "1", requiredMode = REQUIRED)
        Integer id,

        @Schema(description = "은행 이름", example = "국민은행", requiredMode = REQUIRED)
        String name,

        @Schema(description = "은행 이미지 URL", example = "https://example.com/banks/kookmin.png", requiredMode = REQUIRED)
        String imageUrl
    ) {
        public static BankResponse from(Bank bank) {
            return new BankResponse(
                bank.getId(),
                bank.getName(),
                bank.getImageUrl()
            );
        }
    }

    public static BanksResponse from(List<Bank> banks) {
        return new BanksResponse(
            banks.stream()
                .map(BankResponse::from)
                .toList()
        );
    }
}
