package com.smartcloset.weather.infrastructure.kma;

import com.smartcloset.weather.domain.ForecastPeriod;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherType;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * KMA forecast item 목록을 SmartCloset의 간단한 날씨 모델로 매핑한다.
 *
 * <p>KMA 응답은 TMP/SKY/PTY/PCP/WSD처럼 category별 row로 내려오므로, 먼저 forecast
 * date/time으로 묶은 뒤 필요한 category가 모두 있는 시간대를 선택한다.</p>
 */
public class KmaWeatherConditionMapper {

    private static final List<String> REQUIRED_CATEGORIES = List.of("TMP", "SKY", "PTY", "PCP", "WSD");
    private static final DateTimeFormatter FORECAST_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?\\d+(?:\\.\\d+)?");
    private static final String PCP_NO_PRECIPITATION = "\uAC15\uC218\uC5C6\uC74C";

    /**
     * 요청한 예보 시간대에 맞는 KMA forecast group을 골라 내부 날씨 조건으로 변환한다.
     */
    public KmaMappedWeather map(List<KmaForecastItem> items, ZonedDateTime now, ForecastPeriod forecastPeriod) {
        Objects.requireNonNull(items, "items must not be null");
        if (items.isEmpty()) {
            throw new KmaWeatherMappingException("KMA forecast items are empty");
        }

        LocalDateTime nowKst = Objects.requireNonNull(now, "now must not be null")
                .withZoneSameInstant(KmaForecastBaseTimeCalculator.KST_ZONE)
                .toLocalDateTime();
        ForecastPeriod resolvedForecastPeriod = Objects.requireNonNull(
                forecastPeriod,
                "forecastPeriod must not be null"
        );
        Map<LocalDateTime, Map<String, String>> groups = groupByForecastTime(items);
        Map.Entry<LocalDateTime, Map<String, String>> selectedEntry = selectForecastGroup(
                groups,
                nowKst,
                resolvedForecastPeriod
        );
        Map<String, String> selectedGroup = selectedEntry.getValue();

        validateRequiredCategories(selectedGroup);

        int temperature = parseRoundedInteger("TMP", selectedGroup.get("TMP"));
        int pty = parseInteger("PTY", selectedGroup.get("PTY"));
        int sky = parseInteger("SKY", selectedGroup.get("SKY"));
        boolean rainy = pty != 0 || hasPrecipitation(selectedGroup.get("PCP"));
        boolean windy = parseDouble("WSD", selectedGroup.get("WSD")) >= 4.0;

        WeatherCondition condition = WeatherCondition.of(temperature, mapWeatherType(pty, sky), rainy, windy);
        LocalDateTime forecastDateTime = selectedEntry.getKey();
        return new KmaMappedWeather(
                condition,
                forecastDateTime.format(DateTimeFormatter.BASIC_ISO_DATE),
                forecastDateTime.format(DateTimeFormatter.ofPattern("HHmm"))
        );
    }

    /**
     * CURRENT forecast period 기준으로 KMA forecast item을 내부 날씨 조건으로 변환한다.
     */
    public KmaMappedWeather map(List<KmaForecastItem> items, ZonedDateTime now) {
        return map(items, now, ForecastPeriod.CURRENT);
    }

    private Map<LocalDateTime, Map<String, String>> groupByForecastTime(List<KmaForecastItem> items) {
        Map<LocalDateTime, Map<String, String>> groups = new TreeMap<>();
        for (KmaForecastItem item : items) {
            LocalDateTime forecastDateTime = parseForecastDateTime(item);
            groups.computeIfAbsent(forecastDateTime, ignored -> new LinkedHashMap<>())
                    .put(item.category(), item.fcstValue());
        }
        return groups;
    }

