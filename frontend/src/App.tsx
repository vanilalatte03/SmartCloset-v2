import { useCallback, useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { accessTokenStorageKey, ApiClientError } from './api/client';
import { toErrorResponse } from './api/errorHelpers';
import {
  getApiBaseUrl,
  getCurrentUser,
  getUserLocation,
  login,
  signup,
} from './api/smartClosetApi';
import { ApiErrorMessage } from './components/ApiErrorMessage';
import { StatusBadge } from './components/StatusBadge';
import { ClosetPanel } from './features/clothes/ClosetPanel';
import { LocationPanel } from './features/location/LocationPanel';
import { RecommendationPanel } from './features/recommendation/RecommendationPanel';
import type {
  CurrentUserResponse,
  ErrorResponse,
  UserLocationResponse,
} from './types/api';
import './App.css';

type ConnectionState = 'signed-out' | 'checking' | 'connected' | 'error';
type AuthMode = 'login' | 'signup';

type AuthFormState = {
  email: string;
  password: string;
  name: string;
};

const initialAuthForm: AuthFormState = {
  email: '',
  password: '',
  name: '',
};

function isUnauthorized(caught: unknown): boolean {
  return caught instanceof ApiClientError && caught.status === 401;
}

function App() {
  const [accessToken, setAccessToken] = useState<string | null>(() =>
    sessionStorage.getItem(accessTokenStorageKey)
  );
  const [currentUser, setCurrentUser] = useState<CurrentUserResponse | null>(null);
  const [location, setLocation] = useState<UserLocationResponse | null>(null);
  const [connectionState, setConnectionState] = useState<ConnectionState>(
    accessToken ? 'checking' : 'signed-out'
  );
  const [authMode, setAuthMode] = useState<AuthMode>('login');
  const [authForm, setAuthForm] = useState<AuthFormState>(initialAuthForm);
  const [authSubmitting, setAuthSubmitting] = useState(false);
  const [error, setError] = useState<ErrorResponse | null>(null);

  const clearSession = useCallback(() => {
    sessionStorage.removeItem(accessTokenStorageKey);
    setAccessToken(null);
    setCurrentUser(null);
    setLocation(null);
    setConnectionState('signed-out');
  }, []);

  const loadSession = useCallback(
    async (token: string) => {
      setConnectionState('checking');
      setError(null);

      try {
        const [user, userLocation] = await Promise.all([
          getCurrentUser(token),
          getUserLocation(token),
        ]);
        setCurrentUser(user);
        setLocation(userLocation);
        setConnectionState('connected');
      } catch (caught) {
        setCurrentUser(null);
        setLocation(null);
        if (isUnauthorized(caught)) {
          clearSession();
        } else {
          setConnectionState('error');
          setError(toErrorResponse(caught, 'Unable to reach the SmartCloset API.'));
        }
      }
    },
    [clearSession]
  );

  useEffect(() => {
    if (accessToken) {
      void loadSession(accessToken);
    } else {
      setConnectionState('signed-out');
    }
  }, [accessToken, loadSession]);

  const handleAuthSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setAuthSubmitting(true);
    setError(null);

    try {
      const email = authForm.email.trim();
      const password = authForm.password;
      const response =
        authMode === 'login'
          ? await login({ email, password })
          : await signup({ email, password, name: authForm.name.trim() });

      sessionStorage.setItem(accessTokenStorageKey, response.accessToken);
      setAccessToken(response.accessToken);
      setCurrentUser(response.user);
      setAuthForm(initialAuthForm);
      setConnectionState('checking');
    } catch (caught) {
      setError(toErrorResponse(caught, 'Authentication failed.'));
    } finally {
      setAuthSubmitting(false);
    }
  };

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
          {currentUser ? <span>{currentUser.email}</span> : <span>Signed out</span>}
          <StatusBadge status={connectionState === 'signed-out' ? 'error' : connectionState}>
            {connectionState === 'connected'
              ? 'API connected'
              : connectionState === 'checking'
                ? 'Checking API'
                : connectionState === 'signed-out'
                  ? 'Signed out'
                  : 'API error'}
          </StatusBadge>
          {accessToken && currentUser ? (
            <button className="secondary-button" type="button" onClick={clearSession}>
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
        {accessToken && currentUser ? (
          <button
            type="button"
            className="secondary-button"
            onClick={() => void loadSession(accessToken)}
            disabled={connectionState === 'checking'}
          >
            Refresh
          </button>
        ) : null}
      </section>

      {error ? <ApiErrorMessage error={error} className="error-banner" /> : null}

      {accessToken && currentUser ? (
        <section className="panel-grid" aria-label="SmartCloset workspace">
          <LocationPanel
            accessToken={accessToken}
            location={location}
            loading={connectionState === 'checking'}
            onLocationChange={handleLocationChange}
            onAuthExpired={clearSession}
          />
          <ClosetPanel accessToken={accessToken} onAuthExpired={clearSession} />
          <RecommendationPanel
            accessToken={accessToken}
            location={location}
            onAuthExpired={clearSession}
          />
        </section>
      ) : (
        <section className="auth-grid" aria-label="Authentication">
          <article className="panel auth-panel">
            <div className="section-title-row">
              <h2>{authMode === 'login' ? 'Login' : 'Sign up'}</h2>
              <button
                className="secondary-button"
                type="button"
                onClick={() => {
                  setAuthMode(authMode === 'login' ? 'signup' : 'login');
                  setError(null);
                }}
              >
                {authMode === 'login' ? 'Use signup' : 'Use login'}
              </button>
            </div>
            <form className="panel-form auth-form" onSubmit={handleAuthSubmit}>
              {authMode === 'signup' ? (
                <label className="field">
                  <span>Name</span>
                  <input
                    value={authForm.name}
                    maxLength={50}
                    onChange={(event) =>
                      setAuthForm({ ...authForm, name: event.target.value })
                    }
                    required
                  />
                </label>
              ) : null}
              <label className="field">
                <span>Email</span>
                <input
                  type="email"
                  value={authForm.email}
                  onChange={(event) =>
                    setAuthForm({ ...authForm, email: event.target.value })
                  }
                  required
                />
              </label>
              <label className="field">
                <span>Password</span>
                <input
                  type="password"
                  minLength={8}
                  value={authForm.password}
                  onChange={(event) =>
                    setAuthForm({ ...authForm, password: event.target.value })
                  }
                  required
                />
              </label>
              <button className="primary-button" type="submit" disabled={authSubmitting}>
                {authSubmitting
                  ? 'Submitting'
                  : authMode === 'login'
                    ? 'Login'
                    : 'Create account'}
              </button>
            </form>
          </article>
        </section>
      )}
    </main>
  );
}

export default App;
