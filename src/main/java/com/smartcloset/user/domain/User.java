package com.smartcloset.user.domain;

import com.smartcloset.common.domain.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "users")
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    protected User() {
    }

    private User(String name) {
        this.name = requireName(name);
    }

    public static User createSeedUser(String name) {
        return new User(name);
    }

    public void rename(String name) {
        this.name = requireName(name);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    private String requireName(String name) {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (name.length() > 50) {
            throw new IllegalArgumentException("name must be 50 characters or less");
        }
        return name;
    }
}
