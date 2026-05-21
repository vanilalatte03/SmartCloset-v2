package com.smartcloset.clothing.application;

import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.dto.ClothingRequest;
import com.smartcloset.clothing.dto.ClothingResponse;
import com.smartcloset.clothing.repository.ClothingItemRepository;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClothingService {

    private final ClothingItemRepository clothingItemRepository;
    private final UserRepository userRepository;

    public ClothingService(ClothingItemRepository clothingItemRepository, UserRepository userRepository) {
        this.clothingItemRepository = clothingItemRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ClothingResponse createClothing(Long userId, ClothingRequest request) {
        User user = findUser(userId);
        ClothingItem clothingItem = ClothingItem.create(
                user,
                request.name(),
                request.category(),
                request.color(),
                request.material(),
                request.minTemperature(),
                request.maxTemperature(),
                request.rainSuitable()
        );

        return ClothingResponse.from(clothingItemRepository.save(clothingItem));
    }

    @Transactional(readOnly = true)
    public List<ClothingResponse> getActiveClothes(Long userId) {
        findUser(userId);
        return clothingItemRepository.findByUserIdAndArchivedFalseOrderByIdAsc(userId)
                .stream()
                .map(ClothingResponse::from)
                .toList();
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.USER_NOT_FOUND));
    }
}
