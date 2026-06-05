package com.smartcloset.auth.infrastructure;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Refresh token을 응답 cookie에 쓰거나 만료시키는 infrastructure helper다.
 *
 * <p>Cookie 속성은 환경별 설정으로 분리하고, token 원문은 JSON response에 넣지 않는다.</p>
 */
@Component
public class RefreshTokenCookieWriter {

    private final RefreshTokenCookieProperties properties;

    public RefreshTokenCookieWriter(RefreshTokenCookieProperties properties) {
        this.properties = properties;
    }

    /**
     * refresh token 원문을 HttpOnly cookie로 내려보낸다.
     */
    public void write(HttpServletResponse response, String refreshToken) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(refreshToken, properties.maxAge()).toString());
    }

    /**
     * 동일한 cookie 이름에 max-age 0을 설정해 브라우저 refresh cookie를 만료시킨다.
     */
    public void expire(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(properties.name(), value)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path(properties.path())
                .maxAge(maxAge);
        if (properties.domain() != null && !properties.domain().isBlank()) {
            builder.domain(properties.domain());
        }
        return builder.build();
    }
}
