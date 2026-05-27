package com.smartcloset.recommendation.repository;

import com.smartcloset.recommendation.domain.RecommendationResultItem;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecommendationResultItemRepository extends JpaRepository<RecommendationResultItem, Long> {

    @Query("""
            select item
            from RecommendationResultItem item
            join fetch item.clothingItem
            where item.recommendationResult.id in :recommendationResultIds
            order by item.recommendationResult.id asc, item.id asc
            """)
    List<RecommendationResultItem> findByRecommendationResultIdInWithClothingItem(
            @Param("recommendationResultIds") Collection<Long> recommendationResultIds
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from RecommendationResultItem item
            where item.recommendationResult.id in (
                select result.id
                from RecommendationResult result
                where result.user.id = :userId
            )
            """)
    int deleteByRecommendationResultUserId(@Param("userId") Long userId);
}
