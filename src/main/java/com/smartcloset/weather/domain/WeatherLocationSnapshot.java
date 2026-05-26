package com.smartcloset.weather.domain;

import com.smartcloset.location.domain.LocationSource;
import com.smartcloset.user.application.UserLocationSnapshot;
import java.util.Objects;

public record WeatherLocationSnapshot(
        String code,
        String name,
        String fullName,
        int nx,
        int ny,
        LocationSource source
) {

    public WeatherLocationSnapshot {
        code = requireText(code, "code");
        name = requireText(name, "name");
        fullName = requireText(fullName == null ? name : fullName, "fullName");
        if (nx <= 0) {
            throw new IllegalArgumentException("nx must be positive");
        }
        if (ny <= 0) {
            throw new IllegalArgumentException("ny must be positive");
        }
        Objects.requireNonNull(source, "source must not be null");
    }

    public static WeatherLocationSnapshot from(UserLocationSnapshot location) {
        Objects.requireNonNull(location, "location must not be null");
        return new WeatherLocationSnapshot(
                location.code(),
                location.name(),
                location.fullName(),
                location.nx(),
                location.ny(),
                location.source()
        );
    }

    private static String requireText(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
