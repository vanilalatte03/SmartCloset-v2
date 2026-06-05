package com.smartcloset.clothing.application;

import com.smartcloset.clothing.infrastructure.analysis.ClothingAnalysisProperties;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * MVP10 범위의 user별 in-memory 분석 호출 제한이다.
 */
@Component
public class ClothingAnalysisDailyLimiter {

    private final ClothingAnalysisProperties properties;
    private final Clock clock;
    private final ConcurrentMap<DailyLimitKey, AtomicInteger> counters = new ConcurrentHashMap<>();

    @Autowired
    public ClothingAnalysisDailyLimiter(ClothingAnalysisProperties properties) {
        this(properties, Clock.systemDefaultZone());
    }

    ClothingAnalysisDailyLimiter(ClothingAnalysisProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    public void checkAndIncrement(Long userId) {
        int dailyLimit = properties.dailyLimit();
        if (dailyLimit < 1) {
            throw new SmartClosetException(ErrorCode.CLOTHING_ANALYSIS_LIMIT_EXCEEDED);
        }

        DailyLimitKey key = new DailyLimitKey(userId, LocalDate.now(clock));
        int count = counters.computeIfAbsent(key, ignored -> new AtomicInteger()).incrementAndGet();
        if (count > dailyLimit) {
            throw new SmartClosetException(ErrorCode.CLOTHING_ANALYSIS_LIMIT_EXCEEDED);
        }
    }

    private record DailyLimitKey(Long userId, LocalDate date) {
    }
}
