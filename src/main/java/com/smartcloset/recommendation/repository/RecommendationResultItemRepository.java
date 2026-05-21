package com.smartcloset.recommendation.repository;

import com.smartcloset.recommendation.domain.RecommendationResultItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationResultItemRepository extends JpaRepository<RecommendationResultItem, Long> {
}
