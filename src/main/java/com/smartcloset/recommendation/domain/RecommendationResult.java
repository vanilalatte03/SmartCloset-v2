package com.smartcloset.recommendation.domain;

import com.smartcloset.common.domain.BaseTimeEntity;
import com.smartcloset.location.domain.LocationSource;
import com.smartcloset.user.domain.User;
import com.smartcloset.weather.domain.ForecastPeriod;
import com.smartcloset.weather.domain.WeatherCondition;
import com.smartcloset.weather.domain.WeatherLocationSnapshot;
import com.smartcloset.weather.domain.WeatherProviderType;
import com.smartcloset.weather.domain.WeatherSnapshot;
import com.smartcloset.weather.domain.WeatherSource;
import com.smartcloset.weather.domain.WeatherType;
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

@Entity
@Table(
        name = "recommendation_results",
        indexes = {
                @Index(name = "idx_recommendation_results_user_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_recommendation_results_user_worn", columnList = "user_id, worn"),
                @Index(
                        name = "idx_recommendation_results_user_feedback_updated_at",
                        columnList = "user_id, feedback_updated_at"
                ),
                @Index(
                        name = "idx_recommendation_results_user_forecast_created_at",
                        columnList = "user_id, forecast_period, created_at"
                ),
                @Index(name = "idx_recommendation_results_weather_location_code", columnList = "weather_location_code")
        }
)
public class RecommendationResult extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "situation", nullable = false, length = 30, columnDefinition = "varchar(30) default 'CASUAL'")
    private RecommendationSituation situation;

    @Enumerated(EnumType.STRING)
    @Column(name = "forecast_period", nullable = false, length = 30, columnDefinition = "varchar(30) default 'CURRENT'")
    private ForecastPeriod forecastPeriod;

    @Column(name = "weather_temperature", nullable = false)
    private int weatherTemperature;

    @Enumerated(EnumType.STRING)
    @Column(name = "weather_type", nullable = false, length = 30)
    private WeatherType weatherType;

    @Column(name = "rainy", nullable = false)
    private boolean rainy;

    @Column(name = "windy", nullable = false)
    private boolean windy;

    @Column(name = "weather_location_code", nullable = false, length = 30)
    private String weatherLocationCode;

    @Column(name = "weather_location_name", nullable = false, length = 50)
    private String weatherLocationName;

    @Column(name = "weather_location_full_name", length = 100)
    private String weatherLocationFullName;

    @Column(name = "weather_location_nx", nullable = false)
    private int weatherLocationNx;

    @Column(name = "weather_location_ny", nullable = false)
    private int weatherLocationNy;

    @Enumerated(EnumType.STRING)
    @Column(name = "weather_location_source", nullable = false, length = 30)
    private LocationSource weatherLocationSource;

    @Enumerated(EnumType.STRING)
    @Column(name = "weather_provider", nullable = false, length = 30)
    private WeatherProviderType weatherProvider;

    @Column(name = "weather_kma_used", nullable = false)
    private boolean weatherKmaUsed;

    @Column(name = "weather_fallback_used", nullable = false)
    private boolean weatherFallbackUsed;

    @Column(name = "weather_base_date", length = 8)
    private String weatherBaseDate;

    @Column(name = "weather_base_time", length = 4)
    private String weatherBaseTime;

    @Column(name = "weather_forecast_date", length = 8)
    private String weatherForecastDate;

    @Column(name = "weather_forecast_time", length = 4)
    private String weatherForecastTime;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "sentiment_feedback", length = 30)
    private RecommendationFeedbackSentiment sentimentFeedback;

    @Enumerated(EnumType.STRING)
    @Column(name = "thermal_feedback", length = 30)
    private RecommendationThermalFeedback thermalFeedback;

    @Column(name = "feedback_updated_at")
    private LocalDateTime feedbackUpdatedAt;

    protected RecommendationResult() {
    }

    private RecommendationResult(
            User user,
            RecommendationSituation situation,
            ForecastPeriod forecastPeriod,
            WeatherSnapshot weather,
            RecommendationScore score,
            String reasonsJson
    ) {
        this.user = Objects.requireNonNull(user, "user must not be null");
        this.situation = Objects.requireNonNull(situation, "situation must not be null");
        this.forecastPeriod = Objects.requireNonNull(forecastPeriod, "forecastPeriod must not be null");
        WeatherSnapshot requiredWeather = Objects.requireNonNull(weather, "weather must not be null");
        WeatherCondition requiredCondition = requiredWeather.condition();
        WeatherLocationSnapshot requiredLocation = requiredWeather.location();
        WeatherSource requiredSource = requiredWeather.source();
        RecommendationScore requiredScore = Objects.requireNonNull(score, "score must not be null");
        this.weatherTemperature = requiredCondition.temperature();
        this.weatherType = requiredCondition.weatherType();
        this.rainy = requiredCondition.rainy();
        this.windy = requiredCondition.windy();
        this.weatherLocationCode = requiredLocation.code();
        this.weatherLocationName = requiredLocation.name();
        this.weatherLocationFullName = requiredLocation.fullName();
        this.weatherLocationNx = requiredLocation.nx();
        this.weatherLocationNy = requiredLocation.ny();
        this.weatherLocationSource = requiredLocation.source();
        this.weatherProvider = requiredSource.provider();
        this.weatherKmaUsed = requiredSource.kmaUsed();
        this.weatherFallbackUsed = requiredSource.fallbackUsed();
        this.weatherBaseDate = requiredSource.baseDate();
        this.weatherBaseTime = requiredSource.baseTime();
        this.weatherForecastDate = requiredSource.forecastDate();
        this.weatherForecastTime = requiredSource.forecastTime();
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
            RecommendationSituation situation,
            ForecastPeriod forecastPeriod,
            WeatherSnapshot weather,
            RecommendationScore score,
            String reasonsJson
    ) {
        return new RecommendationResult(user, situation, forecastPeriod, weather, score, reasonsJson);
    }

    public static RecommendationResult create(
            User user,
            RecommendationSituation situation,
            WeatherCondition weather,
            RecommendationScore score,
            String reasonsJson
    ) {
        return create(user, situation, ForecastPeriod.CURRENT, defaultWeatherSnapshot(weather), score, reasonsJson);
    }

    public static RecommendationResult create(
            User user,
            WeatherCondition weather,
            RecommendationScore score,
            String reasonsJson
    ) {
        return create(user, RecommendationSituation.CASUAL, weather, score, reasonsJson);
    }

    private static WeatherSnapshot defaultWeatherSnapshot(WeatherCondition weather) {
        return new WeatherSnapshot(
                Objects.requireNonNull(weather, "weather must not be null"),
                new WeatherLocationSnapshot(
                        "SEOUL",
                        "서울특별시",
                        "서울특별시",
                        60,
                        127,
                        LocationSource.MANUAL_SEARCH
                ),
                WeatherSource.fallback(null, null)
        );
    }

    public void markWorn() {
        this.worn = true;
    }

    public void replaceFeedback(
            RecommendationFeedbackSentiment sentimentFeedback,
            RecommendationThermalFeedback thermalFeedback,
            LocalDateTime updatedAt
    ) {
        if (sentimentFeedback == null && thermalFeedback == null) {
            clearFeedback();
            return;
        }
        this.sentimentFeedback = sentimentFeedback;
        this.thermalFeedback = thermalFeedback;
        this.feedbackUpdatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public void clearFeedback() {
        this.sentimentFeedback = null;
        this.thermalFeedback = null;
        this.feedbackUpdatedAt = null;
    }

    public boolean hasFeedback() {
        return sentimentFeedback != null || thermalFeedback != null;
    }

    public boolean isWorn() {
        return worn;
    }

    public RecommendationFeedbackSentiment getSentimentFeedback() {
        return sentimentFeedback;
    }

    public RecommendationThermalFeedback getThermalFeedback() {
        return thermalFeedback;
    }

    public LocalDateTime getFeedbackUpdatedAt() {
        return feedbackUpdatedAt;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public RecommendationSituation getSituation() {
        return situation;
    }

    public ForecastPeriod getForecastPeriod() {
        return forecastPeriod;
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

    public String getWeatherLocationCode() {
        return weatherLocationCode;
    }

    public String getWeatherLocationName() {
        return weatherLocationName;
    }

    public String getWeatherLocationFullName() {
        return weatherLocationFullName;
    }

    public int getWeatherLocationNx() {
        return weatherLocationNx;
    }

    public int getWeatherLocationNy() {
        return weatherLocationNy;
    }

    public LocationSource getWeatherLocationSource() {
        return weatherLocationSource;
    }

    public WeatherProviderType getWeatherProvider() {
        return weatherProvider;
    }

    public boolean isWeatherKmaUsed() {
        return weatherKmaUsed;
    }

    public boolean isWeatherFallbackUsed() {
        return weatherFallbackUsed;
    }

    public String getWeatherBaseDate() {
        return weatherBaseDate;
    }

    public String getWeatherBaseTime() {
        return weatherBaseTime;
    }

    public String getWeatherForecastDate() {
        return weatherForecastDate;
    }

    public String getWeatherForecastTime() {
        return weatherForecastTime;
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

    private String requireReasonsJson(String reasonsJson) {
        Objects.requireNonNull(reasonsJson, "reasonsJson must not be null");
        if (reasonsJson.isBlank()) {
            throw new IllegalArgumentException("reasonsJson must not be blank");
        }
        return reasonsJson;
    }
}
