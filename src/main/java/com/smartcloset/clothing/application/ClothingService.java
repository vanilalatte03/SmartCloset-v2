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
    public ClothingResponse createClothing(Long currentUserId, ClothingRequest request) {
        User user = findUser(currentUserId);
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
    public List<ClothingResponse> getActiveClothes(Long currentUserId) {
        findUser(currentUserId);
        return clothingItemRepository.findByUserIdAndArchivedFalseOrderByIdAsc(currentUserId)
                .stream()
                .map(ClothingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClothingResponse getClothing(Long currentUserId, Long clothingId) {
        findUser(currentUserId);
        return ClothingResponse.from(findClothingOwnedByUser(clothingId, currentUserId));
    }

    @Transactional
    public ClothingResponse updateClothing(Long currentUserId, Long clothingId, ClothingRequest request) {
        findUser(currentUserId);
        ClothingItem clothingItem = findClothingOwnedByUser(clothingId, currentUserId);
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
    public ClothingArchiveResponse archiveClothing(Long currentUserId, Long clothingId) {
        findUser(currentUserId);
        ClothingItem clothingItem = findClothingOwnedByUser(clothingId, currentUserId);
        clothingItem.archive();
        return ClothingArchiveResponse.from(clothingItem);
    }

    private User findUser(Long currentUserId) {
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.USER_NOT_FOUND));
    }

    private ClothingItem findClothingOwnedByUser(Long clothingId, Long currentUserId) {
        return clothingItemRepository.findByIdAndUserId(clothingId, currentUserId)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.CLOTHING_NOT_FOUND));
    }
}
