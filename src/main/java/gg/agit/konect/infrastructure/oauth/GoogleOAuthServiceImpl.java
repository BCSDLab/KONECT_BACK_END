package gg.agit.konect.infrastructure.oauth;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import gg.agit.konect.domain.user.enums.Provider;
import gg.agit.konect.domain.user.model.UnRegisteredUser;
import gg.agit.konect.domain.user.model.User;
import gg.agit.konect.domain.user.repository.UnRegisteredUserRepository;
import gg.agit.konect.domain.user.repository.UserRepository;
import gg.agit.konect.global.auth.oauth.SocialOAuthService;
import gg.agit.konect.global.util.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service("google")
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GoogleOAuthServiceImpl extends DefaultOAuth2UserService implements SocialOAuthService {

    private static final String PEOPLE_API_URL =
        "https://people.googleapis.com/v1/people/me?personFields=phoneNumbers";

    private final UserRepository userRepository;
    private final UnRegisteredUserRepository unRegisteredUserRepository;
    private final RestTemplate restTemplate;

    @Transactional
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        String email = oAuth2User.getAttribute("email");
        String providerId = oAuth2User.getAttribute("sub");
        String phoneNumber = fetchPhoneNumberFromPeopleApi(userRequest);

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
                .providerId(providerId)
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

    private String fetchPhoneNumberFromPeopleApi(OAuth2UserRequest userRequest) {
        try {
            String accessToken = userRequest.getAccessToken().getTokenValue();

            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                PEOPLE_API_URL,
                HttpMethod.GET,
                entity,
                Map.class
            );

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> phoneNumbers =
                (List<Map<String, Object>>)response.getBody().get("phoneNumbers");

            if (phoneNumbers == null || phoneNumbers.isEmpty()) {
                return null;
            }

            String rawPhoneNumber = findPrimaryPhoneNumber(phoneNumbers);
            return PhoneNumberUtils.format(rawPhoneNumber);
        } catch (Exception e) {
            log.error("Google People API 전화번호 조회 실패", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String findPrimaryPhoneNumber(List<Map<String, Object>> phoneNumbers) {
        return phoneNumbers.stream()
            .filter(phone -> {
                Map<String, Object> metadata = (Map<String, Object>)phone.get("metadata");
                return metadata != null && Boolean.TRUE.equals(metadata.get("primary"));
            })
            .findFirst()
            .map(phone -> (String)phone.get("value"))
            .orElse((String)phoneNumbers.get(0).get("value"));
    }
}
