package com.smartcloset.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class LoginAttemptThrottleTest {

    @Test
    void repeatedAttemptsForSameNormalizedEmailAndClientAreLimited() {
        LoginAttemptThrottle throttle = throttle(maxFailures(2), MutableClock.fixed("2026-06-17T10:00:00+09:00"));

        throttle.checkAndRecordAttempt(" USER@example.COM ", " 203.0.113.10 ");
        throttle.checkAndRecordAttempt("user@example.com", "203.0.113.10");

        assertThat(throttle.attemptCountFor("user@example.com", "203.0.113.10")).isEqualTo(2);
        assertThatThrownBy(() -> throttle.checkAndRecordAttempt("user@example.com", "203.0.113.10"))
                .isInstanceOfSatisfying(SmartClosetException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.LOGIN_ATTEMPT_LIMIT_EXCEEDED));

        assertThatCode(() -> throttle.checkAndRecordAttempt("user@example.com", "203.0.113.11"))
                .doesNotThrowAnyException();
    }

    @Test
    void repeatedAttemptsForDifferentEmailsFromSameClientAreLimited() {
        LoginAttemptThrottle throttle = throttle(maxFailures(2), MutableClock.fixed("2026-06-17T10:00:00+09:00"));

        throttle.checkAndRecordAttempt("first@example.com", "203.0.113.12");
        throttle.checkAndRecordAttempt("second@example.com", "203.0.113.12");

        assertThat(throttle.clientAttemptCountFor("203.0.113.12")).isEqualTo(2);
        assertThatThrownBy(() -> throttle.checkAndRecordAttempt("third@example.com", "203.0.113.12"))
                .isInstanceOfSatisfying(SmartClosetException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.LOGIN_ATTEMPT_LIMIT_EXCEEDED));
    }

    @Test
    void expiredWindowAllowsAttemptsAgain() {
        LoginAttemptThrottleProperties properties = maxFailures(2);
        MutableClock clock = MutableClock.fixed("2026-06-17T10:00:00+09:00");
        LoginAttemptThrottle throttle = throttle(properties, clock);

        throttle.checkAndRecordAttempt("window@example.com", "203.0.113.20");
        throttle.checkAndRecordAttempt("window@example.com", "203.0.113.20");
        assertThatThrownBy(() -> throttle.checkAndRecordAttempt("window@example.com", "203.0.113.20"))
                .isInstanceOf(SmartClosetException.class);

        clock.advance(properties.window());

        assertThatCode(() -> throttle.checkAndRecordAttempt("window@example.com", "203.0.113.20"))
                .doesNotThrowAnyException();
        assertThat(throttle.attemptCountFor("window@example.com", "203.0.113.20")).isEqualTo(1);
    }

    @Test
    void successfulLoginClearsEmailAttemptsAndKeepsPreviousClientFailures() {
        LoginAttemptThrottle throttle = throttle(maxFailures(2), MutableClock.fixed("2026-06-17T10:00:00+09:00"));

        throttle.checkAndRecordAttempt("success@example.com", "203.0.113.30");
        throttle.checkAndRecordAttempt("success@example.com", "203.0.113.30");
        throttle.recordSuccess("success@example.com", "203.0.113.30");

        assertThat(throttle.attemptCountFor("success@example.com", "203.0.113.30")).isZero();
        assertThat(throttle.clientAttemptCountFor("203.0.113.30")).isOne();
        throttle.checkAndRecordAttempt("other@example.com", "203.0.113.30");
        assertThatThrownBy(() -> throttle.checkAndRecordAttempt("third@example.com", "203.0.113.30"))
                .isInstanceOfSatisfying(SmartClosetException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.LOGIN_ATTEMPT_LIMIT_EXCEEDED));
    }

    @Test
    void rollbackAttemptRemovesOnlyCurrentReservedAttempt() {
        LoginAttemptThrottle throttle = throttle(maxFailures(2), MutableClock.fixed("2026-06-17T10:00:00+09:00"));

        throttle.checkAndRecordAttempt("rollback@example.com", "203.0.113.31");
        throttle.checkAndRecordAttempt("rollback@example.com", "203.0.113.31");
        throttle.rollbackAttempt("rollback@example.com", "203.0.113.31");

        assertThat(throttle.attemptCountFor("rollback@example.com", "203.0.113.31")).isEqualTo(1);
        assertThat(throttle.clientAttemptCountFor("203.0.113.31")).isEqualTo(1);
    }

    @Test
    void disabledThrottleDoesNotRecordAttempts() {
        LoginAttemptThrottleProperties properties = maxFailures(1);
        properties.setEnabled(false);
        LoginAttemptThrottle throttle = throttle(properties, MutableClock.fixed("2026-06-17T10:00:00+09:00"));

        throttle.checkAndRecordAttempt("disabled@example.com", "203.0.113.40");
        throttle.checkAndRecordAttempt("disabled@example.com", "203.0.113.40");

        assertThat(throttle.attemptCountFor("disabled@example.com", "203.0.113.40")).isZero();
    }

    private LoginAttemptThrottleProperties maxFailures(int maxFailures) {
        LoginAttemptThrottleProperties properties = new LoginAttemptThrottleProperties();
        properties.setMaxFailures(maxFailures);
        properties.setWindow(Duration.ofMinutes(5));
        return properties;
    }

    private LoginAttemptThrottle throttle(LoginAttemptThrottleProperties properties, Clock clock) {
        return new LoginAttemptThrottle(properties, clock);
    }

    private static final class MutableClock extends Clock {

        private final ZoneId zone;
        private Instant instant;

        private MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        private static MutableClock fixed(String value) {
            ZonedDateTime dateTime = ZonedDateTime.parse(value);
            return new MutableClock(dateTime.toInstant(), dateTime.getZone());
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
