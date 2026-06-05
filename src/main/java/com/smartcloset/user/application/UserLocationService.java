package com.smartcloset.user.application;

import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.common.response.ErrorDetail;
import com.smartcloset.location.domain.LocationCatalog;
import com.smartcloset.location.domain.LocationOption;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.dto.UpdateUserLocationRequest;
import com.smartcloset.user.dto.UserLocationResponse;
import com.smartcloset.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 사용자의 추천 기준 위치를 조회/변경한다.
 *
 * <p>저장되는 값은 catalog의 행정구역 snapshot이며, 브라우저 GPS 원문 좌표는 저장하지 않는다.</p>
 */
@Service
public class UserLocationService {

    private final UserRepository userRepository;
    private final LocationCatalog locationCatalog;

    public UserLocationService(UserRepository userRepository, LocationCatalog locationCatalog) {
        this.userRepository = userRepository;
        this.locationCatalog = locationCatalog;
    }

    /**
     * 위치가 비어 있는 기존 사용자도 읽는 순간 기본 Seoul 위치를 채운다.
     */
    @Transactional
    public UserLocationResponse getUserLocation(Long userId) {
        User user = findUser(userId);
        user.ensureDefaultLocation();
        return UserLocationResponse.from(user);
    }

    /**
     * catalog location code로 찾은 행정구역 snapshot과 선택 source를 현재 사용자 위치로 저장한다.
     */
    @Transactional
    public UserLocationResponse updateUserLocation(Long userId, UpdateUserLocationRequest request) {
        User user = findUser(userId);
        LocationOption location = findLocation(request.locationCode());
        user.updateLocation(location, request.resolvedSource());
        return UserLocationResponse.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.USER_NOT_FOUND));
    }

    private LocationOption findLocation(String locationCode) {
        return locationCatalog.findByCode(locationCode)
                .orElseThrow(() -> new SmartClosetException(
                        ErrorCode.INVALID_REQUEST,
                        ErrorCode.INVALID_REQUEST.message(),
                        List.of(ErrorDetail.of("locationCode", locationCode))
                ));
    }
}
