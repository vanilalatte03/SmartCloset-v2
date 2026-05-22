package com.smartcloset.recommendation.presentation;

import com.smartcloset.common.response.ApiResponse;
import com.smartcloset.recommendation.application.RecommendationService;
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
    public ResponseEntity<ApiResponse<RecommendationResponse>> createRecommendation(
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        RecommendationResponse response = recommendationService.createRecommendation(principal.userId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(response));
    }

    @GetMapping
    public ApiResponse<List<RecommendationResponse>> getRecommendationHistory(
            @AuthenticationPrincipal CurrentUserPrincipal principal,
            @RequestParam(defaultValue = "20") Integer limit
    ) {
        return ApiResponse.of(recommendationService.getRecommendationHistory(principal.userId(), limit));
    }

    @PatchMapping("/{recommendationId}/worn")
    public ApiResponse<RecommendationWornResponse> markWorn(
            @PathVariable Long recommendationId,
            @AuthenticationPrincipal CurrentUserPrincipal principal
    ) {
        return ApiResponse.of(recommendationService.markWorn(principal.userId(), recommendationId));
    }
}
