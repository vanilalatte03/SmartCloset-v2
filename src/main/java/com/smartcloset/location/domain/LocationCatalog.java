package com.smartcloset.location.domain;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class LocationCatalog {

    private static final List<LocationOption> LOCATIONS = List.of(
            LocationOption.defaultSeoul(),
            new LocationOption("BUSAN", "부산광역시", 98, 76),
            new LocationOption("DAEGU", "대구광역시", 89, 90),
            new LocationOption("INCHEON", "인천광역시", 55, 124),
            new LocationOption("GWANGJU", "광주광역시", 58, 74),
            new LocationOption("DAEJEON", "대전광역시", 67, 100),
            new LocationOption("ULSAN", "울산광역시", 102, 84),
            new LocationOption("SEJONG", "세종특별자치시", 66, 103),
            new LocationOption("JEJU", "제주특별자치도", 52, 38)
    );

    public List<LocationOption> findAll() {
        return LOCATIONS;
    }

    public List<LocationOption> search(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return findAll();
        }

        String trimmedKeyword = keyword.trim();
        String normalizedKeyword = trimmedKeyword.toUpperCase(Locale.ROOT);
        return LOCATIONS.stream()
                .filter(location -> matches(location, trimmedKeyword, normalizedKeyword))
                .toList();
    }

    public Optional<LocationOption> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        String normalizedCode = code.trim().toUpperCase(Locale.ROOT);
        return LOCATIONS.stream()
                .filter(location -> location.code().equals(normalizedCode))
                .findFirst();
    }

    public LocationOption defaultLocation() {
        return LocationOption.defaultSeoul();
    }

    private boolean matches(LocationOption location, String keyword, String normalizedKeyword) {
        return location.code().contains(normalizedKeyword) || location.name().contains(keyword);
    }
}
