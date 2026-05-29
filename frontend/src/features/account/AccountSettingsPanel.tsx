import { useState } from 'react';
import type { FormEvent } from 'react';
import { toErrorResponse } from '../../api/errorHelpers';
import { deleteAccount } from '../../api/smartClosetApi';
import { ApiErrorMessage } from '../../components/ApiErrorMessage';
import type {
  AuthProvider,
  AccountDeletionRequest,
  CurrentUserResponse,
  ErrorResponse,
} from '../../types/api';

type AccountSettingsPanelProps = {
  accessToken: string;
  currentUser: CurrentUserResponse;
  onAccountDeleted: () => void;
};

export function AccountSettingsPanel({
  accessToken,
  currentUser,
  onAccountDeleted,
}: AccountSettingsPanelProps) {
  const [confirmation, setConfirmation] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<ErrorResponse | null>(null);

  const handleDeleteAccount = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    if (confirmation !== 'DELETE') {
      setSubmitting(false);
      return;
    }

    const body: AccountDeletionRequest = {
      confirmation,
      ...(currentUser.passwordLoginEnabled ? { password } : {}),
    };

    try {
      await deleteAccount(accessToken, body);
      onAccountDeleted();
    } catch (caught) {
      const nextError = toErrorResponse(caught, '계정을 삭제할 수 없습니다.');
      setError(nextError);
    } finally {
      setSubmitting(false);
    }
  };

  const providerLabels: Record<AuthProvider, string> = {
    PASSWORD: '이메일 로그인',
    GOOGLE: 'Google 로그인',
  };
  const providerSet = new Set<AuthProvider>(currentUser.authProviders);
  const profileInitial = currentUser.name.trim().charAt(0) || currentUser.email.charAt(0);
  const joinedAt = formatDateTime(currentUser.createdAt);
  const updatedAt = formatDateTime(currentUser.updatedAt);
  const accountStateText = currentUser.emailVerified ? '이메일 인증 완료' : '이메일 인증 필요';
  const accountStateDetail = [
    currentUser.passwordLoginEnabled ? '이메일 로그인 사용 가능' : '이메일 로그인 사용 안 함',
    providerSet.has('GOOGLE') ? 'Google 연결됨' : 'Google 연결 안 됨',
  ].join(' · ');

  return (
    <div className="account-settings-panel">
      <article className="account-hero-card">
        <div className="account-hero-copy">
          <p className="eyebrow">프로필에서 열린 계정 설정</p>
          <div className="account-hero-title">
            <span className="account-hero-avatar" aria-hidden="true">
              {profileInitial.toUpperCase()}
            </span>
            <div>
              <h2>{currentUser.email}</h2>
              <p>{accountStateText} · {accountStateDetail}</p>
            </div>
          </div>
          <div className="account-status-chip-row" aria-label="계정 상태 요약">
            <span>{currentUser.emailVerified ? '이메일 인증됨' : '인증 필요'}</span>
            <span>{currentUser.passwordLoginEnabled ? '이메일 로그인' : 'Password 비활성'}</span>
            <span>{providerSet.has('GOOGLE') ? 'Google 연결' : 'Google 미연결'}</span>
          </div>
        </div>
        <span className="account-context-pill">계정 설정</span>
      </article>

      <div className="account-settings-grid">
        <article className="panel account-summary-card">
          <header className="section-title-row account-section-heading">
            <div>
              <p className="eyebrow">프로필</p>
              <h2>계정 정보</h2>
            </div>
          </header>
          <dl className="account-info-list">
            <div>
              <dt>이메일</dt>
              <dd>
                <span>{currentUser.email}</span>
                <strong>{currentUser.emailVerified ? '인증 완료' : '인증 필요'}</strong>
              </dd>
            </div>
            <div>
              <dt>이름</dt>
              <dd>
                <span>{currentUser.name}</span>
                <strong>{currentUser.role}</strong>
              </dd>
            </div>
            <div>
              <dt>가입일</dt>
              <dd>
                <span>{joinedAt}</span>
                <strong>최근 갱신 {updatedAt}</strong>
              </dd>
            </div>
          </dl>
        </article>

        <aside className="account-side-stack" aria-label="계정 상태">
          <article className="panel account-session-card">
            <header className="account-compact-heading">
              <p className="eyebrow">세션</p>
              <h2>안전하게 유지 중</h2>
            </header>
            <p className="muted account-panel-copy">
              이 기기에서 로그인 중입니다. 새로고침 후에도 refresh cookie로 세션을 복구할 수 있습니다.
            </p>
          </article>

          <article className="panel account-provider-card">
            <header className="account-compact-heading">
              <p className="eyebrow">로그인 방법</p>
              <h2>제공자 상태</h2>
            </header>
            <div className="account-provider-grid">
              {(['PASSWORD', 'GOOGLE'] as AuthProvider[]).map((provider) => {
                const connected =
                  provider === 'PASSWORD'
                    ? currentUser.passwordLoginEnabled
                    : providerSet.has(provider);

                return (
                  <div className="account-provider-tile" data-connected={connected} key={provider}>
                    <strong>{providerLabels[provider]}</strong>
                    <span>{connected ? '사용 가능' : '연결 안 됨'}</span>
                  </div>
                );
              })}
            </div>
            {currentUser.passwordLoginEnabled ? (
              <p className="muted account-panel-copy">
                비밀번호 재설정은 로그아웃 후 인증 화면의 비밀번호 재설정에서 진행할 수 있습니다.
              </p>
            ) : null}
          </article>
        </aside>
      </div>

      <article className="panel account-danger-card">
        <header className="section-title-row account-section-heading">
          <div>
            <p className="eyebrow">계정 삭제</p>
            <h2>내 데이터 삭제</h2>
          </div>
        </header>
        <p className="muted account-delete-copy">
          계정과 옷장, 위치, 취향, 추천 이력, 이미지 파일이 함께 삭제됩니다.
          {currentUser.passwordLoginEnabled
            ? ' Password 계정은 현재 비밀번호 확인이 필요합니다.'
            : ' Google-only 계정은 확인 문구만 필요합니다.'}
        </p>
        {error ? <ApiErrorMessage error={error} className="error-banner" /> : null}
        <form className="panel-form account-delete-form" onSubmit={handleDeleteAccount}>
          <label className="field">
            <span>확인 문구</span>
            <input
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
              placeholder="DELETE"
              required
            />
          </label>
          {currentUser.passwordLoginEnabled ? (
            <label className="field">
              <span>현재 비밀번호</span>
              <input
                type="password"
                value={password}
                autoComplete="current-password"
                onChange={(event) => setPassword(event.target.value)}
                required
              />
            </label>
          ) : null}
          <button
            className="secondary-button danger-button"
            type="submit"
            disabled={submitting || confirmation !== 'DELETE'}
          >
            {submitting ? '삭제 중' : '계정 삭제'}
          </button>
        </form>
      </article>
    </div>
  );
}

function formatDateTime(value: string) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return new Intl.DateTimeFormat('ko-KR', {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date);
}
