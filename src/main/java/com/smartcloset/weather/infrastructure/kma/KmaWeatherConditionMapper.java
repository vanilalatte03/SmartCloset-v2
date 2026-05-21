package com.smartcloset.weather.infrastructure.kma;

import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherType;
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

public class KmaWeatherConditionMapper {

    private static final List<String> REQUIRED_CATEGORIES = List.of("TMP", "SKY", "PTY", "PCP", "WSD");
    private static final DateTimeFormatter FORECAST_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[-+]?\\d+(?:\\.\\d+)?");
    private static final String PCP_NO_PRECIPITATION = "\uAC15\uC218\uC5C6\uC74C";

    public WeatherCondition map(List<KmaForecastItem> items, ZonedDateTime now) {
        Objects.requireNonNull(items, "items must not be null");
        if (items.isEmpty()) {
            throw new KmaWeatherMappingException("KMA forecast items are empty");
        }

        LocalDateTime nowKst = Objects.requireNonNull(now, "now must not be null")
                .withZoneSameInstant(KmaForecastBaseTimeCalculator.KST_ZONE)
                .toLocalDateTime();
        Map<LocalDateTime, Map<String, String>> groups = groupByForecastTime(items);
        Map<String, String> selectedGroup = groups.entrySet()
                .stream()
                .filter(entry -> !entry.getKey().isBefore(nowKst))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElseThrow(() -> new KmaWeatherMappingException("No forecast group exists at or after current KST time"));

        validateRequiredCategories(selectedGroup);

        int temperature = parseRoundedInteger("TMP", selectedGroup.get("TMP"));
        int pty = parseInteger("PTY", selectedGroup.get("PTY"));
        int sky = parseInteger("SKY", selectedGroup.get("SKY"));
        boolean rainy = pty != 0 || hasPrecipitation(selectedGroup.get("PCP"));
        boolean windy = parseDouble("WSD", selectedGroup.get("WSD")) >= 4.0;

        return WeatherCondition.of(temperature, mapWeatherType(pty, sky), rainy, windy);
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
