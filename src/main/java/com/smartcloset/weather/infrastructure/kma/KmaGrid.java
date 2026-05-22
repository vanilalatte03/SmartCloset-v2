package com.smartcloset.weather.infrastructure.kma;

public record KmaGrid(int nx, int ny) {

    public KmaGrid {
        if (nx <= 0) {
            throw new IllegalArgumentException("nx must be positive");
        }
        if (ny <= 0) {
            throw new IllegalArgumentException("ny must be positive");
        }
    }
}
