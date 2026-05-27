package com.smartcloset.auth.infrastructure;

import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import java.util.Objects;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class GoogleOAuthClient {

    private final RestClient restClient;

    public GoogleOAuthClient() {
        this(RestClient.create());
    }

    GoogleOAuthClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public GoogleUserProfile fetchUserProfile(String code, GoogleOAuthProperties properties) {
        GoogleOAuthProperties.Google google = properties.google();
        try {
            GoogleTokenResponse token = requestToken(code, google);
            return restClient.get()
                    .uri(google.userInfoUri())
                    .headers(headers -> headers.setBearerAuth(token.accessToken()))
                    .retrieve()
                    .body(GoogleUserProfile.class);
        } catch (RestClientException exception) {
            throw new SmartClosetException(ErrorCode.OAUTH2_PROVIDER_UNAVAILABLE);
        }
    }

    private GoogleTokenResponse requestToken(String code, GoogleOAuthProperties.Google google) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("code", requireCode(code));
        body.add("client_id", google.clientId());
        body.add("client_secret", google.clientSecret());
        body.add("redirect_uri", google.redirectUri());
        body.add("grant_type", "authorization_code");

        GoogleTokenResponse response = restClient.post()
                .uri(google.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(GoogleTokenResponse.class);
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new SmartClosetException(ErrorCode.OAUTH2_PROVIDER_UNAVAILABLE);
        }
        return response;
    }

    private String requireCode(String code) {
        Objects.requireNonNull(code, "code must not be null");
        if (code.isBlank()) {
            throw new SmartClosetException(ErrorCode.INVALID_REQUEST);
        }
        return code;
    }
}
