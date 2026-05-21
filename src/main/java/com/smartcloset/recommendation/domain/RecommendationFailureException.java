package com.smartcloset.recommendation.domain;

import java.util.Objects;

public class RecommendationFailureException extends RuntimeException {

    private final RecommendationFailureCode failureCode;

    public RecommendationFailureException(RecommendationFailureCode failureCode) {
        super(failureCode.name());
        this.failureCode = Objects.requireNonNull(failureCode, "failureCode must not be null");
    }

    public RecommendationFailureCode failureCode() {
        return failureCode;
    }
}
