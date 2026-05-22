package com.smartcloset.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void findsUserByUniqueEmailAndChecksDuplicateEmail() {
        String passwordHash = passwordEncoder.encode("password123!");
        User user = userRepository.save(User.create("account@example.com", passwordHash, "Account User"));

        assertThat(userRepository.findByEmail("account@example.com"))
                .hasValueSatisfying(found -> assertThat(found.getId()).isEqualTo(user.getId()));
        assertThat(userRepository.existsByEmail("account@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
        assertThat(passwordEncoder.matches("password123!", user.getPasswordHash())).isTrue();
    }
}
