package com.smartcloset.recommendation.domain;

import com.smartcloset.common.domain.BaseTimeEntity;
import com.smartcloset.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(
        name = "wear_histories",
        indexes = {
                @Index(name = "idx_wear_histories_user_worn_at", columnList = "user_id, worn_at")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_wear_histories_recommendation_result",
                        columnNames = "recommendation_result_id"
                )
        }
)
public class WearHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_result_id", nullable = false, unique = true)
    private RecommendationResult recommendationResult;

    @Column(name = "worn_at", nullable = false)
    private LocalDateTime wornAt;

    protected WearHistory() {
    }

    private WearHistory(User user, RecommendationResult recommendationResult, LocalDateTime wornAt) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.recommendationResult = Objects.requireNonNull(
                recommendationResult,
                "recommendationResult must not be null"
        );
        this.wornAt = Objects.requireNonNull(wornAt, "wornAt must not be null");
    }

    public static WearHistory record(User user, RecommendationResult recommendationResult, LocalDateTime wornAt) {
        return new WearHistory(user, recommendationResult, wornAt);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public RecommendationResult getRecommendationResult() {
        return recommendationResult;
    }

    public LocalDateTime getWornAt() {
        return wornAt;
    }
}
