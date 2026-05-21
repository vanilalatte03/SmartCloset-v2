package com.smartcloset.weather.infrastructure.kma;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;

public class KmaForecastBaseTimeCalculator {

    public static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private static final DateTimeFormatter BASE_DATE_FORMATTER = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter BASE_TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");
    private static final List<LocalTime> RELEASE_TIMES = List.of(
            LocalTime.of(2, 0),
            LocalTime.of(5, 0),
            LocalTime.of(8, 0),
            LocalTime.of(11, 0),
            LocalTime.of(14, 0),
            LocalTime.of(17, 0),
            LocalTime.of(20, 0),
            LocalTime.of(23, 0)
    );

    public KmaForecastBaseTime calculate(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        return calculate(ZonedDateTime.now(clock));
    }

    public KmaForecastBaseTime calculate(ZonedDateTime now) {
        ZonedDateTime nowKst = Objects.requireNonNull(now, "now must not be null")
                .withZoneSameInstant(KST_ZONE);
        LocalDate baseDate = nowKst.toLocalDate();
        LocalTime currentTime = nowKst.toLocalTime();

        for (int index = RELEASE_TIMES.size() - 1; index >= 0; index--) {
            LocalTime releaseTime = RELEASE_TIMES.get(index);
            if (!currentTime.isBefore(releaseTime.plusMinutes(10))) {
                return new KmaForecastBaseTime(
                        baseDate.format(BASE_DATE_FORMATTER),
                        releaseTime.format(BASE_TIME_FORMATTER)
                );
            }
        }

        LocalTime previousDayLastRelease = RELEASE_TIMES.getLast();
        return new KmaForecastBaseTime(
                baseDate.minusDays(1).format(BASE_DATE_FORMATTER),
                previousDayLastRelease.format(BASE_TIME_FORMATTER)
        );
    }
}
