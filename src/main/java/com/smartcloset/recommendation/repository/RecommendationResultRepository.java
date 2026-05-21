package com.smartcloset.recommendation.repository;

import com.smartcloset.recommendation.domain.RecommendationResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationResultRepository extends JpaRepository<RecommendationResult, Long> {

    Optional<RecommendationResult> findByIdAndUserId(Long id, Long userId);

    List<RecommendationResult> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);

    List<RecommendationResult> findByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            Long userId,
            LocalDateTime createdAt
    );
}
