import { useCallback, useEffect, useState } from 'react';
import type { CSSProperties } from 'react';
import { setRefreshAccessTokenHandler } from './api/client';
import { isUnauthorizedError, toErrorResponse } from './api/errorHelpers';
import {
  getApiBaseUrl,
  getCurrentUser,
  getUserLocation,
  logout,
  refreshSession,
} from './api/smartClosetApi';
import { ApiErrorMessage } from './components/ApiErrorMessage';
import { StatusBadge } from './components/StatusBadge';
import { AccountSettingsPanel } from './features/account/AccountSettingsPanel';
import { AuthPanel } from './features/auth/AuthPanel';
import { ClosetPanel } from './features/clothes/ClosetPanel';
import { HistoryPanel } from './features/history/HistoryPanel';
import { LocationPanel } from './features/location/LocationPanel';
import { PreferencesPanel } from './features/preferences/PreferencesPanel';
import { TodayPanel } from './features/today/TodayPanel';
import type {
  AuthResponse,
  ClothingCategory,
  CurrentUserResponse,
  ErrorResponse,
  UserLocationResponse,
} from './types/api';
import authEditorialUrl from './assets/auth-london-editorial.png';
import './App.css';

type SessionState = 'restoring' | 'anonymous' | 'authenticated' | 'expired';
type ConnectionState = 'checking' | 'connected' | 'error';
type AppView = 'today' | 'closet' | 'preferences' | 'location' | 'history' | 'account';
type AppNavigationOptions = {
  closetCategory?: ClothingCategory;
};

const appViews: Array<{
  id: AppView;
  label: string;
}> = [
  { id: 'today', label: '추천' },
  { id: 'closet', label: '옷장' },
  { id: 'preferences', label: '내 취향' },
  { id: 'location', label: '위치' },
  { id: 'history', label: '기록' },
];

const appViewLabels: Record<AppView, string> = {
  today: '추천',
  closet: '옷장',
  preferences: '내 취향',
  location: '위치',
  history: '기록',
  account: '계정 설정',
};

const appViewEyebrows: Record<AppView, string> = {
  today: '오늘의 옷차림',
  closet: '내 옷장',
  preferences: '개인화',
  location: '동네 날씨',
  history: '기록',
  account: '계정',
};

const viewHeadings: Record<AppView, { title: string; subtitle: string }> = {
  today: {
    title: '코디 추천',
    subtitle: '날씨, 예보 시간대, 옷장 준비 상태, 최근 기록을 한 화면에서 스캔합니다.',
  },
  closet: {
    title: '옷장',
    subtitle: '내 옷 목록을 먼저 보고, 필요할 때 상세 등록 화면으로 들어갑니다.',
  },
  preferences: {
    title: '내 취향',
    subtitle: '선호 색상, 소재, 스타일 태그를 저장하고 추천 미리보기와 연결합니다.',
  },
  location: {
    title: '위치',
    subtitle: '지도 없이 동네 검색과 현재 위치 후보 선택으로 날씨 기준을 정합니다.',
  },
  history: {
    title: '기록',
    subtitle: '최근 추천과 착용 여부, 날씨 스냅샷, 피드백을 시간순으로 확인합니다.',
  },
  account: {
    title: '계정 설정',
    subtitle: '프로필, 로그인 방법, 세션 상태, 계정 삭제 조건을 확인합니다.',
  },
};

const connectionLabels: Record<ConnectionState, string> = {
  checking: '확인 중',
  connected: 'API 연결됨',
  error: 'API 오류',
};

