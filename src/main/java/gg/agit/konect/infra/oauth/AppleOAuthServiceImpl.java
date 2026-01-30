package gg.agit.konect.infra.oauth;

import java.util.Optional;

import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.UnRegisteredUser;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UnRegisteredUserRepository;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.code.ApiResponseCode;
import gg.agit.konect.global.exception.CustomException;
import lombok.RequiredArgsConstructor;

@Service("apple")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AppleOAuthServiceImpl extends OidcUserService {

    private final UserRepository userRepository;
    private final UnRegisteredUserRepository unRegisteredUserRepository;

    @Transactional
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String email = oidcUser.getAttribute("email");
        String providerId = oidcUser.getSubject();

        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        Provider provider = Provider.valueOf(registrationId);

        if (userRepository.existsByProviderIdAndProvider(providerId, provider)) {
            return oidcUser;
        }

        if (StringUtils.hasText(email)) {
            Optional<User> registeredByEmail = userRepository.findByEmailAndProvider(email, provider);

            if (registeredByEmail.isPresent()) {
                return oidcUser;
            }
        }

        if (!unRegisteredUserRepository.existsByProviderIdAndProvider(providerId, provider)) {
            if (!StringUtils.hasText(email)) {
                throw CustomException.of(ApiResponseCode.FAILED_EXTRACT_EMAIL);
            }

            UnRegisteredUser newUser = UnRegisteredUser.builder()
                .email(email)
                .provider(provider)
                .providerId(providerId)
                .build();

            unRegisteredUserRepository.save(newUser);
        }

        return oidcUser;
    }
}
