import { useCallback, useEffect, useState } from 'react';
import { ApiClientError } from '../../api/client';
import { toErrorResponse } from '../../api/errorHelpers';
import {
  createRecommendation,
  getRecommendationHistory,
  markRecommendationWorn,
} from '../../api/smartClosetApi';
import { ApiErrorMessage } from '../../components/ApiErrorMessage';
import type {
  ErrorResponse,
  OutfitItemResponse,
  RecommendationResponse,
  UserLocationResponse,
} from '../../types/api';

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
        <span>{item ? `${item.name} - ${item.color} - ${item.material}` : 'None'}</span>
      </div>
      {item ? <span className="item-meta">#{item.id}</span> : null}
    </div>
  );
}

export function RecommendationPanel({
  accessToken,
  location,
  onAuthExpired,
}: RecommendationPanelProps) {
  const [recommendation, setRecommendation] = useState<RecommendationResponse | null>(null);
  const [history, setHistory] = useState<RecommendationResponse[]>([]);
  const [wornAt, setWornAt] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(true);
  const [markingWorn, setMarkingWorn] = useState(false);
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<ErrorResponse | null>(null);

  const loadHistory = useCallback(async () => {
    setHistoryLoading(true);
    try {
      setHistory(await getRecommendationHistory(accessToken, 20));
    } catch (caught) {
      if (caught instanceof ApiClientError && caught.status === 401) {
        onAuthExpired();
        return;
      }
      setError(toErrorResponse(caught, 'Unable to load recommendation history.'));
      setHistory([]);
    } finally {
      setHistoryLoading(false);
    }
  }, [accessToken, onAuthExpired]);

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
      if (caught instanceof ApiClientError && caught.status === 401) {
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
      const response = await markRecommendationWorn(
        accessToken,
        recommendation.recommendationId
      );
      setRecommendation({
        ...recommendation,
        worn: response.worn,
      });
      setWornAt(response.wornAt);
      setStatus('Recommendation marked as worn.');
      await loadHistory();
    } catch (caught) {
      if (caught instanceof ApiClientError && caught.status === 401) {
        onAuthExpired();
        return;
      }
      setError(toErrorResponse(caught, 'Unable to mark the recommendation as worn.'));
    } finally {
      setMarkingWorn(false);
    }
  };

  return (
    <article className="panel">
      <h2>Recommendation</h2>
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

      {error ? <ApiErrorMessage error={error} /> : null}
      {status ? (
        <p className="panel-success" role="status">
          {status}
        </p>
      ) : null}

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
                  {recommendation.weather.weatherType}
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
              {renderOutfitItem('Top', recommendation.outfit.top)}
              {renderOutfitItem('Bottom', recommendation.outfit.bottom)}
              {renderOutfitItem('Outer', recommendation.outfit.outer)}
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

      <section className="panel-section" aria-label="Recommendation history">
        <div className="section-title-row">
          <h3>History</h3>
          <button
            className="secondary-button"
            type="button"
            onClick={() => void loadHistory()}
            disabled={historyLoading}
          >
            {historyLoading ? 'Loading' : 'Refresh'}
          </button>
        </div>
        {historyLoading ? (
          <p className="muted">Loading recent recommendations.</p>
        ) : history.length > 0 ? (
          <div className="item-list">
            {history.map((item) => (
              <div className="item-row" key={item.recommendationId}>
                <div>
                  <strong>Recommendation #{item.recommendationId}</strong>
                  <span>
                    {item.outfit.top.name} + {item.outfit.bottom.name}
                    {item.outfit.outer ? ` + ${item.outfit.outer.name}` : ''}
                  </span>
                </div>
                <span className="item-meta">
                  score {item.score.totalScore} - preference {item.score.preferenceScore}
                  {item.worn ? ' - worn' : ''}
                </span>
              </div>
            ))}
          </div>
        ) : (
          <p className="muted">No recommendation history yet.</p>
        )}
      </section>
    </article>
  );
}
