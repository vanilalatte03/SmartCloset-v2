package com.smartcloset.recommendation.application;

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

class RecommendationCreationThrottleTest {

    @Test
    void repeatedRequestsForSameUserAreLimitedWithinWindow() {
        RecommendationCreationThrottle throttle = throttle(
                maxRequests(2),
                MutableClock.fixed("2026-06-21T10:00:00+09:00")
        );

        throttle.checkAndRecord(1L);
        throttle.checkAndRecord(1L);

        assertThat(throttle.requestCountFor(1L)).isEqualTo(2);
        assertThatThrownBy(() -> throttle.checkAndRecord(1L))
                .isInstanceOfSatisfying(SmartClosetException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.RECOMMENDATION_CREATION_LIMIT_EXCEEDED));
        assertThatCode(() -> throttle.checkAndRecord(2L)).doesNotThrowAnyException();
    }

    @Test
    void expiredWindowAllowsRequestsAgain() {
        RecommendationCreationThrottleProperties properties = maxRequests(1);
        MutableClock clock = MutableClock.fixed("2026-06-21T10:00:00+09:00");
        RecommendationCreationThrottle throttle = throttle(properties, clock);

        throttle.checkAndRecord(3L);
        assertThatThrownBy(() -> throttle.checkAndRecord(3L)).isInstanceOf(SmartClosetException.class);

        clock.advance(properties.window());

        assertThatCode(() -> throttle.checkAndRecord(3L)).doesNotThrowAnyException();
        assertThat(throttle.requestCountFor(3L)).isEqualTo(1);
    }

    @Test
    void disabledThrottleDoesNotRecordRequests() {
        RecommendationCreationThrottleProperties properties = maxRequests(1);
        properties.setEnabled(false);
        RecommendationCreationThrottle throttle = throttle(
                properties,
                MutableClock.fixed("2026-06-21T10:00:00+09:00")
        );

        throttle.checkAndRecord(4L);
        throttle.checkAndRecord(4L);

        assertThat(throttle.requestCountFor(4L)).isZero();
    }

    private RecommendationCreationThrottleProperties maxRequests(int maxRequests) {
        RecommendationCreationThrottleProperties properties = new RecommendationCreationThrottleProperties();
        properties.setMaxRequests(maxRequests);
        properties.setWindow(Duration.ofMinutes(5));
        return properties;
    }

    private RecommendationCreationThrottle throttle(RecommendationCreationThrottleProperties properties, Clock clock) {
        return new RecommendationCreationThrottle(properties, clock);
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
