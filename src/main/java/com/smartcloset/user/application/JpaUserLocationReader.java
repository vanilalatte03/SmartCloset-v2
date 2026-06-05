package com.smartcloset.user.application;

import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 entity에서 저장 위치를 읽어 날씨 provider용 snapshot으로 변환하는 JPA adapter다.
 *
 * <p>저장 위치가 비어 있으면 사용자 기본 위치를 보정한 뒤 snapshot을 반환한다.</p>
 */
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
