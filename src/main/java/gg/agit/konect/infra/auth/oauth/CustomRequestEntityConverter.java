package gg.agit.konect.infra.auth.oauth;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.oauth2.client.endpoint.DefaultOAuth2TokenRequestParametersConverter;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.MultiValueMap;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CustomRequestEntityConverter
    implements Converter<OAuth2AuthorizationCodeGrantRequest, MultiValueMap<String, String>> {

    private static final String APPLE_REGISTRATION_ID = "apple";

    private final DefaultOAuth2TokenRequestParametersConverter<OAuth2AuthorizationCodeGrantRequest> delegate =
        new DefaultOAuth2TokenRequestParametersConverter<>();
    private final AppleClientSecretProvider clientSecretProvider;

    @Override
    public MultiValueMap<String, String> convert(OAuth2AuthorizationCodeGrantRequest request) {
        MultiValueMap<String, String> parameters = delegate.convert(request);

        if (!isApple(request)) {
            return parameters;
        }

        parameters.set(OAuth2ParameterNames.CLIENT_SECRET, clientSecretProvider.getClientSecret());

        return parameters;
    }

    private boolean isApple(OAuth2AuthorizationCodeGrantRequest request) {
        return APPLE_REGISTRATION_ID.equals(request.getClientRegistration().getRegistrationId());
    }

}
