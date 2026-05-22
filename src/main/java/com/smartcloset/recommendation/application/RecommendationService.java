package com.smartcloset.recommendation.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.clothing.domain.ClothingColor;
import com.smartcloset.clothing.domain.ClothingItem;
import com.smartcloset.clothing.domain.ClothingMaterial;
import com.smartcloset.clothing.repository.ClothingItemRepository;
import com.smartcloset.common.exception.ErrorCode;
import com.smartcloset.common.exception.SmartClosetException;
import com.smartcloset.recommendation.domain.OutfitCandidate;
import com.smartcloset.recommendation.domain.OutfitCandidateGenerator;
import com.smartcloset.recommendation.domain.OutfitSlot;
import com.smartcloset.recommendation.domain.RecommendationFailureException;
import com.smartcloset.recommendation.domain.RecommendationReasonGenerator;
import com.smartcloset.recommendation.domain.RecommendationResult;
import com.smartcloset.recommendation.domain.RecommendationScorer;
import com.smartcloset.recommendation.domain.ScoredOutfitCandidate;
import com.smartcloset.recommendation.domain.WeatherFilteredClothes;
import com.smartcloset.recommendation.domain.WeatherSuitabilityFilter;
import com.smartcloset.recommendation.domain.WearHistory;
import com.smartcloset.recommendation.dto.RecommendationResponse;
import com.smartcloset.recommendation.dto.RecommendationWornResponse;
import com.smartcloset.recommendation.repository.RecommendationResultRepository;
import com.smartcloset.recommendation.repository.WearHistoryRepository;
import com.smartcloset.user.domain.PreferenceJsonMapper;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import com.smartcloset.weather.application.WeatherProvider;
import com.smartcloset.weather.domain.WeatherCondition;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class RecommendationService {

    private static final int DEFAULT_HISTORY_LIMIT = 20;
    private static final int MIN_HISTORY_LIMIT = 1;
    private static final int MAX_HISTORY_LIMIT = 50;
    private static final TypeReference<List<String>> REASONS_TYPE = new TypeReference<>() {
    };

    private final UserRepository userRepository;
    private final ClothingItemRepository clothingItemRepository;
    private final RecommendationResultRepository recommendationResultRepository;
    private final WearHistoryRepository wearHistoryRepository;
    private final WeatherProvider weatherProvider;
    private final PreferenceJsonMapper preferenceJsonMapper;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WeatherSuitabilityFilter weatherSuitabilityFilter = new WeatherSuitabilityFilter();
    private final OutfitCandidateGenerator outfitCandidateGenerator = new OutfitCandidateGenerator();
    private final RecommendationScorer recommendationScorer = new RecommendationScorer();
    private final RecommendationReasonGenerator recommendationReasonGenerator = new RecommendationReasonGenerator();

    public RecommendationService(
            UserRepository userRepository,
            ClothingItemRepository clothingItemRepository,
            RecommendationResultRepository recommendationResultRepository,
            WearHistoryRepository wearHistoryRepository,
            WeatherProvider weatherProvider,
            PreferenceJsonMapper preferenceJsonMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.userRepository = userRepository;
        this.clothingItemRepository = clothingItemRepository;
        this.recommendationResultRepository = recommendationResultRepository;
        this.wearHistoryRepository = wearHistoryRepository;
        this.weatherProvider = weatherProvider;
        this.preferenceJsonMapper = preferenceJsonMapper;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public RecommendationResponse createRecommendation(Long userId) {
        WeatherCondition weather = weatherProvider.getCurrentWeather(userId);
        LocalDateTime requestedAt = LocalDateTime.now();
        return Objects.requireNonNull(transactionTemplate.execute(status ->
                createRecommendationInTransaction(userId, weather, requestedAt)
        ));
    }

    private RecommendationResponse createRecommendationInTransaction(
            Long userId,
            WeatherCondition weather,
            LocalDateTime requestedAt
    ) {
        User user = findUser(userId);
        List<ClothingItem> activeClothes = clothingItemRepository.findByUserIdAndArchivedFalseOrderByIdAsc(userId);
        List<WearHistory> wearHistories = wearHistoryRepository.findByUserIdAndWornAtGreaterThanEqualOrderByWornAtDesc(
                userId,
                requestedAt.minusDays(7)
        );
        List<RecommendationResult> recommendationHistories = findRecommendationHistories(userId, requestedAt);
        List<ClothingColor> preferredColors = preferenceJsonMapper.readColors(user.getPreferredColorsJson());
        List<ClothingMaterial> preferredMaterials = preferenceJsonMapper.readMaterials(user.getPreferredMaterialsJson());

        try {
            WeatherFilteredClothes filteredClothes = weatherSuitabilityFilter.filter(activeClothes, weather);
            List<OutfitCandidate> candidates = outfitCandidateGenerator.generate(filteredClothes, weather);
            List<ScoredOutfitCandidate> scoredCandidates = recommendationScorer.scoreAll(
                    candidates,
                    weather,
                    wearHistories,
                    recommendationHistories,
                    requestedAt,
                    preferredColors,
                    preferredMaterials
            );
            ScoredOutfitCandidate best = recommendationScorer.selectBest(scoredCandidates, weather);
            List<String> reasons = recommendationReasonGenerator.generate(
                    best.candidate(),
                    best.score(),
                    weather,
                    wearHistories,
                    recommendationHistories,
                    requestedAt
            );
            RecommendationResult recommendationResult = saveRecommendation(user, weather, best, reasons);
            return RecommendationResponse.from(recommendationResult, best.candidate(), reasons);
        } catch (RecommendationFailureException exception) {
            throw toSmartClosetException(exception);
        }
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendationHistory(Long userId, Integer limit) {
        int resolvedLimit = validateHistoryLimit(limit);
        return recommendationResultRepository.findByUserIdOrderByCreatedAtDesc(
                        userId,
                        PageRequest.of(0, resolvedLimit)
                )
                .stream()
                .map(recommendationResult -> RecommendationResponse.from(
                        recommendationResult,
                        readReasonsJson(recommendationResult.getReasonsJson())
                ))
                .toList();
    }

    @Transactional
    public RecommendationWornResponse markWorn(Long userId, Long recommendationId) {
        User user = findUser(userId);
        RecommendationResult recommendationResult = recommendationResultRepository
                .findByIdAndUserId(recommendationId, userId)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.RECOMMENDATION_NOT_FOUND));

        Optional<WearHistory> existingWearHistory = wearHistoryRepository.findByRecommendationResultId(
                recommendationResult.getId()
        );
        if (existingWearHistory.isPresent()) {
            recommendationResult.markWorn();
            return RecommendationWornResponse.of(recommendationResult.getId(), existingWearHistory.get().getWornAt());
        }

        recommendationResult.markWorn();
        WearHistory wearHistory = wearHistoryRepository.save(WearHistory.record(
                user,
                recommendationResult,
                LocalDateTime.now()
        ));
        return RecommendationWornResponse.of(recommendationResult.getId(), wearHistory.getWornAt());
    }

    private RecommendationResult saveRecommendation(
            User user,
            WeatherCondition weather,
            ScoredOutfitCandidate best,
            List<String> reasons
    ) {
        RecommendationResult recommendationResult = RecommendationResult.create(
                user,
                weather,
                best.score(),
                writeReasonsJson(reasons)
        );
        OutfitCandidate candidate = best.candidate();
        recommendationResult.addItem(candidate.top(), OutfitSlot.TOP);
        recommendationResult.addItem(candidate.bottom(), OutfitSlot.BOTTOM);
        if (candidate.hasOuter()) {
            recommendationResult.addItem(candidate.outer(), OutfitSlot.OUTER);
        }
        return recommendationResultRepository.save(recommendationResult);
    }

    private List<RecommendationResult> findRecommendationHistories(Long userId, LocalDateTime requestedAt) {
        List<RecommendationResult> lastSevenDays = recommendationResultRepository
                .findByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(userId, requestedAt.minusDays(7));
        List<RecommendationResult> recentFive = recommendationResultRepository.findTop5ByUserIdOrderByCreatedAtDesc(
                userId
        );

        Map<Long, RecommendationResult> histories = new LinkedHashMap<>();
        Stream.concat(lastSevenDays.stream(), recentFive.stream())
                .forEach(history -> histories.putIfAbsent(history.getId(), history));
        return List.copyOf(histories.values());
    }

    private String writeReasonsJson(List<String> reasons) {
        try {
            return objectMapper.writeValueAsString(reasons);
        } catch (JsonProcessingException exception) {
            throw new SmartClosetException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private List<String> readReasonsJson(String reasonsJson) {
        try {
            return objectMapper.readValue(reasonsJson, REASONS_TYPE);
        } catch (JsonProcessingException exception) {
            throw new SmartClosetException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private int validateHistoryLimit(Integer limit) {
        int resolvedLimit = limit == null ? DEFAULT_HISTORY_LIMIT : limit;
        if (resolvedLimit < MIN_HISTORY_LIMIT || resolvedLimit > MAX_HISTORY_LIMIT) {
            throw new SmartClosetException(ErrorCode.INVALID_REQUEST);
        }
        return resolvedLimit;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new SmartClosetException(ErrorCode.USER_NOT_FOUND));
    }

    private SmartClosetException toSmartClosetException(RecommendationFailureException exception) {
        return new SmartClosetException(ErrorCode.valueOf(exception.failureCode().name()));
    }
}
