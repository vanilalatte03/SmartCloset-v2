package com.smartcloset.recommendation.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 추천 생성 command의 process-local user별 반복 호출 제한 설정이다.
 */
@ConfigurationProperties(prefix = "smartcloset.recommendation.creation-throttle")
public class RecommendationCreationThrottleProperties {

    public static final int DEFAULT_MAX_REQUESTS = 30;
    public static final Duration DEFAULT_WINDOW = Duration.ofMinutes(1);

    private boolean enabled = true;

    private int maxRequests = DEFAULT_MAX_REQUESTS;

    private Duration window = DEFAULT_WINDOW;

    public boolean enabled() {
        return enabled;
    }

    public int maxRequests() {
        return maxRequests;
    }

    public Duration window() {
        if (window == null || window.isZero() || window.isNegative()) {
            return DEFAULT_WINDOW;
        }
        return window;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxRequests() {
        return maxRequests;
    }

    public void setMaxRequests(int maxRequests) {
        this.maxRequests = maxRequests;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }
}
