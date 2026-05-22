package com.smartcloset.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserAccountTest {

    private static final String PASSWORD_HASH =
            "$2a$10$CwTycUXWue0Thq9StjUM0uJ8Yc2qkYQd1sQj1Y6vTLM8bNn2bC5lW";

    @Test
    void createAccountUserHasUserRoleDefaultSeoulLocationAndEmptyPreferences() {
        User user = User.create("demo@example.com", PASSWORD_HASH, "Demo User");

        assertThat(user.getEmail()).isEqualTo("demo@example.com");
        assertThat(user.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getLocationCode()).isEqualTo("SEOUL");
        assertThat(user.getLocationName()).isEqualTo("서울특별시");
        assertThat(user.getLocationNx()).isEqualTo(60);
        assertThat(user.getLocationNy()).isEqualTo(127);
        assertThat(user.getPreferredColorsJson()).isEqualTo("[]");
        assertThat(user.getPreferredMaterialsJson()).isEqualTo("[]");
        assertThat(user.getStyleTagsJson()).isEqualTo("[]");
    }

    @Test
    void createLegacyUserKeepsLocationSnapshotEmptyButInitializesAccountFields() {
        User user = User.create("legacy-user");

        assertThat(user.getEmail()).endsWith("@smartcloset.local");
        assertThat(user.getPasswordHash()).isNotBlank();
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.hasLocation()).isFalse();
        assertThat(user.getPreferredColorsJson()).isEqualTo("[]");
        assertThat(user.getPreferredMaterialsJson()).isEqualTo("[]");
        assertThat(user.getStyleTagsJson()).isEqualTo("[]");
    }
}
