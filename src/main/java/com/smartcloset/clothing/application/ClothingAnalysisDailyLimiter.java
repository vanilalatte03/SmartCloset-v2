package com.smartcloset.clothing.application;

import com.smartcloset.clothing.infrastructure.analysis.ClothingAnalysisProperties;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MVP10 범위의 user별 in-memory 분석 호출 제한이다.
 *
 * <p>Redis나 영구 저장소를 추가하지 않는 MVP10 결정에 따라 application process 안에서 날짜별 카운트를
 * 유지한다. 분산/재시작 내구성은 후속 운영 adapter 범위다.</p>
 */
@Component
public class ClothingAnalysisDailyLimiter {

    private final ClothingAnalysisProperties properties;
    private final Clock clock;
    private final ConcurrentMap<DailyLimitKey, AtomicInteger> counters = new ConcurrentHashMap<>();
    private final AtomicReference<LocalDate> lastCleanupDate = new AtomicReference<>(LocalDate.MIN);

    @Autowired
    public ClothingAnalysisDailyLimiter(ClothingAnalysisProperties properties) {
        this(properties, Clock.systemDefaultZone());
    }

    ClothingAnalysisDailyLimiter(ClothingAnalysisProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * 현재 날짜의 사용자별 분석 요청 수를 증가시키고 daily limit 초과 시 stable error code로 실패한다.
     */
    public void checkAndIncrement(Long userId) {
        LocalDate today = LocalDate.now(clock);
        cleanupExpiredCounters(today);

        int dailyLimit = properties.dailyLimit();
        if (dailyLimit < 1) {
            throw new SmartClosetException(ErrorCode.CLOTHING_ANALYSIS_LIMIT_EXCEEDED);
        }

        while (true) {
            DailyLimitKey key = new DailyLimitKey(userId, latestObservedDate(today));
            AtomicInteger counter = counters.computeIfAbsent(key, ignored -> new AtomicInteger());
            int count = counter.incrementAndGet();
            if (removeIfExpiredByCompletedCleanup(key, counter)) {
                today = LocalDate.now(clock);
                cleanupExpiredCounters(today);
                continue;
            }
            if (count > dailyLimit) {
                throw new SmartClosetException(ErrorCode.CLOTHING_ANALYSIS_LIMIT_EXCEEDED);
            }
            return;
        }
    }

    int counterSize() {
        return counters.size();
    }

    boolean hasCounterFor(Long userId, LocalDate date) {
        return counters.containsKey(new DailyLimitKey(userId, date));
    }

    private void cleanupExpiredCounters(LocalDate today) {
        while (true) {
            LocalDate lastCleanup = lastCleanupDate.get();
            if (!today.isAfter(lastCleanup)) {
                return;
            }
            if (lastCleanupDate.compareAndSet(lastCleanup, today)) {
                break;
            }
        }
        counters.keySet().removeIf(key -> key.date().isBefore(today));
    }

    private LocalDate latestObservedDate(LocalDate today) {
        LocalDate lastCleanup = lastCleanupDate.get();
        if (today.isBefore(lastCleanup)) {
            return lastCleanup;
        }
        return today;
    }

    private boolean removeIfExpiredByCompletedCleanup(DailyLimitKey key, AtomicInteger counter) {
        if (key.date().isBefore(lastCleanupDate.get())) {
            counters.remove(key, counter);
            return true;
        }
        return false;
    }

    private record DailyLimitKey(Long userId, LocalDate date) {
    }
}
