import { useState } from 'react';
import type { FormEvent } from 'react';
import { toErrorResponse } from '../../api/errorHelpers';
import { login, signup } from '../../api/smartClosetApi';
import { ApiErrorMessage } from '../../components/ApiErrorMessage';
import type { AuthResponse, ErrorResponse, LoginRequest, SignupRequest } from '../../types/api';

type AuthPanelProps = {
  onAuthenticated: (response: AuthResponse) => void;
};

type AuthMode = 'login' | 'signup';

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
  const [authMode, setAuthMode] = useState<AuthMode>('login');
  const [loginForm, setLoginForm] = useState<LoginRequest>(defaultLoginForm);
  const [signupForm, setSignupForm] = useState<SignupRequest>(defaultSignupForm);
  const [submitting, setSubmitting] = useState<'login' | 'signup' | null>(null);
  const [error, setError] = useState<ErrorResponse | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

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
      onAuthenticated(response);
      setLoginForm(defaultLoginForm);
    } catch (caught) {
      setError(toErrorResponse(caught, '로그인할 수 없습니다.'));
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
      await signup({
        email: signupEmail,
        password: signupForm.password,
        name: signupForm.name.trim(),
      });
      setAuthMode('login');
      setLoginForm({ email: signupEmail, password: '' });
      setSignupForm(defaultSignupForm);
      setSuccessMessage('회원가입이 완료되었습니다. 가입한 이메일로 로그인해주세요.');
    } catch (caught) {
      setError(toErrorResponse(caught, '회원가입할 수 없습니다.'));
    } finally {
      setSubmitting(null);
    }
  };

  return (
    <section className="auth-layout" aria-label="인증">
      <article className="panel auth-panel">
        <header className="auth-panel-heading">
          <h2>{authMode === 'login' ? '로그인' : '회원가입'}</h2>
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
              <button className="primary-button" type="submit" disabled={submitting !== null}>
                {submitting === 'login' ? '로그인 중' : '로그인'}
              </button>
            </form>
          </div>
        ) : (
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
        )}
      </article>
    </section>
  );
}
