package com.smartcloset.location.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LocationCatalogTest {

    private final LocationCatalog catalog = new LocationCatalog();

    @Test
    void findAllReturnsKmaCatalogWithLegacyDefaultsFirst() {
        List<LocationOption> locations = catalog.findAll();

        assertThat(locations)
                .extracting(LocationOption::code)
                .startsWith(
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
        assertThat(locations).hasSizeGreaterThan(1_000);
    }

    @Test
    void searchMatchesCodeCaseInsensitively() {
        List<LocationOption> locations = catalog.search("seo");

        assertThat(locations)
                .singleElement()
                .satisfies(location -> {
                    assertThat(location.code()).isEqualTo("SEOUL");
                    assertThat(location.name()).isEqualTo("서울특별시");
                    assertThat(location.nx()).isEqualTo(60);
                    assertThat(location.ny()).isEqualTo(127);
                });
    }

    @Test
    void searchMatchesNumericAdministrativeCodeWithoutReturningAllLocations() {
        List<LocationOption> locations = catalog.search("4128751000");

        assertThat(locations)
                .singleElement()
                .satisfies(location -> {
                    assertThat(location.code()).isEqualTo("KMA_4128751000");
                    assertThat(location.fullName()).isEqualTo("경기도 고양시일산서구 일산1동");
                });
    }

    @Test
    void searchWithKoreanAndDigitsDoesNotMatchCodeDigitsOnly() {
        List<LocationOption> locations = catalog.search("신사제1동");

        assertThat(locations).isNotEmpty();
        assertThat(locations).hasSizeLessThan(catalog.findAll().size());
        assertThat(locations)
                .extracting(LocationOption::name)
                .contains("신사제1동");
    }

    @Test
    void searchMatchesName() {
        List<LocationOption> locations = catalog.search("부산");

        assertThat(locations)
                .anySatisfy(location -> {
                    assertThat(location.code()).isEqualTo("BUSAN");
                    assertThat(location.name()).isEqualTo("부산광역시");
                    assertThat(location.fullName()).isEqualTo("부산광역시");
                    assertThat(location.region1()).isEqualTo("부산광역시");
                    assertThat(location.region2()).isNull();
                    assertThat(location.region3()).isNull();
                    assertThat(location.nx()).isEqualTo(98);
                    assertThat(location.ny()).isEqualTo(76);
                    assertThat(location.latitude()).isNotNull();
                    assertThat(location.longitude()).isNotNull();
                });
    }

    @Test
    void blankSearchReturnsAllLocations() {
        assertThat(catalog.search(" ")).hasSameSizeAs(catalog.findAll());
    }

    @Test
    void searchIlsanDongReturnsMultipleAdministrativeCandidates() {
        List<LocationOption> locations = catalog.search("일산동");

        assertThat(locations)
                .extracting(LocationOption::fullName)
                .contains(
                        "경기도 고양시일산서구 일산1동",
                        "경기도 고양시일산서구 일산2동",
                        "경기도 고양시일산서구 일산3동"
                );
        assertThat(locations).hasSizeGreaterThanOrEqualTo(3);
    }

    @Test
    void searchMatchesRegionFields() {
        List<LocationOption> locations = catalog.search("고양시일산동구");

        assertThat(locations)
                .extracting(LocationOption::region2)
                .containsOnly("고양시일산동구");
    }

    @Test
    void ilsanSeoGuRowsUseCorrectDistrictForKnownKmaSourceTypos() {
        assertIlsanSeoGu("KMA_4128754500", "탄현1동");
        assertIlsanSeoGu("KMA_4128754600", "탄현2동");
        assertIlsanSeoGu("KMA_4128760000", "덕이동");
        assertIlsanSeoGu("KMA_4128761000", "가좌동");
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

    private void assertIlsanSeoGu(String code, String region3) {
        assertThat(catalog.findByCode(code))
                .hasValueSatisfying(location -> {
                    assertThat(location.region2()).isEqualTo("고양시일산서구");
                    assertThat(location.region3()).isEqualTo(region3);
                    assertThat(location.fullName()).isEqualTo("경기도 고양시일산서구 " + region3);
                });
    }
}
