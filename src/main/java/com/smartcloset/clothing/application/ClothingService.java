package com.smartcloset.clothing.application;

import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.dto.ClothingArchiveResponse;
import com.smartcloset.clothing.dto.ClothingImageFileResponse;
import com.smartcloset.clothing.dto.ClothingRequest;
import com.smartcloset.clothing.dto.ClothingResponse;
import com.smartcloset.clothing.infrastructure.file.ClothingImageStorage;
import com.smartcloset.clothing.infrastructure.file.ClothingImageValidator;
import com.smartcloset.clothing.infrastructure.file.StoredClothingImage;
import com.smartcloset.clothing.infrastructure.file.ValidatedClothingImage;
import com.smartcloset.clothing.repository.ClothingItemRepository;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ClothingService {

    private final ClothingItemRepository clothingItemRepository;
    private final UserRepository userRepository;
    private final ClothingImageValidator clothingImageValidator;
    private final ClothingImageStorage clothingImageStorage;

    public ClothingService(
            ClothingItemRepository clothingItemRepository,
            UserRepository userRepository,
            ClothingImageValidator clothingImageValidator,
            ClothingImageStorage clothingImageStorage
    ) {
        this.clothingItemRepository = clothingItemRepository;
        this.userRepository = userRepository;
        this.clothingImageValidator = clothingImageValidator;
        this.clothingImageStorage = clothingImageStorage;
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

    @Transactional
    public ClothingResponse uploadClothingImage(Long currentUserId, Long clothingId, MultipartFile image) {
        findUser(currentUserId);
        ClothingItem clothingItem = findClothingOwnedByUser(clothingId, currentUserId);
        ValidatedClothingImage validatedImage = clothingImageValidator.validate(image);
        StoredClothingImage storedImage = clothingImageStorage.store(image, validatedImage.extension());
        String previousStoredFilename = clothingItem.getImageStoredFilename();

        try {
            clothingItem.updateImageMetadata(
                    storedImage.storedFilename(),
                    validatedImage.contentType(),
                    validatedImage.sizeBytes(),
                    LocalDateTime.now()
            );
            clothingItemRepository.flush();
        } catch (RuntimeException exception) {
            clothingImageStorage.delete(storedImage.storedFilename());
            throw exception;
        }

        clothingImageStorage.delete(previousStoredFilename);
        return ClothingResponse.from(clothingItem);
    }

    @Transactional(readOnly = true)
    public ClothingImageFileResponse getClothingImage(Long currentUserId, Long clothingId) {
        findUser(currentUserId);
        ClothingItem clothingItem = findClothingOwnedByUser(clothingId, currentUserId);
        String storedFilename = clothingItem.getImageStoredFilename();
        if (storedFilename == null) {
            throw new SmartClosetException(ErrorCode.CLOTHING_IMAGE_NOT_FOUND);
        }
        byte[] bytes = clothingImageStorage.read(storedFilename)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.CLOTHING_IMAGE_NOT_FOUND));
        return new ClothingImageFileResponse(clothingItem.getImageContentType(), bytes);
    }

    @Transactional
    public ClothingResponse deleteClothingImage(Long currentUserId, Long clothingId) {
        findUser(currentUserId);
        ClothingItem clothingItem = findClothingOwnedByUser(clothingId, currentUserId);
        String storedFilename = clothingItem.getImageStoredFilename();
        clothingImageStorage.delete(storedFilename);
        clothingItem.clearImageMetadata();
        return ClothingResponse.from(clothingItem);
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
