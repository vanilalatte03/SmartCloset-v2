import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { toErrorResponse } from '../../api/errorHelpers';
import { deleteAccount, updateCurrentUser } from '../../api/smartClosetApi';
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
  onCurrentUserUpdated: (currentUser: CurrentUserResponse) => void;
};

export function AccountSettingsPanel({
  accessToken,
  currentUser,
  onAccountDeleted,
  onCurrentUserUpdated,
}: AccountSettingsPanelProps) {
  const [confirmation, setConfirmation] = useState('');
  const [password, setPassword] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<ErrorResponse | null>(null);
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false);
  const [editingName, setEditingName] = useState(false);
  const [draftName, setDraftName] = useState(currentUser.name);
  const [nameSubmitting, setNameSubmitting] = useState(false);
  const [nameError, setNameError] = useState<ErrorResponse | null>(null);

  useEffect(() => {
    if (!editingName) {
      setDraftName(currentUser.name);
    }
  }, [currentUser.name, editingName]);

  const closeDeleteDialog = () => {
    if (submitting) {
      return;
    }

    setDeleteDialogOpen(false);
    setConfirmation('');
    setPassword('');
    setError(null);
  };

  const handleDeleteAccount = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    if (confirmation !== 'DELETE') {
      setSubmitting(false);
      return;
    }

    // Google-only 계정은 확인 문구만 보내고, password login 계정은 서버에서 비밀번호를 재검증한다.
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

  const openNameEditor = () => {
    setDraftName(currentUser.name);
    setNameError(null);
    setEditingName(true);
  };

  const closeNameEditor = () => {
    if (nameSubmitting) {
      return;
    }

    setDraftName(currentUser.name);
    setNameError(null);
    setEditingName(false);
  };

  const handleUpdateName = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const nextName = draftName.trim();

    if (!nextName) {
      setNameError({
        code: 'INVALID_REQUEST',
        message: '이름을 입력해주세요.',
        details: [],
      });
      return;
    }

    setNameSubmitting(true);
    setNameError(null);

    try {
      const updatedUser = await updateCurrentUser(accessToken, { name: nextName });
      onCurrentUserUpdated(updatedUser);
      setEditingName(false);
    } catch (caught) {
      setNameError(toErrorResponse(caught, '이름을 수정할 수 없습니다.'));
    } finally {
      setNameSubmitting(false);
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

  return (
    <div className="account-settings-panel">
      <article className="account-hero-card">
        <p className="account-hero-eyebrow">계정 상태</p>
        <div className="account-hero-title">
          <span className="account-hero-avatar" aria-hidden="true">
            {profileInitial.toUpperCase()}
          </span>
          <h2>{currentUser.email}</h2>
        </div>
        <div className="account-status-chip-row" aria-label="계정 상태 요약">
          <span>{currentUser.emailVerified ? '이메일 확인됨' : '확인 필요'}</span>
          <span>{currentUser.passwordLoginEnabled ? '이메일 로그인' : 'Password 비활성'}</span>
          <span>{providerSet.has('GOOGLE') ? 'Google 연결' : 'Google 미연결'}</span>
        </div>
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
                {editingName ? (
                  <form className="account-name-edit-form" onSubmit={handleUpdateName}>
                    <input
                      aria-label="이름"
                      value={draftName}
                      maxLength={50}
                      onChange={(event) => setDraftName(event.target.value)}
                      disabled={nameSubmitting}
                      required
                    />
                    <div className="account-name-edit-actions">
                      <button
                        className="account-inline-action account-inline-action-muted"
                        type="button"
                        onClick={closeNameEditor}
                        disabled={nameSubmitting}
                      >
                        취소
                      </button>
                      <button
                        className="account-inline-action"
                        type="submit"
                        disabled={nameSubmitting || draftName.trim() === currentUser.name}
                      >
                        {nameSubmitting ? '저장 중' : '저장'}
                      </button>
                    </div>
                  </form>
                ) : (
                  <>
                    <span>{currentUser.name}</span>
                    <button
                      className="account-inline-action"
                      type="button"
                      onClick={openNameEditor}
                    >
                      이름 수정
                    </button>
                  </>
                )}
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
          {nameError ? <ApiErrorMessage error={nameError} className="error-banner" /> : null}

          <div className="account-summary-actions" aria-label="계정 관리">
            <button
              className="account-danger-trigger"
              type="button"
              onClick={() => setDeleteDialogOpen(true)}
            >
              계정 삭제
            </button>
          </div>
        </article>

        <aside className="account-side-stack" aria-label="계정 상태">
          <article className="panel account-session-card">
            <header className="account-compact-heading">
              <p className="eyebrow">세션</p>
              <h2>안전하게 유지 중</h2>
            </header>
            <p className="muted account-panel-copy">
              새로고침 후에도 로그인 상태를 복구할 수 있습니다.
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
                    <span>{connected ? '연결됨' : '연결 안 됨'}</span>
                  </div>
                );
              })}
            </div>
            {currentUser.passwordLoginEnabled ? (
              <p className="muted account-panel-copy">
                비밀번호 관리는 로그아웃 후 인증 화면에서 진행할 수 있습니다.
              </p>
            ) : null}
          </article>
        </aside>
      </div>

      {deleteDialogOpen ? (
        <div
          className="account-delete-modal-backdrop"
          role="presentation"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closeDeleteDialog();
            }
          }}
        >
          <section
            className="account-delete-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="account-delete-title"
          >
            <header className="account-delete-modal-heading">
              <div>
                <p className="eyebrow">계정 삭제</p>
                <h2 id="account-delete-title">내 데이터 삭제</h2>
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
              <div className="account-delete-modal-actions">
                <button
                  className="secondary-button"
                  type="button"
                  onClick={closeDeleteDialog}
                  disabled={submitting}
                >
                  취소
                </button>
                <button
                  className="secondary-button danger-button"
                  type="submit"
                  disabled={submitting || confirmation !== 'DELETE'}
                >
                  {submitting ? '삭제 중' : '계정 삭제'}
                </button>
              </div>
            </form>
          </section>
        </div>
      ) : null}
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
