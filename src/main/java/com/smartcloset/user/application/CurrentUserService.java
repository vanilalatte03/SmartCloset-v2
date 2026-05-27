package com.smartcloset.user.application;

import com.smartcloset.auth.application.AuthProviderService;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.user.dto.CurrentUserResponse;
import com.smartcloset.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrentUserService {

    private final UserRepository userRepository;
    private final AuthProviderService authProviderService;

    public CurrentUserService(UserRepository userRepository, AuthProviderService authProviderService) {
        this.userRepository = userRepository;
        this.authProviderService = authProviderService;
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .map(user -> CurrentUserResponse.from(user, authProviderService.providersFor(user)))
                .orElseThrow(() -> new SmartClosetException(ErrorCode.USER_NOT_FOUND));
    }
}
