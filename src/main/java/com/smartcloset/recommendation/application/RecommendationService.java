package com.smartcloset.recommendation.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartcloset.clothing.application.ClothingStyleTagMapper;
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
import com.smartcloset.recommendation.domain.RecommendationHistorySnapshot;
import com.smartcloset.recommendation.domain.RecommendationReasonGenerator;
import com.smartcloset.recommendation.domain.RecommendationResult;
import com.smartcloset.recommendation.domain.RecommendationResultItem;
import com.smartcloset.recommendation.domain.RecommendationScorer;
import com.smartcloset.recommendation.domain.ScoredOutfitCandidate;
import com.smartcloset.recommendation.domain.WeatherFilteredClothes;
import com.smartcloset.recommendation.domain.WeatherSuitabilityFilter;
import com.smartcloset.recommendation.domain.WearHistory;
import com.smartcloset.recommendation.domain.WearHistorySnapshot;
import com.smartcloset.recommendation.dto.RecommendationResponse;
import com.smartcloset.recommendation.dto.RecommendationWornResponse;
import com.smartcloset.recommendation.repository.RecommendationResultItemRepository;
import com.smartcloset.recommendation.repository.RecommendationResultRepository;
import com.smartcloset.recommendation.repository.WearHistoryRepository;
import com.smartcloset.user.domain.PreferenceJsonMapper;
import com.smartcloset.user.domain.User;
import com.smartcloset.user.repository.UserRepository;
import com.smartcloset.weather.application.WeatherProvider;
import com.smartcloset.weather.domain.WeatherCondition;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final RecommendationResultItemRepository recommendationResultItemRepository;
    private final WearHistoryRepository wearHistoryRepository;
    private final WeatherProvider weatherProvider;
    private final PreferenceJsonMapper preferenceJsonMapper;
    private final ClothingStyleTagMapper clothingStyleTagMapper;
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
            RecommendationResultItemRepository recommendationResultItemRepository,
            WearHistoryRepository wearHistoryRepository,
            WeatherProvider weatherProvider,
            PreferenceJsonMapper preferenceJsonMapper,
            ClothingStyleTagMapper clothingStyleTagMapper,
            PlatformTransactionManager transactionManager
    ) {
        this.userRepository = userRepository;
        this.clothingItemRepository = clothingItemRepository;
        this.recommendationResultRepository = recommendationResultRepository;
        this.recommendationResultItemRepository = recommendationResultItemRepository;
        this.wearHistoryRepository = wearHistoryRepository;
        this.weatherProvider = weatherProvider;
        this.preferenceJsonMapper = preferenceJsonMapper;
        this.clothingStyleTagMapper = clothingStyleTagMapper;
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
        List<WearHistorySnapshot> wearHistories = findWearHistorySnapshots(userId, requestedAt);
        List<RecommendationHistorySnapshot> recommendationHistories = findRecommendationHistories(userId, requestedAt);
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
            return RecommendationResponse.from(recommendationResult, best.candidate(), reasons, clothingStyleTagMapper);
        } catch (RecommendationFailureException exception) {
            throw toSmartClosetException(exception);
        }
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> getRecommendationHistory(Long userId, Integer limit) {
        int resolvedLimit = validateHistoryLimit(limit);
        List<Long> orderedResultIds = recommendationResultRepository.findIdsByUserIdOrderByCreatedAtDesc(
                userId,
                PageRequest.of(0, resolvedLimit)
        );
        if (orderedResultIds.isEmpty()) {
            return List.of();
        }

        Map<Long, List<RecommendationResultItem>> itemsByResultId = findItemsByRecommendationResultIds(orderedResultIds);
        return findResultsInOrderedIds(orderedResultIds).stream()
                .map(result -> RecommendationResponse.from(
                        result,
                        itemsByResultId.getOrDefault(result.getId(), List.of()),
                        readReasonsJson(result.getReasonsJson()),
                        clothingStyleTagMapper
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
        RecommendationResult savedRecommendationResult = recommendationResultRepository.save(recommendationResult);
        recommendationResultItemRepository.saveAll(createRecommendationResultItems(
                savedRecommendationResult,
                best.candidate()
        ));
        return savedRecommendationResult;
    }

    private List<RecommendationResultItem> createRecommendationResultItems(
            RecommendationResult recommendationResult,
            OutfitCandidate candidate
    ) {
        List<RecommendationResultItem> resultItems = new ArrayList<>();
        resultItems.add(RecommendationResultItem.of(recommendationResult, candidate.top(), OutfitSlot.TOP));
        resultItems.add(RecommendationResultItem.of(recommendationResult, candidate.bottom(), OutfitSlot.BOTTOM));
        if (candidate.hasOuter()) {
            resultItems.add(RecommendationResultItem.of(recommendationResult, candidate.outer(), OutfitSlot.OUTER));
        }
        return resultItems;
    }

    private List<WearHistorySnapshot> findWearHistorySnapshots(Long userId, LocalDateTime requestedAt) {
        List<WearHistory> wearHistories = wearHistoryRepository.findByUserIdAndWornAtGreaterThanEqualOrderByWornAtDesc(
                userId,
                requestedAt.minusDays(7)
        );
        List<Long> orderedResultIds = wearHistories.stream()
                .map(history -> history.getRecommendationResult().getId())
                .toList();
        Map<Long, Set<Long>> itemIdsByResultId = findItemIdsByRecommendationResultIds(orderedResultIds);

        return wearHistories.stream()
                .map(history -> {
                    Long recommendationResultId = history.getRecommendationResult().getId();
                    return new WearHistorySnapshot(
                            recommendationResultId,
                            history.getWornAt(),
                            itemIdsByResultId.getOrDefault(recommendationResultId, Set.of())
                    );
                })
                .toList();
    }

    private List<RecommendationHistorySnapshot> findRecommendationHistories(Long userId, LocalDateTime requestedAt) {
        List<Long> lastSevenDaysIds = recommendationResultRepository
                .findIdsByUserIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(userId, requestedAt.minusDays(7));
        List<Long> recentFiveIds = recommendationResultRepository.findIdsByUserIdOrderByCreatedAtDesc(
                userId,
                PageRequest.of(0, 5)
        );

        // Preserve the previous priority: 7-day ids first, then recent-five ids as backfill.
        LinkedHashSet<Long> orderedHistoryIds = new LinkedHashSet<>();
        orderedHistoryIds.addAll(lastSevenDaysIds);
        orderedHistoryIds.addAll(recentFiveIds);
        return findRecommendationHistorySnapshots(List.copyOf(orderedHistoryIds));
    }

    private List<RecommendationHistorySnapshot> findRecommendationHistorySnapshots(List<Long> orderedResultIds) {
        if (orderedResultIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Set<Long>> itemIdsByResultId = findItemIdsByRecommendationResultIds(orderedResultIds);
        return findResultsInOrderedIds(orderedResultIds).stream()
                .map(result -> new RecommendationHistorySnapshot(
                        result.getId(),
                        result.getCreatedAt(),
                        itemIdsByResultId.getOrDefault(result.getId(), Set.of())
                ))
                .toList();
    }

    private List<RecommendationResult> findResultsInOrderedIds(List<Long> orderedResultIds) {
        Map<Long, RecommendationResult> resultById = recommendationResultRepository.findByIdIn(orderedResultIds)
                .stream()
                .collect(Collectors.toMap(
                        RecommendationResult::getId,
                        Function.identity()
                ));
        // IN queries do not guarantee row order; the preselected id list is the source of truth.
        return orderedResultIds.stream()
                .map(resultId -> requireResult(resultById, resultId))
                .toList();
    }

    private Map<Long, List<RecommendationResultItem>> findItemsByRecommendationResultIds(List<Long> orderedResultIds) {
        if (orderedResultIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, List<RecommendationResultItem>> itemsByResultId = new LinkedHashMap<>();
        for (Long resultId : orderedResultIds) {
            itemsByResultId.put(resultId, new ArrayList<>());
        }

        recommendationResultItemRepository.findByRecommendationResultIdInWithClothingItem(orderedResultIds)
                .forEach(item -> {
                    List<RecommendationResultItem> resultItems = itemsByResultId.get(item.getRecommendationResult().getId());
                    if (resultItems != null) {
                        resultItems.add(item);
                    }
                });
        return itemsByResultId;
    }

    private Map<Long, Set<Long>> findItemIdsByRecommendationResultIds(List<Long> orderedResultIds) {
        return findItemsByRecommendationResultIds(orderedResultIds).entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream()
                                .map(RecommendationResultItem::getClothingItem)
                                .map(ClothingItem::getId)
                                .filter(Objects::nonNull)
                                .collect(Collectors.toUnmodifiableSet()),
                        (left, right) -> left,
                        LinkedHashMap::new
                ));
    }

    private RecommendationResult requireResult(Map<Long, RecommendationResult> resultById, Long resultId) {
        RecommendationResult result = resultById.get(resultId);
        if (result == null) {
            throw new SmartClosetException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return result;
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
