import { useState } from 'react';
import type { FormEvent } from 'react';
import { toErrorResponse } from '../../api/errorHelpers';
import { deleteAccount } from '../../api/smartClosetApi';
import { ApiErrorMessage } from '../../components/ApiErrorMessage';
import type {
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

  const providers =
    currentUser.authProviders.length > 0 ? currentUser.authProviders.join(', ') : '연결 없음';

  return (
    <div className="account-settings-panel">
      <article className="panel account-summary-card">
        <header className="section-title-row account-section-heading">
          <div>
            <p className="eyebrow">계정 설정</p>
            <h2>{currentUser.name}</h2>
          </div>
        </header>
        <dl className="metric-list account-metric-list">
          <div>
            <dt>이메일</dt>
            <dd>{currentUser.email}</dd>
          </div>
          <div>
            <dt>이메일 인증</dt>
            <dd>{currentUser.emailVerified ? '완료' : '필요'}</dd>
          </div>
          <div>
            <dt>로그인 제공자</dt>
            <dd>{providers}</dd>
          </div>
          <div>
            <dt>Password login</dt>
            <dd>{currentUser.passwordLoginEnabled ? '사용 가능' : '사용 안 함'}</dd>
          </div>
        </dl>
      </article>

      <article className="panel account-danger-card">
        <header className="section-title-row account-section-heading">
          <div>
            <p className="eyebrow">계정 삭제</p>
            <h2>데이터 삭제</h2>
          </div>
        </header>
        <p className="muted account-delete-copy">
          계정과 옷장, 추천 이력, 이미지 파일이 삭제됩니다.
        </p>
        {error ? <ApiErrorMessage error={error} className="error-banner" /> : null}
        <form className="panel-form account-delete-form" onSubmit={handleDeleteAccount}>
          <label className="field">
            <span>확인 문구</span>
            <input
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
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