function App() {
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [sessionState, setSessionState] = useState<SessionState>('restoring');
  const [currentUser, setCurrentUser] = useState<CurrentUserResponse | null>(null);
  const [connectionState, setConnectionState] = useState<ConnectionState>('checking');
  const [activeView, setActiveView] = useState<AppView>('today');
  const [closetInitialCategory, setClosetInitialCategory] =
    useState<ClothingCategory | null>(null);
  const [location, setLocation] = useState<UserLocationResponse | null>(null);
  const [locationRevision, setLocationRevision] = useState(0);
  const [preferencesRevision, setPreferencesRevision] = useState(0);
  const [error, setError] = useState<ErrorResponse | null>(null);
  const [profileMenuOpen, setProfileMenuOpen] = useState(false);

  const clearSession = useCallback((nextState: SessionState = 'anonymous') => {
    setAccessToken(null);
    setCurrentUser(null);
    setLocation(null);
    setActiveView('today');
    setProfileMenuOpen(false);
    setClosetInitialCategory(null);
    setLocationRevision(0);
    setPreferencesRevision(0);
    setSessionState(nextState);
    setConnectionState('connected');
  }, []);

  const handleAuthExpired = useCallback(() => {
    clearSession('expired');
    setError({
      code: 'UNAUTHORIZED',
      message: '로그인이 만료되었습니다. 다시 로그인해주세요.',
      details: [],
    });
  }, [clearSession]);

  const loadCurrentSession = useCallback(async (token: string, user?: CurrentUserResponse) => {
    setConnectionState('checking');
    setError(null);

    try {
      const loadedUser = user ?? (await getCurrentUser(token));
      const userLocation = await getUserLocation(token);
      setCurrentUser(loadedUser);
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

  const refreshAccessToken = useCallback(async (): Promise<string | null> => {
    try {
      const response = await refreshSession();
      setAccessToken(response.accessToken);
      setCurrentUser(response.user);
      setSessionState('authenticated');
      setConnectionState('connected');
      setError(null);
      return response.accessToken;
    } catch {
      handleAuthExpired();
      return null;
    }
  }, [handleAuthExpired]);

  useEffect(() => {
    setRefreshAccessTokenHandler(refreshAccessToken);

    return () => {
      setRefreshAccessTokenHandler(null);
    };
  }, [refreshAccessToken]);

  useEffect(() => {
    let cancelled = false;

    refreshSession()
      .then((response) => {
        if (cancelled) {
          return;
        }
        setAccessToken(response.accessToken);
        void loadCurrentSession(response.accessToken, response.user);
      })
      .catch(() => {
        if (cancelled) {
          return;
        }
        clearSession('anonymous');
        setError(null);
      });

    return () => {
      cancelled = true;
    };
  }, [clearSession, loadCurrentSession]);

  const handleAuthenticated = useCallback((response: AuthResponse) => {
    setAccessToken(response.accessToken);
    setCurrentUser(response.user);
    setActiveView('today');
    setClosetInitialCategory(null);
    setSessionState('authenticated');
    setConnectionState('checking');
    setError(null);
    void loadCurrentSession(response.accessToken, response.user);
  }, [loadCurrentSession]);

  const handleLogout = useCallback(async () => {
    try {
      await logout();
    } catch {
      // Local session cleanup is still correct when the logout request cannot complete.
    }
    setProfileMenuOpen(false);
    clearSession('anonymous');
    setError(null);
  }, [clearSession]);

  const handleAccountDeleted = useCallback(() => {
    clearSession('anonymous');
    setError({
      code: 'ACCOUNT_DELETED',
      message: '계정이 삭제되었습니다.',
      details: [],
    });
  }, [clearSession]);

  const handleLocationChange = useCallback((updatedLocation: UserLocationResponse) => {
    setLocation(updatedLocation);
    setLocationRevision((revision) => revision + 1);
    setConnectionState('connected');
    setError(null);
  }, []);

  const handlePreferencesConfirmed = useCallback(() => {
    setPreferencesRevision((revision) => revision + 1);
  }, []);

  const handleNavigate = useCallback((view: AppView, options?: AppNavigationOptions) => {
    if (view === 'closet') {
      setClosetInitialCategory(options?.closetCategory ?? null);
    } else {
      setClosetInitialCategory(null);
    }
    setProfileMenuOpen(false);
    setActiveView(view);
  }, []);

  if (sessionState !== 'authenticated' || !accessToken || !currentUser) {
    return (
      <main
        className="auth-shell"
        style={{ '--auth-editorial-image': `url(${authEditorialUrl})` } as CSSProperties}
      >
        {error ? <ApiErrorMessage error={error} className="error-banner" /> : null}

        {sessionState === 'restoring' ? (
          <section className="panel" aria-label="세션 복구">
            <h2>세션을 확인하고 있어요</h2>
            <p className="muted">로그인 상태를 복구합니다.</p>
          </section>
        ) : (
          <AuthPanel onAuthenticated={handleAuthenticated} />
        )}
      </main>
    );
  }

  const currentViewLabel = appViewLabels[activeView];
  const locationLabel = location ? location.name : '위치 확인 중';
  const profileInitial = currentUser.name.trim().charAt(0) || currentUser.email.charAt(0);

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
            onClick={() => handleNavigate(view.id)}
          >
            {view.label}
          </button>
        );
      })}
    </nav>
  );

  const renderProfileMenu = () => (
    <details
      className="profile-menu"
      open={profileMenuOpen}
      onToggle={(event) => setProfileMenuOpen(event.currentTarget.open)}
    >
      <summary className="profile-pill" aria-label="프로필 메뉴">
        <span className="profile-avatar" aria-hidden="true">
          {profileInitial.toUpperCase()}
        </span>
        <span className="profile-copy">
          <strong>{currentUser.name}</strong>
          <span>{currentUser.email}</span>
        </span>
      </summary>
      <div className="profile-menu-panel">
        <button type="button" className="profile-menu-action" onClick={() => handleNavigate('account')}>
          계정 설정
        </button>
        <button type="button" className="profile-menu-action" onClick={handleLogout}>
          로그아웃
        </button>
      </div>
    </details>
  );

  const renderActiveView = () => {
    switch (activeView) {
      case 'today':
        return (
          <section className="view-stack today-view" aria-label={viewHeadings.today.title}>
            <TodayPanel
              accessToken={accessToken}
              location={location}
              locationRevision={locationRevision}
              preferencesRevision={preferencesRevision}
              onAuthExpired={handleAuthExpired}
              onNavigate={handleNavigate}
            />
          </section>
        );
      case 'closet':
        return (
          <section className="view-stack closet-view" aria-label={viewHeadings.closet.title}>
            <ClosetPanel
              accessToken={accessToken}
              initialCategory={closetInitialCategory}
              onAuthExpired={handleAuthExpired}
            />
          </section>
        );
      case 'preferences':
        return (
          <section
            className="view-stack preferences-view"
            aria-label={viewHeadings.preferences.title}
          >
            <PreferencesPanel
              accessToken={accessToken}
              onAuthExpired={handleAuthExpired}
              onPreferencesConfirmed={handlePreferencesConfirmed}
            />
          </section>
        );
      case 'location':
        return (
          <section className="view-stack location-view" aria-label={viewHeadings.location.title}>
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
          <section className="view-stack history-view" aria-label={viewHeadings.history.title}>
            <HistoryPanel accessToken={accessToken} onAuthExpired={handleAuthExpired} />
          </section>
        );
      case 'account':
        return (
          <section className="view-stack account-view" aria-label={viewHeadings.account.title}>
            <AccountSettingsPanel
              accessToken={accessToken}
              currentUser={currentUser}
              onAccountDeleted={handleAccountDeleted}
            />
          </section>
        );
      default:
        return null;
    }
  };

  return (
    <main className="app-shell authenticated-shell">
      <header className="desktop-sidebar">
        <div className="sidebar-brand">
          <p className="eyebrow">{appViewEyebrows[activeView]}</p>
          <h1>SmartCloset</h1>
        </div>
        {renderViewNavigation('desktop-view-nav', '주요 화면')}
        {renderProfileMenu()}
      </header>

      <section className="workspace-shell" aria-label="SmartCloset workspace">
        <header className="mobile-app-bar">
          <div>
            <p className="eyebrow">SmartCloset</p>
            <h1>{currentViewLabel}</h1>
          </div>
          {renderProfileMenu()}
        </header>

        <header className="top-status-bar" aria-label="현재 상태">
          <div className="status-cluster">
            <span className="label">현재 위치</span>
            <strong>{locationLabel}</strong>
            {location ? <span className="status-detail">{location.code}</span> : null}
          </div>
          <div className="status-cluster">
            <span className="label">연결</span>
            <StatusBadge status={connectionState}>{connectionLabels[connectionState]}</StatusBadge>
          </div>
          <details className="api-details">
            <summary>개발 정보</summary>
            <code>{getApiBaseUrl()}</code>
          </details>
          <button
            type="button"
            className="secondary-button"
            onClick={() => void loadCurrentSession(accessToken)}
            disabled={connectionState === 'checking'}
          >
            새로고침
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
