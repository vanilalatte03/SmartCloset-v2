package com.smartcloset.user.domain;

import com.smartcloset.common.domain.BaseTimeEntity;
import com.smartcloset.location.domain.LocationOption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Entity
@Table(
        name = "users",
        indexes = @Index(name = "idx_users_location_code", columnList = "location_code"),
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
public class User extends BaseTimeEntity {

    public static final String EMPTY_PREFERENCE_JSON = "[]";
    private static final String LEGACY_BCRYPT_HASH_PLACEHOLDER =
            "$2a$10$7qQdW3TfEpsxtWwHyaCjHu2qJIRq6s2ePLfYvRrFRtzqvxbbRb6bW";
    private static final AtomicLong LEGACY_USER_SEQUENCE = new AtomicLong();

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private UserRole role;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "location_code", length = 30)
    private String locationCode;

    @Column(name = "location_name", length = 50)
    private String locationName;

    @Column(name = "location_nx")
    private Integer locationNx;

    @Column(name = "location_ny")
    private Integer locationNy;

    @Column(name = "preferred_colors_json", nullable = false, columnDefinition = "TEXT")
    private String preferredColorsJson;

    @Column(name = "preferred_materials_json", nullable = false, columnDefinition = "TEXT")
    private String preferredMaterialsJson;

    @Column(name = "style_tags_json", nullable = false, columnDefinition = "TEXT")
    private String styleTagsJson;

    protected User() {
    }

    private User(String email, String passwordHash, String name, boolean applyDefaultLocation) {
        this.email = requireEmail(email);
        this.passwordHash = requirePasswordHash(passwordHash);
        this.role = UserRole.USER;
        this.name = requireName(name);
        this.preferredColorsJson = EMPTY_PREFERENCE_JSON;
        this.preferredMaterialsJson = EMPTY_PREFERENCE_JSON;
        this.styleTagsJson = EMPTY_PREFERENCE_JSON;
        if (applyDefaultLocation) {
            ensureDefaultLocation();
        }
    }

    public static User create(String email, String passwordHash, String name) {
        return new User(email, passwordHash, name, true);
    }

    public static User createWithoutLocation(String email, String passwordHash, String name) {
        return new User(email, passwordHash, name, false);
    }

    public static User create(String name) {
        return createWithoutLocation(legacyEmail(name), LEGACY_BCRYPT_HASH_PLACEHOLDER, name);
    }

    public static User createSeedUser(String name) {
        return create(legacyEmail(name), LEGACY_BCRYPT_HASH_PLACEHOLDER, name);
    }

    public void rename(String name) {
        this.name = requireName(name);
    }

    public void updateLocation(LocationOption location) {
        LocationOption requiredLocation = Objects.requireNonNull(location, "location must not be null");
        this.locationCode = requiredLocation.code();
        this.locationName = requiredLocation.name();
        this.locationNx = requiredLocation.nx();
        this.locationNy = requiredLocation.ny();
    }

    public void ensureDefaultLocation() {
        if (hasLocation()) {
            return;
        }
        updateLocation(LocationOption.defaultSeoul());
    }

    public boolean hasLocation() {
        return locationCode != null && locationName != null && locationNx != null && locationNy != null;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserRole getRole() {
        return role;
    }

    public String getName() {
        return name;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public String getLocationName() {
        return locationName;
    }

    public Integer getLocationNx() {
        return locationNx;
    }

    public Integer getLocationNy() {
        return locationNy;
    }

    public String getPreferredColorsJson() {
        return preferredColorsJson;
    }

    public String getPreferredMaterialsJson() {
        return preferredMaterialsJson;
    }

    public String getStyleTagsJson() {
        return styleTagsJson;
    }

    private String requireEmail(String email) {
        Objects.requireNonNull(email, "email must not be null");
        if (email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        return email;
    }

    private String requirePasswordHash(String passwordHash) {
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        if (passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
        return passwordHash;
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

    private static String legacyEmail(String name) {
        String localPart = name == null ? "user" : name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (localPart.isBlank()) {
            localPart = "user";
        }
        return localPart + "-" + LEGACY_USER_SEQUENCE.incrementAndGet() + "@legacy.smartcloset.local";
    }
}
