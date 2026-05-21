package com.smartcloset.clothing.domain;

import com.smartcloset.common.domain.BaseTimeEntity;
import com.smartcloset.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(
        name = "clothing_items",
        indexes = {
                @Index(name = "idx_clothing_items_user_archived_id", columnList = "user_id, archived, id"),
                @Index(name = "idx_clothing_items_user_category_archived", columnList = "user_id, category, archived")
        }
)
public class ClothingItem extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private ClothingCategory category;

    @Enumerated(EnumType.STRING)
    @Column(name = "color", nullable = false, length = 30)
    private ClothingColor color;

    @Enumerated(EnumType.STRING)
    @Column(name = "material", nullable = false, length = 30)
    private ClothingMaterial material;

    @Column(name = "min_temperature", nullable = false)
    private int minTemperature;

    @Column(name = "max_temperature", nullable = false)
    private int maxTemperature;

    @Column(name = "rain_suitable", nullable = false)
    private boolean rainSuitable;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    protected ClothingItem() {
    }

    private ClothingItem(
            User user,
            String name,
            ClothingCategory category,
            ClothingColor color,
            ClothingMaterial material,
            int minTemperature,
            int maxTemperature,
            boolean rainSuitable
    ) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        updateDetails(name, category, color, material, minTemperature, maxTemperature, rainSuitable);
        this.archived = false;
    }

    public static ClothingItem create(
            User user,
            String name,
            ClothingCategory category,
            ClothingColor color,
            ClothingMaterial material,
            int minTemperature,
            int maxTemperature,
            boolean rainSuitable
    ) {
        return new ClothingItem(user, name, category, color, material, minTemperature, maxTemperature, rainSuitable);
    }

    public void updateDetails(
            String name,
            ClothingCategory category,
            ClothingColor color,
            ClothingMaterial material,
            int minTemperature,
            int maxTemperature,
            boolean rainSuitable
    ) {
        validateTemperatureRange(minTemperature, maxTemperature);
        this.name = requireName(name);
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.color = Objects.requireNonNull(color, "color must not be null");
        this.material = Objects.requireNonNull(material, "material must not be null");
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.rainSuitable = rainSuitable;
    }

    public void archive() {
        this.archived = true;
    }

    public boolean belongsTo(Long userId) {
        return user != null && Objects.equals(user.getId(), userId);
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public String getName() {
        return name;
    }

    public ClothingCategory getCategory() {
        return category;
    }

    public ClothingColor getColor() {
        return color;
    }

    public ClothingMaterial getMaterial() {
        return material;
    }

    public int getMinTemperature() {
        return minTemperature;
    }

    public int getMaxTemperature() {
        return maxTemperature;
    }

    public boolean isRainSuitable() {
        return rainSuitable;
    }

    public boolean isArchived() {
        return archived;
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

    private void validateTemperatureRange(int minTemperature, int maxTemperature) {
        if (minTemperature > maxTemperature) {
            throw new IllegalArgumentException("minTemperature must be less than or equal to maxTemperature");
        }
    }
}
