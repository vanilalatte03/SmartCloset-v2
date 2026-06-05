package com.smartcloset.user.application;

import com.smartcloset.auth.application.AuthProviderService;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.user.dto.CurrentUserResponse;
import com.smartcloset.user.dto.UpdateCurrentUserRequest;
import com.smartcloset.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현재 인증 사용자의 프로필과 연결된 로그인 provider 정보를 제공한다.
 */
@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private final AuthProviderService authProviderService;

    public CurrentUserService(UserRepository userRepository, AuthProviderService authProviderService) {
        this.userRepository = userRepository;
        this.authProviderService = authProviderService;
    }

    /**
     * 현재 사용자 프로필과 연결 provider 목록을 조회하되 응답에는 userId를 노출하지 않는다.
     */
    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .map(user -> CurrentUserResponse.from(user, authProviderService.providersFor(user)))
                .orElseThrow(() -> new SmartClosetException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 현재 사용자의 표시 이름을 trim된 요청 값으로 교체한다.
     */
    @Transactional
    public CurrentUserResponse updateCurrentUser(Long userId, UpdateCurrentUserRequest request) {
        return userRepository.findById(userId)
                .map(user -> {
                    user.rename(request.name().trim());
                    return CurrentUserResponse.from(user, authProviderService.providersFor(user));
                })
                .orElseThrow(() -> new SmartClosetException(ErrorCode.USER_NOT_FOUND));
    }
}
