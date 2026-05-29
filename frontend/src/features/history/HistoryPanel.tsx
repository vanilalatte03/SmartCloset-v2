import { useCallback, useEffect, useState } from 'react';
import { isUnauthorizedError, toErrorResponse } from '../../api/errorHelpers';
import {
  getRecommendationHistory,
  markRecommendationWorn,
} from '../../api/smartClosetApi';
import { ApiErrorMessage } from '../../components/ApiErrorMessage';
import {
  AuthenticatedClothingThumbnail,
  ColorSwatch,
  MaterialChip,
  WeatherBadge,
  WeatherLabel,
  WeatherTrustSnapshot,
} from '../../components/DisplayTokens';
import type {
  ErrorResponse,
  OutfitItemResponse,
  RecommendationResponse,
} from '../../types/api';
import {
  clothingCategoryLabels,
  forecastPeriodLabels,
  recommendationFeedbackSentimentLabels,
  recommendationSituationLabels,
  recommendationThermalFeedbackLabels,
} from '../../utils/displayMappings';

type HistoryPanelProps = {
  accessToken: string;
  onAuthExpired: () => void;
};

const historyLimit = 20;
const historyApiPath = '/api/recommendations?limit=20';

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

function formatDateParts(value: string): {
  dayLabel: string;
  timeLabel: string;
  monthLabel: string;
} {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return {
      dayLabel: value,
      timeLabel: '시간 확인 불가',
      monthLabel: '추천 기록',
    };
  }

  return {
    dayLabel: new Intl.DateTimeFormat('ko-KR', {
      month: 'long',
      day: 'numeric',
      weekday: 'short',
    }).format(date),
    timeLabel: new Intl.DateTimeFormat('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
    }).format(date),
    monthLabel: new Intl.DateTimeFormat('ko-KR', {
      year: 'numeric',
      month: 'long',
    }).format(date),
  };
}

