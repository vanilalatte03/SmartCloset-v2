import { useCallback, useEffect, useState } from 'react';
import { ApiClientError } from './api/client';
import { getApiBaseUrl, getUserLocation } from './api/smartClosetApi';
import { StatusBadge } from './components/StatusBadge';
import { ClosetPanel } from './features/clothes/ClosetPanel';
import { LocationPanel } from './features/location/LocationPanel';
import { RecommendationPanel } from './features/recommendation/RecommendationPanel';
import type { ErrorResponse, UserLocationResponse } from './types/api';
import './App.css';

const userId = 1;

type ConnectionState = 'checking' | 'connected' | 'error';

function unknownErrorResponse(message: string): ErrorResponse {
  return {
    code: 'NETWORK_ERROR',
    message,
    details: [],
  };
}

function App() {
  const [connectionState, setConnectionState] = useState<ConnectionState>('checking');
  const [location, setLocation] = useState<UserLocationResponse | null>(null);
  const [error, setError] = useState<ErrorResponse | null>(null);

  const checkConnection = useCallback(async () => {
    setConnectionState('checking');
    setError(null);

    try {
      const userLocation = await getUserLocation(userId);
      setLocation(userLocation);
      setConnectionState('connected');
    } catch (caught) {
      setLocation(null);
      setConnectionState('error');
      if (caught instanceof ApiClientError) {
        setError(caught.response);
        return;
      }
      setError(unknownErrorResponse('Unable to reach the SmartCloset API.'));
    }
  }, []);

  useEffect(() => {
    void checkConnection();
  }, [checkConnection]);

  return (
    <main className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">SmartCloset MVP 2</p>
          <h1>SmartCloset</h1>
        </div>
        <div className="header-meta" aria-label="Runtime status">
          <span>User ID {userId}</span>
          <StatusBadge status={connectionState}>
            {connectionState === 'connected'
              ? 'API connected'
              : connectionState === 'checking'
                ? 'Checking API'
                : 'API error'}
          </StatusBadge>
        </div>
      </header>

      <section className="status-strip" aria-label="API connection">
        <div>
          <span className="label">API base URL</span>
          <code>{getApiBaseUrl()}</code>
        </div>
        <button
          type="button"
          className="secondary-button"
          onClick={() => void checkConnection()}
          disabled={connectionState === 'checking'}
        >
          Refresh
        </button>
      </section>

      {error ? (
        <section className="error-banner" role="status">
          <strong>{error.code}</strong>
          <span>{error.message}</span>
        </section>
      ) : null}

      <section className="panel-grid" aria-label="SmartCloset workspace">
        <LocationPanel location={location} loading={connectionState === 'checking'} />
        <ClosetPanel />
        <RecommendationPanel />
      </section>
    </main>
  );
}

export default App;
