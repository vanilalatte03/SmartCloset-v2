package com.smartcloset.clothing.application;

import com.smartcloset.clothing.dto.ClothingAnalysisResponse;
import com.smartcloset.clothing.infrastructure.analysis.ClothingAnalysisImage;
import com.smartcloset.clothing.infrastructure.analysis.ClothingAnalysisResult;
import com.smartcloset.clothing.infrastructure.analysis.ClothingImageAnalysisDisabledException;
import com.smartcloset.clothing.infrastructure.analysis.ClothingImageAnalysisUnavailableException;
import com.smartcloset.clothing.infrastructure.analysis.ClothingImageAnalyzer;
import com.smartcloset.clothing.infrastructure.file.ClothingImageValidator;
import com.smartcloset.clothing.infrastructure.file.ValidatedClothingImage;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import java.io.IOException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 옷 사진 분석 API의 validation, limit, provider 호출, HTTP DTO mapping을 조율한다.
 *
 * <p>기존 옷 이미지 검증 규칙을 재사용하지만 분석 이미지는 저장하지 않는다. provider-specific 예외는
 * 공개 API error code로 변환해 controller가 business rule을 갖지 않게 한다.</p>
 */
@Service
public class ClothingAnalysisService {

    private final ClothingImageValidator clothingImageValidator;
    private final ClothingImageAnalyzer clothingImageAnalyzer;
    private final ClothingAnalysisDailyLimiter dailyLimiter;

    public ClothingAnalysisService(
            ClothingImageValidator clothingImageValidator,
            ClothingImageAnalyzer clothingImageAnalyzer,
            ClothingAnalysisDailyLimiter dailyLimiter
    ) {
        this.clothingImageValidator = clothingImageValidator;
        this.clothingImageAnalyzer = clothingImageAnalyzer;
        this.dailyLimiter = dailyLimiter;
    }

    /**
     * 현재 인증 사용자의 분석 요청을 처리하고 저장 전 후보 응답을 반환한다.
     */
    public ClothingAnalysisResponse analyze(Long currentUserId, MultipartFile image) {
        ValidatedClothingImage validatedImage = clothingImageValidator.validate(image);
        if (!clothingImageAnalyzer.isAvailable()) {
            throw new SmartClosetException(ErrorCode.CLOTHING_ANALYSIS_DISABLED);
        }
        dailyLimiter.checkAndIncrement(currentUserId);

        try {
            ClothingAnalysisResult result = clothingImageAnalyzer.analyze(
                    new ClothingAnalysisImage(image.getBytes(), validatedImage.contentType())
            );
            return ClothingAnalysisResponse.from(result);
        } catch (ClothingImageAnalysisDisabledException exception) {
            throw new SmartClosetException(ErrorCode.CLOTHING_ANALYSIS_DISABLED);
        } catch (ClothingImageAnalysisUnavailableException exception) {
            throw new SmartClosetException(ErrorCode.CLOTHING_ANALYSIS_UNAVAILABLE);
        } catch (IOException exception) {
            throw new SmartClosetException(ErrorCode.INVALID_REQUEST);
        }
    }
}
