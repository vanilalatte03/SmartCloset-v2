package com.smartcloset.location.domain;

public record LocationGrid(
        int nx,
        int ny
) {

    public LocationGrid {
        if (nx <= 0) {
            throw new IllegalArgumentException("nx must be positive");
        }
        if (ny <= 0) {
            throw new IllegalArgumentException("ny must be positive");
        }
    }
}
