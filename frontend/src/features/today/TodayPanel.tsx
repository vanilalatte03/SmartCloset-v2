import { useCallback, useEffect, useMemo, useState } from 'react';
import {
  isUnauthorizedError,
  toErrorResponse,
  toRecommendationFailureCta,
} from '../../api/errorHelpers';
import {
  createRecommendation,
  getClothes,
  getCurrentWeather,
  getRecommendationHistory,
  getUserPreferences,
  markRecommendationWorn,
  replaceRecommendationFeedback,
} from '../../api/smartClosetApi';
import { ApiErrorMessage } from '../../components/ApiErrorMessage';
import {
  ColorSwatch,
  MaterialChip,
  WeatherLabel,
} from '../../components/DisplayTokens';
import { RecommendationPanel } from '../recommendation/RecommendationPanel';
import type {
  ClothingCategory,
  ClothingResponse,
  ErrorResponse,
  ForecastPeriod,
  RecommendationFeedbackSentiment,
  RecommendationResponse,
  RecommendationSituation,
  RecommendationThermalFeedback,
  UserLocationResponse,
  UserPreferencesResponse,
  WeatherResponse,
} from '../../types/api';
import {
  clothingCategoryLabels,
  forecastPeriodLabels,
  recommendationFeedbackSentimentLabels,
  recommendationSituationLabels,
  recommendationThermalFeedbackLabels,
  weatherTypeLabels,
  type RecommendationFailureCta,
} from '../../utils/displayMappings';

type TodayTargetView = 'closet' | 'preferences' | 'location' | 'history';
type TodayNavigationOptions = {
  closetCategory?: ClothingCategory;
};

type TodayPanelProps = {
  accessToken: string;
  location: UserLocationResponse | null;
  locationRevision: number;
  preferencesRevision: number;
  onAuthExpired: () => void;
  onNavigate: (view: TodayTargetView, options?: TodayNavigationOptions) => void;
};

type ChecklistItem = {
  id: string;
  label: string;
  complete: boolean;
  detail: string;
  ctaLabel: string;
  targetView: TodayTargetView;
  category?: ClothingCategory;
};

const recentHistoryLimit = 20;
const recentHistoryPreviewCount = 3;
const recentHistoryApiPath = '/api/recommendations?limit=20';

const requiredCategories: ClothingCategory[] = ['TOP', 'BOTTOM', 'OUTER'];

function getActiveCategoryCounts(clothes: ClothingResponse[]): Record<ClothingCategory, number> {
  return clothes.reduce<Record<ClothingCategory, number>>(
    (counts, item) => {
      if (!item.archived) {
        counts[item.category] += 1;
      }
      return counts;
    },
    {
      TOP: 0,
      BOTTOM: 0,
      OUTER: 0,
    }
  );
}

