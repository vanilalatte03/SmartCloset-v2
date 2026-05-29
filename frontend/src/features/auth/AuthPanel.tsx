import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { toErrorResponse } from '../../api/errorHelpers';
import {
  confirmEmailVerification,
  confirmPasswordReset,
  getApiBaseUrl,
  getOAuthProviders,
  login,
  requestEmailVerification,
  requestPasswordReset,
  signup,
} from '../../api/smartClosetApi';
import { ApiErrorMessage } from '../../components/ApiErrorMessage';
import type {
  AuthResponse,
  ErrorResponse,
  LoginRequest,
  OAuthProvidersResponse,
  SignupRequest,
} from '../../types/api';

type AuthPanelProps = {
  onAuthenticated: (response: AuthResponse) => void;
};

type AuthMode = 'login' | 'signup' | 'verify' | 'reset';

const rememberedEmailStorageKey = 'smartcloset.auth.rememberedEmail';

const authModeTitles: Record<AuthMode, string> = {
  login: '로그인',
  signup: '회원가입',
  verify: '이메일 인증',
  reset: '비밀번호 재설정',
};

const defaultLoginForm: LoginRequest = {
  email: '',
  password: '',
};

const defaultSignupForm: SignupRequest = {
  email: '',
  password: '',
  name: '',
};

