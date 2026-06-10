package com.smartcloset.recommendation.repository;

import com.smartcloset.recommendation.domain.RecommendationResult;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecommendationResultRepository extends JpaRepository<RecommendationResult, Long> {

    Optional<RecommendationResult> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select result
            from RecommendationResult result
            where result.id = :id
              and result.user.id = :userId
            """)
    Optional<RecommendationResult> findByIdAndUserIdForWorn(
            @Param("id") Long id,
            @Param("userId") Long userId
    );

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
            @Param("createdAt") LocalDateTime createdAt,
            Pageable pageable
    );

    @Query("""
            select r.id
            from RecommendationResult r
            where r.user.id = :userId
              and r.feedbackUpdatedAt >= :feedbackUpdatedAt
            order by r.feedbackUpdatedAt desc, r.id desc
            """)
    List<Long> findIdsByUserIdAndFeedbackUpdatedAtGreaterThanEqualOrderByFeedbackUpdatedAtDesc(
            @Param("userId") Long userId,
            @Param("feedbackUpdatedAt") LocalDateTime feedbackUpdatedAt,
            Pageable pageable
    );

    List<RecommendationResult> findByIdIn(Collection<Long> ids);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from RecommendationResult result where result.user.id = :userId")
    int deleteByUserId(@Param("userId") Long userId);
}
