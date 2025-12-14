package gg.agit.konect.security.dto;

import gg.agit.konect.domain.user.enums.Provider;

public record LoginSuccessResponse(
    Integer userId,
    String email,
    Provider provider
) {

    public static LoginSuccessResponse of(Integer userId, String email, Provider provider) {
        return new LoginSuccessResponse(userId, email, provider);
    }
}