function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('ko-KR', {
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

function formatWeatherTime(value: string | null): string {
  if (!value) {
    return '--:--';
  }

  const digits = value.replace(/\D/g, '');
  if (digits.length >= 4) {
    return `${digits.slice(0, 2)}:${digits.slice(2, 4)}`;
  }

  return value;
}

function renderWeatherState(weather: WeatherResponse, forecastPeriod: ForecastPeriod) {
  const rainLabel = weather.rainy ? '비 가능' : '비 없음';
  const windLabel = weather.windy ? '바람 강함' : '바람 잔잔';
  const updateTime = formatWeatherTime(weather.source.baseTime);
  const forecastTime = formatWeatherTime(weather.source.forecastTime);

  return (
    <div className="today-weather-state">
      <div className="today-weather-main">
        <div>
          <p className="eyebrow">날씨</p>
          <strong>{weather.temperature}도</strong>
          <span>
            {weatherTypeLabels[weather.weatherType]} · {rainLabel}
          </span>
        </div>
        <span className="today-weather-period">{forecastPeriodLabels[forecastPeriod]}</span>
      </div>

      <dl className="today-weather-metrics">
        <div>
          <dt>위치</dt>
          <dd>{weather.location.name}</dd>
        </div>
        <div>
          <dt>바람</dt>
          <dd>{windLabel}</dd>
        </div>
        <div>
          <dt>업데이트</dt>
          <dd>{updateTime}</dd>
        </div>
        <div>
          <dt>예보 시간</dt>
          <dd>{forecastTime}</dd>
        </div>
      </dl>
    </div>
  );
}

function renderHistoryOutfit(item: RecommendationResponse): string {
  const outerName = item.outfit.outer ? ` / ${item.outfit.outer.name}` : '';
  return `${item.outfit.top.name} / ${item.outfit.bottom.name}${outerName}`;
}

export function TodayPanel({
  accessToken,
  location,
  locationRevision,
  preferencesRevision,
  onAuthExpired,
  onNavigate,
}: TodayPanelProps) {
  const [weather, setWeather] = useState<WeatherResponse | null>(null);
  const [weatherLoading, setWeatherLoading] = useState(true);
  const [weatherError, setWeatherError] = useState<ErrorResponse | null>(null);
  const [preferences, setPreferences] = useState<UserPreferencesResponse | null>(null);
  const [clothes, setClothes] = useState<ClothingResponse[]>([]);
  const [readinessLoading, setReadinessLoading] = useState(true);
  const [preferencesError, setPreferencesError] = useState<ErrorResponse | null>(null);
  const [clothesError, setClothesError] = useState<ErrorResponse | null>(null);
  const [history, setHistory] = useState<RecommendationResponse[]>([]);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [historyError, setHistoryError] = useState<ErrorResponse | null>(null);
  const [recommendation, setRecommendation] = useState<RecommendationResponse | null>(null);
  const [recommendationFailure, setRecommendationFailure] =
    useState<RecommendationFailureCta | null>(null);
  const [recommendationError, setRecommendationError] = useState<ErrorResponse | null>(null);
  const [recommendationStatus, setRecommendationStatus] = useState<string | null>(null);
  const [recommendationLoading, setRecommendationLoading] = useState(false);
  const [markingRecommendationWorn, setMarkingRecommendationWorn] = useState(false);
  const [recommendationWornAt, setRecommendationWornAt] = useState<string | null>(null);
  const [selectedSituation, setSelectedSituation] =
    useState<RecommendationSituation>('CASUAL');
  const [selectedForecastPeriod, setSelectedForecastPeriod] =
    useState<ForecastPeriod>('CURRENT');
  const [feedbackSaving, setFeedbackSaving] = useState(false);

  const loadWeather = useCallback(async () => {
    setWeatherLoading(true);
    setWeatherError(null);

    try {
      const currentWeather = await getCurrentWeather(accessToken);
      setWeather(currentWeather);
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setWeather(null);
      setWeatherError(toErrorResponse(caught, '현재 날씨를 불러오지 못했습니다.'));
    } finally {
      setWeatherLoading(false);
    }
  }, [accessToken, onAuthExpired]);

  const loadReadiness = useCallback(async () => {
    setReadinessLoading(true);
    setPreferencesError(null);
    setClothesError(null);

    const [preferencesResult, clothesResult] = await Promise.allSettled([
      getUserPreferences(accessToken),
      getClothes(accessToken),
    ]);

    if (
      (preferencesResult.status === 'rejected' && isUnauthorizedError(preferencesResult.reason)) ||
      (clothesResult.status === 'rejected' && isUnauthorizedError(clothesResult.reason))
    ) {
      onAuthExpired();
      return;
    }

    if (preferencesResult.status === 'fulfilled') {
      setPreferences(preferencesResult.value);
    } else {
      setPreferences(null);
      setPreferencesError(
        toErrorResponse(preferencesResult.reason, '선호도를 확인하지 못했습니다.')
      );
    }

    if (clothesResult.status === 'fulfilled') {
      setClothes(clothesResult.value);
    } else {
      setClothes([]);
      setClothesError(toErrorResponse(clothesResult.reason, '옷장을 확인하지 못했습니다.'));
    }

    setReadinessLoading(false);
  }, [accessToken, onAuthExpired]);

  const loadHistoryPreview = useCallback(async () => {
    setHistoryLoading(true);
    setHistoryError(null);

    try {
      const recentHistory = await getRecommendationHistory(accessToken, recentHistoryLimit);
      setHistory(recentHistory);
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setHistory([]);
      setHistoryError(toErrorResponse(caught, '최근 추천 이력을 불러오지 못했습니다.'));
    } finally {
      setHistoryLoading(false);
    }
  }, [accessToken, onAuthExpired]);

  useEffect(() => {
    void loadWeather();
  }, [loadWeather, location?.code, location?.updatedAt, locationRevision]);

  useEffect(() => {
    void loadReadiness();
  }, [loadReadiness, preferencesRevision]);

  useEffect(() => {
    void loadHistoryPreview();
  }, [loadHistoryPreview]);

  const handleCreateRecommendation = async () => {
    setRecommendationLoading(true);
    setRecommendationFailure(null);
    setRecommendationError(null);
    setRecommendationStatus(null);

    try {
      const nextRecommendation = await createRecommendation(accessToken, {
        situation: selectedSituation,
        forecastPeriod: selectedForecastPeriod,
      });
      setRecommendation(nextRecommendation);
      setRecommendationWornAt(nextRecommendation.wornAt);
      setRecommendationStatus('추천을 만들었습니다.');
      await loadHistoryPreview();
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }

      const nextError = toErrorResponse(caught, '추천을 만들지 못했습니다.');
      const nextFailureCta = toRecommendationFailureCta(nextError);
      if (nextFailureCta) {
        setRecommendationFailure(nextFailureCta);
        setRecommendationError(null);
      } else {
        setRecommendationError(nextError);
      }
    } finally {
      setRecommendationLoading(false);
    }
  };

  const handleMarkRecommendationWorn = async () => {
    if (!recommendation) {
      return;
    }

    setMarkingRecommendationWorn(true);
    setRecommendationFailure(null);
    setRecommendationError(null);
    setRecommendationStatus(null);

    try {
      const response = await markRecommendationWorn(
        accessToken,
        recommendation.recommendationId
      );
      setRecommendation((current) =>
        current?.recommendationId === response.recommendationId
          ? { ...current, worn: response.worn, wornAt: response.wornAt }
          : current
      );
      setRecommendationWornAt(response.wornAt);
      await loadHistoryPreview();
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setRecommendationError(toErrorResponse(caught, '착용 완료를 기록하지 못했습니다.'));
    } finally {
      setMarkingRecommendationWorn(false);
    }
  };

  const handleReplaceRecommendationFeedback = async (
    sentiment: RecommendationFeedbackSentiment | null,
    thermal: RecommendationThermalFeedback | null
  ) => {
    if (!recommendation) {
      return;
    }

    setFeedbackSaving(true);
    setRecommendationFailure(null);
    setRecommendationError(null);
    setRecommendationStatus(null);

    try {
      const response = await replaceRecommendationFeedback(
        accessToken,
        recommendation.recommendationId,
        { sentiment, thermal }
      );
      setRecommendation((current) =>
        current?.recommendationId === response.recommendationId
          ? { ...current, feedback: response.feedback }
          : current
      );
      setRecommendationStatus(response.feedback ? '피드백을 저장했습니다.' : '피드백을 지웠습니다.');
      await loadHistoryPreview();
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setRecommendationError(toErrorResponse(caught, '피드백을 저장하지 못했습니다.'));
    } finally {
      setFeedbackSaving(false);
    }
  };

  const handleRecommendationFailureCta = (category?: ClothingCategory) => {
    onNavigate('closet', category ? { closetCategory: category } : undefined);
  };

  const activeCategoryCounts = useMemo(() => getActiveCategoryCounts(clothes), [clothes]);
  const preferenceChecked = preferences !== null;
  const previewHistory = history.slice(0, recentHistoryPreviewCount);
  const allRequiredClothesReady = requiredCategories.every(
    (category) => activeCategoryCounts[category] > 0
  );
  const readyForFirstRecommendation =
    Boolean(location) && preferenceChecked && allRequiredClothesReady;

  const preferenceDetail = preferenceChecked
    ? `색상 ${preferences.preferredColors.length}개, 소재 ${preferences.preferredMaterials.length}개 확인`
    : '기본값이라도 한 번 확인하면 완료됩니다.';

  const checklistItems: ChecklistItem[] = [
    {
      id: 'location',
      label: '위치 확인',
      complete: Boolean(location),
      detail: location ? `${location.fullName || location.name} 기준` : '현재 위치를 확인해주세요.',
      ctaLabel: location ? '위치 변경' : '위치 확인',
      targetView: 'location',
    },
    {
      id: 'preferences',
      label: '선호도 저장/확인',
      complete: preferenceChecked,
      detail: preferenceDetail,
      ctaLabel: preferenceChecked ? '선호도 보기' : '선호도 확인',
      targetView: 'preferences',
    },
    ...requiredCategories.map((category) => {
      const count = activeCategoryCounts[category];
      const categoryLabel = clothingCategoryLabels[category];

      return {
        id: `clothing-${category}`,
        label: `${categoryLabel} 등록`,
        complete: count > 0,
        detail: count > 0 ? `활성 옷 ${count}개` : `${categoryLabel}가 아직 없어요.`,
        ctaLabel: count > 0 ? '옷장 보기' : `${categoryLabel} 등록하기`,
        targetView: 'closet' as const,
        category,
      };
    }),
  ];

  return (
    <div className="today-layout">
      <article className="panel today-weather-panel" aria-label="현재 위치와 날씨">
        {weatherLoading ? (
          <div className="today-weather-loading">
            <p className="eyebrow">날씨</p>
            <strong>{location ? location.name : '위치 확인 중'}</strong>
            <span>현재 날씨를 확인하고 있어요.</span>
          </div>
        ) : null}
        {!weatherLoading && weather ? (
          renderWeatherState(weather, selectedForecastPeriod)
        ) : null}
        {!weatherLoading && weatherError ? (
          <div className="today-soft-error">
            <ApiErrorMessage error={weatherError} />
            <p className="muted">날씨가 없어도 체크리스트와 화면 이동은 계속 사용할 수 있어요.</p>
          </div>
        ) : null}
      </article>

      <RecommendationPanel
        location={location}
        currentWeather={weather}
        recommendation={recommendation}
        failureCta={recommendationFailure}
        error={recommendationError}
        status={recommendationStatus}
        wornAt={recommendationWornAt}
        loading={recommendationLoading}
        markingWorn={markingRecommendationWorn}
        feedbackSaving={feedbackSaving}
        selectedSituation={selectedSituation}
        selectedForecastPeriod={selectedForecastPeriod}
        accessToken={accessToken}
        onCreate={handleCreateRecommendation}
        onSituationChange={setSelectedSituation}
        onForecastPeriodChange={setSelectedForecastPeriod}
        onMarkWorn={handleMarkRecommendationWorn}
        onReplaceFeedback={handleReplaceRecommendationFeedback}
        onFailureCta={handleRecommendationFailureCta}
        onAuthExpired={onAuthExpired}
      />

      <article className="panel today-checklist-panel" aria-label="첫 추천 체크리스트">
        <div className="section-title-row">
          <h3>첫 추천 체크리스트</h3>
          <button
            className="secondary-button"
            type="button"
            onClick={() => void loadReadiness()}
            disabled={readinessLoading}
          >
            다시 확인
          </button>
        </div>

        {readinessLoading ? (
          <p className="muted">준비 상태를 확인하고 있어요.</p>
        ) : (
          <ul className="today-checklist">
            {checklistItems.map((item) => (
              <li className="today-checklist-item" key={item.id}>
                <span
                  className={
                    item.complete ? 'checklist-status complete' : 'checklist-status pending'
                  }
                  aria-label={item.complete ? '완료' : '필요'}
                />
                <div>
                  <strong>{item.label}</strong>
                  <span>{item.detail}</span>
                </div>
                <button
                  className="secondary-button"
                  type="button"
                  onClick={() =>
                    onNavigate(
                      item.targetView,
                      item.category ? { closetCategory: item.category } : undefined
                    )
                  }
                >
                  {item.ctaLabel}
                </button>
              </li>
            ))}
          </ul>
        )}

        <p className="today-checklist-note">
          기온이 12도 안팎이면 아우터까지 준비되어야 추천 성공률이 높아요.
        </p>

        {preferencesError ? <ApiErrorMessage error={preferencesError} /> : null}
        {clothesError ? <ApiErrorMessage error={clothesError} /> : null}
      </article>

      <article
        className={
          readyForFirstRecommendation
            ? 'panel today-hero-panel ready'
            : 'panel today-hero-panel pending'
        }
      >
        <div className="today-hero-copy">
          <p className="eyebrow">추천 준비 상태</p>
          <h2>
            {readyForFirstRecommendation
              ? '첫 추천 준비가 끝났어요'
              : '부족한 항목을 채우면 추천이 완성돼요'}
          </h2>
          <p className="muted">
            {readyForFirstRecommendation
              ? '이제 추천을 만들어 옷 조합과 이유를 확인하면 됩니다.'
              : '위치, 선호도, 상의, 하의, 아우터를 채우면 추천을 만들 수 있어요.'}
          </p>
        </div>
        <div className="today-readiness-summary">
          <span
            className={
              readyForFirstRecommendation
                ? 'readiness-pill complete'
                : 'readiness-pill pending'
            }
          >
            {readyForFirstRecommendation ? '준비 완료' : '준비 중'}
          </span>
          <span className="today-cta-note">
            {readyForFirstRecommendation
              ? '바로 아래에서 시작하세요.'
              : '부족한 항목은 체크리스트에서 바로 이동할 수 있어요.'}
          </span>
        </div>
      </article>

      <article
        className="panel today-history-preview"
        aria-label="최근 추천 미리보기"
        data-api-path={recentHistoryApiPath}
      >
        <div className="section-title-row">
          <h3>최근 추천 미리보기</h3>
          <button className="secondary-button" type="button" onClick={() => onNavigate('history')}>
            이력 보기
          </button>
        </div>

        {historyLoading ? <p className="muted">최근 추천을 확인하고 있어요.</p> : null}
        {!historyLoading && historyError ? <ApiErrorMessage error={historyError} /> : null}
        {!historyLoading && !historyError && previewHistory.length > 0 ? (
          <div className="item-list today-history-list">
            {previewHistory.map((item) => (
              <div className="item-row today-history-row" key={item.recommendationId}>
                <div>
                  <strong>{formatDateTime(item.createdAt)}</strong>
                  <span>{renderHistoryOutfit(item)}</span>
                  <span className="token-row">
                    <span className="situation-pill">
                      {recommendationSituationLabels[item.situation]}
                    </span>
                    <span className="situation-pill">
                      {forecastPeriodLabels[item.forecastPeriod]}
                    </span>
                    <span>{item.weather.temperature}°C</span>
                    <WeatherLabel weatherType={item.weather.weatherType} />
                    <ColorSwatch color={item.outfit.top.color} showLabel={false} />
                    <MaterialChip material={item.outfit.top.material} />
                  </span>
                  <span className="token-row">
                    <span>{item.weather.location.fullName || item.weather.location.name}</span>
                    <span className={item.worn ? 'history-worn-pill complete' : 'history-worn-pill'}>
                      {item.worn
                        ? `착용 완료${item.wornAt ? ` · ${formatDateTime(item.wornAt)}` : ''}`
                        : '착용 전'}
                    </span>
                    <span className="feedback-state-pill">
                      {item.feedback
                        ? [
                            item.feedback.sentiment
                              ? recommendationFeedbackSentimentLabels[item.feedback.sentiment]
                              : null,
                            item.feedback.thermal
                              ? recommendationThermalFeedbackLabels[item.feedback.thermal]
                              : null,
                          ]
                            .filter(Boolean)
                            .join(' · ')
                        : '피드백 없음'}
                    </span>
                  </span>
                </div>
                <span className="item-meta">{item.worn ? '착용 완료' : '착용 전'}</span>
              </div>
            ))}
          </div>
        ) : null}
        {!historyLoading && !historyError && previewHistory.length === 0 ? (
          <p className="muted">아직 추천 이력이 없어요.</p>
        ) : null}
      </article>
    </div>
  );
}
