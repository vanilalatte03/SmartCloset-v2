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

/**
 * 옷 CRUD, 보관함, 이미지 업로드/조회/삭제를 담당하는 application service다.
 *
 * <p>모든 조회와 변경은 currentUserId 기준으로 수행해 다른 사용자의 옷에 접근하지 못하게 한다.</p>
 */
@Service
public class ClothingService {

    private final ClothingItemRepository clothingItemRepository;
    private final UserRepository userRepository;
    private final ClothingImageValidator clothingImageValidator;
    private final ClothingImageStorage clothingImageStorage;
    private final ClothingImageCleanupScheduler clothingImageCleanupScheduler;
    private final ClothingStyleTagMapper clothingStyleTagMapper;

    public ClothingService(
            ClothingItemRepository clothingItemRepository,
            UserRepository userRepository,
            ClothingImageValidator clothingImageValidator,
            ClothingImageStorage clothingImageStorage,
            ClothingImageCleanupScheduler clothingImageCleanupScheduler,
            ClothingStyleTagMapper clothingStyleTagMapper
    ) {
        this.clothingItemRepository = clothingItemRepository;
        this.userRepository = userRepository;
        this.clothingImageValidator = clothingImageValidator;
        this.clothingImageStorage = clothingImageStorage;
        this.clothingImageCleanupScheduler = clothingImageCleanupScheduler;
        this.clothingStyleTagMapper = clothingStyleTagMapper;
    }

    /**
     * 현재 사용자 소유의 새 옷을 등록하고 style tag 배열을 JSON snapshot으로 저장한다.
     */
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
                request.rainSuitable(),
                clothingStyleTagMapper.toJson(request.styleTags())
        );

        return ClothingResponse.from(clothingItemRepository.save(clothingItem), clothingStyleTagMapper);
    }

    /**
     * 보관되지 않은 옷만 id 오름차순으로 조회해 기본 옷장과 추천 후보의 기준 목록으로 사용한다.
     */
    @Transactional(readOnly = true)
    public List<ClothingResponse> getActiveClothes(Long currentUserId) {
        findUser(currentUserId);
        return clothingItemRepository.findByUserIdAndArchivedFalseOrderByIdAsc(currentUserId)
                .stream()
                .map(clothingItem -> ClothingResponse.from(clothingItem, clothingStyleTagMapper))
                .toList();
    }

    /**
     * 보관된 옷만 조회해 복원 가능한 옷장 archive 목록으로 반환한다.
     */
    @Transactional(readOnly = true)
    public List<ClothingResponse> getArchivedClothes(Long currentUserId) {
        findUser(currentUserId);
        return clothingItemRepository.findByUserIdAndArchivedTrueOrderByIdAsc(currentUserId)
                .stream()
                .map(clothingItem -> ClothingResponse.from(clothingItem, clothingStyleTagMapper))
                .toList();
    }

    /**
     * 존재하지 않는 옷과 다른 사용자 옷을 같은 NOT_FOUND 경로로 처리하며 단일 옷을 조회한다.
     */
    @Transactional(readOnly = true)
    public ClothingResponse getClothing(Long currentUserId, Long clothingId) {
        findUser(currentUserId);
        return ClothingResponse.from(findClothingOwnedByUser(clothingId, currentUserId), clothingStyleTagMapper);
    }

    /**
     * 현재 사용자 소유 옷의 기본 정보와 style tag JSON을 전체 교체한다.
     */
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
                request.rainSuitable(),
                clothingStyleTagMapper.toJson(request.styleTags())
        );
        return ClothingResponse.from(clothingItem, clothingStyleTagMapper);
    }

    /**
     * 옷을 보관 상태로 바꿔 기본 옷장 목록과 추천 후보에서 제외한다.
     */
    @Transactional
    public ClothingArchiveResponse archiveClothing(Long currentUserId, Long clothingId) {
        findUser(currentUserId);
        ClothingItem clothingItem = findClothingOwnedByUser(clothingId, currentUserId);
        clothingItem.archive();
        return ClothingArchiveResponse.from(clothingItem);
    }

    /**
     * 보관된 옷을 활성 상태로 되돌려 추천 후보에 다시 포함될 수 있게 한다.
     */
    @Transactional
    public ClothingArchiveResponse unarchiveClothing(Long currentUserId, Long clothingId) {
        findUser(currentUserId);
        ClothingItem clothingItem = findClothingOwnedByUser(clothingId, currentUserId);
        clothingItem.unarchive();
        return ClothingArchiveResponse.from(clothingItem);
    }

    /**
     * 이미지 파일을 먼저 저장한 뒤 DB metadata를 갱신한다.
     *
     * <p>새 파일은 rollback 때 보상 삭제하고, 이전 파일은 commit 이후에만 삭제한다.</p>
     */
    @Transactional
    public ClothingResponse uploadClothingImage(Long currentUserId, Long clothingId, MultipartFile image) {
        findUser(currentUserId);
        ClothingItem clothingItem = findClothingOwnedByUser(clothingId, currentUserId);
        ValidatedClothingImage validatedImage = clothingImageValidator.validate(image);
        StoredClothingImage storedImage = clothingImageStorage.store(image, validatedImage.extension());
        String previousStoredFilename = clothingItem.getImageStoredFilename();
        clothingImageCleanupScheduler.deleteAfterRollback(storedImage.storedFilename());

        try {
            clothingItem.updateImageMetadata(
                    storedImage.storedFilename(),
                    validatedImage.contentType(),
                    validatedImage.sizeBytes(),
                    LocalDateTime.now()
            );
            clothingItemRepository.flush();
        } catch (RuntimeException exception) {
            clothingImageCleanupScheduler.deleteNowAddingSuppressed(storedImage.storedFilename(), exception);
            throw exception;
        }

        clothingImageCleanupScheduler.deleteAfterCommit(previousStoredFilename);
        return ClothingResponse.from(clothingItem, clothingStyleTagMapper);
    }

    /**
     * 현재 사용자가 소유한 옷의 저장 이미지를 읽어 HTTP bytes 응답에 필요한 content type과 함께 반환한다.
     */
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

    /**
     * DB 이미지 메타데이터를 먼저 clear하고 commit 이후 저장소 파일을 삭제한다.
     */
    @Transactional
    public ClothingResponse deleteClothingImage(Long currentUserId, Long clothingId) {
        findUser(currentUserId);
        ClothingItem clothingItem = findClothingOwnedByUser(clothingId, currentUserId);
        String storedFilename = clothingItem.getImageStoredFilename();
        clothingItem.clearImageMetadata();
        clothingItemRepository.flush();
        clothingImageCleanupScheduler.deleteAfterCommit(storedFilename);
        return ClothingResponse.from(clothingItem, clothingStyleTagMapper);
    }

    private User findUser(Long currentUserId) {
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 존재하지 않는 옷과 다른 사용자의 옷은 같은 NOT_FOUND로 처리해 소유권 정보를 노출하지 않는다.
     */
    private ClothingItem findClothingOwnedByUser(Long clothingId, Long currentUserId) {
        return clothingItemRepository.findByIdAndUserId(clothingId, currentUserId)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.CLOTHING_NOT_FOUND));
    }
}
