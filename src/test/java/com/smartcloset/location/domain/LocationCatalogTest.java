package com.smartcloset.location.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LocationCatalogTest {

    private final LocationCatalog catalog = new LocationCatalog();

    @Test
    void findAllReturnsNineRepresentativeLocationsInCatalogOrder() {
        List<LocationOption> locations = catalog.findAll();

        assertThat(locations)
                .extracting(LocationOption::code)
                .containsExactly(
                        "SEOUL",
                        "BUSAN",
                        "DAEGU",
                        "INCHEON",
                        "GWANGJU",
                        "DAEJEON",
                        "ULSAN",
                        "SEJONG",
                        "JEJU"
                );
    }

    @Test
    void searchMatchesCodeCaseInsensitively() {
        List<LocationOption> locations = catalog.search("seo");

        assertThat(locations).containsExactly(LocationOption.defaultSeoul());
    }

    @Test
    void searchMatchesName() {
        List<LocationOption> locations = catalog.search("부산");

        assertThat(locations)
                .singleElement()
                .satisfies(location -> {
                    assertThat(location.code()).isEqualTo("BUSAN");
                    assertThat(location.name()).isEqualTo("부산광역시");
                    assertThat(location.nx()).isEqualTo(98);
                    assertThat(location.ny()).isEqualTo(76);
                });
    }

    @Test
    void blankSearchReturnsAllLocations() {
        assertThat(catalog.search(" ")).hasSize(9);
    }

    @Test
    void findByCodeMatchesCaseInsensitively() {
        assertThat(catalog.findByCode("jeju"))
                .hasValueSatisfying(location -> {
                    assertThat(location.name()).isEqualTo("제주특별자치도");
                    assertThat(location.nx()).isEqualTo(52);
                    assertThat(location.ny()).isEqualTo(38);
                });
    }
}
