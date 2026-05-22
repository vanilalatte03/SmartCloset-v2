import { useCallback, useEffect, useState } from 'react';
import { isUnauthorizedError, toErrorResponse } from './api/errorHelpers';
import { getApiBaseUrl, getCurrentUser, getUserLocation } from './api/smartClosetApi';
import { ApiErrorMessage } from './components/ApiErrorMessage';
import { StatusBadge } from './components/StatusBadge';
import { AuthPanel } from './features/auth/AuthPanel';
import { ClosetPanel } from './features/clothes/ClosetPanel';
import { LocationPanel } from './features/location/LocationPanel';
import { PreferencesPanel } from './features/preferences/PreferencesPanel';
import { RecommendationPanel } from './features/recommendation/RecommendationPanel';
import type { AuthResponse, CurrentUserResponse, ErrorResponse, UserLocationResponse } from './types/api';
import './App.css';

const accessTokenStorageKey = 'smartcloset.accessToken';

type SessionState = 'restoring' | 'anonymous' | 'authenticated';
type ConnectionState = 'checking' | 'connected' | 'error';

function readStoredAccessToken(): string | null {
  return sessionStorage.getItem(accessTokenStorageKey);
}

function App() {
  const [accessToken, setAccessToken] = useState<string | null>(() => readStoredAccessToken());
  const [sessionState, setSessionState] = useState<SessionState>(
    accessToken ? 'restoring' : 'anonymous'
  );
  const [currentUser, setCurrentUser] = useState<CurrentUserResponse | null>(null);
  const [connectionState, setConnectionState] = useState<ConnectionState>(
    accessToken ? 'checking' : 'connected'
  );
  const [location, setLocation] = useState<UserLocationResponse | null>(null);
  const [error, setError] = useState<ErrorResponse | null>(null);

  const clearSession = useCallback(() => {
    sessionStorage.removeItem(accessTokenStorageKey);
    setAccessToken(null);
    setCurrentUser(null);
    setLocation(null);
    setSessionState('anonymous');
    setConnectionState('connected');
  }, []);

  const handleAuthExpired = useCallback(() => {
    clearSession();
    setError({
      code: 'UNAUTHORIZED',
      message: 'Your session has expired. Please log in again.',
      details: [],
    });
  }, [clearSession]);

  const loadCurrentSession = useCallback(async (token: string) => {
    setConnectionState('checking');
    setError(null);

    try {
      const user = await getCurrentUser(token);
      const userLocation = await getUserLocation(token);
      setCurrentUser(user);
      setLocation(userLocation);
      setSessionState('authenticated');
      setConnectionState('connected');
    } catch (caught) {
      if (isUnauthorizedError(caught)) {
        handleAuthExpired();
        return;
      }
      setSessionState((state) => (state === 'restoring' ? 'anonymous' : state));
      setConnectionState('error');
      setError(toErrorResponse(caught, 'Unable to reach the SmartCloset API.'));
    }
  }, [handleAuthExpired]);

  useEffect(() => {
    if (!accessToken) {
      setSessionState('anonymous');
      setConnectionState('connected');
      return;
    }

    void loadCurrentSession(accessToken);
  }, [accessToken, loadCurrentSession]);

  const handleAuthenticated = useCallback((response: AuthResponse) => {
    sessionStorage.setItem(accessTokenStorageKey, response.accessToken);
    setAccessToken(response.accessToken);
    setCurrentUser(response.user);
    setSessionState('authenticated');
    setConnectionState('checking');
    setError(null);
  }, []);

  const handleLogout = useCallback(() => {
    clearSession();
    setError(null);
  }, [clearSession]);

  const handleLocationChange = useCallback((updatedLocation: UserLocationResponse) => {
    setLocation(updatedLocation);
    setConnectionState('connected');
    setError(null);
  }, []);

  return (
    <main className="app-shell">
      <header className="app-header">
        <div>
          <p className="eyebrow">SmartCloset MVP 3</p>
          <h1>SmartCloset</h1>
        </div>
        <div className="header-meta" aria-label="Runtime status">
          {currentUser ? (
            <span>
              {currentUser.name} ({currentUser.email})
            </span>
          ) : (
            <span>Signed out</span>
          )}
          <StatusBadge status={connectionState}>
            {connectionState === 'connected'
              ? 'API connected'
              : connectionState === 'checking'
                ? 'Checking API'
                : 'API error'}
          </StatusBadge>
          {currentUser ? (
            <button className="secondary-button" type="button" onClick={handleLogout}>
              Logout
            </button>
          ) : null}
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
          onClick={() => {
            if (accessToken) {
              void loadCurrentSession(accessToken);
            }
          }}
          disabled={!accessToken || connectionState === 'checking'}
        >
          Refresh session
        </button>
      </section>

      {error ? <ApiErrorMessage error={error} className="error-banner" /> : null}

      {sessionState === 'authenticated' && accessToken && currentUser ? (
        <section className="panel-grid" aria-label="SmartCloset workspace">
          <LocationPanel
            accessToken={accessToken}
            location={location}
            loading={connectionState === 'checking'}
            onAuthExpired={handleAuthExpired}
            onLocationChange={handleLocationChange}
          />
          <PreferencesPanel accessToken={accessToken} onAuthExpired={handleAuthExpired} />
          <ClosetPanel accessToken={accessToken} onAuthExpired={handleAuthExpired} />
          <RecommendationPanel
            accessToken={accessToken}
            location={location}
            onAuthExpired={handleAuthExpired}
          />
        </section>
      ) : sessionState === 'restoring' ? (
        <section className="panel" aria-label="Session restore">
          <h2>Restoring session</h2>
          <p className="muted">Checking saved access token.</p>
        </section>
      ) : (
        <AuthPanel onAuthenticated={handleAuthenticated} />
      )}
    </main>
  );
}

export default App;
