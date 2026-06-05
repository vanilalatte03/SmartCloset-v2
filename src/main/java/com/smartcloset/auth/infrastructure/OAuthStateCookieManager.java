package com.smartcloset.auth.infrastructure;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * OAuth2 authorization request와 callback 사이의 state 값을 HttpOnly cookie로 보관하고 검증한다.
 *
 * <p>State cookie는 callback 성공/실패와 관계없이 만료시켜 재사용 가능성을 줄인다.</p>
 */
@Component
public class OAuthStateCookieManager {

    private final OAuthStateCookieProperties properties;

    public OAuthStateCookieManager(OAuthStateCookieProperties properties) {
        this.properties = properties;
    }

    /**
     * OAuth2 authorization request의 state 값을 HttpOnly cookie로 내려 callback 검증에 사용한다.
     */
    public void write(HttpServletResponse response, String state) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(state, properties.maxAge()).toString());
    }

    /**
     * callback 처리 후 state cookie를 즉시 만료해 같은 state가 다시 쓰이지 않게 한다.
     */
    public void expire(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
    }

    /**
     * callback query의 state가 보관된 cookie 값과 일치하는지 확인한다.
     */
    public boolean matches(HttpServletRequest request, String state) {
        if (state == null || state.isBlank()) {
            return false;
        }
        return read(request)
                .filter(state::equals)
                .isPresent();
    }

    private Optional<String> read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> properties.name().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
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
