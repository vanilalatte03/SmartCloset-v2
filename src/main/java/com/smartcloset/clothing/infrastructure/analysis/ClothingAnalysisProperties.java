package com.smartcloset.clothing.infrastructure.analysis;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 옷 사진 분석 기능의 활성화와 비용 방어 설정을 환경별로 바인딩한다.
 */
@ConfigurationProperties(prefix = "smartcloset.clothing.analysis")
public class ClothingAnalysisProperties {

    public static final double DEFAULT_LOW_CONFIDENCE_THRESHOLD = 0.75;
    public static final int DEFAULT_DAILY_LIMIT = 20;
    public static final int DEFAULT_TIMEOUT_SECONDS = 10;
    public static final int DEFAULT_MAX_ATTEMPTS = 2;
    public static final Duration DEFAULT_RETRY_BACKOFF = Duration.ofMillis(200);
    public static final int DEFAULT_CIRCUIT_BREAKER_FAILURE_THRESHOLD = 3;
    public static final Duration DEFAULT_CIRCUIT_BREAKER_OPEN_DURATION = Duration.ofSeconds(30);

    private boolean enabled = false;

    private double lowConfidenceThreshold = DEFAULT_LOW_CONFIDENCE_THRESHOLD;

    private int dailyLimit = DEFAULT_DAILY_LIMIT;

    private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

    private Duration retryBackoff = DEFAULT_RETRY_BACKOFF;

    private int circuitBreakerFailureThreshold = DEFAULT_CIRCUIT_BREAKER_FAILURE_THRESHOLD;

    private Duration circuitBreakerOpenDuration = DEFAULT_CIRCUIT_BREAKER_OPEN_DURATION;

    public boolean enabled() {
        return enabled;
    }

    public double lowConfidenceThreshold() {
        return lowConfidenceThreshold;
    }

    public int dailyLimit() {
        return dailyLimit;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    public int maxAttempts() {
        return maxAttempts;
    }

    public Duration retryBackoff() {
        return retryBackoff;
    }

    public int circuitBreakerFailureThreshold() {
        return circuitBreakerFailureThreshold;
    }

    public Duration circuitBreakerOpenDuration() {
        return circuitBreakerOpenDuration;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public double getLowConfidenceThreshold() {
        return lowConfidenceThreshold;
    }

    public void setLowConfidenceThreshold(double lowConfidenceThreshold) {
        this.lowConfidenceThreshold = lowConfidenceThreshold;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public Duration getRetryBackoff() {
        return retryBackoff;
    }

    public void setRetryBackoff(Duration retryBackoff) {
        this.retryBackoff = retryBackoff == null ? DEFAULT_RETRY_BACKOFF : retryBackoff;
    }

    public int getCircuitBreakerFailureThreshold() {
        return circuitBreakerFailureThreshold;
    }

    public void setCircuitBreakerFailureThreshold(int circuitBreakerFailureThreshold) {
        this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold;
    }

    public Duration getCircuitBreakerOpenDuration() {
        return circuitBreakerOpenDuration;
    }

    public void setCircuitBreakerOpenDuration(Duration circuitBreakerOpenDuration) {
        this.circuitBreakerOpenDuration = circuitBreakerOpenDuration == null
                ? DEFAULT_CIRCUIT_BREAKER_OPEN_DURATION
                : circuitBreakerOpenDuration;
    }
}
