package com.smartcloset.location.domain;

import org.springframework.stereotype.Component;

@Component
public class KmaGridConverter {

    private static final double EARTH_RADIUS_KM = 6371.00877;
    private static final double GRID_KM = 5.0;
    private static final double STANDARD_LATITUDE_1 = 30.0;
    private static final double STANDARD_LATITUDE_2 = 60.0;
    private static final double ORIGIN_LONGITUDE = 126.0;
    private static final double ORIGIN_LATITUDE = 38.0;
    private static final double ORIGIN_X = 43.0;
    private static final double ORIGIN_Y = 136.0;
    private static final double DEGREES_TO_RADIANS = Math.PI / 180.0;

    private final double gridRadius;
    private final double projectionN;
    private final double projectionF;
    private final double originRadius;

    public KmaGridConverter() {
        this.gridRadius = EARTH_RADIUS_KM / GRID_KM;
        this.projectionN = calculateProjectionN();
        this.projectionF = calculateProjectionF();
        this.originRadius = calculateRadius(ORIGIN_LATITUDE);
    }

    public LocationGrid toGrid(double latitude, double longitude) {
        double radius = calculateRadius(latitude);
        double theta = longitude * DEGREES_TO_RADIANS - ORIGIN_LONGITUDE * DEGREES_TO_RADIANS;
        if (theta > Math.PI) {
            theta -= 2.0 * Math.PI;
        }
        if (theta < -Math.PI) {
            theta += 2.0 * Math.PI;
        }
        theta *= projectionN;

        int nx = (int) Math.floor(radius * Math.sin(theta) + ORIGIN_X + 0.5);
        int ny = (int) Math.floor(originRadius - radius * Math.cos(theta) + ORIGIN_Y + 0.5);
        return new LocationGrid(nx, ny);
    }

    private double calculateProjectionN() {
        double slat1 = STANDARD_LATITUDE_1 * DEGREES_TO_RADIANS;
        double slat2 = STANDARD_LATITUDE_2 * DEGREES_TO_RADIANS;
        return Math.log(Math.cos(slat1) / Math.cos(slat2))
                / Math.log(Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5));
    }

    private double calculateProjectionF() {
        double slat1 = STANDARD_LATITUDE_1 * DEGREES_TO_RADIANS;
        return Math.pow(Math.tan(Math.PI * 0.25 + slat1 * 0.5), projectionN)
                * Math.cos(slat1)
                / projectionN;
    }

    private double calculateRadius(double latitude) {
        double lat = latitude * DEGREES_TO_RADIANS;
        return gridRadius * projectionF / Math.pow(Math.tan(Math.PI * 0.25 + lat * 0.5), projectionN);
    }
}
