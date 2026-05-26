package com.smartcloset.location.domain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class LocationCatalog {

    private static final String CATALOG_RESOURCE = "kma-location-catalog.csv";

    private final List<LocationOption> locations;

    public LocationCatalog() {
        this.locations = loadLocations();
    }

    public List<LocationOption> findAll() {
        return locations;
    }

    public List<LocationOption> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }

        String trimmedKeyword = keyword.trim();
        String normalizedKeyword = trimmedKeyword.toUpperCase(Locale.ROOT);
        String compactKeyword = compact(trimmedKeyword);
        String digitKeyword = digitsOnly(trimmedKeyword);
        boolean hasCompactKeyword = !compactKeyword.isBlank();
        boolean hasNumericCodeKeyword = !digitKeyword.isBlank() && digitKeyword.length() == trimmedKeyword.length();
        return locations.stream()
                .filter(location -> matches(
                        location,
                        trimmedKeyword,
                        normalizedKeyword,
                        compactKeyword,
                        digitKeyword,
                        hasCompactKeyword,
                        hasNumericCodeKeyword
                ))
                .toList();
    }

    public Optional<LocationOption> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        return locations.stream()
                .filter(location -> location.code().equals(normalizedCode))
                .findFirst();
    }

    public List<LocationOption> findNearest(LocationGrid grid, BigDecimal latitude, BigDecimal longitude, int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        Objects.requireNonNull(grid, "grid must not be null");
        return locations.stream()
                .sorted(Comparator
                        .comparingDouble((LocationOption location) -> distance(location, grid, latitude, longitude))
                        .thenComparing(LocationOption::code))
                .limit(limit)
                .toList();
    }

    public LocationOption defaultLocation() {
        return LocationOption.defaultSeoul();
    }

    private boolean matches(
            LocationOption location,
            String keyword,
            String normalizedKeyword,
            String compactKeyword,
            String digitKeyword,
            boolean hasCompactKeyword,
            boolean hasNumericCodeKeyword
    ) {
        return containsIgnoreCase(location.code(), normalizedKeyword)
                || (hasNumericCodeKeyword && contains(digitsOnly(location.code()), digitKeyword))
                || contains(location.name(), keyword)
                || contains(location.fullName(), keyword)
                || contains(location.region1(), keyword)
                || contains(location.region2(), keyword)
                || contains(location.region3(), keyword)
                || (hasCompactKeyword && (
                        contains(compact(location.name()), compactKeyword)
                                || contains(compact(location.fullName()), compactKeyword)
                                || contains(compact(location.region1()), compactKeyword)
                                || contains(compact(location.region2()), compactKeyword)
                                || contains(compact(location.region3()), compactKeyword)
                ));
    }

    private List<LocationOption> loadLocations() {
        ClassPathResource resource = new ClassPathResource(CATALOG_RESOURCE);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                resource.getInputStream(),
                StandardCharsets.UTF_8
        ))) {
            return reader.lines()
                    .skip(1)
                    .filter(line -> !line.isBlank())
                    .map(this::parseLocation)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load KMA location catalog", exception);
        }
    }

    private LocationOption parseLocation(String line) {
        String[] columns = line.split(",", -1);
        if (columns.length != 10) {
            throw new IllegalStateException("Invalid KMA location catalog row: " + line);
        }
        return new LocationOption(
                required(columns[0]).toUpperCase(Locale.ROOT),
                required(columns[4]),
                required(columns[5]),
                required(columns[1]),
                nullable(columns[2]),
                nullable(columns[3]),
                Integer.parseInt(required(columns[6])),
                Integer.parseInt(required(columns[7])),
                decimalOrNull(columns[8]),
                decimalOrNull(columns[9])
        );
    }

    private boolean containsIgnoreCase(String value, String normalizedKeyword) {
        return value.toUpperCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.contains(keyword);
    }

    private String compact(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("[\\s\\d]+", "");
    }

    private String digitsOnly(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\D+", "");
    }

    private String required(String value) {
        return Objects.requireNonNull(value, "catalog value must not be null").trim();
    }

    private String nullable(String value) {
        String trimmed = required(value);
        return trimmed.isEmpty() ? null : trimmed;
    }

    private BigDecimal decimalOrNull(String value) {
        String trimmed = required(value);
        return trimmed.isEmpty() ? null : new BigDecimal(trimmed);
    }

    private double distance(LocationOption location, LocationGrid grid, BigDecimal latitude, BigDecimal longitude) {
        if (latitude != null && longitude != null && location.latitude() != null && location.longitude() != null) {
            return haversineDistanceKm(latitude, longitude, location.latitude(), location.longitude());
        }
        int dx = location.nx() - grid.nx();
        int dy = location.ny() - grid.ny();
        return Math.sqrt(dx * dx + dy * dy);
    }

    private double haversineDistanceKm(
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal locationLatitude,
            BigDecimal locationLongitude
    ) {
        double lat1 = latitude.setScale(8, RoundingMode.HALF_UP).doubleValue();
        double lon1 = longitude.setScale(8, RoundingMode.HALF_UP).doubleValue();
        double lat2 = locationLatitude.setScale(8, RoundingMode.HALF_UP).doubleValue();
        double lon2 = locationLongitude.setScale(8, RoundingMode.HALF_UP).doubleValue();
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2.0) * Math.sin(latDistance / 2.0)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2.0) * Math.sin(lonDistance / 2.0);
        return 6371.00877 * 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
    }
}
