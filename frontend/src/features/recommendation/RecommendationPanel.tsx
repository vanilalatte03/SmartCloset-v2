import { ApiErrorMessage } from '../../components/ApiErrorMessage';
import {
  AuthenticatedClothingThumbnail,
  ColorSwatch,
  ColorSwatchPlaceholder,
  MaterialChip,
  MaterialChipPlaceholder,
  WeatherBadge,
  WeatherTrustSnapshot,
} from '../../components/DisplayTokens';
import type {
  ClothingCategory,
  ErrorResponse,
  ForecastPeriod,
  OutfitItemResponse,
  RecommendationFeedbackSentiment,
  RecommendationResponse,
  RecommendationSituation,
  RecommendationThermalFeedback,
  UserLocationResponse,
  WeatherResponse,
} from '../../types/api';
import {
  clothingCategoryLabels,
  forecastPeriodLabels,
  forecastPeriodOptions,
  recommendationFeedbackSentimentLabels,
  recommendationSituationLabels,
  recommendationSituationOptions,
  recommendationThermalFeedbackLabels,
  type RecommendationFailureCta,
} from '../../utils/displayMappings';

type RecommendationPanelProps = {
  location: UserLocationResponse | null;
  currentWeather: WeatherResponse | null;
  recommendation: RecommendationResponse | null;
  failureCta: RecommendationFailureCta | null;
  error: ErrorResponse | null;
  status: string | null;
  wornAt: string | null;
  loading: boolean;
  markingWorn: boolean;
  feedbackSaving: boolean;
  selectedSituation: RecommendationSituation;
  selectedForecastPeriod: ForecastPeriod;
  accessToken: string;
  onCreate: () => void;
  onSituationChange: (situation: RecommendationSituation) => void;
  onForecastPeriodChange: (forecastPeriod: ForecastPeriod) => void;
  onMarkWorn: () => void;
  onReplaceFeedback: (
    sentiment: RecommendationFeedbackSentiment | null,
    thermal: RecommendationThermalFeedback | null
  ) => void;
  onFailureCta: (category?: ClothingCategory) => void;
  onAuthExpired: () => void;
};

const outfitSlots: Array<{
  category: ClothingCategory;
  emptyMessage: string;
  glyph: string;
}> = [
  { category: 'TOP', emptyMessage: '추천을 만들면 상의가 표시됩니다.', glyph: '상' },
  { category: 'BOTTOM', emptyMessage: '추천을 만들면 하의가 표시됩니다.', glyph: '하' },
  { category: 'OUTER', emptyMessage: '필요한 날씨에는 아우터가 표시됩니다.', glyph: '겉' },
];

const scoreItems: Array<{
  key: keyof RecommendationResponse['score'];
  label: string;
}> = [
  { key: 'totalScore', label: '총점' },
  { key: 'weatherScore', label: '날씨 적합도' },
  { key: 'colorScore', label: '색상 조합' },
  { key: 'wearHistoryScore', label: '착용 이력' },
  { key: 'recommendationHistoryScore', label: '추천 이력' },
  { key: 'preferenceScore', label: '선호 반영' },
];

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

function getOutfitItemByCategory(
  recommendation: RecommendationResponse,
  category: ClothingCategory
): OutfitItemResponse | null {
  if (category === 'TOP') {
    return recommendation.outfit.top;
  }

  if (category === 'BOTTOM') {
    return recommendation.outfit.bottom;
  }

  return recommendation.outfit.outer;
}

function renderOutfitSlotCard(
  category: ClothingCategory,
  item: OutfitItemResponse | null,
  emptyMessage: string,
  glyph: string,
  weather: WeatherResponse | null,
  accessToken: string,
  onAuthExpired: () => void
) {
  const label = clothingCategoryLabels[category];

  return (
    <article className={item ? 'outfit-slot-card' : 'outfit-slot-card empty'}>
      {item ? (
        <AuthenticatedClothingThumbnail
          accessToken={accessToken}
          image={item.image}
          alt={`${item.name} 이미지`}
          fallbackLabel={glyph}
          category={item.category}
          color={item.color}
          className="outfit-slot-thumbnail"
          onAuthExpired={onAuthExpired}
        />
      ) : null}

      <div className="outfit-slot-header">
        <span className="slot-glyph" aria-hidden="true">
          {glyph}
        </span>
        <div>
          <strong>{label}</strong>
          <span>{item ? item.name : emptyMessage}</span>
        </div>
      </div>

      <div className="outfit-slot-tokens">
        {item ? <ColorSwatch color={item.color} /> : <ColorSwatchPlaceholder />}
        {item ? <MaterialChip material={item.material} /> : <MaterialChipPlaceholder />}
      </div>

      {item ? (
        <div className="tag-list outfit-slot-tags" aria-label={`${item.name} 스타일 태그`}>
          {item.styleTags.length > 0 ? (
            item.styleTags.map((tag) => (
              <span className="tag-chip readonly" key={tag}>
                {tag}
              </span>
            ))
          ) : (
            <span className="muted">스타일 태그 없음</span>
          )}
        </div>
      ) : null}

      {weather ? (
        <WeatherBadge weather={weather} />
      ) : (
        <span className="weather-badge muted-token">현재 날씨 반영</span>
      )}
    </article>
  );
}

