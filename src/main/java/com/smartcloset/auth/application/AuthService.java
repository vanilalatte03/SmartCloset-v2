package com.smartcloset.auth.application;

import com.smartcloset.auth.dto.AuthResponse;
import com.smartcloset.auth.dto.LoginRequest;
import com.smartcloset.auth.dto.SignupRequest;
import com.smartcloset.clothing.application.DefaultClothingPresetSeeder;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.security.CurrentUserPrincipal;
import com.smartcloset.security.JwtTokenProvider;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.dto.CurrentUserResponse;
import com.smartcloset.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final DefaultClothingPresetSeeder defaultClothingPresetSeeder;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            DefaultClothingPresetSeeder defaultClothingPresetSeeder,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.defaultClothingPresetSeeder = defaultClothingPresetSeeder;
        this.refreshTokenService = refreshTokenService;
    }

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new SmartClosetException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        User user = userRepository.save(User.create(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.name()
        ));
        defaultClothingPresetSeeder.seedIfEmpty(user);
        return authResponse(user);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new SmartClosetException(ErrorCode.UNAUTHORIZED));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new SmartClosetException(ErrorCode.UNAUTHORIZED);
        }
        defaultClothingPresetSeeder.seedIfEmpty(user);
        return authResponse(user);
    }

    @Transactional
    public RefreshTokenBundle loginWithRefreshSession(LoginRequest request) {
        AuthResponse response = login(request);
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new SmartClosetException(ErrorCode.UNAUTHORIZED));
        RefreshTokenService.IssuedRefreshToken refreshToken = refreshTokenService.issue(user);
        return new RefreshTokenBundle(response, refreshToken.refreshToken());
    }

    @Transactional
    public RefreshTokenBundle refresh(String refreshToken) {
        RefreshTokenService.RotatedRefreshToken rotated = refreshTokenService.rotate(refreshToken);
        return new RefreshTokenBundle(authResponse(rotated.user()), rotated.refreshToken());
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revokeIfPresent(refreshToken);
        }
    }

    private AuthResponse authResponse(User user) {
        CurrentUserPrincipal principal = new CurrentUserPrincipal(user.getId(), user.getEmail(), user.getRole());
        String accessToken = jwtTokenProvider.createAccessToken(principal);
        return AuthResponse.bearer(accessToken, CurrentUserResponse.from(user));
    }
}
