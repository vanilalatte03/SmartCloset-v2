package com.smartcloset.weather.infrastructure.kma;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherType;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class KmaWeatherConditionMapperTest {

    private final KmaWeatherConditionMapper mapper = new KmaWeatherConditionMapper();

    @Test
    void mapsNearestForecastGroupAtOrAfterCurrentKst() {
        WeatherCondition weather = mapper.map(
                List.of(
                        item("20260521", "1000", "TMP", "5"),
                        item("20260521", "1000", "SKY", "4"),
                        item("20260521", "1000", "PTY", "0"),
                        item("20260521", "1000", "PCP", "1.0mm"),
                        item("20260521", "1000", "WSD", "5.0"),
                        item("20260521", "1100", "TMP", "12.6"),
                        item("20260521", "1100", "SKY", "1"),
                        item("20260521", "1100", "PTY", "0"),
                        item("20260521", "1100", "PCP", "-"),
                        item("20260521", "1100", "WSD", "3.9"),
                        item("20260521", "1200", "TMP", "3"),
                        item("20260521", "1200", "SKY", "4"),
                        item("20260521", "1200", "PTY", "3"),
                        item("20260521", "1200", "PCP", "0"),
                        item("20260521", "1200", "WSD", "4.0")
                ),
                kst("2026-05-21T10:30:00+09:00")
        );

        assertThat(weather.temperature()).isEqualTo(13);
        assertThat(weather.weatherType()).isEqualTo(WeatherType.SUNNY);
        assertThat(weather.rainy()).isFalse();
        assertThat(weather.windy()).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"1", "2", "4"})
    void mapsRainyPtyValues(String pty) {
        WeatherCondition weather = mapper.map(
                completeGroup("20260521", "1100", "16", "1", pty, "-", "3.0"),
                kst("2026-05-21T10:00:00+09:00")
        );

        assertThat(weather.weatherType()).isEqualTo(WeatherType.RAINY);
        assertThat(weather.rainy()).isTrue();
    }

    @Test
    void mapsSnowyPtyValueBeforeSkyValue() {
        WeatherCondition weather = mapper.map(
                completeGroup("20260521", "1100", "0", "1", "3", "-", "3.0"),
                kst("2026-05-21T10:00:00+09:00")
        );

        assertThat(weather.weatherType()).isEqualTo(WeatherType.SNOWY);
        assertThat(weather.rainy()).isTrue();
    }

    @Test
    void mapsCloudySkyAndPcpRainAndWindThreshold() {
        WeatherCondition weather = mapper.map(
                completeGroup("20260521", "1100", "18", "3", "0", "0.5mm", "4.0"),
                kst("2026-05-21T10:00:00+09:00")
        );

        assertThat(weather.weatherType()).isEqualTo(WeatherType.CLOUDY);
        assertThat(weather.rainy()).isTrue();
        assertThat(weather.windy()).isTrue();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"-", "null", "0", "0.0", "\uAC15\uC218\uC5C6\uC74C"})
    void treatsDocumentedPcpNoRainValuesAsNotRainy(String pcp) {
        WeatherCondition weather = mapper.map(
                completeGroup("20260521", "1100", "18", "1", "0", pcp, "3.0"),
                kst("2026-05-21T10:00:00+09:00")
        );

        assertThat(weather.rainy()).isFalse();
    }

    @Test
    void doesNotMoveToNextForecastGroupWhenSelectedGroupMissesRequiredCategory() {
        List<KmaForecastItem> items = List.of(
                item("20260521", "1100", "TMP", "18"),
                item("20260521", "1100", "SKY", "1"),
                item("20260521", "1100", "PTY", "0"),
                item("20260521", "1100", "PCP", "-"),
                item("20260521", "1200", "TMP", "18"),
                item("20260521", "1200", "SKY", "1"),
                item("20260521", "1200", "PTY", "0"),
                item("20260521", "1200", "PCP", "-"),
                item("20260521", "1200", "WSD", "3.0")
        );

        assertThatThrownBy(() -> mapper.map(items, kst("2026-05-21T10:00:00+09:00")))
                .isInstanceOf(KmaWeatherMappingException.class)
                .hasMessageContaining("WSD");
    }

    @Test
    void failsWhenThereIsNoForecastGroupAtOrAfterCurrentKst() {
        assertThatThrownBy(() -> mapper.map(
                completeGroup("20260521", "0900", "18", "1", "0", "-", "3.0"),
                kst("2026-05-21T10:00:00+09:00")
        ))
                .isInstanceOf(KmaWeatherMappingException.class)
                .hasMessageContaining("No forecast group");
    }

    private List<KmaForecastItem> completeGroup(
            String fcstDate,
            String fcstTime,
            String tmp,
            String sky,
            String pty,
            String pcp,
            String wsd
    ) {
        return List.of(
                item(fcstDate, fcstTime, "TMP", tmp),
                item(fcstDate, fcstTime, "SKY", sky),
                item(fcstDate, fcstTime, "PTY", pty),
                item(fcstDate, fcstTime, "PCP", pcp),
                item(fcstDate, fcstTime, "WSD", wsd)
        );
    }

    private KmaForecastItem item(String fcstDate, String fcstTime, String category, String fcstValue) {
        return new KmaForecastItem(fcstDate, fcstTime, category, fcstValue);
    }

    private ZonedDateTime kst(String value) {
        return ZonedDateTime.parse(value);
    }
}
