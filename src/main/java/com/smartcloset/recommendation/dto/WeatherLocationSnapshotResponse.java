package com.smartcloset.recommendation.dto;

import com.smartcloset.location.domain.LocationSource;
import com.smartcloset.weather.domain.WeatherLocationSnapshot;

public record WeatherLocationSnapshotResponse(
        String code,
        String name,
        String fullName,
        int nx,
        int ny,
        LocationSource source
) {

    public static WeatherLocationSnapshotResponse from(WeatherLocationSnapshot location) {
        if (location == null) {
            return null;
        }
        return new WeatherLocationSnapshotResponse(
                location.code(),
                location.name(),
                location.fullName(),
                location.nx(),
                location.ny(),
                location.source()
        );
    }
}
