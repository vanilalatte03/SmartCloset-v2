package com.smartcloset.location.domain;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
        return locations.stream()
                .filter(location -> matches(location, trimmedKeyword, normalizedKeyword, compactKeyword))
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

    public LocationOption defaultLocation() {
        return LocationOption.defaultSeoul();
    }

    private boolean matches(
            LocationOption location,
            String keyword,
            String normalizedKeyword,
            String compactKeyword
    ) {
        return containsIgnoreCase(location.code(), normalizedKeyword)
                || contains(location.name(), keyword)
                || contains(location.fullName(), keyword)
                || contains(location.region1(), keyword)
                || contains(location.region2(), keyword)
                || contains(location.region3(), keyword)
                || contains(compact(location.name()), compactKeyword)
                || contains(compact(location.fullName()), compactKeyword)
                || contains(compact(location.region1()), compactKeyword)
                || contains(compact(location.region2()), compactKeyword)
                || contains(compact(location.region3()), compactKeyword);
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
}
