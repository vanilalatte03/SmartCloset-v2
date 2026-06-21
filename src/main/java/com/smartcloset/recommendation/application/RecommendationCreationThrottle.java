package com.smartcloset.recommendation.application;

import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.common.observability.SmartClosetMetrics;
import com.smartcloset.recommendation.domain.RecommendationSituation;
import com.smartcloset.weather.domain.ForecastPeriod;
import io.micrometer.core.instrument.Timer;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 추천 생성 command의 user별 process-local fixed-window throttle이다.
 *
 * <p>Redis나 DB-backed limiter는 후속 운영 adapter 범위로 두고, 현재 process 안에서 반복 호출 부하를
 * 먼저 제한한다.</p>
 */
@Component
public class RecommendationCreationThrottle {

    private final RecommendationCreationThrottleProperties properties;
    private final SmartClosetMetrics metrics;
    private final Clock clock;
    private final ConcurrentMap<Long, RequestWindow> requests = new ConcurrentHashMap<>();

    @Autowired
    public RecommendationCreationThrottle(
            RecommendationCreationThrottleProperties properties,
            SmartClosetMetrics metrics
    ) {
        this(properties, metrics, Clock.systemDefaultZone());
    }

    RecommendationCreationThrottle(RecommendationCreationThrottleProperties properties, Clock clock) {
        this(properties, SmartClosetMetrics.noop(), clock);
    }

    RecommendationCreationThrottle(
            RecommendationCreationThrottleProperties properties,
            SmartClosetMetrics metrics,
            Clock clock
    ) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
        this.metrics = Objects.requireNonNull(metrics, "metrics must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    /**
     * 추천 생성 비용을 쓰기 전에 user별 window에 현재 요청을 기록하고, 초과 시 stable 429 error로 실패한다.
     */
    public void checkAndRecord(Long userId) {
        checkAndRecord(userId, RecommendationSituation.CASUAL, ForecastPeriod.CURRENT);
    }

    /**
     * 추천 생성 비용을 쓰기 전에 user별 window에 현재 요청을 기록하고, 초과 시 stable 429 error와 metric을 남긴다.
     */
    public void checkAndRecord(
            Long userId,
            RecommendationSituation situation,
            ForecastPeriod forecastPeriod
    ) {
        if (!properties.enabled()) {
            return;
        }

        int maxRequests = properties.maxRequests();
        if (maxRequests < 1) {
            throwLimitExceeded(situation, forecastPeriod);
        }

        Instant now = clock.instant();
        Duration window = properties.window();
        cleanupExpiredRequests(now, window);

        RequestWindow updated = requests.compute(userId, (ignored, current) -> {
            if (current == null || current.isExpired(now, window)) {
                return new RequestWindow(now, 1);
            }
            return current.increment();
        });
        if (updated.requests() > maxRequests) {
            throwLimitExceeded(situation, forecastPeriod);
        }
    }

    int requestCountFor(Long userId) {
        RequestWindow window = requests.get(userId);
        return window == null ? 0 : window.requests();
    }

    private void cleanupExpiredRequests(Instant now, Duration window) {
        requests.entrySet().removeIf(entry -> entry.getValue().isExpired(now, window));
    }

    private void throwLimitExceeded(RecommendationSituation situation, ForecastPeriod forecastPeriod) {
        Timer.Sample sample = metrics.startTimer();
        metrics.recordRecommendation(sample, situation, forecastPeriod, "limit");
        throw new SmartClosetException(ErrorCode.RECOMMENDATION_CREATION_LIMIT_EXCEEDED);
    }

    private record RequestWindow(Instant startedAt, int requests) {

        private boolean isExpired(Instant now, Duration window) {
            return !now.isBefore(startedAt.plus(window));
        }

        private RequestWindow increment() {
            return new RequestWindow(startedAt, requests + 1);
        }
    }
}
