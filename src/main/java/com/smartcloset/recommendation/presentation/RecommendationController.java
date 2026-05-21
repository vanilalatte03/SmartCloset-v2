package com.smartcloset.recommendation.presentation;

import com.smartcloset.common.response.ApiResponse;
import com.smartcloset.recommendation.application.RecommendationService;
import com.smartcloset.recommendation.dto.RecommendationResponse;
import com.smartcloset.recommendation.dto.RecommendationWornResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RecommendationResponse>> createTodayRecommendation(@RequestParam Long userId) {
        RecommendationResponse response = recommendationService.createTodayRecommendation(userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @PatchMapping("/{recommendationId}/worn")
    public ApiResponse<RecommendationWornResponse> markWorn(
            @PathVariable Long recommendationId,
            @RequestParam Long userId
    ) {
        return ApiResponse.of(recommendationService.markWorn(userId, recommendationId));
    }
}
