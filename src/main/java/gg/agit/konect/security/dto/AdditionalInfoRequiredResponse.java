package gg.agit.konect.security.dto;

import gg.agit.konect.domain.user.enums.Provider;

public record AdditionalInfoRequiredResponse(
    String status,
    String email,
    Provider provider
) {
    public static AdditionalInfoRequiredResponse of(String email, Provider provider) {
        return new AdditionalInfoRequiredResponse("NEED_ADDITIONAL_INFO", email, provider);
    }
}
