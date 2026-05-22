package com.smartcloset.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findsUserByEmailAndChecksEmailExistence() {
        User user = userRepository.save(UserTestFixtures.authenticatedUser("repo-user@example.com", "Repo User"));
        userRepository.flush();

        assertThat(userRepository.findByEmail("repo-user@example.com")).contains(user);
        assertThat(userRepository.existsByEmail("repo-user@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
    }

    @Test
    void enforcesUniqueEmail() {
        userRepository.save(UserTestFixtures.authenticatedUser("duplicate@example.com", "Duplicate One"));
        userRepository.flush();

        assertThatThrownBy(() -> {
            userRepository.save(UserTestFixtures.authenticatedUser("duplicate@example.com", "Duplicate Two"));
            userRepository.flush();
        })
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
