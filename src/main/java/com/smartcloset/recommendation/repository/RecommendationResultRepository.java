package com.smartcloset.recommendation.repository;

import com.smartcloset.recommendation.domain.RecommendationResult;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecommendationResultRepository extends JpaRepository<RecommendationResult, Long> {

    Optional<RecommendationResult> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select r.id
            from RecommendationResult r
            where r.user.id = :userId
            order by r.createdAt desc, r.id desc
            """)
    List<Long> findIdsByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select r.id
            from RecommendationResult r
            where r.user.id = :userId
              and r.createdAt >= :createdAt
            order by r.createdAt desc, r.id desc
            """)
    List<Long> findIdsByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("createdAt") LocalDateTime createdAt
    );

    List<RecommendationResult> findByIdIn(Collection<Long> ids);
}
