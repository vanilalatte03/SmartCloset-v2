package com.smartcloset.recommendation.domain;

import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.weather.domain.WeatherCondition;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * 날씨 필터를 통과한 옷 목록으로 가능한 상의/하의/아우터 조합을 만든다.
 *
 * <p>여기서는 후보를 제거하지 않고 전부 생성한다. 어떤 조합이 더 나은지는
 * {@link RecommendationScorer}가 점수와 tie-break로 결정한다.</p>
 */
public class OutfitCandidateGenerator {

    /**
     * 날씨 조건상 아우터가 필수인 경우에는 아우터 포함 후보만, 그 외에는 상하의 후보도 함께 생성한다.
     */
    public List<OutfitCandidate> generate(WeatherFilteredClothes clothes, WeatherCondition weather) {
        List<OutfitCandidate> candidates = new ArrayList<>();
        forEach(clothes, weather, candidates::add);
        return List.copyOf(candidates);
    }

    /**
     * 전체 후보 목록을 만들지 않고 생성 순서대로 후보를 하나씩 전달한다.
     */
    public void forEach(
            WeatherFilteredClothes clothes,
            WeatherCondition weather,
            Consumer<OutfitCandidate> candidateConsumer
    ) {
        Objects.requireNonNull(clothes, "clothes must not be null");
        Objects.requireNonNull(weather, "weather must not be null");
        Objects.requireNonNull(candidateConsumer, "candidateConsumer must not be null");

        int generationOrder = 0;
        boolean generated = false;

        for (ClothingItem top : clothes.tops()) {
            for (ClothingItem bottom : clothes.bottoms()) {
                if (!WeatherSuitabilityFilter.isOuterRequired(weather)) {
                    candidateConsumer.accept(OutfitCandidate.withoutOuter(top, bottom, generationOrder++));
                    generated = true;
                }
                // 추운 날에는 WeatherSuitabilityFilter가 아우터 존재를 보장하므로 아우터 포함 후보만 남는다.
                for (ClothingItem outer : clothes.outers()) {
                    candidateConsumer.accept(OutfitCandidate.withOuter(top, bottom, outer, generationOrder++));
                    generated = true;
                }
            }
        }

        if (!generated) {
            throw new RecommendationFailureException(RecommendationFailureCode.INSUFFICIENT_CLOSET_ITEMS);
        }
    }
}
