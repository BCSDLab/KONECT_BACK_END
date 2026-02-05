package gg.agit.konect.infrastructure.oauth;

import java.util.Map;
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
import gg.agit.konect.global.util.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;

@Service("naver")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NaverOAuthServiceImpl extends DefaultOAuth2UserService implements SocialOAuthService {

    private final UserRepository userRepository;
    private final UnRegisteredUserRepository unRegisteredUserRepository;

    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> response = oAuth2User.getAttribute("response");
        String email = (String)response.get("email");
        String phoneNumber = PhoneNumberUtils.format((String)response.get("mobile"));

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
                .phoneNumber(phoneNumber)
                .build();

            unRegisteredUserRepository.save(newUser);
        } else {
            UnRegisteredUser existingUser = unregistered.get();
            if (phoneNumber != null && existingUser.getPhoneNumber() == null) {
                existingUser.updatePhoneNumber(phoneNumber);
            }
        }

        return oAuth2User;
    }
}