function formatDateTime(value: string): string {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

function getPrimaryReason(item: RecommendationResponse): string {
  return item.reasons[0] ?? '추천 당시의 날씨와 선호도를 기준으로 만든 조합입니다.';
}

function getOutfitItems(item: RecommendationResponse): Array<{
  label: string;
  item: OutfitItemResponse | null;
  emptyMessage: string;
}> {
  return [
    {
      label: clothingCategoryLabels.TOP,
      item: item.outfit.top,
      emptyMessage: '상의 없음',
    },
    {
      label: clothingCategoryLabels.BOTTOM,
      item: item.outfit.bottom,
      emptyMessage: '하의 없음',
    },
    {
      label: clothingCategoryLabels.OUTER,
      item: item.outfit.outer,
      emptyMessage: '선택된 아우터 없음',
    },
  ];
}

function renderOutfitItem(
  label: string,
  item: OutfitItemResponse | null,
  emptyMessage: string,
  accessToken: string,
  onAuthExpired: () => void
) {
  return (
    <div className="history-outfit-item">
      {item ? (
        <>
          <AuthenticatedClothingThumbnail
            accessToken={accessToken}
            image={item.image}
            alt={`${item.name} 이미지`}
            fallbackLabel={label.slice(0, 1)}
            category={item.category}
            color={item.color}
            className="history-outfit-thumbnail"
            onAuthExpired={onAuthExpired}
          />
          <div className="history-outfit-detail">
            <strong>{label}</strong>
            <span className="history-outfit-name">{item.name}</span>
            <span className="token-row">
              <ColorSwatch color={item.color} />
              <MaterialChip material={item.material} />
            </span>
            <span className="tag-list history-outfit-tags">
              {item.styleTags.length > 0 ? (
                item.styleTags.map((tag) => (
                  <span className="tag-chip readonly" key={tag}>
                    {tag}
                  </span>
                ))
              ) : (
                <span className="muted">스타일 태그 없음</span>
              )}
            </span>
          </div>
        </>
      ) : (
        <div className="history-outfit-detail">
          <strong>{label}</strong>
          <span className="muted">{emptyMessage}</span>
        </div>
      )}
    </div>
  );
}

function renderOutfitPreview(
  label: string,
  item: OutfitItemResponse | null,
  emptyMessage: string,
  accessToken: string,
  onAuthExpired: () => void
) {
  return (
    <div className={item ? 'history-outfit-preview-card' : 'history-outfit-preview-card empty'}>
      {item ? (
        <>
          <AuthenticatedClothingThumbnail
            accessToken={accessToken}
            image={item.image}
            alt={`${item.name} 이미지`}
            fallbackLabel={label.slice(0, 1)}
            category={item.category}
            color={item.color}
            className="history-outfit-preview-thumbnail"
            onAuthExpired={onAuthExpired}
          />
          <span>{label}</span>
        </>
      ) : (
        <>
          <div className="history-outfit-preview-empty" aria-hidden="true">
            {label.slice(0, 1)}
          </div>
          <span>{emptyMessage}</span>
        </>
      )}
    </div>
  );
}

function renderWeatherSnapshot(item: RecommendationResponse) {
  return (
    <dl className="metric-list history-weather-snapshot">
      <div>
        <dt>위치</dt>
        <dd>{item.weather.location.fullName || item.weather.location.name}</dd>
      </div>
      <div>
        <dt>예보 시간대</dt>
        <dd>{forecastPeriodLabels[item.forecastPeriod]}</dd>
      </div>
      <div>
        <dt>기온</dt>
        <dd>{item.weather.temperature}°C</dd>
      </div>
      <div>
        <dt>날씨</dt>
        <dd>
          <WeatherLabel weatherType={item.weather.weatherType} />
        </dd>
      </div>
      <div>
        <dt>비</dt>
        <dd>{item.weather.rainy ? '비 가능' : '비 없음'}</dd>
      </div>
      <div>
        <dt>바람</dt>
        <dd>{item.weather.windy ? '바람 강함' : '바람 잔잔'}</dd>
      </div>
    </dl>
  );
}

export function HistoryPanel({ accessToken, onAuthExpired }: HistoryPanelProps) {
  const [history, setHistory] = useState<RecommendationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ErrorResponse | null>(null);
  const [status, setStatus] = useState<string | null>(null);
  const [markingWornId, setMarkingWornId] = useState<number | null>(null);
  const [wornAtById, setWornAtById] = useState<Record<number, string>>({});

  const loadHistory = useCallback(async () => {
    setLoading(true);
    setError(null);

    try {
      const recentHistory = await getRecommendationHistory(accessToken, historyLimit);
      setHistory(recentHistory);
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setHistory([]);
      setError(toErrorResponse(caught, '추천 이력을 불러오지 못했습니다.'));
    } finally {
      setLoading(false);
    }
  }, [accessToken, onAuthExpired]);

  useEffect(() => {
    void loadHistory();
  }, [loadHistory]);

  const handleMarkWorn = async (recommendationId: number) => {
    setMarkingWornId(recommendationId);
    setError(null);
    setStatus(null);

    try {
      const response = await markRecommendationWorn(accessToken, recommendationId);
      setHistory((currentHistory) =>
        currentHistory.map((item) =>
          item.recommendationId === response.recommendationId
            ? { ...item, worn: response.worn, wornAt: response.wornAt }
            : item
        )
      );
      setWornAtById((current) => ({
        ...current,
        [response.recommendationId]: response.wornAt,
      }));
      setStatus('착용 완료로 기록했습니다.');
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setError(toErrorResponse(caught, '착용 완료를 기록하지 못했습니다.'));
    } finally {
      setMarkingWornId(null);
    }
  };

  return (
    <div className="history-panel" data-api-path={historyApiPath}>
      <article className="panel history-summary-panel" aria-label="추천 이력 요약">
        <div>
          <p className="eyebrow">추천 이력</p>
          <h3>최근 추천을 최신순으로 확인하세요</h3>
          <p className="muted history-summary-copy">
            기본 20개 이력을 불러오고, 각 항목에서 착용 완료를 기록할 수 있어요.
          </p>
        </div>
        <div className="history-summary-actions">
          <span className="history-count-pill">
            {loading ? '불러오는 중' : `${history.length}개 · 최신순`}
          </span>
          <button
            className="secondary-button"
            type="button"
            onClick={() => void loadHistory()}
            disabled={loading || markingWornId !== null}
          >
            새로고침
          </button>
        </div>
      </article>

      {status ? (
        <p className="panel-success history-status" role="status">
          {status}
        </p>
      ) : null}
      {error ? <ApiErrorMessage error={error} className="error-banner" /> : null}

      {loading ? <article className="panel">추천 이력을 확인하고 있어요.</article> : null}

      {!loading && !error && history.length === 0 ? (
        <article className="panel">
          <h3>아직 추천 이력이 없어요</h3>
          <p className="muted">오늘 화면에서 추천을 만들면 이곳에 최신순으로 쌓입니다.</p>
        </article>
      ) : null}

      {!loading && history.length > 0 ? (
        <div className="history-card-list" aria-label="추천 이력 목록">
          {history.map((item) => {
            const wornAt = wornAtById[item.recommendationId] ?? item.wornAt;
            const markingThisItem = markingWornId === item.recommendationId;
            const dateParts = formatDateParts(item.createdAt);
            const outfitItems = getOutfitItems(item);
            const feedbackLabels = item.feedback
              ? [
                  item.feedback.sentiment
                    ? recommendationFeedbackSentimentLabels[item.feedback.sentiment]
                    : null,
                  item.feedback.thermal
                    ? recommendationThermalFeedbackLabels[item.feedback.thermal]
                    : null,
                ].filter(Boolean)
              : [];

            return (
              <article className="panel history-card" key={item.recommendationId}>
                <header className="history-card-summary">
                  <div className="history-card-summary-main">
                    <p className="eyebrow">추천 #{item.recommendationId} · {dateParts.monthLabel}</p>
                    <h3 className="history-card-date">{dateParts.dayLabel}</h3>
                    <p className="muted history-card-time">
                      {dateParts.timeLabel} 기록 · {item.weather.location.name}
                    </p>
                    <div className="history-summary-badge-row">
                      <span
                        className={item.worn ? 'history-worn-pill complete' : 'history-worn-pill'}
                      >
                        {item.worn
                          ? `착용 완료${wornAt ? ` · ${formatDateTime(wornAt)}` : ''}`
                          : '착용 전'}
                      </span>
                      <span className="situation-pill">
                        {recommendationSituationLabels[item.situation]}
                      </span>
                      <span className="situation-pill">
                        {forecastPeriodLabels[item.forecastPeriod]}
                      </span>
                      <span className="feedback-state-pill">
                        {feedbackLabels.length > 0 ? feedbackLabels.join(' · ') : '피드백 없음'}
                      </span>
                      <WeatherBadge weather={item.weather} />
                    </div>
                  </div>
                  <div className="history-card-actions">
                    <button
                      className="secondary-button"
                      type="button"
                      onClick={() => void handleMarkWorn(item.recommendationId)}
                      disabled={item.worn || markingWornId !== null}
                    >
                      {item.worn ? '착용 완료' : markingThisItem ? '저장 중' : '착용 완료하기'}
                    </button>
                  </div>
                </header>

                <section className="history-timeline-entry" aria-label="기록 요약">
                  <div className="history-timeline-marker" aria-hidden="true" />
                  <div className="history-timeline-card">
                    <div className="history-outfit-preview-grid">
                      {outfitItems.map((outfitItem) => (
                        <div className="history-outfit-preview-cell" key={outfitItem.label}>
                          {renderOutfitPreview(
                            outfitItem.label,
                            outfitItem.item,
                            outfitItem.emptyMessage,
                            accessToken,
                            onAuthExpired
                          )}
                        </div>
                      ))}
                    </div>
                    <div className="history-timeline-copy">
                      <div className="history-weather-line">
                        <WeatherBadge weather={item.weather} />
                        <span>{item.weather.location.fullName || item.weather.location.name}</span>
                      </div>
                      <p>{getPrimaryReason(item)}</p>
                      <div className="history-feedback-tags" aria-label="피드백 상태">
                        {feedbackLabels.length > 0 ? (
                          feedbackLabels.map((label) => (
                            <span className="feedback-state-pill" key={label}>
                              {label}
                            </span>
                          ))
                        ) : (
                          <span className="feedback-state-pill">피드백 없음</span>
                        )}
                      </div>
                    </div>
                  </div>
                </section>

                <section className="history-outfit-summary" aria-label="추천 옷 조합">
                  <h3>옷 상세</h3>
                  <div className="history-outfit-list">
                    {outfitItems.map((outfitItem) => (
                      <div className="history-outfit-list-cell" key={outfitItem.label}>
                        {renderOutfitItem(
                          outfitItem.label,
                          outfitItem.item,
                          outfitItem.emptyMessage,
                          accessToken,
                          onAuthExpired
                        )}
                      </div>
                    ))}
                  </div>
                </section>

                <details className="history-detail-details">
                  <summary>이유 · 날씨 · 점수 자세히 보기</summary>
                  <div className="history-detail-grid">
                    <section className="history-card-section" aria-label="오늘 입기 좋은 이유">
                      <h3>오늘 입기 좋은 이유</h3>
                      <ul className="reason-list history-reason-list">
                        {item.reasons.map((reason) => (
                          <li key={reason}>{reason}</li>
                        ))}
                      </ul>
                    </section>

                    <section className="history-card-section" aria-label="추천 날씨 스냅샷">
                      <h3>날씨 스냅샷</h3>
                      {renderWeatherSnapshot(item)}
                      <WeatherTrustSnapshot weather={item.weather} />
                    </section>
                  </div>

                  <section className="history-card-section" aria-label="추천 점수 상세">
                    <h3>점수 상세</h3>
                    <dl className="score-grid history-score-grid">
                      {scoreItems.map((scoreItem) => (
                        <div key={scoreItem.key}>
                          <dt>{scoreItem.label}</dt>
                          <dd>{item.score[scoreItem.key]}</dd>
                        </div>
                      ))}
                    </dl>
                  </section>
                </details>
              </article>
            );
          })}
        </div>
      ) : null}
    </div>
  );
}
