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
import com.smartcloset.common.observability.SmartClosetMetrics;
import io.micrometer.core.instrument.Timer;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final SmartClosetMetrics metrics;

    @Autowired
    public ClothingAnalysisService(
            ClothingImageValidator clothingImageValidator,
            ClothingImageAnalyzer clothingImageAnalyzer,
            ClothingAnalysisDailyLimiter dailyLimiter,
            SmartClosetMetrics metrics
    ) {
        this.clothingImageValidator = clothingImageValidator;
        this.clothingImageAnalyzer = clothingImageAnalyzer;
        this.dailyLimiter = dailyLimiter;
        this.metrics = metrics;
    }

    ClothingAnalysisService(
            ClothingImageValidator clothingImageValidator,
            ClothingImageAnalyzer clothingImageAnalyzer,
            ClothingAnalysisDailyLimiter dailyLimiter
    ) {
        this(clothingImageValidator, clothingImageAnalyzer, dailyLimiter, SmartClosetMetrics.noop());
    }

    /**
     * 현재 인증 사용자의 분석 요청을 처리하고 저장 전 후보 응답을 반환한다.
     */
    public ClothingAnalysisResponse analyze(Long currentUserId, MultipartFile image) {
        Timer.Sample sample = metrics.startTimer();
        try {
            ValidatedClothingImage validatedImage = clothingImageValidator.validate(image);
            if (!clothingImageAnalyzer.isAvailable()) {
                throw new SmartClosetException(ErrorCode.CLOTHING_ANALYSIS_DISABLED);
            }
            dailyLimiter.checkAndIncrement(currentUserId);
            ClothingAnalysisResult result = clothingImageAnalyzer.analyze(
                    new ClothingAnalysisImage(image.getBytes(), validatedImage.contentType())
            );
            ClothingAnalysisResponse response = ClothingAnalysisResponse.from(result);
            metrics.recordClothingAnalysis(sample, response.analyzable() ? "success" : "not_analyzable");
            return response;
        } catch (ClothingImageAnalysisDisabledException exception) {
            metrics.recordClothingAnalysis(sample, "disabled");
            throw new SmartClosetException(ErrorCode.CLOTHING_ANALYSIS_DISABLED);
        } catch (ClothingImageAnalysisUnavailableException exception) {
            metrics.recordClothingAnalysis(sample, "unavailable");
            throw new SmartClosetException(ErrorCode.CLOTHING_ANALYSIS_UNAVAILABLE);
        } catch (IOException exception) {
            metrics.recordClothingAnalysis(sample, "invalid_request");
            throw new SmartClosetException(ErrorCode.INVALID_REQUEST);
        } catch (SmartClosetException exception) {
            metrics.recordClothingAnalysis(sample, outcomeFrom(exception.errorCode()));
            throw exception;
        } catch (RuntimeException exception) {
            metrics.recordClothingAnalysis(sample, "failure");
            throw exception;
        }
    }

    private String outcomeFrom(ErrorCode errorCode) {
        return switch (errorCode) {
            case CLOTHING_ANALYSIS_DISABLED -> "disabled";
            case CLOTHING_ANALYSIS_UNAVAILABLE -> "unavailable";
            case CLOTHING_ANALYSIS_LIMIT_EXCEEDED -> "limit_exceeded";
            case INVALID_REQUEST, MAX_UPLOAD_SIZE_EXCEEDED, MULTIPART_EXCEPTION -> "invalid_request";
            default -> "failure";
        };
    }
}
