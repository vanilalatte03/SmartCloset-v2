package com.smartcloset.auth.application;

import com.smartcloset.auth.dto.AuthResponse;
import com.smartcloset.auth.dto.LoginRequest;
import com.smartcloset.auth.dto.SignupRequest;
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

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
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
        return issueAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new SmartClosetException(ErrorCode.UNAUTHORIZED));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new SmartClosetException(ErrorCode.UNAUTHORIZED);
        }
        return issueAuthResponse(user);
    }

    private AuthResponse issueAuthResponse(User user) {
        CurrentUserPrincipal principal = new CurrentUserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getRole()
        );
        String accessToken = jwtTokenProvider.createAccessToken(principal);
        return AuthResponse.bearer(accessToken, CurrentUserResponse.from(user));
    }
}