export function AuthPanel({ onAuthenticated }: AuthPanelProps) {
  const rememberedEmail = readRememberedEmail();
  const [authMode, setAuthMode] = useState<AuthMode>('login');
  const [loginForm, setLoginForm] = useState<LoginRequest>({
    ...defaultLoginForm,
    email: rememberedEmail,
  });
  const [signupForm, setSignupForm] = useState<SignupRequest>(defaultSignupForm);
  const [rememberEmail, setRememberEmail] = useState(rememberedEmail.length > 0);
  const [verificationEmail, setVerificationEmail] = useState(rememberedEmail);
  const [verificationToken, setVerificationToken] = useState('');
  const [resetEmail, setResetEmail] = useState(rememberedEmail);
  const [resetToken, setResetToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [submitting, setSubmitting] = useState<
    'login' | 'signup' | 'verification-request' | 'verification-confirm' | 'reset-request' | 'reset-confirm' | null
  >(null);
  const [oauthProviders, setOAuthProviders] = useState<OAuthProvidersResponse | null>(null);
  const [error, setError] = useState<ErrorResponse | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    getOAuthProviders()
      .then((providers) => {
        if (!cancelled) {
          setOAuthProviders(providers);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setOAuthProviders({ google: { enabled: false, loginUrl: null } });
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const handleAuthModeChange = (mode: AuthMode) => {
    setAuthMode(mode);
    setError(null);
    setSuccessMessage(null);
  };

  const handleLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting('login');
    setError(null);
    setSuccessMessage(null);

    try {
      const response = await login({
        email: loginForm.email.trim(),
        password: loginForm.password,
      });
      if (rememberEmail) {
        saveRememberedEmail(loginForm.email.trim());
      } else {
        clearRememberedEmail();
      }
      onAuthenticated(response);
      setLoginForm(defaultLoginForm);
    } catch (caught) {
      const nextError = toErrorResponse(caught, '로그인할 수 없습니다.');
      setError(nextError);
      if (nextError.code === 'EMAIL_VERIFICATION_REQUIRED') {
        const email = loginForm.email.trim();
        setVerificationEmail(email);
        setAuthMode('verify');
        setSuccessMessage('이메일 인증을 완료하면 로그인할 수 있습니다.');
      }
    } finally {
      setSubmitting(null);
    }
  };

  const handleSignup = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting('signup');
    setError(null);
    setSuccessMessage(null);
    const signupEmail = signupForm.email.trim();

    try {
      const response = await signup({
        email: signupEmail,
        password: signupForm.password,
        name: signupForm.name.trim(),
      });
      setAuthMode('verify');
      setLoginForm({ email: signupEmail, password: '' });
      setVerificationEmail(response.email);
      setSignupForm(defaultSignupForm);
      setSuccessMessage(response.message);
    } catch (caught) {
      setError(toErrorResponse(caught, '회원가입할 수 없습니다.'));
    } finally {
      setSubmitting(null);
    }
  };

  const handleVerificationRequest = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting('verification-request');
    setError(null);
    setSuccessMessage(null);

    try {
      await requestEmailVerification({ email: verificationEmail.trim() });
      setSuccessMessage('인증 메일을 다시 보냈습니다. 받은 링크나 token으로 인증을 완료해주세요.');
    } catch (caught) {
      setError(toErrorResponse(caught, '인증 메일을 요청할 수 없습니다.'));
    } finally {
      setSubmitting(null);
    }
  };

  const handleVerificationConfirm = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting('verification-confirm');
    setError(null);
    setSuccessMessage(null);

    try {
      await confirmEmailVerification({ token: verificationToken.trim() });
      setAuthMode('login');
      setLoginForm({ email: verificationEmail.trim(), password: '' });
      setVerificationToken('');
      setSuccessMessage('이메일 인증이 완료되었습니다. 이제 로그인할 수 있습니다.');
    } catch (caught) {
      setError(toErrorResponse(caught, '이메일 인증을 완료할 수 없습니다.'));
    } finally {
      setSubmitting(null);
    }
  };

  const handlePasswordResetRequest = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting('reset-request');
    setError(null);
    setSuccessMessage(null);

    try {
      await requestPasswordReset({ email: resetEmail.trim() });
      setSuccessMessage('비밀번호 재설정 안내를 보냈습니다. 받은 token으로 새 비밀번호를 설정해주세요.');
    } catch (caught) {
      setError(toErrorResponse(caught, '비밀번호 재설정을 요청할 수 없습니다.'));
    } finally {
      setSubmitting(null);
    }
  };

  const handlePasswordResetConfirm = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting('reset-confirm');
    setError(null);
    setSuccessMessage(null);

    try {
      await confirmPasswordReset({
        token: resetToken.trim(),
        newPassword,
      });
      setAuthMode('login');
      setLoginForm({ email: resetEmail.trim(), password: '' });
      setResetToken('');
      setNewPassword('');
      setSuccessMessage('비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.');
    } catch (caught) {
      setError(toErrorResponse(caught, '비밀번호를 변경할 수 없습니다.'));
    } finally {
      setSubmitting(null);
    }
  };

  const googleEnabled = oauthProviders?.google.enabled ?? false;
  const googleLoginUrl = oauthProviders?.google.loginUrl;
  const googleHref =
    googleEnabled && googleLoginUrl
      ? googleLoginUrl.startsWith('http')
        ? googleLoginUrl
        : `${getApiBaseUrl()}${googleLoginUrl}`
      : undefined;

  return (
    <section className="auth-layout" aria-label="인증">
      <article className="panel auth-panel">
        <header className="auth-panel-heading">
          <p className="eyebrow">SmartCloset</p>
          <h2>{authModeTitles[authMode]}</h2>
          <div className="auth-tabs" role="tablist" aria-label="인증 방식">
            <button
              className="auth-tab"
              id="login-tab"
              type="button"
              role="tab"
              aria-selected={authMode === 'login'}
              aria-controls="login-panel"
              onClick={() => handleAuthModeChange('login')}
            >
              로그인
            </button>
            <button
              className="auth-tab"
              id="signup-tab"
              type="button"
              role="tab"
              aria-selected={authMode === 'signup'}
              aria-controls="signup-panel"
              onClick={() => handleAuthModeChange('signup')}
            >
              회원가입
            </button>
            <button
              className="auth-tab"
              id="verify-tab"
              type="button"
              role="tab"
              aria-selected={authMode === 'verify'}
              aria-controls="verify-panel"
              onClick={() => handleAuthModeChange('verify')}
            >
              이메일 인증
            </button>
            <button
              className="auth-tab"
              id="reset-tab"
              type="button"
              role="tab"
              aria-selected={authMode === 'reset'}
              aria-controls="reset-panel"
              onClick={() => handleAuthModeChange('reset')}
            >
              비밀번호 재설정
            </button>
          </div>
        </header>

        <div className="auth-message-slot" aria-live="polite">
          {successMessage ? <p className="success-banner auth-success">{successMessage}</p> : null}
          {error ? <ApiErrorMessage error={error} className="error-banner auth-error" /> : null}
        </div>

        {authMode === 'login' ? (
          <div
            className="auth-tab-panel"
            id="login-panel"
            role="tabpanel"
            aria-labelledby="login-tab"
          >
            <form className="panel-form compact-form" onSubmit={handleLogin}>
              <label className="field">
                <span>이메일</span>
                <input
                  type="email"
                  value={loginForm.email}
                  autoComplete="email"
                  onChange={(event) => setLoginForm({ ...loginForm, email: event.target.value })}
                  required
                />
              </label>
              <label className="field">
                <span>비밀번호</span>
                <input
                  type="password"
                  value={loginForm.password}
                  autoComplete="current-password"
                  onChange={(event) =>
                    setLoginForm({ ...loginForm, password: event.target.value })
                  }
                  required
                />
              </label>
              <label className="checkbox-field auth-checkbox">
                <input
                  type="checkbox"
                  checked={rememberEmail}
                  onChange={(event) => setRememberEmail(event.target.checked)}
                />
                <span>이메일 저장</span>
              </label>
              <button className="primary-button" type="submit" disabled={submitting !== null}>
                {submitting === 'login' ? '로그인 중' : '로그인'}
              </button>
            </form>
            <div className="auth-secondary-actions">
              <button className="secondary-button" type="button" onClick={() => handleAuthModeChange('verify')}>
                인증하기
              </button>
              <button className="secondary-button" type="button" onClick={() => handleAuthModeChange('reset')}>
                비밀번호 재설정
              </button>
            </div>
            <div className="oauth-box">
              <a
                className={`primary-button oauth-button${googleHref ? '' : ' disabled'}`}
                href={googleHref}
                aria-disabled={!googleHref}
                onClick={(event) => {
                  if (!googleHref) {
                    event.preventDefault();
                  }
                }}
              >
                Google로 계속
              </a>
              {!googleEnabled ? (
                <p className="muted auth-helper">Google 로그인은 현재 사용할 수 없습니다.</p>
              ) : null}
            </div>
          </div>
        ) : authMode === 'signup' ? (
          <div
            className="auth-tab-panel"
            id="signup-panel"
            role="tabpanel"
            aria-labelledby="signup-tab"
          >
            <form className="panel-form compact-form" onSubmit={handleSignup}>
              <label className="field">
                <span>이름</span>
                <input
                  value={signupForm.name}
                  autoComplete="name"
                  maxLength={50}
                  onChange={(event) => setSignupForm({ ...signupForm, name: event.target.value })}
                  required
                />
              </label>
              <label className="field">
                <span>이메일</span>
                <input
                  type="email"
                  value={signupForm.email}
                  autoComplete="email"
                  onChange={(event) => setSignupForm({ ...signupForm, email: event.target.value })}
                  required
                />
              </label>
              <label className="field">
                <span>비밀번호</span>
                <input
                  type="password"
                  value={signupForm.password}
                  autoComplete="new-password"
                  minLength={8}
                  onChange={(event) =>
                    setSignupForm({ ...signupForm, password: event.target.value })
                  }
                  required
                />
              </label>
              <button className="primary-button" type="submit" disabled={submitting !== null}>
                {submitting === 'signup' ? '가입 중' : '회원가입'}
              </button>
            </form>
          </div>
        ) : authMode === 'verify' ? (
          <div
            className="auth-tab-panel auth-flow-panel"
            id="verify-panel"
            role="tabpanel"
            aria-labelledby="verify-tab"
          >
            <form className="panel-form compact-form" onSubmit={handleVerificationRequest}>
              <label className="field">
                <span>이메일</span>
                <input
                  type="email"
                  value={verificationEmail}
                  autoComplete="email"
                  onChange={(event) => setVerificationEmail(event.target.value)}
                  required
                />
              </label>
              <button className="secondary-button" type="submit" disabled={submitting !== null}>
                {submitting === 'verification-request' ? '요청 중' : '인증 메일 다시 받기'}
              </button>
            </form>
            <form className="panel-form compact-form" onSubmit={handleVerificationConfirm}>
              <label className="field">
                <span>인증 token</span>
                <input
                  value={verificationToken}
                  autoComplete="one-time-code"
                  onChange={(event) => setVerificationToken(event.target.value)}
                  required
                />
              </label>
              <button className="primary-button" type="submit" disabled={submitting !== null}>
                {submitting === 'verification-confirm' ? '확인 중' : '인증 완료'}
              </button>
            </form>
          </div>
        ) : (
          <div
            className="auth-tab-panel auth-flow-panel"
            id="reset-panel"
            role="tabpanel"
            aria-labelledby="reset-tab"
          >
            <form className="panel-form compact-form" onSubmit={handlePasswordResetRequest}>
              <label className="field">
                <span>이메일</span>
                <input
                  type="email"
                  value={resetEmail}
                  autoComplete="email"
                  onChange={(event) => setResetEmail(event.target.value)}
                  required
                />
              </label>
              <button className="secondary-button" type="submit" disabled={submitting !== null}>
                {submitting === 'reset-request' ? '요청 중' : '재설정 안내 받기'}
              </button>
            </form>
            <form className="panel-form compact-form" onSubmit={handlePasswordResetConfirm}>
              <label className="field">
                <span>재설정 token</span>
                <input
                  value={resetToken}
                  autoComplete="one-time-code"
                  onChange={(event) => setResetToken(event.target.value)}
                  required
                />
              </label>
              <label className="field">
                <span>새 비밀번호</span>
                <input
                  type="password"
                  value={newPassword}
                  autoComplete="new-password"
                  minLength={8}
                  onChange={(event) => setNewPassword(event.target.value)}
                  required
                />
              </label>
              <button className="primary-button" type="submit" disabled={submitting !== null}>
                {submitting === 'reset-confirm' ? '변경 중' : '비밀번호 변경'}
              </button>
            </form>
          </div>
        )}
      </article>
    </section>
  );
}

function readRememberedEmail(): string {
  try {
    return localStorage.getItem(rememberedEmailStorageKey) ?? '';
  } catch {
    return '';
  }
}

function saveRememberedEmail(email: string) {
  try {
    localStorage.setItem(rememberedEmailStorageKey, email);
  } catch {
    // Remembering the email is a convenience feature only.
  }
}

function clearRememberedEmail() {
  try {
    localStorage.removeItem(rememberedEmailStorageKey);
  } catch {
    // Nothing else should fail because email storage is unavailable.
  }
}
