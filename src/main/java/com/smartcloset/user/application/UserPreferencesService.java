package com.smartcloset.user.application;

import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.user.domain.PreferenceJsonMapper;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.dto.UpdateUserPreferencesRequest;
import com.smartcloset.user.dto.UserPreferencesResponse;
import com.smartcloset.user.repository.UserRepository;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPreferencesService {

    private final UserRepository userRepository;
    private final PreferenceJsonMapper preferenceJsonMapper;

    public UserPreferencesService(UserRepository userRepository, PreferenceJsonMapper preferenceJsonMapper) {
        this.userRepository = userRepository;
        this.preferenceJsonMapper = preferenceJsonMapper;
    }

    @Transactional(readOnly = true)
    public UserPreferencesResponse getUserPreferences(Long userId) {
        User user = findUser(userId);
        return toResponse(user);
    }

    @Transactional
    public UserPreferencesResponse updateUserPreferences(Long userId, UpdateUserPreferencesRequest request) {
        User user = findUser(userId);
        List<ClothingColor> preferredColors = unique(request.preferredColors());
        List<ClothingMaterial> preferredMaterials = unique(request.preferredMaterials());
        List<String> styleTags = unique(request.styleTags().stream()
                .map(String::trim)
                .toList());

        user.updatePreferences(
                preferenceJsonMapper.toColorsJson(preferredColors),
                preferenceJsonMapper.toMaterialsJson(preferredMaterials),
                preferenceJsonMapper.toStyleTagsJson(styleTags)
        );
        return toResponse(user);
    }

    private UserPreferencesResponse toResponse(User user) {
        return new UserPreferencesResponse(
                preferenceJsonMapper.readColors(user.getPreferredColorsJson()),
                preferenceJsonMapper.readMaterials(user.getPreferredMaterialsJson()),
                preferenceJsonMapper.readStyleTags(user.getStyleTagsJson())
        );
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.USER_NOT_FOUND));
    }

    private <T> List<T> unique(List<T> values) {
        return List.copyOf(new LinkedHashSet<>(values));
    }
}
