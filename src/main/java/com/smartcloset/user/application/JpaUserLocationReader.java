package com.smartcloset.user.application;

import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JpaUserLocationReader implements UserLocationReader {

    private final UserRepository userRepository;

    public JpaUserLocationReader(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public UserLocationSnapshot getRequiredLocationSnapshot(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.USER_NOT_FOUND));
        user.ensureDefaultLocation();
        return UserLocationSnapshot.from(user);
    }
}
