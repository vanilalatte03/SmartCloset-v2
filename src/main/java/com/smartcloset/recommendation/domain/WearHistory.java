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

/**
 * 사용자가 특정 추천을 실제로 착용했다고 표시한 이력을 저장하는 JPA entity다.
 *
 * <p>추천 결과당 하나의 착용 이력만 허용해 점수 계산의 recent wear history 입력을 안정적으로 유지한다.</p>
 */
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

    /**
     * 특정 추천 결과를 실제 착용한 이력으로 기록한다.
     */
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
