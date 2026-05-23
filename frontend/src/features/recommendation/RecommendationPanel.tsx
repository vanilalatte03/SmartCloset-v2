import { ApiErrorMessage } from '../../components/ApiErrorMessage';
import { ColorSwatch, MaterialChip, WeatherLabel } from '../../components/DisplayTokens';
import type {
  ClothingCategory,
  ErrorResponse,
  OutfitItemResponse,
  RecommendationResponse,
  UserLocationResponse,
} from '../../types/api';
import {
  clothingCategoryLabels,
  type RecommendationFailureCta,
} from '../../utils/displayMappings';

type RecommendationPanelProps = {
  location: UserLocationResponse | null;
  recommendation: RecommendationResponse | null;
  failureCta: RecommendationFailureCta | null;
  error: ErrorResponse | null;
  status: string | null;
  wornAt: string | null;
  loading: boolean;
  markingWorn: boolean;
  onCreate: () => void;
  onMarkWorn: () => void;
  onFailureCta: (category?: ClothingCategory) => void;
};

const scoreItems: Array<{
  key: keyof RecommendationResponse['score'];
  label: string;
}> = [
  { key: 'totalScore', label: '총점' },
  { key: 'weatherScore', label: '날씨 적합도' },
  { key: 'colorScore', label: '색상 조합' },
  { key: 'wearHistoryScore', label: '착용 이력' },
  { key: 'recommendationHistoryScore', label: '추천 이력' },
  { key: 'preferenceScore', label: 'preferenceScore' },
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

function renderOutfitItem(
  label: string,
  item: OutfitItemResponse | null,
  emptyMessage: string
) {
  return (
    <div className="item-row recommendation-outfit-row">
      <div>
        <strong>{label}</strong>
        {item ? (
          <span className="token-row">
            <span>{item.name}</span>
            <ColorSwatch color={item.color} />
            <MaterialChip material={item.material} />
          </span>
        ) : (
          <span>{emptyMessage}</span>
        )}
      </div>
      {item ? <span className="item-meta">#{item.id}</span> : null}
    </div>
  );
}

export function RecommendationPanel({
  location,
  recommendation,
  failureCta,
  error,
  status,
  wornAt,
  loading,
  markingWorn,
  onCreate,
  onMarkWorn,
  onFailureCta,
}: RecommendationPanelProps) {
  return (
    <article
      className="panel recommendation-panel"
      id="today-recommendation"
      aria-label="오늘 추천 생성"
    >
      <div className="section-title-row recommendation-heading">
        <div>
          <p className="eyebrow">오늘 추천</p>
          <h3>오늘 추천 만들기</h3>
          <p className="muted recommendation-heading-copy">
            현재 위치와 옷장 상태로 오늘 입을 조합을 생성합니다.
          </p>
        </div>
        <div className="recommendation-action-bar">
          <button
            className="primary-button recommendation-create-button"
            type="button"
            onClick={() => onCreate()}
            disabled={loading || markingWorn}
          >
            {loading ? '추천 생성 중' : '추천 만들기'}
          </button>
        </div>
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
            <dl className="metric-list compact recommendation-weather-snapshot">
              <div>
                <dt>추천 생성 시점 날씨</dt>
                <dd>
                  {location ? `${location.name} · ` : ''}
                  {recommendation.weather.temperature}°C ·{' '}
                  <WeatherLabel weatherType={recommendation.weather.weatherType} />
                  {recommendation.weather.rainy ? ' · 비 가능' : ' · 비 없음'}
                  {recommendation.weather.windy ? ' · 바람 강함' : ' · 바람 잔잔'}
                </dd>
              </div>
            </dl>
            <div className="item-list recommendation-outfit-list">
              {renderOutfitItem(
                clothingCategoryLabels.TOP,
                recommendation.outfit.top,
                '상의 없음'
              )}
              {renderOutfitItem(
                clothingCategoryLabels.BOTTOM,
                recommendation.outfit.bottom,
                '하의 없음'
              )}
              {renderOutfitItem(
                clothingCategoryLabels.OUTER,
                recommendation.outfit.outer,
                '선택된 아우터 없음'
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
              disabled={recommendation.worn || markingWorn || loading}
            >
              {recommendation.worn
                ? '착용 완료'
                : markingWorn
                  ? '저장 중'
                  : '착용 완료하기'}
            </button>
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
        <p className="muted recommendation-empty">
          아직 생성된 추천이 없어요. 추천 만들기를 누르면 옷 조합과 이유가 먼저 표시됩니다.
        </p>
      )}
    </article>
  );
}
