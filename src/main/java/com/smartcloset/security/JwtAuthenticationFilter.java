package com.smartcloset.security;

import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.user.domain.UserRole;
import com.smartcloset.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authorization header의 bearer access token을 SecurityContext로 변환하는 filter다.
 *
 * <p>토큰이 없으면 공개 API나 이후 security rule이 판단하도록 그대로 통과시키고,
 * 토큰이 있으면 서명/만료/사용자 존재 여부를 검증한다.</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final SecurityErrorResponseWriter errorResponseWriter;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            SecurityErrorResponseWriter errorResponseWriter,
            UserRepository userRepository
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.errorResponseWriter = errorResponseWriter;
        this.userRepository = userRepository;
    }

    /**
     * Bearer access token이 있으면 현재 사용자 principal을 검증해 SecurityContext에 채운다.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            CurrentUserPrincipal principal = jwtTokenProvider.parseAccessToken(
                    authorization.substring(BEARER_PREFIX.length()));
            if (!userRepository.existsById(principal.userId())) {
                // 계정 삭제 후 남은 access token은 USER_NOT_FOUND로 끊어 stale session을 방지한다.
                SecurityContextHolder.clearContext();
                errorResponseWriter.write(request, response, ErrorCode.USER_NOT_FOUND);
                return;
            }
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    authorities(principal.role())
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (JwtTokenException exception) {
            SecurityContextHolder.clearContext();
            errorResponseWriter.write(request, response, ErrorCode.INVALID_TOKEN, exception);
        }
    }

    private List<SimpleGrantedAuthority> authorities(UserRole role) {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }
}
