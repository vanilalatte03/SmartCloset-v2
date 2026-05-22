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

@Service
public class UserLocationService {

    private final UserRepository userRepository;
    private final LocationCatalog locationCatalog;

    public UserLocationService(UserRepository userRepository, LocationCatalog locationCatalog) {
        this.userRepository = userRepository;
        this.locationCatalog = locationCatalog;
    }

    @Transactional
    public UserLocationResponse getUserLocation(Long userId) {
        User user = findUser(userId);
        user.ensureDefaultLocation();
        return UserLocationResponse.from(user);
    }

    @Transactional
    public UserLocationResponse updateUserLocation(Long userId, UpdateUserLocationRequest request) {
        User user = findUser(userId);
        LocationOption location = findLocation(request.locationCode());
        user.updateLocation(location);
        return UserLocationResponse.from(user);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.USER_NOT_FOUND));
    }

    private LocationOption findLocation(String locationCode) {
        return locationCatalog.findByCode(locationCode)
                .orElseThrow(() -> new SmartClosetException(
                        ErrorCode.LOCATION_NOT_FOUND,
                        ErrorCode.LOCATION_NOT_FOUND.message(),
                        List.of(ErrorDetail.of("locationCode", locationCode))
                ));
    }
}
