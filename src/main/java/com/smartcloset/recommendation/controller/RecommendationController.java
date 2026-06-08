package com.smartcloset.recommendation.controller;

import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.common.response.ApiResponse;
import com.smartcloset.recommendation.application.RecommendationService;
import com.smartcloset.recommendation.dto.RecommendationFeedbackRequest;
import com.smartcloset.recommendation.dto.RecommendationFeedbackResponse;
import com.smartcloset.recommendation.dto.RecommendationRequest;
import com.smartcloset.recommendation.dto.RecommendationResponse;
import com.smartcloset.recommendation.dto.RecommendationWornResponse;
import com.smartcloset.security.CurrentUserPrincipal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 사용자의 추천 생성, 추천 이력, 착용 표시, 피드백 교체 endpoint를 제공한다.
 *
 * <p>요청 기본값과 단순 query 형식 검증만 처리하고, 추천 후보 생성과 사용자 소유권 검증은
 * {@link RecommendationService}에 위임한다.</p>
 */
@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /**
     * 요청 body가 없어도 기본 상황/예보 시간대를 적용해 추천 생성을 유지한다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RecommendationResponse>> createRecommendation(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestBody(required = false) RecommendationRequest request
    ) {
        RecommendationRequest resolvedRequest = resolveRequest(request);
        RecommendationResponse response = recommendationService.createRecommendation(
                principal.userId(),
                resolvedRequest.situationOrDefault(),
                resolvedRequest.forecastPeriodOrDefault()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    /**
     * 현재 사용자의 추천 이력을 최신순으로 조회하고 limit 문자열만 controller에서 숫자로 변환한다.
     */
    @GetMapping
    public ApiResponse<List<RecommendationResponse>> getRecommendationHistory(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(required = false) String limit
    ) {
        return ApiResponse.of(recommendationService.getRecommendationHistory(principal.userId(), parseLimit(limit)));
    }

    /**
     * 현재 사용자가 소유한 추천 결과를 착용 완료로 표시한다.
     */
    @PatchMapping("/{recommendationId}/worn")
    public ApiResponse<RecommendationWornResponse> markWorn(
            @PathVariable Long recommendationId,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(recommendationService.markWorn(principal.userId(), recommendationId));
    }

    /**
     * 피드백을 전체 교체하며 body가 없으면 기존 피드백을 clear하는 service 계약을 사용한다.
     */
    @PutMapping("/{recommendationId}/feedback")
    public ApiResponse<RecommendationFeedbackResponse> replaceFeedback(
            @PathVariable Long recommendationId,
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestBody(required = false) RecommendationFeedbackRequest request
    ) {
        return ApiResponse.of(recommendationService.replaceFeedback(principal.userId(), recommendationId, request));
    }

    /**
     * limit은 숫자 형식만 controller에서 확인하고, 범위 검증은 service 정책에 맡긴다.
     */
    private Integer parseLimit(String limit) {
        if (limit == null) {
            return null;
        }
        if (limit.isBlank()) {
            throw new SmartClosetException(ErrorCode.INVALID_PAGINATION);
        }
        try {
            return Integer.valueOf(limit);
        } catch (NumberFormatException exception) {
            throw new SmartClosetException(ErrorCode.INVALID_PAGINATION);
        }
    }

    private RecommendationRequest resolveRequest(RecommendationRequest request) {
        return request == null ? new RecommendationRequest(null, null) : request;
    }
}
