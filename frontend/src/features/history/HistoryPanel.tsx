import { useCallback, useEffect, useMemo, useState } from 'react';
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
  WeatherLabel,
} from '../../components/DisplayTokens';
import type {
  ErrorResponse,
  OutfitItemResponse,
  RecommendationResponse,
} from '../../types/api';
import {
  clothingCategoryLabels,
  getDisplayStyleTags,
  recommendationFeedbackSentimentLabels,
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
  max: number;
}> = [
  { key: 'weatherScore', label: '날씨', max: 35 },
  { key: 'colorScore', label: '색상', max: 25 },
  { key: 'wearHistoryScore', label: '착용', max: 20 },
  { key: 'recommendationHistoryScore', label: '추천 이력', max: 10 },
  { key: 'preferenceScore', label: '취향', max: 10 },
];

const calendarWeekdayLabels = ['월', '화', '수', '목', '금', '토', '일'];

function getValidDate(value: string | null | undefined): Date | null {
  if (!value) {
    return null;
  }

  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? null : date;
}

function toDateKey(date: Date): string {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');

  return `${year}-${month}-${day}`;
}

function dateFromKey(dateKey: string): Date {
  const [year, month, day] = dateKey.split('-').map(Number);
  return new Date(year, month - 1, day);
}

function shiftDate(date: Date, days: number): Date {
  const nextDate = new Date(date);
  nextDate.setDate(nextDate.getDate() + days);
  return nextDate;
}

function startOfCalendarWeek(date: Date): Date {
  const weekStart = new Date(date);
  const day = weekStart.getDay();
  const mondayOffset = day === 0 ? -6 : 1 - day;
  weekStart.setDate(weekStart.getDate() + mondayOffset);
  weekStart.setHours(0, 0, 0, 0);
  return weekStart;
}

function formatCalendarMonthLabel(date: Date): string {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
  }).format(date);
}

function getRelativeDateLabel(date: Date): string {
  const dateKey = toDateKey(date);
  const today = new Date();
  const yesterday = shiftDate(today, -1);

  if (dateKey === toDateKey(today)) {
    return '오늘';
  }

  if (dateKey === toDateKey(yesterday)) {
    return '어제';
  }

  return new Intl.DateTimeFormat('ko-KR', {
    weekday: 'short',
  }).format(date);
}

function formatDateParts(value: string): {
  dateKey: string;
  dayLabel: string;
  flowDateLabel: string;
  relativeLabel: string;
  timeLabel: string;
  monthLabel: string;
} {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return {
      dateKey: value,
      dayLabel: value,
      flowDateLabel: value,
      relativeLabel: '추천일',
      timeLabel: '시간 확인 불가',
      monthLabel: '추천 기록',
    };
  }

  return {
    dateKey: toDateKey(date),
    dayLabel: new Intl.DateTimeFormat('ko-KR', {
      month: 'long',
      day: 'numeric',
      weekday: 'short',
    }).format(date),
    flowDateLabel: new Intl.DateTimeFormat('ko-KR', {
      month: 'long',
      day: 'numeric',
    }).format(date),
    relativeLabel: getRelativeDateLabel(date),
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

function getOutfitStyleTags(item: RecommendationResponse): string[] {
  const tags = [
    ...(item.outfit.outer?.styleTags ?? []),
    ...item.outfit.top.styleTags,
    ...item.outfit.bottom.styleTags,
  ];

  return getDisplayStyleTags(tags).slice(0, 2);
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
      emptyMessage: '아우터 없음',
    },
  ];
}

function getOutfitPreviewItems(item: RecommendationResponse) {
  return getOutfitItems(item);
}

