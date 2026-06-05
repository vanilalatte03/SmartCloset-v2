package com.smartcloset.user.application;

import com.smartcloset.auth.repository.AccountActionTokenRepository;
import com.smartcloset.auth.repository.RefreshSessionRepository;
import com.smartcloset.auth.repository.SocialAccountRepository;
import com.smartcloset.clothing.infrastructure.file.ClothingImageStorage;
import com.smartcloset.clothing.repository.ClothingItemRepository;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.recommendation.repository.RecommendationResultItemRepository;
import com.smartcloset.recommendation.repository.RecommendationResultRepository;
import com.smartcloset.recommendation.repository.WearHistoryRepository;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.dto.AccountDeletionRequest;
import com.smartcloset.user.dto.AccountDeletionResponse;
import com.smartcloset.user.repository.UserRepository;
import java.util.List;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정 삭제 요청을 hard delete로 처리한다.
 *
 * <p>사용자 소유 DB row를 먼저 삭제하고 flush가 성공한 뒤 이미지 파일을 지워,
 * DB 실패 때문에 계정 데이터는 남았는데 이미지가 사라지는 상황을 피한다.</p>
 */
@Service
public class AccountDeletionService {

    private static final String DELETE_CONFIRMATION = "DELETE";

    private final UserRepository userRepository;
    private final ClothingItemRepository clothingItemRepository;
    private final RecommendationResultRepository recommendationResultRepository;
    private final RecommendationResultItemRepository recommendationResultItemRepository;
    private final WearHistoryRepository wearHistoryRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final AccountActionTokenRepository accountActionTokenRepository;
    private final SocialAccountRepository socialAccountRepository;
    private final ClothingImageStorage clothingImageStorage;
    private final PasswordEncoder passwordEncoder;

    public AccountDeletionService(
            UserRepository userRepository,
            ClothingItemRepository clothingItemRepository,
            RecommendationResultRepository recommendationResultRepository,
            RecommendationResultItemRepository recommendationResultItemRepository,
            WearHistoryRepository wearHistoryRepository,
            RefreshSessionRepository refreshSessionRepository,
            AccountActionTokenRepository accountActionTokenRepository,
            SocialAccountRepository socialAccountRepository,
            ClothingImageStorage clothingImageStorage,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.clothingItemRepository = clothingItemRepository;
        this.recommendationResultRepository = recommendationResultRepository;
        this.recommendationResultItemRepository = recommendationResultItemRepository;
        this.wearHistoryRepository = wearHistoryRepository;
        this.refreshSessionRepository = refreshSessionRepository;
        this.accountActionTokenRepository = accountActionTokenRepository;
        this.socialAccountRepository = socialAccountRepository;
        this.clothingImageStorage = clothingImageStorage;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * password 계정은 비밀번호 확인이 필요하고, Google-only 계정은 DELETE 문구 확인만 요구한다.
     */
    @Transactional
    public AccountDeletionResponse deleteAccount(Long currentUserId, AccountDeletionRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.USER_NOT_FOUND));
        validateRequest(user, request);

        List<String> imageFilenames = clothingItemRepository.findImageStoredFilenamesByUserId(currentUserId);

        // FK 의존성이 있는 자식 row부터 지우고 마지막에 user row를 삭제한다.
        wearHistoryRepository.deleteByUserId(currentUserId);
        recommendationResultItemRepository.deleteByRecommendationResultUserId(currentUserId);
        recommendationResultRepository.deleteByUserId(currentUserId);
        clothingItemRepository.deleteByUserId(currentUserId);
        refreshSessionRepository.deleteByUserId(currentUserId);
        accountActionTokenRepository.deleteByUserId(currentUserId);
        socialAccountRepository.deleteByUserId(currentUserId);
        userRepository.delete(user);
        userRepository.flush();

        // 파일 시스템은 DB처럼 rollback되지 않으므로, DB 삭제가 flush된 성공 경로에서만 파일을 정리한다.
        imageFilenames.forEach(clothingImageStorage::delete);
        return AccountDeletionResponse.success();
    }

    private void validateRequest(User user, AccountDeletionRequest request) {
        if (!DELETE_CONFIRMATION.equals(request.confirmation())) {
            throw new SmartClosetException(ErrorCode.INVALID_REQUEST);
        }
        if (!user.isPasswordLoginEnabled()) {
            return;
        }
        String password = request.password();
        if (password == null || password.isBlank() || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new SmartClosetException(ErrorCode.UNAUTHORIZED);
        }
    }
}
