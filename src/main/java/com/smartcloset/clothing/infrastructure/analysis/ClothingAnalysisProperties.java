package com.smartcloset.clothing.infrastructure.analysis;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 옷 사진 분석 기능의 활성화와 비용 방어 설정을 환경별로 바인딩한다.
 */
@ConfigurationProperties(prefix = "smartcloset.clothing.analysis")
public class ClothingAnalysisProperties {

    public static final double DEFAULT_LOW_CONFIDENCE_THRESHOLD = 0.75;
    public static final int DEFAULT_DAILY_LIMIT = 20;
    public static final int DEFAULT_TIMEOUT_SECONDS = 10;

    private boolean enabled = false;

    private double lowConfidenceThreshold = DEFAULT_LOW_CONFIDENCE_THRESHOLD;

    private int dailyLimit = DEFAULT_DAILY_LIMIT;

    private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

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
}
