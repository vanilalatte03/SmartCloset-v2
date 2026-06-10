package com.smartcloset.recommendation.repository;

import com.smartcloset.recommendation.domain.WearHistory;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WearHistoryRepository extends JpaRepository<WearHistory, Long> {

    boolean existsByRecommendationResultId(Long recommendationResultId);

    Optional<WearHistory> findByRecommendationResultId(Long recommendationResultId);

    List<WearHistory> findByRecommendationResultIdIn(Collection<Long> recommendationResultIds);

    @Query("""
            select history
            from WearHistory history
            join fetch history.recommendationResult
            where history.user.id = :userId
              and history.wornAt >= :wornAt
            order by history.wornAt desc, history.id desc
            """)
    List<WearHistory> findByUserIdAndWornAtGreaterThanEqualOrderByWornAtDesc(
            @Param("userId") Long userId,
            @Param("wornAt") LocalDateTime wornAt,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from WearHistory history where history.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
