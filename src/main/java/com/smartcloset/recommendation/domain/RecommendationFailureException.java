package com.smartcloset.recommendation.domain;

import java.util.Objects;

public class RecommendationFailureException extends RuntimeException {

    private final RecommendationFailureCode failureCode;

    public RecommendationFailureException(RecommendationFailureCode failureCode) {
        super(failureCode.name());
        this.failureCode = Objects.requireNonNull(failureCode, "failureCode must not be null");
    }

    /**
     * 추천 생성 실패를 API error code로 매핑할 때 사용할 domain failure code를 반환한다.
     */
    public RecommendationFailureCode failureCode() {
        return failureCode;
    }
}
