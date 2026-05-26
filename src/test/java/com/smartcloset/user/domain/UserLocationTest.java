package com.smartcloset.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.location.domain.LocationOption;
import com.smartcloset.location.domain.LocationSource;
import org.junit.jupiter.api.Test;

class UserLocationTest {

    @Test
    void createSeedUserHasDefaultSeoulLocation() {
        User user = User.createSeedUser("demo-user");

        assertThat(user.getLocationCode()).isEqualTo("SEOUL");
        assertThat(user.getLocationName()).isEqualTo("서울특별시");
        assertThat(user.getLocationFullName()).isEqualTo("서울특별시");
        assertThat(user.getLocationRegion1()).isEqualTo("서울특별시");
        assertThat(user.getLocationNx()).isEqualTo(60);
        assertThat(user.getLocationNy()).isEqualTo(127);
        assertThat(user.getLocationSource()).isEqualTo(LocationSource.MANUAL_SEARCH);
    }

    @Test
    void ensureDefaultLocationFillsEmptyLocationSnapshot() {
        User user = User.create("legacy-user");

        user.ensureDefaultLocation();

        assertThat(user.getLocationCode()).isEqualTo("SEOUL");
        assertThat(user.getLocationName()).isEqualTo("서울특별시");
        assertThat(user.getLocationNx()).isEqualTo(60);
        assertThat(user.getLocationNy()).isEqualTo(127);
        assertThat(user.getLocationSource()).isEqualTo(LocationSource.MANUAL_SEARCH);
    }

    @Test
    void ensureDefaultLocationDoesNotOverwriteExistingLocation() {
        User user = User.create("location-user");
        user.updateLocation(new LocationOption("BUSAN", "부산광역시", 98, 76));

        user.ensureDefaultLocation();

        assertThat(user.getLocationCode()).isEqualTo("BUSAN");
        assertThat(user.getLocationName()).isEqualTo("부산광역시");
        assertThat(user.getLocationNx()).isEqualTo(98);
        assertThat(user.getLocationNy()).isEqualTo(76);
        assertThat(user.getLocationSource()).isEqualTo(LocationSource.MANUAL_SEARCH);
    }

    @Test
    void updateLocationStoresLocationSource() {
        User user = User.create("location-source-user");

        user.updateLocation(new LocationOption("BUSAN", "부산광역시", 98, 76), LocationSource.BROWSER_GEOLOCATION);

        assertThat(user.getLocationCode()).isEqualTo("BUSAN");
        assertThat(user.getLocationSource()).isEqualTo(LocationSource.BROWSER_GEOLOCATION);
    }
}
