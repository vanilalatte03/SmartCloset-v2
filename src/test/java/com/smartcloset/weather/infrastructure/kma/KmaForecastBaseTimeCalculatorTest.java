package com.smartcloset.weather.infrastructure.kma;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class KmaForecastBaseTimeCalculatorTest {

    private final KmaForecastBaseTimeCalculator calculator = new KmaForecastBaseTimeCalculator();

    @Test
    void selectsPreviousDayLastReleaseBeforeFirstReleaseBecomesAvailable() {
        KmaForecastBaseTime baseTime = calculator.calculate(kst("2026-05-21T00:05:00+09:00"));

        assertThat(baseTime.baseDate()).isEqualTo("20260520");
        assertThat(baseTime.baseTime()).isEqualTo("2300");
    }

    @Test
    void waitsUntilTenMinutesAfterReleaseTime() {
        KmaForecastBaseTime beforeAvailable = calculator.calculate(kst("2026-05-21T02:09:59+09:00"));
        KmaForecastBaseTime available = calculator.calculate(kst("2026-05-21T02:10:00+09:00"));

        assertThat(beforeAvailable.baseDate()).isEqualTo("20260520");
        assertThat(beforeAvailable.baseTime()).isEqualTo("2300");
        assertThat(available.baseDate()).isEqualTo("20260521");
        assertThat(available.baseTime()).isEqualTo("0200");
    }

    @Test
    void selectsLatestAvailableReleaseThroughTheDay() {
        KmaForecastBaseTime beforeFourteen = calculator.calculate(kst("2026-05-21T14:09:00+09:00"));
        KmaForecastBaseTime afterFourteen = calculator.calculate(kst("2026-05-21T14:10:00+09:00"));

        assertThat(beforeFourteen.baseDate()).isEqualTo("20260521");
        assertThat(beforeFourteen.baseTime()).isEqualTo("1100");
        assertThat(afterFourteen.baseDate()).isEqualTo("20260521");
        assertThat(afterFourteen.baseTime()).isEqualTo("1400");
    }

    @Test
    void normalizesInputInstantToKst() {
        KmaForecastBaseTime baseTime = calculator.calculate(
                ZonedDateTime.parse("2026-05-20T17:10:00Z").withZoneSameInstant(ZoneId.of("UTC"))
        );

        assertThat(baseTime.baseDate()).isEqualTo("20260521");
        assertThat(baseTime.baseTime()).isEqualTo("0200");
    }

    private ZonedDateTime kst(String value) {
        return ZonedDateTime.parse(value);
    }
}
