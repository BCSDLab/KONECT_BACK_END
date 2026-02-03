package gg.agit.konect.infrastructure.oauth;

import java.util.Optional;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.UnRegisteredUser;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UnRegisteredUserRepository;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.auth.oauth.SocialOAuthService;
import lombok.RequiredArgsConstructor;

@Service("google")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoogleOAuthServiceImpl extends DefaultOAuth2UserService implements SocialOAuthService {

    private final UserRepository userRepository;
    private final UnRegisteredUserRepository unRegisteredUserRepository;

    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String email = oAuth2User.getAttribute("email");

        String registrationId = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        Provider provider = Provider.valueOf(registrationId);

        Optional<User> registered = userRepository.findByEmailAndProvider(email, provider);

        if (registered.isPresent()) {
            return oAuth2User;
        }

        Optional<UnRegisteredUser> unregistered =
            unRegisteredUserRepository.findByEmailAndProvider(email, provider);

        if (unregistered.isEmpty()) {
            UnRegisteredUser newUser = UnRegisteredUser.builder()
                .email(email)
                .provider(provider)
                .build();

            unRegisteredUserRepository.save(newUser);
        }

        return oAuth2User;
    }
}
