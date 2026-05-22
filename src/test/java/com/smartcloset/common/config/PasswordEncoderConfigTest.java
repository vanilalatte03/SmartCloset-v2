package com.smartcloset.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordEncoderConfigTest {

    @Test
    void passwordEncoderUsesBcryptHashing() {
        PasswordEncoder passwordEncoder = new PasswordEncoderConfig().passwordEncoder();

        String encoded = passwordEncoder.encode("password123!");

        assertThat(encoded).isNotEqualTo("password123!");
        assertThat(encoded).startsWith("$2");
        assertThat(passwordEncoder.matches("password123!", encoded)).isTrue();
    }
}
