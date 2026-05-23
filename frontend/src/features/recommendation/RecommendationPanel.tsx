import { useCallback, useEffect, useState } from 'react';
import { isUnauthorizedError, toErrorResponse } from '../../api/errorHelpers';
import {
  createRecommendation,
  getRecommendationHistory,
  markRecommendationWorn,
} from '../../api/smartClosetApi';
import { ApiErrorMessage } from '../../components/ApiErrorMessage';
import { ColorSwatch, MaterialChip, WeatherLabel } from '../../components/DisplayTokens';
import type {
  ErrorResponse,
  OutfitItemResponse,
  RecommendationResponse,
  UserLocationResponse,
} from '../../types/api';
import { clothingCategoryLabels } from '../../utils/displayMappings';

type RecommendationPanelProps = {
  accessToken: string;
  location: UserLocationResponse | null;
  onAuthExpired: () => void;
};

function renderOutfitItem(label: string, item: OutfitItemResponse | null) {
  return (
    <div className="item-row">
      <div>
        <strong>{label}</strong>
        {item ? (
          <span className="token-row">
            <span>{item.name}</span>
            <ColorSwatch color={item.color} />
            <MaterialChip material={item.material} />
          </span>
        ) : (
          <span>None</span>
        )}
      </div>
      {item ? <span className="item-meta">#{item.id}</span> : null}
    </div>
  );
}

function validationError(message: string): ErrorResponse {
  return {
    code: 'INVALID_REQUEST',
    message,
    details: [],
  };
}

export function RecommendationPanel({
  accessToken,
  location,
  onAuthExpired,
}: RecommendationPanelProps) {
  const [recommendation, setRecommendation] = useState<RecommendationResponse | null>(null);
  const [history, setHistory] = useState<RecommendationResponse[]>([]);
  const [historyLimit, setHistoryLimit] = useState(20);
  const [wornAt, setWornAt] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [markingWorn, setMarkingWorn] = useState(false);
  const [historyWornId, setHistoryWornId] = useState<number | null>(null);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<ErrorResponse | null>(null);

  const loadHistory = useCallback(async () => {
    if (!Number.isInteger(historyLimit) || historyLimit < 1 || historyLimit > 50) {
      setError(validationError('History limit must be between 1 and 50.'));
      return;
    }

    setHistoryLoading(true);
    setError(null);

    try {
      const nextHistory = await getRecommendationHistory(accessToken, historyLimit);
      setHistory(nextHistory);
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setHistory([]);
      setError(toErrorResponse(caught, 'Unable to load recommendation history.'));
    } finally {
      setHistoryLoading(false);
    }
  }, [accessToken, historyLimit, onAuthExpired]);

  useEffect(() => {
    void loadHistory();
  }, [loadHistory]);

  const handleCreate = async () => {
    setLoading(true);
    setError(null);
    setStatus(null);

    try {
      const nextRecommendation = await createRecommendation(accessToken);
      setRecommendation(nextRecommendation);
      setWornAt(null);
      setStatus('Recommendation generated.');
      await loadHistory();
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setError(toErrorResponse(caught, 'Unable to create a recommendation.'));
    } finally {
      setLoading(false);
    }
  };

  const handleMarkWorn = async () => {
    if (!recommendation) {
      return;
    }

    setMarkingWorn(true);
    setError(null);
    setStatus(null);

    try {
      const response = await markRecommendationWorn(accessToken, recommendation.recommendationId);
      setRecommendation({
        ...recommendation,
        worn: response.worn,
      });
      setHistory((currentHistory) =>
        currentHistory.map((item) =>
          item.recommendationId === response.recommendationId
            ? { ...item, worn: response.worn }
            : item
        )
      );
      setWornAt(response.wornAt);
      setStatus('Recommendation marked as worn.');
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setError(toErrorResponse(caught, 'Unable to mark the recommendation as worn.'));
    } finally {
      setMarkingWorn(false);
    }
  };

  const handleMarkHistoryWorn = async (historyItem: RecommendationResponse) => {
    setHistoryWornId(historyItem.recommendationId);
    setError(null);
    setStatus(null);

    try {
      const response = await markRecommendationWorn(accessToken, historyItem.recommendationId);
      setHistory((currentHistory) =>
        currentHistory.map((item) =>
          item.recommendationId === response.recommendationId
            ? { ...item, worn: response.worn }
            : item
        )
      );
      if (recommendation?.recommendationId === response.recommendationId) {
        setRecommendation({ ...recommendation, worn: response.worn });
        setWornAt(response.wornAt);
      }
      setStatus('History item marked as worn.');
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        onAuthExpired();
        return;
      }
      setError(toErrorResponse(caught, 'Unable to mark the history item as worn.'));
    } finally {
      setHistoryWornId(null);
    }
  };

  return (
    <article className="panel recommendation-panel">
      <h2>Recommendation</h2>

      {error ? <ApiErrorMessage error={error} /> : null}
      {status ? (
        <p className="panel-success" role="status">
          {status}
        </p>
      ) : null}

      <div className="recommendation-layout">
        <section aria-label="Current recommendation">
          <div className="section-title-row">
            <h3>Current result</h3>
            <button
              className="primary-button"
              type="button"
              onClick={() => void handleCreate()}
              disabled={loading || markingWorn}
            >
              {loading ? 'Generating' : 'Generate'}
            </button>
          </div>

          {recommendation ? (
            <>
          <section className="panel-section" aria-label="Recommendation context">
            <dl className="metric-list compact">
              <div>
                <dt>Location</dt>
                <dd>
                  {location
                    ? `${location.name} (${location.code}, nx=${location.nx}, ny=${location.ny})`
                    : 'Not loaded'}
                </dd>
              </div>
              <div>
                <dt>Weather</dt>
                <dd>
                  {recommendation.weather.temperature}C -{' '}
                  <WeatherLabel weatherType={recommendation.weather.weatherType} />
                  {recommendation.weather.rainy ? ' - rainy' : ''}
                  {recommendation.weather.windy ? ' - windy' : ''}
                </dd>
              </div>
              <div>
                <dt>Total score</dt>
                <dd>{recommendation.score.totalScore}</dd>
              </div>
            </dl>
          </section>

          <section className="panel-section" aria-label="Recommended outfit">
            <h3>Outfit</h3>
            <div className="item-list">
              {renderOutfitItem(
                clothingCategoryLabels.TOP,
                recommendation.outfit.top
              )}
              {renderOutfitItem(
                clothingCategoryLabels.BOTTOM,
                recommendation.outfit.bottom
              )}
              {renderOutfitItem(
                clothingCategoryLabels.OUTER,
                recommendation.outfit.outer
              )}
            </div>
          </section>

          <section className="panel-section" aria-label="Score breakdown">
            <h3>Score</h3>
            <dl className="score-grid">
              <div>
                <dt>Weather</dt>
                <dd>{recommendation.score.weatherScore}</dd>
              </div>
              <div>
                <dt>Color</dt>
                <dd>{recommendation.score.colorScore}</dd>
              </div>
              <div>
                <dt>Wear</dt>
                <dd>{recommendation.score.wearHistoryScore}</dd>
              </div>
              <div>
                <dt>History</dt>
                <dd>{recommendation.score.recommendationHistoryScore}</dd>
              </div>
              <div>
                <dt>Preference</dt>
                <dd>{recommendation.score.preferenceScore}</dd>
              </div>
            </dl>
          </section>

          <section className="panel-section" aria-label="Recommendation reasons">
            <h3>Reasons</h3>
            <ul className="reason-list">
              {recommendation.reasons.map((reason) => (
                <li key={reason}>{reason}</li>
              ))}
            </ul>
          </section>

          <section className="panel-section" aria-label="Worn state">
            <dl className="metric-list compact">
              <div>
                <dt>Worn</dt>
                <dd>{recommendation.worn ? 'true' : 'false'}</dd>
              </div>
              {wornAt ? (
                <div>
                  <dt>Worn at</dt>
                  <dd>{wornAt}</dd>
                </div>
              ) : null}
            </dl>
            <button
              className="secondary-button"
              type="button"
              onClick={() => void handleMarkWorn()}
              disabled={recommendation.worn || markingWorn}
            >
              {recommendation.worn
                ? 'Worn'
                : markingWorn
                  ? 'Saving'
                  : 'Mark worn'}
            </button>
          </section>
            </>
          ) : (
            <p className="muted">No recommendation generated.</p>
          )}
        </section>

        <section aria-label="Recommendation history">
          <div className="section-title-row">
            <h3>History</h3>
            <button
              className="secondary-button"
              type="button"
              onClick={() => void loadHistory()}
              disabled={historyLoading || historyWornId !== null}
            >
              Refresh
            </button>
          </div>

          <form
            className="inline-form compact-history-form"
            onSubmit={(event) => {
              event.preventDefault();
              void loadHistory();
            }}
          >
            <label className="field">
              <span>Limit</span>
              <input
                type="number"
                min={1}
                max={50}
                value={historyLimit}
                onChange={(event) => setHistoryLimit(Number(event.target.value))}
              />
            </label>
            <button className="secondary-button" type="submit" disabled={historyLoading}>
              Apply
            </button>
          </form>

          {historyLoading ? (
            <p className="muted">Loading recommendation history.</p>
          ) : history.length > 0 ? (
            <div className="item-list history-list">
              {history.map((item) => (
                <div className="item-row history-row" key={item.recommendationId}>
                  <div>
                    <strong>#{item.recommendationId}</strong>
                    <span>
                      {item.createdAt} - {item.weather.temperature}C - total{' '}
                      {item.score.totalScore} - preference {item.score.preferenceScore}
                    </span>
                    <span>
                      {item.outfit.top.name} / {item.outfit.bottom.name}
                      {item.outfit.outer ? ` / ${item.outfit.outer.name}` : ''}
                    </span>
                  </div>
                  <div className="history-actions">
                    <span className="item-meta">{item.worn ? 'Worn' : 'Not worn'}</span>
                    <button
                      className="secondary-button"
                      type="button"
                      onClick={() => void handleMarkHistoryWorn(item)}
                      disabled={item.worn || historyWornId !== null || markingWorn}
                    >
                      {item.worn
                        ? 'Worn'
                        : historyWornId === item.recommendationId
                          ? 'Saving'
                          : 'Mark worn'}
                    </button>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <p className="muted">No recommendation history loaded.</p>
          )}
        </section>
      </div>
    </article>
  );
}
