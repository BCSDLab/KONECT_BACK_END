package gg.agit.konect.global.auth.oauth;

import java.util.Map;
import java.util.Optional;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.security.enums.Provider;
import gg.agit.konect.domain.user.model.UnRegisteredUser;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UnRegisteredUserRepository;
import gg.agit.konect.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service("kakao")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KakaoOAuthServiceImpl extends DefaultOAuth2UserService implements SocialOAuthService {

    private final UserRepository userRepository;
    private final UnRegisteredUserRepository unRegisteredUserRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> kakaoAccount = oAuth2User.getAttribute("kakao_account");
        String email = (String) kakaoAccount.get("email");

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
