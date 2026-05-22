package com.smartcloset.recommendation.domain;

import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.common.domain.BaseTimeEntity;
import com.smartcloset.user.domain.User;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherType;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
        name = "recommendation_results",
        indexes = {
                @Index(name = "idx_recommendation_results_user_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_recommendation_results_user_worn", columnList = "user_id, worn")
        }
)
public class RecommendationResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "weather_temperature", nullable = false)
    private int weatherTemperature;

    @Enumerated(EnumType.STRING)
    @Column(name = "weather_type", nullable = false, length = 30)
    private WeatherType weatherType;

    @Column(name = "rainy", nullable = false)
    private boolean rainy;

    @Column(name = "windy", nullable = false)
    private boolean windy;

    @Column(name = "total_score", nullable = false)
    private int totalScore;

    @Column(name = "weather_score", nullable = false)
    private int weatherScore;

    @Column(name = "color_score", nullable = false)
    private int colorScore;

    @Column(name = "wear_history_score", nullable = false)
    private int wearHistoryScore;

    @Column(name = "recommendation_history_score", nullable = false)
    private int recommendationHistoryScore;

    @Column(name = "preference_score", nullable = false)
    private int preferenceScore;

    @Column(name = "reasons_json", nullable = false, columnDefinition = "json")
    private String reasonsJson;

    @Column(name = "worn", nullable = false)
    private boolean worn;

    @OneToMany(mappedBy = "recommendationResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<RecommendationResultItem> items = new ArrayList<>();

    @OneToOne(mappedBy = "recommendationResult", fetch = FetchType.LAZY)
    private WearHistory wearHistory;

    protected RecommendationResult() {
    }

    private RecommendationResult(User user, WeatherCondition weather, RecommendationScore score, String reasonsJson) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        WeatherCondition requiredWeather = Objects.requireNonNull(weather, "weather must not be null");
        RecommendationScore requiredScore = Objects.requireNonNull(score, "score must not be null");
        this.weatherTemperature = requiredWeather.temperature();
        this.weatherType = requiredWeather.weatherType();
        this.rainy = requiredWeather.rainy();
        this.windy = requiredWeather.windy();
        this.totalScore = requiredScore.totalScore();
        this.weatherScore = requiredScore.weatherScore();
        this.colorScore = requiredScore.colorScore();
        this.wearHistoryScore = requiredScore.wearHistoryScore();
        this.recommendationHistoryScore = requiredScore.recommendationHistoryScore();
        this.preferenceScore = requiredScore.preferenceScore();
        this.reasonsJson = requireReasonsJson(reasonsJson);
        this.worn = false;
    }

    public static RecommendationResult create(
            User user,
            WeatherCondition weather,
            RecommendationScore score,
            String reasonsJson
    ) {
        return new RecommendationResult(user, weather, score, reasonsJson);
    }

    public void addItem(ClothingItem clothingItem, OutfitSlot slot) {
        RecommendationResultItem item = RecommendationResultItem.of(this, clothingItem, slot);
        this.items.add(item);
    }

    public void markWorn() {
        this.worn = true;
    }

    public boolean isWorn() {
        return worn;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public int getWeatherTemperature() {
        return weatherTemperature;
    }

    public WeatherType getWeatherType() {
        return weatherType;
    }

    public boolean isRainy() {
        return rainy;
    }

    public boolean isWindy() {
        return windy;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public int getWeatherScore() {
        return weatherScore;
    }

    public int getColorScore() {
        return colorScore;
    }

    public int getWearHistoryScore() {
        return wearHistoryScore;
    }

    public int getRecommendationHistoryScore() {
        return recommendationHistoryScore;
    }

    public int getPreferenceScore() {
        return preferenceScore;
    }

    public String getReasonsJson() {
        return reasonsJson;
    }

    public List<RecommendationResultItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public WearHistory getWearHistory() {
        return wearHistory;
    }

    private String requireReasonsJson(String reasonsJson) {
        Objects.requireNonNull(reasonsJson, "reasonsJson must not be null");
        if (reasonsJson.isBlank()) {
            throw new IllegalArgumentException("reasonsJson must not be blank");
        }
        return reasonsJson;
    }
}
