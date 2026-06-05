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

/**
 * 추천 개인화에 쓰는 사용자 선호 색상/소재/스타일 태그를 관리한다.
 */
@Service
public class UserPreferencesService {

    private final UserRepository userRepository;
    private final PreferenceJsonMapper preferenceJsonMapper;

    public UserPreferencesService(UserRepository userRepository, PreferenceJsonMapper preferenceJsonMapper) {
        this.userRepository = userRepository;
        this.preferenceJsonMapper = preferenceJsonMapper;
    }

    /**
     * DB JSON 문자열로 저장된 선호 값을 API 배열 DTO로 변환해 반환한다.
     */
    @Transactional(readOnly = true)
    public UserPreferencesResponse getUserPreferences(Long userId) {
        User user = findUser(userId);
        return toResponse(user);
    }

    /**
     * 중복 값은 입력 순서를 유지한 채 제거해 추천 점수 계산에 같은 선호가 여러 번 반영되지 않게 한다.
     */
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
