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
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 사용자가 등록한 옷 한 벌을 표현하는 entity다.
 *
 * <p>이미지 bytes는 DB에 저장하지 않고, 이 entity에는 저장소에서 파일을 찾기 위한 메타데이터만 둔다.
 * {@code archived=true}는 삭제가 아니라 추천 후보에서 제외되는 보관 상태를 뜻한다.</p>
 */
@Entity
@Table(
        name = "clothing_items",
        indexes = {
                @Index(name = "idx_clothing_items_user_archived_id", columnList = "user_id, archived, id"),
                @Index(name = "idx_clothing_items_user_category_archived", columnList = "user_id, category, archived")
        }
)
public class ClothingItem extends BaseTimeEntity {

    private static final String EMPTY_JSON_ARRAY = "[]";

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

    @Column(name = "style_tags_json", nullable = false, columnDefinition = "TEXT")
    private String styleTagsJson;

    @Column(name = "archived", nullable = false)
    private boolean archived;

    @Column(name = "image_stored_filename", length = 255)
    private String imageStoredFilename;

    @Column(name = "image_content_type", length = 100)
    private String imageContentType;

    @Column(name = "image_size_bytes")
    private Long imageSizeBytes;

    @Column(name = "image_uploaded_at")
    private LocalDateTime imageUploadedAt;

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
            boolean rainSuitable,
            String styleTagsJson
    ) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        updateDetails(name, category, color, material, minTemperature, maxTemperature, rainSuitable, styleTagsJson);
        this.archived = false;
    }

    /**
     * style tag가 없는 기본 옷 등록 경로에서 빈 JSON 배열을 기본값으로 둔다.
     */
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
        return new ClothingItem(
                user,
                name,
                category,
                color,
                material,
                minTemperature,
                maxTemperature,
                rainSuitable,
                EMPTY_JSON_ARRAY
        );
    }

    /**
     * style tag JSON snapshot까지 포함해 새 옷 entity를 만든다.
     */
    public static ClothingItem create(
            User user,
            String name,
            ClothingCategory category,
            ClothingColor color,
            ClothingMaterial material,
            int minTemperature,
            int maxTemperature,
            boolean rainSuitable,
            String styleTagsJson
    ) {
        return new ClothingItem(
                user,
                name,
                category,
                color,
                material,
                minTemperature,
                maxTemperature,
                rainSuitable,
                styleTagsJson
        );
    }

    /**
     * style tag를 유지한 채 옷 기본 정보만 전체 교체한다.
     */
    public void updateDetails(
            String name,
            ClothingCategory category,
            ClothingColor color,
            ClothingMaterial material,
            int minTemperature,
            int maxTemperature,
            boolean rainSuitable
    ) {
        updateDetails(name, category, color, material, minTemperature, maxTemperature, rainSuitable, getStyleTagsJson());
    }

    /**
     * 옷 기본 정보는 수정 시 전체 교체한다. styleTagsJson은 빈 배열 문자열까지 유효한 값으로 취급한다.
     */
    public void updateDetails(
            String name,
            ClothingCategory category,
            ClothingColor color,
            ClothingMaterial material,
            int minTemperature,
            int maxTemperature,
            boolean rainSuitable,
            String styleTagsJson
    ) {
        validateTemperatureRange(minTemperature, maxTemperature);
        this.name = requireName(name);
        this.category = Objects.requireNonNull(category, "category must not be null");
        this.color = Objects.requireNonNull(color, "color must not be null");
        this.material = Objects.requireNonNull(material, "material must not be null");
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.rainSuitable = rainSuitable;
        this.styleTagsJson = requireJsonArrayString(styleTagsJson, "styleTagsJson");
    }

    /**
     * 옷을 보관 상태로 전환해 기본 목록과 추천 후보에서 제외되게 한다.
     */
    public void archive() {
        this.archived = true;
    }

    /**
     * 보관된 옷을 활성 상태로 되돌린다.
     */
    public void unarchive() {
        this.archived = false;
    }

    /**
     * 저장소에 파일 쓰기가 끝난 뒤 DB에 남길 이미지 메타데이터를 갱신한다.
     */
    public void updateImageMetadata(
            String storedFilename,
            String contentType,
            long sizeBytes,
            LocalDateTime uploadedAt
    ) {
        this.imageStoredFilename = requireImageStoredFilename(storedFilename);
        this.imageContentType = requireImageContentType(contentType);
        this.imageSizeBytes = requireImageSizeBytes(sizeBytes);
        this.imageUploadedAt = Objects.requireNonNull(uploadedAt, "uploadedAt must not be null");
    }

    /**
     * 저장소 파일 삭제 후 DB에 남은 이미지 참조 정보를 모두 제거한다.
     */
    public void clearImageMetadata() {
        this.imageStoredFilename = null;
        this.imageContentType = null;
        this.imageSizeBytes = null;
        this.imageUploadedAt = null;
    }

    /**
     * 테스트와 도메인 정책 검증에서 소유자 경계를 확인할 때 사용한다.
     */
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

    public String getStyleTagsJson() {
        // 과거 row 또는 JPA fixture에서 null이 들어와도 추천 로직에는 빈 배열로 전달한다.
        return styleTagsJson == null ? EMPTY_JSON_ARRAY : styleTagsJson;
    }

    public boolean isArchived() {
        return archived;
    }

    public String getImageStoredFilename() {
        return imageStoredFilename;
    }

    public String getImageContentType() {
        return imageContentType;
    }

    public Long getImageSizeBytes() {
        return imageSizeBytes;
    }

    public LocalDateTime getImageUploadedAt() {
        return imageUploadedAt;
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

    private String requireImageStoredFilename(String storedFilename) {
        Objects.requireNonNull(storedFilename, "storedFilename must not be null");
        if (storedFilename.isBlank()) {
            throw new IllegalArgumentException("storedFilename must not be blank");
        }
        if (storedFilename.length() > 255) {
            throw new IllegalArgumentException("storedFilename must be 255 characters or less");
        }
        return storedFilename;
    }

    private String requireImageContentType(String contentType) {
        Objects.requireNonNull(contentType, "contentType must not be null");
        if (contentType.isBlank()) {
            throw new IllegalArgumentException("contentType must not be blank");
        }
        if (contentType.length() > 100) {
            throw new IllegalArgumentException("contentType must be 100 characters or less");
        }
        return contentType;
    }

    private Long requireImageSizeBytes(long sizeBytes) {
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("sizeBytes must be positive");
        }
        return sizeBytes;
    }

    private String requireJsonArrayString(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        String trimmed = value.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException(fieldName + " must be a JSON array string");
        }
        return trimmed;
    }

    private void validateTemperatureRange(int minTemperature, int maxTemperature) {
        if (minTemperature > maxTemperature) {
            throw new IllegalArgumentException("minTemperature must be less than or equal to maxTemperature");
        }
    }
}
