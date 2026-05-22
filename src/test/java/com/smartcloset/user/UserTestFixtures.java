package com.smartcloset.user;

import com.smartcloset.user.domain.User;

public final class UserTestFixtures {

    public static final String BCRYPT_HASH = "$2a$10$7qQdW3TfEpsxtWwHyaCjHu2qJIRq6s2ePLfYvRrFRtzqvxbbRb6bW";

    private UserTestFixtures() {
    }

    public static User authenticatedUser(String email, String name) {
        return User.create(email, BCRYPT_HASH, name);
    }

    public static User authenticatedUserWithoutLocation(String email, String name) {
        return User.createWithoutLocation(email, BCRYPT_HASH, name);
    }
}