    private LocalDateTime parseForecastDateTime(KmaForecastItem item) {
        try {
            return LocalDateTime.parse(item.fcstDate() + item.fcstTime(), FORECAST_DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            throw new KmaWeatherMappingException(
                    "Invalid forecast date/time: " + item.fcstDate() + " " + item.fcstTime(),
                    exception
            );
        }
    }

    /**
     * CURRENT는 현재 이후 첫 예보를, 고정 시간대는 그 시간 이후 첫 예보를 우선 선택한다.
     */
    private Map.Entry<LocalDateTime, Map<String, String>> selectForecastGroup(
            Map<LocalDateTime, Map<String, String>> groups,
            LocalDateTime nowKst,
            ForecastPeriod forecastPeriod
    ) {
        return switch (forecastPeriod) {
            case CURRENT -> groups.entrySet()
                    .stream()
                    .filter(entry -> !entry.getKey().isBefore(nowKst))
                    .findFirst()
                    .orElseThrow(() -> new KmaWeatherMappingException(
                            "No forecast group exists at or after current KST time"
                    ));
            case MORNING, AFTERNOON, EVENING -> selectTargetPeriodGroup(groups, nowKst.toLocalDate(), forecastPeriod);
        };
    }

    private Map.Entry<LocalDateTime, Map<String, String>> selectTargetPeriodGroup(
            Map<LocalDateTime, Map<String, String>> groups,
            LocalDate today,
            ForecastPeriod forecastPeriod
    ) {
        LocalDateTime targetDateTime = LocalDateTime.of(today, targetTime(forecastPeriod));
        return groups.entrySet()
                .stream()
                .filter(entry -> entry.getKey().toLocalDate().equals(today))
                .filter(entry -> !entry.getKey().isBefore(targetDateTime))
                .findFirst()
                .or(() -> groups.entrySet()
                        .stream()
                        .filter(entry -> entry.getKey().toLocalDate().equals(today))
                        .filter(entry -> entry.getKey().isBefore(targetDateTime))
                        .reduce((previous, current) -> current))
                .orElseThrow(() -> new KmaWeatherMappingException(
                        "No forecast group exists for " + forecastPeriod + " on current KST date"
                ));
    }

    private java.time.LocalTime targetTime(ForecastPeriod forecastPeriod) {
        return switch (forecastPeriod) {
            case MORNING -> java.time.LocalTime.of(9, 0);
            case AFTERNOON -> java.time.LocalTime.of(15, 0);
            case EVENING -> java.time.LocalTime.of(21, 0);
            case CURRENT -> throw new IllegalArgumentException("CURRENT does not have a fixed target time");
        };
    }

    private void validateRequiredCategories(Map<String, String> group) {
        List<String> missingCategories = REQUIRED_CATEGORIES.stream()
                .filter(category -> !group.containsKey(category))
                .toList();
        if (!missingCategories.isEmpty()) {
            throw new KmaWeatherMappingException("Missing required KMA categories: " + missingCategories);
        }
    }

    private WeatherType mapWeatherType(int pty, int sky) {
        return switch (pty) {
            case 1, 2, 4 -> WeatherType.RAINY;
            case 3 -> WeatherType.SNOWY;
            case 0 -> switch (sky) {
                case 1 -> WeatherType.SUNNY;
                case 3, 4 -> WeatherType.CLOUDY;
                default -> throw new KmaWeatherMappingException("Unsupported SKY value: " + sky);
            };
            default -> throw new KmaWeatherMappingException("Unsupported PTY value: " + pty);
        };
    }

    private int parseRoundedInteger(String category, String value) {
        return Math.toIntExact(Math.round(parseDouble(category, value)));
    }

    private int parseInteger(String category, String value) {
        double parsed = parseDouble(category, value);
        if (parsed % 1 != 0) {
            throw new KmaWeatherMappingException("Invalid integer value for " + category + ": " + value);
        }
        return (int) parsed;
    }

    private double parseDouble(String category, String value) {
        if (value == null) {
            throw new KmaWeatherMappingException("Missing value for KMA category " + category);
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            throw new KmaWeatherMappingException("Invalid numeric value for " + category + ": " + value, exception);
        }
    }

    /**
     * PCP는 "강수없음", "-", "1.0mm"처럼 숫자와 문구가 섞일 수 있어 숫자 추출로 판단한다.
     */
    private boolean hasPrecipitation(String value) {
        if (value == null) {
            return false;
        }
        String normalized = value.trim();
        if (normalized.equals("-")
                || normalized.equalsIgnoreCase("null")
                || normalized.equals("0")
                || normalized.equals("0.0")
                || normalized.equals(PCP_NO_PRECIPITATION)) {
            return false;
        }

        Matcher matcher = NUMBER_PATTERN.matcher(normalized);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group()) > 0.0;
        }
        throw new KmaWeatherMappingException("Invalid PCP value: " + value);
    }
}
