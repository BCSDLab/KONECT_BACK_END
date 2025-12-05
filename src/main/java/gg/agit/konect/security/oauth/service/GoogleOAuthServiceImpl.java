package gg.agit.konect.security.oauth.service;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import gg.agit.konect.security.enums.Provider;
import gg.agit.konect.user.model.User;
import gg.agit.konect.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

@Service("google")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoogleOAuthServiceImpl extends DefaultOAuth2UserService implements SocialOAuthService {

    private final UserRepository userRepository;

    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String email = oAuth2User.getAttribute("email");

        userRepository.findByEmail(email)
            .orElseGet(() -> {
                User newUser = User.builder()
                    .email(email)
                    .provider(Provider.GOOGLE)
                    .build();

                return userRepository.save(newUser);
            });

        return oAuth2User;
    }
}
