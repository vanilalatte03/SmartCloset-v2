package com.smartcloset.clothing.application;

import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.dto.ClothingArchiveResponse;
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

    @Transactional(readOnly = true)
    public ClothingResponse getClothing(Long userId, Long clothingId) {
        findUser(userId);
        return ClothingResponse.from(findClothingOwnedByUser(clothingId, userId));
    }

    @Transactional
    public ClothingResponse updateClothing(Long userId, Long clothingId, ClothingRequest request) {
        findUser(userId);
        ClothingItem clothingItem = findClothingOwnedByUser(clothingId, userId);
        clothingItem.updateDetails(
                request.name(),
                request.category(),
                request.color(),
                request.material(),
                request.minTemperature(),
                request.maxTemperature(),
                request.rainSuitable()
        );
        return ClothingResponse.from(clothingItem);
    }

    @Transactional
    public ClothingArchiveResponse archiveClothing(Long userId, Long clothingId) {
        findUser(userId);
        ClothingItem clothingItem = findClothingOwnedByUser(clothingId, userId);
        clothingItem.archive();
        return ClothingArchiveResponse.from(clothingItem);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.USER_NOT_FOUND));
    }

    private ClothingItem findClothingOwnedByUser(Long clothingId, Long userId) {
        return clothingItemRepository.findByIdAndUserId(clothingId, userId)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.CLOTHING_NOT_FOUND));
    }
}
