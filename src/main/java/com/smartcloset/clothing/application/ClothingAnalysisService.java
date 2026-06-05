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

    public ClothingAnalysisResponse analyze(Long currentUserId, MultipartFile image) {
        ValidatedClothingImage validatedImage = clothingImageValidator.validate(image);
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
