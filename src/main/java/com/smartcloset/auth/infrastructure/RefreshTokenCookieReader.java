package com.smartcloset.auth.infrastructure;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Refresh token cookie에서 rotation/revoke 대상 token 원문을 읽어오는 helper다.
 *
 * <p>Cookie가 없거나 비어 있으면 인증 실패 처리는 caller가 결정할 수 있도록 {@link Optional#empty()}를 반환한다.</p>
 */
@Component
public class RefreshTokenCookieReader {

    private final RefreshTokenCookieProperties properties;

    public RefreshTokenCookieReader(RefreshTokenCookieProperties properties) {
        this.properties = properties;
    }

    /**
     * 설정된 cookie 이름에서 refresh token 원문을 읽고 빈 값은 없는 값으로 처리한다.
     */
    public Optional<String> read(HttpServletRequest request) {
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
}
