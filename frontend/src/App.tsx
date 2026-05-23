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
import type {
  AuthResponse,
  CurrentUserResponse,
  ErrorResponse,
  UserLocationResponse,
} from './types/api';
import './App.css';

const accessTokenStorageKey = 'smartcloset.accessToken';

type SessionState = 'restoring' | 'anonymous' | 'authenticated';
type ConnectionState = 'checking' | 'connected' | 'error';
type AppView = 'today' | 'closet' | 'preferences' | 'location' | 'history';

const appViews: Array<{
  id: AppView;
  label: string;
}> = [
  { id: 'today', label: '오늘' },
  { id: 'closet', label: '옷장' },
  { id: 'preferences', label: '선호도' },
  { id: 'location', label: '위치' },
  { id: 'history', label: '이력' },
];

const connectionLabels: Record<ConnectionState, string> = {
  checking: '확인 중',
  connected: 'API 연결됨',
  error: 'API 오류',
};

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
  const [activeView, setActiveView] = useState<AppView>('today');
  const [location, setLocation] = useState<UserLocationResponse | null>(null);
  const [error, setError] = useState<ErrorResponse | null>(null);

  const clearSession = useCallback(() => {
    sessionStorage.removeItem(accessTokenStorageKey);
    setAccessToken(null);
    setCurrentUser(null);
    setLocation(null);
    setActiveView('today');
    setSessionState('anonymous');
    setConnectionState('connected');
  }, []);

  const handleAuthExpired = useCallback(() => {
    clearSession();
    setError({
      code: 'UNAUTHORIZED',
      message: '로그인이 만료되었습니다. 다시 로그인해주세요.',
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
      setError(toErrorResponse(caught, 'SmartCloset API에 연결할 수 없습니다.'));
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
    setActiveView('today');
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

  if (sessionState !== 'authenticated' || !accessToken || !currentUser) {
    return (
      <main className="auth-shell">
        <header className="auth-header">
          <p className="eyebrow">SmartCloset MVP4</p>
          <h1>SmartCloset</h1>
        </header>

        {error ? <ApiErrorMessage error={error} className="error-banner" /> : null}

        {sessionState === 'restoring' ? (
          <section className="panel" aria-label="세션 복구">
            <h2>세션을 확인하고 있어요</h2>
            <p className="muted">저장된 access token으로 로그인 상태를 복구합니다.</p>
          </section>
        ) : (
          <AuthPanel onAuthenticated={handleAuthenticated} />
        )}
      </main>
    );
  }

  const currentView = appViews.find((view) => view.id === activeView) ?? appViews[0];
  const locationLabel = location ? location.name : '위치 확인 중';

  const renderViewNavigation = (className: string, ariaLabel: string) => (
    <nav className={className} aria-label={ariaLabel}>
      {appViews.map((view) => {
        const selected = activeView === view.id;

        return (
          <button
            className="view-nav-button"
            type="button"
            key={view.id}
            aria-current={selected ? 'page' : undefined}
            onClick={() => setActiveView(view.id)}
          >
            {view.label}
          </button>
        );
      })}
    </nav>
  );

  const renderActiveView = () => {
    switch (activeView) {
      case 'today':
        return (
          <section className="view-stack today-view" aria-labelledby="today-view-title">
            <header className="view-heading">
              <p className="eyebrow">Today</p>
              <h2 id="today-view-title">오늘</h2>
            </header>
            <RecommendationPanel
              accessToken={accessToken}
              location={location}
              onAuthExpired={handleAuthExpired}
            />
          </section>
        );
      case 'closet':
        return (
          <section className="view-stack closet-view" aria-labelledby="closet-view-title">
            <header className="view-heading">
              <p className="eyebrow">Closet</p>
              <h2 id="closet-view-title">옷장</h2>
            </header>
            <ClosetPanel accessToken={accessToken} onAuthExpired={handleAuthExpired} />
          </section>
        );
      case 'preferences':
        return (
          <section
            className="view-stack preferences-view"
            aria-labelledby="preferences-view-title"
          >
            <header className="view-heading">
              <p className="eyebrow">Preferences</p>
              <h2 id="preferences-view-title">선호도</h2>
            </header>
            <PreferencesPanel accessToken={accessToken} onAuthExpired={handleAuthExpired} />
          </section>
        );
      case 'location':
        return (
          <section className="view-stack location-view" aria-labelledby="location-view-title">
            <header className="view-heading">
              <p className="eyebrow">Location</p>
              <h2 id="location-view-title">위치</h2>
            </header>
            <LocationPanel
              accessToken={accessToken}
              location={location}
              loading={connectionState === 'checking'}
              onAuthExpired={handleAuthExpired}
              onLocationChange={handleLocationChange}
            />
          </section>
        );
      case 'history':
        return (
          <section className="view-stack history-view" aria-labelledby="history-view-title">
            <header className="view-heading">
              <p className="eyebrow">History</p>
              <h2 id="history-view-title">이력</h2>
            </header>
            <article className="panel view-placeholder" data-view="history">
              <h3>최근 추천 이력</h3>
              <p className="muted">이력 목록은 이 화면에서 이어집니다.</p>
            </article>
          </section>
        );
      default:
        return null;
    }
  };

  return (
    <main className="app-shell authenticated-shell">
      <aside className="desktop-sidebar">
        <div className="sidebar-brand">
          <p className="eyebrow">MVP4</p>
          <h1>SmartCloset</h1>
        </div>
        {renderViewNavigation('desktop-view-nav', '주요 화면')}
      </aside>

      <section className="workspace-shell" aria-label="SmartCloset workspace">
        <header className="mobile-app-bar">
          <div>
            <p className="eyebrow">SmartCloset</p>
            <h1>{currentView.label}</h1>
            <span className="mobile-app-meta">{locationLabel}</span>
          </div>
          <button className="secondary-button" type="button" onClick={handleLogout}>
            로그아웃
          </button>
        </header>

        <header className="top-status-bar" aria-label="현재 상태">
          <div className="status-cluster">
            <span className="label">사용자</span>
            <strong>{currentUser.name}</strong>
            <span className="status-detail">{currentUser.email}</span>
          </div>
          <div className="status-cluster">
            <span className="label">현재 위치</span>
            <strong>{locationLabel}</strong>
            {location ? <span className="status-detail">{location.code}</span> : null}
          </div>
          <div className="status-cluster status-cluster-wide">
            <span className="label">API</span>
            <code>{getApiBaseUrl()}</code>
          </div>
          <StatusBadge status={connectionState}>{connectionLabels[connectionState]}</StatusBadge>
          <button
            type="button"
            className="secondary-button"
            onClick={() => void loadCurrentSession(accessToken)}
            disabled={connectionState === 'checking'}
          >
            새로고침
          </button>
          <button className="secondary-button" type="button" onClick={handleLogout}>
            로그아웃
          </button>
        </header>

        {error ? <ApiErrorMessage error={error} className="error-banner" /> : null}

        <section className="view-content" aria-live="polite">
          {renderActiveView()}
        </section>
      </section>

      {renderViewNavigation('mobile-bottom-nav', '하단 탭')}
    </main>
  );
}

export default App;