export function RecommendationPanel({
  location,
  currentWeather,
  recommendation,
  failureCta,
  error,
  status,
  wornAt,
  loading,
  markingWorn,
  feedbackSaving,
  selectedSituation,
  selectedForecastPeriod,
  accessToken,
  onCreate,
  onSituationChange,
  onForecastPeriodChange,
  onMarkWorn,
  onReplaceFeedback,
  onFailureCta,
  onAuthExpired,
}: RecommendationPanelProps) {
  const currentSentiment = recommendation?.feedback?.sentiment ?? null;
  const currentThermal = recommendation?.feedback?.thermal ?? null;
  const controlsDisabled = loading || markingWorn || feedbackSaving;

  return (
    <article
      className="panel recommendation-panel"
      id="today-recommendation"
      aria-label="오늘 추천 생성"
    >
      <div className="section-title-row recommendation-heading">
        <div>
          <p className="eyebrow">오늘 추천</p>
          <h3>오늘 추천</h3>
          <p className="muted recommendation-heading-copy">
            현재 위치, 예보 시간대, 옷장 기준으로 조합을 만듭니다.
          </p>
        </div>
        <div className="recommendation-action-bar">
          <button
            className="primary-button recommendation-create-button"
            type="button"
            onClick={() => onCreate()}
            disabled={controlsDisabled}
          >
            {loading ? '추천 생성 중' : '추천 만들기'}
          </button>
        </div>
      </div>

      <div className="situation-selector" role="group" aria-label="추천 상황 선택">
        {recommendationSituationOptions.map((situation) => (
          <button
            className={
              selectedSituation === situation ? 'situation-button active' : 'situation-button'
            }
            type="button"
            key={situation}
            aria-pressed={selectedSituation === situation}
            onClick={() => onSituationChange(situation)}
            disabled={controlsDisabled}
          >
            {recommendationSituationLabels[situation]}
          </button>
        ))}
      </div>

      <div className="forecast-selector" role="group" aria-label="예보 시간대 선택">
        {forecastPeriodOptions.map((forecastPeriod) => (
          <button
            className={
              selectedForecastPeriod === forecastPeriod
                ? 'situation-button active'
                : 'situation-button'
            }
            type="button"
            key={forecastPeriod}
            aria-pressed={selectedForecastPeriod === forecastPeriod}
            onClick={() => onForecastPeriodChange(forecastPeriod)}
            disabled={controlsDisabled}
          >
            {forecastPeriodLabels[forecastPeriod]}
          </button>
        ))}
      </div>

      {failureCta ? (
        <div className="recommendation-failure-card" role="status">
          <div>
            <strong>추천을 만들기 전에 해결할 항목이 있어요</strong>
            <p>{failureCta.message}</p>
          </div>
          <button
            className="secondary-button"
            type="button"
            onClick={() => onFailureCta(failureCta.category)}
          >
            {failureCta.ctaLabel}
          </button>
        </div>
      ) : null}

      {error ? <ApiErrorMessage error={error} /> : null}
      {status ? (
        <p className="panel-success" role="status">
          {status}
        </p>
      ) : null}

      {recommendation ? (
        <div className="recommendation-result-stack">
          <section className="panel-section" aria-label="추천 옷 조합">
            <div className="section-title-row">
              <h3>추천 옷 조합</h3>
              <span className="item-meta">{formatDateTime(recommendation.createdAt)}</span>
            </div>
            <div className="recommendation-weather-snapshot">
              <span>
                {recommendation.weather.location.fullName ||
                  recommendation.weather.location.name ||
                  location?.name ||
                  '현재 위치'}{' '}
                기준
              </span>
              <span className="situation-pill">
                {recommendationSituationLabels[recommendation.situation]}
              </span>
              <span className="situation-pill">
                {forecastPeriodLabels[recommendation.forecastPeriod]}
              </span>
              <WeatherBadge weather={recommendation.weather} />
            </div>
            <WeatherTrustSnapshot weather={recommendation.weather} />
            <div className="recommendation-slot-grid" aria-label="추천 슬롯">
              {outfitSlots.map((slot) =>
                renderOutfitSlotCard(
                  slot.category,
                  getOutfitItemByCategory(recommendation, slot.category),
                  slot.category === 'OUTER' ? '선택된 아우터 없음' : `${clothingCategoryLabels[slot.category]} 없음`,
                  slot.glyph,
                  recommendation.weather,
                  accessToken,
                  onAuthExpired
                )
              )}
            </div>
          </section>

          <section className="panel-section" aria-label="오늘 입기 좋은 이유">
            <h3>오늘 입기 좋은 이유</h3>
            <ul className="reason-list recommendation-reason-list">
              {recommendation.reasons.map((reason) => (
                <li key={reason}>{reason}</li>
              ))}
            </ul>
          </section>

          <section className="panel-section recommendation-worn-section" aria-label="착용 완료">
            <div>
              <h3>착용 기록</h3>
              <p className="muted">
                {recommendation.worn
                  ? `착용 완료${wornAt ? ` · ${formatDateTime(wornAt)}` : ''}`
                  : '오늘 입기로 했다면 이 추천을 착용 완료로 기록하세요.'}
              </p>
            </div>
            <button
              className="secondary-button"
              type="button"
              onClick={() => onMarkWorn()}
              disabled={recommendation.worn || controlsDisabled}
            >
              {recommendation.worn
                ? '착용 완료'
                : markingWorn
                  ? '저장 중'
                  : '착용 완료하기'}
            </button>
          </section>

          <section className="panel-section recommendation-feedback-section" aria-label="추천 피드백">
            <div>
              <h3>피드백</h3>
              <p className="muted">
                {recommendation.feedback
                  ? `저장됨 · ${formatDateTime(recommendation.feedback.updatedAt)}`
                  : '피드백 없음'}
              </p>
            </div>
            <div className="feedback-control-stack">
              <div className="feedback-button-group" role="group" aria-label="추천 만족도">
                {(['LIKED', 'DISLIKED'] as const).map((sentiment) => (
                  <button
                    className={
                      currentSentiment === sentiment ? 'feedback-button active' : 'feedback-button'
                    }
                    type="button"
                    key={sentiment}
                    aria-pressed={currentSentiment === sentiment}
                    onClick={() =>
                      onReplaceFeedback(
                        currentSentiment === sentiment ? null : sentiment,
                        currentThermal
                      )
                    }
                    disabled={controlsDisabled}
                  >
                    {recommendationFeedbackSentimentLabels[sentiment]}
                  </button>
                ))}
              </div>
              <div className="feedback-button-group" role="group" aria-label="체감 온도">
                {(['TOO_COLD', 'TOO_HOT'] as const).map((thermal) => (
                  <button
                    className={
                      currentThermal === thermal ? 'feedback-button active' : 'feedback-button'
                    }
                    type="button"
                    key={thermal}
                    aria-pressed={currentThermal === thermal}
                    onClick={() =>
                      onReplaceFeedback(
                        currentSentiment,
                        currentThermal === thermal ? null : thermal
                      )
                    }
                    disabled={controlsDisabled}
                  >
                    {recommendationThermalFeedbackLabels[thermal]}
                  </button>
                ))}
              </div>
              <button
                className="secondary-button feedback-clear-button"
                type="button"
                onClick={() => onReplaceFeedback(null, null)}
                disabled={controlsDisabled || (!currentSentiment && !currentThermal)}
              >
                {feedbackSaving ? '저장 중' : '피드백 지우기'}
              </button>
            </div>
          </section>

          <details className="panel-section recommendation-score-details">
            <summary>점수 상세</summary>
            <dl className="score-grid recommendation-score-grid">
              {scoreItems.map((item) => (
                <div key={item.key}>
                  <dt>{item.label}</dt>
                  <dd>{recommendation.score[item.key]}</dd>
                </div>
              ))}
            </dl>
          </details>
        </div>
      ) : (
        <section className="recommendation-empty-state" aria-label="추천 대기 슬롯">
          <p className="muted recommendation-empty">
            추천을 만들면 상의, 하의, 아우터 슬롯에 오늘의 조합이 표시됩니다.
          </p>
          <div className="recommendation-slot-grid">
            {outfitSlots.map((slot) =>
              renderOutfitSlotCard(
                slot.category,
                null,
                slot.emptyMessage,
                slot.glyph,
                currentWeather,
                accessToken,
                onAuthExpired
              )
            )}
          </div>
        </section>
      )}
    </article>
  );
}
