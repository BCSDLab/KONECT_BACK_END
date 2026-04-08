package gg.agit.konect.domain.club.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ClubPreMemberBatchAddRequest(
    @NotNull(message = "회원 목록은 필수입니다.")
    @Size(min = 1, max = 300, message = "회원은 최소 1명에서 최대 300명까지 등록할 수 있습니다.")
    @Schema(description = "사전 등록할 회원 목록", requiredMode = REQUIRED)
    @Valid List<@NotNull ClubPreMemberAddRequest> members
) {
}
