package com.smartcloset.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.location.domain.LocationOption;
import org.junit.jupiter.api.Test;

class UserLocationTest {

    @Test
    void createSeedUserHasDefaultSeoulLocation() {
        User user = User.createSeedUser("demo-user");

        assertThat(user.getLocationCode()).isEqualTo("SEOUL");
        assertThat(user.getLocationName()).isEqualTo("서울특별시");
        assertThat(user.getLocationNx()).isEqualTo(60);
        assertThat(user.getLocationNy()).isEqualTo(127);
    }

    @Test
    void ensureDefaultLocationFillsEmptyLocationSnapshot() {
        User user = User.create("legacy-user");

        user.ensureDefaultLocation();

        assertThat(user.getLocationCode()).isEqualTo("SEOUL");
        assertThat(user.getLocationName()).isEqualTo("서울특별시");
        assertThat(user.getLocationNx()).isEqualTo(60);
        assertThat(user.getLocationNy()).isEqualTo(127);
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
    }
}
