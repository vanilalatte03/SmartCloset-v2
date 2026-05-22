package com.smartcloset.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.user.UserTestFixtures;
import org.junit.jupiter.api.Test;

class UserAccountSchemaTest {

    @Test
    void createsAuthenticatedUserWithUserRoleDefaultLocationAndEmptyPreferences() {
        User user = UserTestFixtures.authenticatedUser("demo@example.com", "Demo User");

        assertThat(user.getEmail()).isEqualTo("demo@example.com");
        assertThat(user.getPasswordHash()).isEqualTo(UserTestFixtures.BCRYPT_HASH);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getName()).isEqualTo("Demo User");
        assertThat(user.getLocationCode()).isEqualTo("SEOUL");
        assertThat(user.getLocationName()).isEqualTo("서울특별시");
        assertThat(user.getLocationNx()).isEqualTo(60);
        assertThat(user.getLocationNy()).isEqualTo(127);
        assertThat(user.getPreferredColorsJson()).isEqualTo("[]");
        assertThat(user.getPreferredMaterialsJson()).isEqualTo("[]");
        assertThat(user.getStyleTagsJson()).isEqualTo("[]");
    }
}