function renderOutfitItem(
  label: string,
  item: OutfitItemResponse | null,
  emptyMessage: string,
  accessToken: string,
  onAuthExpired: () => void
) {
  const displayStyleTags = item ? getDisplayStyleTags(item.styleTags) : [];

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
              {displayStyleTags.length > 0 ? (
                displayStyleTags.map((tag) => (
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
  const previewLabel = item ? item.name : emptyMessage;

  return (
    <div
      className={item ? 'history-outfit-preview-card' : 'history-outfit-preview-card empty'}
      aria-label={`${label}: ${previewLabel}`}
    >
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
          <span title={previewLabel}>{previewLabel}</span>
        </>
      ) : (
        <>
          <div className="history-outfit-preview-empty" aria-hidden="true">
            {label.slice(0, 1)}
          </div>
          <span>{previewLabel}</span>
        </>
      )}
    </div>
  );
}

export function HistoryPanel({ accessToken, onAuthExpired }: HistoryPanelProps) {
  const [history, setHistory] = useState<RecommendationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<ErrorResponse | null>(null);
  const [markingWornId, setMarkingWornId] = useState<number | null>(null);
  const [wornAtById, setWornAtById] = useState<Record<number, string>>({});
  const [selectedDateKey, setSelectedDateKey] = useState<string | null>(null);
  const [calendarWeekStartKey, setCalendarWeekStartKey] = useState(() =>
    toDateKey(startOfCalendarWeek(new Date()))
  );

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

  useEffect(() => {
    const latestHistoryDate = getValidDate(history[0]?.createdAt);

    if (!latestHistoryDate || selectedDateKey !== null) {
      return;
    }

    setSelectedDateKey(toDateKey(latestHistoryDate));
    setCalendarWeekStartKey(toDateKey(startOfCalendarWeek(latestHistoryDate)));
  }, [history, selectedDateKey]);

  const calendarWeekStart = useMemo(
    () => dateFromKey(calendarWeekStartKey),
    [calendarWeekStartKey]
  );
  const calendarDays = useMemo(
    () =>
      Array.from({ length: 7 }, (_, index) => {
        const date = shiftDate(calendarWeekStart, index);

        return {
          date,
          dateKey: toDateKey(date),
          dayNumber: date.getDate(),
          weekdayLabel: calendarWeekdayLabels[index],
        };
      }),
    [calendarWeekStart]
  );
  const calendarMonthLabel = formatCalendarMonthLabel(calendarWeekStart);
  const historyDateCounts = useMemo(
    () =>
      history.reduce<Record<string, number>>((counts, item) => {
        const dateKey = formatDateParts(item.createdAt).dateKey;
        counts[dateKey] = (counts[dateKey] ?? 0) + 1;
        return counts;
      }, {}),
    [history]
  );
  const groupedHistory = useMemo(
    () =>
      history.reduce<
        Array<{
          dateKey: string;
          dateParts: ReturnType<typeof formatDateParts>;
          items: RecommendationResponse[];
        }>
      >((groups, item) => {
        const dateParts = formatDateParts(item.createdAt);
        const lastGroup = groups[groups.length - 1];

        if (lastGroup?.dateKey === dateParts.dateKey) {
          lastGroup.items.push(item);
          return groups;
        }

        groups.push({
          dateKey: dateParts.dateKey,
          dateParts,
          items: [item],
        });
        return groups;
      }, []),
    [history]
  );

  const handleCalendarWeekShift = (days: number) => {
    const nextWeekStart = shiftDate(calendarWeekStart, days);
    setCalendarWeekStartKey(toDateKey(nextWeekStart));
    setSelectedDateKey(toDateKey(nextWeekStart));
  };

  const handleMarkWorn = async (recommendationId: number) => {
    setMarkingWornId(recommendationId);
    setError(null);

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
      <article className="panel history-calendar-panel" aria-label="추천 이력 달력">
        <div className="history-calendar-heading">
          <div>
            <p className="eyebrow">기록</p>
            <h3>{calendarMonthLabel}</h3>
          </div>
          <div className="history-calendar-actions">
            <button
              aria-label="이전 주"
              className="history-calendar-nav-button"
              type="button"
              onClick={() => handleCalendarWeekShift(-7)}
            >
              ‹
            </button>
            <button
              aria-label="다음 주"
              className="history-calendar-nav-button"
              type="button"
              onClick={() => handleCalendarWeekShift(7)}
            >
              ›
            </button>
          </div>
        </div>

        <div className="history-calendar-week" aria-label={`${calendarMonthLabel} 주간 달력`}>
          {calendarDays.map((day) => {
            const dateCount = historyDateCounts[day.dateKey] ?? 0;
            const selected = selectedDateKey === day.dateKey;

            return (
              <button
                aria-label={`${day.weekdayLabel}요일 ${day.dayNumber}일, 기록 ${dateCount}개`}
                aria-pressed={selected}
                className={selected ? 'history-calendar-day selected' : 'history-calendar-day'}
                key={day.dateKey}
                type="button"
                onClick={() => setSelectedDateKey(day.dateKey)}
              >
                <span className="history-calendar-weekday">{day.weekdayLabel}</span>
                <span className="history-calendar-day-number">{day.dayNumber}</span>
                <span
                  className={
                    dateCount > 0
                      ? 'history-calendar-event-dot active'
                      : 'history-calendar-event-dot'
                  }
                  aria-hidden="true"
                />
              </button>
            );
          })}
        </div>
      </article>

      {error ? <ApiErrorMessage error={error} className="error-banner" /> : null}

      {loading ? <article className="panel">추천 이력을 확인하고 있어요.</article> : null}

      {!loading && !error && history.length === 0 ? (
        <article className="panel">
          <h3>아직 추천 이력이 없어요</h3>
          <p className="muted">오늘 화면에서 추천을 만들면 이곳에 최신순으로 쌓입니다.</p>
        </article>
      ) : null}

      {!loading && history.length > 0 ? (
        <div className="history-flow-list" aria-label="추천 이력 날짜 흐름">
          {groupedHistory.map((group) => (
            <section
              className={
                selectedDateKey === group.dateKey
                  ? 'history-flow-day selected'
                  : 'history-flow-day'
              }
              key={group.dateKey}
            >
              <header className="history-flow-date">
                <span className="history-flow-marker" aria-hidden="true" />
                <div>
                  <h3>
                    {group.dateParts.relativeLabel}, {group.dateParts.flowDateLabel}
                  </h3>
                  <span>
                    {group.items.length > 1
                      ? `${group.items.length}개 기록`
                      : `${group.dateParts.timeLabel} 기록`}
                  </span>
                </div>
              </header>

              <div className="history-flow-items">
                {group.items.map((item) => {
                  const wornAt = wornAtById[item.recommendationId] ?? item.wornAt;
                  const markingThisItem = markingWornId === item.recommendationId;
                  const dateParts = formatDateParts(item.createdAt);
                  const outfitItems = getOutfitItems(item);
                  const outfitPreviewItems = getOutfitPreviewItems(item);
                  const outfitStyleTags = getOutfitStyleTags(item);
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
                      <section className="history-timeline-card" aria-label="기록 요약">
                        <div className="history-outfit-preview-grid">
                          {outfitPreviewItems.map((outfitItem) => (
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
                          <div className="history-card-kicker">
                            <span>추천 #{item.recommendationId}</span>
                            <span>{dateParts.timeLabel}</span>
                          </div>
                          <div className="history-weather-line">
                            <span className="history-weather-chip">
                              <strong>{item.weather.temperature}°C</strong>
                              <WeatherLabel weatherType={item.weather.weatherType} />
                            </span>
                            <span className="history-location-chip">
                              {item.weather.location.name}
                            </span>
                          </div>
                          <p>{getPrimaryReason(item)}</p>
                          <div className="history-feedback-tags" aria-label="착용과 피드백 상태">
                            <span
                              className={
                                item.worn ? 'history-worn-pill complete' : 'history-worn-pill'
                              }
                            >
                              {item.worn
                                ? `착용 완료${wornAt ? ` · ${formatDateTime(wornAt)}` : ''}`
                                : '착용 전'}
                            </span>
                            <span className="feedback-state-pill">
                              {feedbackLabels.length > 0 ? feedbackLabels.join(' · ') : '피드백 없음'}
                            </span>
                            {outfitStyleTags.map((tag) => (
                              <span className="tag-chip readonly" key={tag}>
                                #{tag}
                              </span>
                            ))}
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
                        </div>
                      </section>

                      <details className="history-detail-details">
                        <summary>상세보기</summary>
                        <div className="history-detail-content">
                          <div className="history-detail-main">
                            <section className="history-outfit-summary" aria-label="추천 옷 조합">
                              <div className="history-detail-section-heading">
                                <h3>옷 상세</h3>
                              </div>
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

                            <section
                              className="history-card-section history-score-summary-card"
                              aria-label="추천 점수"
                            >
                              <div className="history-score-summary-heading">
                                <div>
                                  <h3>추천 점수</h3>
                                </div>
                                <strong>
                                  {item.score.totalScore}
                                  <span>/100</span>
                                </strong>
                              </div>
                              <dl className="history-score-pill-grid">
                                {scoreItems.map((scoreItem) => (
                                  <div key={scoreItem.key}>
                                    <dt>{scoreItem.label}</dt>
                                    <dd>
                                      {item.score[scoreItem.key]}
                                      <span>/{scoreItem.max}</span>
                                    </dd>
                                  </div>
                                ))}
                              </dl>
                            </section>
                          </div>
                        </div>
                      </details>
                    </article>
                  );
                })}
              </div>
            </section>
          ))}
        </div>
      ) : null}
    </div>
  );
}
