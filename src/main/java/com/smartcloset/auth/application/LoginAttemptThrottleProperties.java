package com.smartcloset.auth.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공개 password login endpoint의 process-local 실패 시도 제한 설정이다.
 */
@ConfigurationProperties(prefix = "smartcloset.security.login-attempt")
public class LoginAttemptThrottleProperties {

    public static final int DEFAULT_MAX_FAILURES = 5;
    public static final Duration DEFAULT_WINDOW = Duration.ofMinutes(15);

    private boolean enabled = true;

    private int maxFailures = DEFAULT_MAX_FAILURES;

    private Duration window = DEFAULT_WINDOW;

    public boolean enabled() {
        return enabled;
    }

    public int maxFailures() {
        return maxFailures;
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

    public int getMaxFailures() {
        return maxFailures;
    }

    public void setMaxFailures(int maxFailures) {
        this.maxFailures = maxFailures;
    }

    public Duration getWindow() {
        return window;
    }

    public void setWindow(Duration window) {
        this.window = window;
    }
}
