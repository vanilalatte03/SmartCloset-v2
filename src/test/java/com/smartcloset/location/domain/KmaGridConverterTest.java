package com.smartcloset.location.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KmaGridConverterTest {

    private final KmaGridConverter converter = new KmaGridConverter();

    @Test
    void convertsSeoulCoordinateToKmaGrid() {
        LocationGrid grid = converter.toGrid(37.5665, 126.9780);

        assertThat(grid.nx()).isEqualTo(60);
        assertThat(grid.ny()).isEqualTo(127);
    }

    @Test
    void convertsIlsanCoordinateToKmaGridNearCatalogGrid() {
        LocationGrid grid = converter.toGrid(37.6843, 126.7707);

        assertThat(grid.nx()).isEqualTo(56);
        assertThat(grid.ny()).isEqualTo(129);
    }
}
