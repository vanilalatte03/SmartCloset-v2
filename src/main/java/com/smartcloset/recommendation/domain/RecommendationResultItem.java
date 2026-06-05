package com.smartcloset.recommendation.domain;

import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;

/**
 * 추천 결과에 포함된 각 옷과 outfit slot을 저장하는 JPA entity다.
 *
 * <p>추천 생성 시점의 슬롯 구성을 보존해 이후 옷장 변경과 무관하게 이력 응답을 재구성할 수 있게 한다.</p>
 */
@Entity
@Table(
        name = "recommendation_result_items",
        indexes = {
                @Index(name = "idx_recommendation_result_items_result", columnList = "recommendation_result_id"),
                @Index(name = "idx_recommendation_result_items_clothing", columnList = "clothing_item_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_recommendation_result_items_result_slot",
                        columnNames = {"recommendation_result_id", "slot"}
                )
        }
)
public class RecommendationResultItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recommendation_result_id", nullable = false)
    private RecommendationResult recommendationResult;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clothing_item_id", nullable = false)
    private ClothingItem clothingItem;

    @Enumerated(EnumType.STRING)
    @Column(name = "slot", nullable = false, length = 30)
    private OutfitSlot slot;

    protected RecommendationResultItem() {
    }

    private RecommendationResultItem(RecommendationResult recommendationResult, ClothingItem clothingItem, OutfitSlot slot) {
        this.recommendationResult = Objects.requireNonNull(
                recommendationResult,
                "recommendationResult must not be null"
        );
        this.clothingItem = Objects.requireNonNull(clothingItem, "clothingItem must not be null");
        this.slot = Objects.requireNonNull(slot, "slot must not be null");
    }

    /**
     * 추천 결과 snapshot 안에서 특정 옷이 차지한 outfit slot을 기록한다.
     */
    public static RecommendationResultItem of(
            RecommendationResult recommendationResult,
            ClothingItem clothingItem,
            OutfitSlot slot
    ) {
        return new RecommendationResultItem(recommendationResult, clothingItem, slot);
    }

    public Long getId() {
        return id;
    }

    public RecommendationResult getRecommendationResult() {
        return recommendationResult;
    }

    public ClothingItem getClothingItem() {
        return clothingItem;
    }

    public OutfitSlot getSlot() {
        return slot;
    }
}
