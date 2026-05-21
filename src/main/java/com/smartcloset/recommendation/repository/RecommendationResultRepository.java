package com.smartcloset.recommendation.repository;

import com.smartcloset.recommendation.domain.RecommendationResult;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationResultRepository extends JpaRepository<RecommendationResult, Long> {

    Optional<RecommendationResult> findByIdAndUserId(Long id, Long userId);

    @EntityGraph(attributePaths = {"items", "items.clothingItem"})
    List<RecommendationResult> findTop5ByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph(attributePaths = {"items", "items.clothingItem"})
    List<RecommendationResult> findByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            Long userId,
            LocalDateTime createdAt
    );
}
