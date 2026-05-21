package com.smartcloset.recommendation.repository;

import com.smartcloset.recommendation.domain.WearHistory;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WearHistoryRepository extends JpaRepository<WearHistory, Long> {

    boolean existsByRecommendationResultId(Long recommendationResultId);

    Optional<WearHistory> findByRecommendationResultId(Long recommendationResultId);

    List<WearHistory> findByUserIdAndWornAtGreaterThanEqualOrderByWornAtDesc(Long userId, LocalDateTime wornAt);
}
