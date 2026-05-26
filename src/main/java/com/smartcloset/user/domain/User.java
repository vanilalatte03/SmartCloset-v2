package com.smartcloset.user.domain;

import com.smartcloset.common.domain.BaseTimeEntity;
import com.smartcloset.location.domain.LocationOption;
import com.smartcloset.location.domain.LocationSource;
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
import java.util.Locale;
import java.util.Objects;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(name = "idx_users_location_code", columnList = "location_code"),
                @Index(name = "idx_users_location_grid", columnList = "location_nx, location_ny")
        },
        uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email")
)
public class User extends BaseTimeEntity {

    private static final String EMPTY_JSON_ARRAY = "[]";
    private static final String DISABLED_LEGACY_PASSWORD_HASH =
            "$2a$10$CwTycUXWue0Thq9StjUM0uJ8Yc2qkYQd1sQj1Y6vTLM8bNn2bC5lW";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 30)
    private UserRole role;

    @Column(name = "location_code", length = 30)
    private String locationCode;

    @Column(name = "location_name", length = 50)
    private String locationName;

    @Column(name = "location_full_name", length = 100)
    private String locationFullName;

    @Column(name = "location_region1", length = 50)
    private String locationRegion1;

    @Column(name = "location_region2", length = 50)
    private String locationRegion2;

    @Column(name = "location_region3", length = 50)
    private String locationRegion3;

    @Column(name = "location_nx")
    private Integer locationNx;

    @Column(name = "location_ny")
    private Integer locationNy;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_source", nullable = false, length = 30)
    private LocationSource locationSource = LocationSource.MANUAL_SEARCH;

    @Column(name = "preferred_colors_json", nullable = false, columnDefinition = "TEXT")
    private String preferredColorsJson;

    @Column(name = "preferred_materials_json", nullable = false, columnDefinition = "TEXT")
    private String preferredMaterialsJson;

    @Column(name = "style_tags_json", nullable = false, columnDefinition = "TEXT")
    private String styleTagsJson;

    protected User() {
    }

    private User(String email, String passwordHash, String name) {
        this.email = requireEmail(email);
        this.passwordHash = requirePasswordHash(passwordHash);
        this.name = requireName(name);
        this.role = UserRole.USER;
        this.preferredColorsJson = EMPTY_JSON_ARRAY;
        this.preferredMaterialsJson = EMPTY_JSON_ARRAY;
        this.styleTagsJson = EMPTY_JSON_ARRAY;
    }

    public static User create(String email, String passwordHash, String name) {
        User user = new User(email, passwordHash, name);
        user.ensureDefaultLocation();
        return user;
    }

    public static User create(String name) {
        return new User(localAccountEmail("legacy", name), DISABLED_LEGACY_PASSWORD_HASH, name);
    }

    public static User createSeedUser(String name) {
        User user = new User(localAccountEmail("seed", name), DISABLED_LEGACY_PASSWORD_HASH, name);
        user.ensureDefaultLocation();
        return user;
    }

    public void rename(String name) {
        this.name = requireName(name);
    }

    public void updateLocation(LocationOption location) {
        updateLocation(location, LocationSource.MANUAL_SEARCH);
    }

    public void updateLocation(LocationOption location, LocationSource source) {
        LocationOption requiredLocation = Objects.requireNonNull(location, "location must not be null");
        LocationSource requiredSource = Objects.requireNonNull(source, "source must not be null");
        this.locationCode = requiredLocation.code();
        this.locationName = requiredLocation.name();
        this.locationFullName = requiredLocation.fullName();
        this.locationRegion1 = requiredLocation.region1();
        this.locationRegion2 = requiredLocation.region2();
        this.locationRegion3 = requiredLocation.region3();
        this.locationNx = requiredLocation.nx();
        this.locationNy = requiredLocation.ny();
        this.locationSource = requiredSource;
    }

    public void updatePreferences(String preferredColorsJson, String preferredMaterialsJson, String styleTagsJson) {
        this.preferredColorsJson = requireJsonArrayString(preferredColorsJson, "preferredColorsJson");
        this.preferredMaterialsJson = requireJsonArrayString(preferredMaterialsJson, "preferredMaterialsJson");
        this.styleTagsJson = requireJsonArrayString(styleTagsJson, "styleTagsJson");
    }

    public void ensureDefaultLocation() {
        if (hasLocation()) {
            ensureLocationSource();
            return;
        }
        updateLocation(LocationOption.defaultSeoul());
    }

    public boolean hasLocation() {
        return locationCode != null && locationName != null && locationNx != null && locationNy != null;
    }

    public void ensureLocationSource() {
        if (locationSource == null) {
            locationSource = LocationSource.MANUAL_SEARCH;
        }
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

    public String getName() {
        return name;
    }

    public UserRole getRole() {
        return role;
    }

    public String getLocationCode() {
        return locationCode;
    }

    public String getLocationName() {
        return locationName;
    }

    public String getLocationFullName() {
        return locationFullName;
    }

    public String getLocationRegion1() {
        return locationRegion1;
    }

    public String getLocationRegion2() {
        return locationRegion2;
    }

    public String getLocationRegion3() {
        return locationRegion3;
    }

    public Integer getLocationNx() {
        return locationNx;
    }

    public Integer getLocationNy() {
        return locationNy;
    }

    public LocationSource getLocationSource() {
        return locationSource;
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
        if (email.length() > 255) {
            throw new IllegalArgumentException("email must be 255 characters or less");
        }
        return email;
    }

    private String requirePasswordHash(String passwordHash) {
        Objects.requireNonNull(passwordHash, "passwordHash must not be null");
        if (passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
        if (passwordHash.length() > 255) {
            throw new IllegalArgumentException("passwordHash must be 255 characters or less");
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

    private String requireJsonArrayString(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String trimmed = value.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException(fieldName + " must be a JSON array string");
        }
        return trimmed;
    }

    private static String localAccountEmail(String prefix, String name) {
        String requiredName = Objects.requireNonNull(name, "name must not be null");
        String slug = requiredName.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.isBlank()) {
            slug = "user";
        }
        String suffix = Integer.toUnsignedString(requiredName.hashCode(), 36);
        return prefix + "-" + slug + "-" + suffix + "@smartcloset.local";
    }
}
