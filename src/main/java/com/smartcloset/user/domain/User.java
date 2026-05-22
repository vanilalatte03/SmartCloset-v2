package com.smartcloset.user.domain;

import com.smartcloset.common.domain.BaseTimeEntity;
import com.smartcloset.location.domain.LocationOption;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(
        name = "users",
        indexes = @Index(name = "idx_users_location_code", columnList = "location_code")
)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    protected User() {
    }

    private User(String name) {
        this.name = requireName(name);
    }

    public static User create(String name) {
        return new User(name);
    }

    public static User createSeedUser(String name) {
        User user = new User(name);
        user.ensureDefaultLocation();
        return user;
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
