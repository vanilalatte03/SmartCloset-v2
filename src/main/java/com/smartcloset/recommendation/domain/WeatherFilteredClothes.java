package com.smartcloset.recommendation.domain;

import com.smartcloset.clothing.domain.ClothingCategory;
import com.smartcloset.clothing.domain.ClothingItem;
import java.util.List;
import java.util.Objects;

/**
 * 날씨 필터를 통과한 옷을 추천 후보 생성용 category bucket으로 묶는 value object다.
 *
 * <p>이미지 보유 여부와 무관하게 TOP/BOTTOM/OUTER 후보군만 표현한다.</p>
 */
public record WeatherFilteredClothes(
        List<ClothingItem> tops,
        List<ClothingItem> bottoms,
        List<ClothingItem> outers
) {

    public WeatherFilteredClothes {
        tops = List.copyOf(Objects.requireNonNull(tops, "tops must not be null"));
        bottoms = List.copyOf(Objects.requireNonNull(bottoms, "bottoms must not be null"));
        outers = List.copyOf(Objects.requireNonNull(outers, "outers must not be null"));
    }

    /**
     * category bucket에 담긴 모든 후보 옷을 하나의 리스트로 펼친다.
     */
    public List<ClothingItem> allItems() {
        return java.util.stream.Stream.of(tops, bottoms, outers)
                .flatMap(List::stream)
                .toList();
    }

    /**
     * 특정 category에 추천 후보가 남아 있는지 확인한다.
     */
    public boolean hasCategory(ClothingCategory category) {
        return switch (category) {
            case TOP -> !tops.isEmpty();
            case BOTTOM -> !bottoms.isEmpty();
            case OUTER -> !outers.isEmpty();
        };
    }
}
