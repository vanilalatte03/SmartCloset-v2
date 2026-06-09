package com.smartcloset.clothing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartcloset.clothing.infrastructure.analysis.ClothingAnalysisProperties;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ClothingAnalysisDailyLimiterTest {

    private ExecutorService executorService;

    @AfterEach
    void cleanup() {
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    @Test
    void cleanupRemovesPreviousDateCountersAndKeepsCurrentDateCounter() {
        ClothingAnalysisProperties properties = new ClothingAnalysisProperties();
        MutableClock clock = MutableClock.fixed("2026-06-08T10:00:00+09:00");
        ClothingAnalysisDailyLimiter limiter = new ClothingAnalysisDailyLimiter(properties, clock);

        limiter.checkAndIncrement(1L);
        limiter.checkAndIncrement(2L);
        assertThat(limiter.counterSize()).isEqualTo(2);

        clock.advance(Duration.ofDays(1));
        limiter.checkAndIncrement(1L);

        assertThat(limiter.counterSize()).isEqualTo(1);
        assertThat(limiter.hasCounterFor(1L, LocalDate.parse("2026-06-09"))).isTrue();
        assertThat(limiter.hasCounterFor(1L, LocalDate.parse("2026-06-08"))).isFalse();
        assertThat(limiter.hasCounterFor(2L, LocalDate.parse("2026-06-08"))).isFalse();
    }

    @Test
    void sameDateLimitBehaviorIsPreserved() {
        ClothingAnalysisProperties properties = new ClothingAnalysisProperties();
        properties.setDailyLimit(1);
        ClothingAnalysisDailyLimiter limiter = new ClothingAnalysisDailyLimiter(
                properties,
                MutableClock.fixed("2026-06-08T10:00:00+09:00")
        );

        limiter.checkAndIncrement(1L);

        assertThatThrownBy(() -> limiter.checkAndIncrement(1L))
                .isInstanceOfSatisfying(SmartClosetException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.CLOTHING_ANALYSIS_LIMIT_EXCEEDED));
        limiter.checkAndIncrement(2L);
    }

    @Test
    void nonPositiveDailyLimitStillFailsWithoutIncrementingCounter() {
        ClothingAnalysisProperties properties = new ClothingAnalysisProperties();
        properties.setDailyLimit(0);
        ClothingAnalysisDailyLimiter limiter = new ClothingAnalysisDailyLimiter(
                properties,
                MutableClock.fixed("2026-06-08T10:00:00+09:00")
        );

        assertThatThrownBy(() -> limiter.checkAndIncrement(1L))
                .isInstanceOfSatisfying(SmartClosetException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.CLOTHING_ANALYSIS_LIMIT_EXCEEDED));
        assertThat(limiter.counterSize()).isZero();
    }

    @Test
    void concurrentCleanupKeepsCurrentDateLimitAtomic() throws Exception {
        ClothingAnalysisProperties properties = new ClothingAnalysisProperties();
        properties.setDailyLimit(1);
        MutableClock clock = MutableClock.fixed("2026-06-08T10:00:00+09:00");
        ClothingAnalysisDailyLimiter limiter = new ClothingAnalysisDailyLimiter(properties, clock);
        limiter.checkAndIncrement(99L);
        clock.advance(Duration.ofDays(1));

        executorService = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Future<LimitAttempt> first = executorService.submit(limitAttempt(limiter, ready, start));
        Future<LimitAttempt> second = executorService.submit(limitAttempt(limiter, ready, start));

        assertThat(ready.await(3, TimeUnit.SECONDS)).isTrue();
        start.countDown();

        List<LimitAttempt> attempts = List.of(first.get(3, TimeUnit.SECONDS), second.get(3, TimeUnit.SECONDS));
        assertThat(attempts).filteredOn(LimitAttempt::success).hasSize(1);
        assertThat(attempts).filteredOn(attempt -> !attempt.success())
                .singleElement()
                .satisfies(attempt -> assertThat(attempt.errorCode())
                        .isEqualTo(ErrorCode.CLOTHING_ANALYSIS_LIMIT_EXCEEDED));
        assertThat(limiter.hasCounterFor(99L, LocalDate.parse("2026-06-08"))).isFalse();
        assertThat(limiter.hasCounterFor(1L, LocalDate.parse("2026-06-09"))).isTrue();
    }

    private Callable<LimitAttempt> limitAttempt(
            ClothingAnalysisDailyLimiter limiter,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        return () -> {
            ready.countDown();
            if (!start.await(3, TimeUnit.SECONDS)) {
                throw new IllegalStateException("concurrent limit start signal timed out");
            }
            try {
                limiter.checkAndIncrement(1L);
                return LimitAttempt.succeeded();
            } catch (SmartClosetException exception) {
                return LimitAttempt.failure(exception.errorCode());
            }
        };
    }

    private record LimitAttempt(boolean success, ErrorCode errorCode) {

        static LimitAttempt succeeded() {
            return new LimitAttempt(true, null);
        }

        static LimitAttempt failure(ErrorCode errorCode) {
            return new LimitAttempt(false, errorCode);
        }
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
