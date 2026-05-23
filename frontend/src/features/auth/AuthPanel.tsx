import { useState } from 'react';
import type { FormEvent } from 'react';
import { toErrorResponse } from '../../api/errorHelpers';
import { login, signup } from '../../api/smartClosetApi';
import { ApiErrorMessage } from '../../components/ApiErrorMessage';
import type { AuthResponse, ErrorResponse, LoginRequest, SignupRequest } from '../../types/api';

type AuthPanelProps = {
  onAuthenticated: (response: AuthResponse) => void;
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
  const [loginForm, setLoginForm] = useState<LoginRequest>(defaultLoginForm);
  const [signupForm, setSignupForm] = useState<SignupRequest>(defaultSignupForm);
  const [submitting, setSubmitting] = useState<'login' | 'signup' | null>(null);
  const [error, setError] = useState<ErrorResponse | null>(null);

  const handleLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setSubmitting('login');
    setError(null);

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

    try {
      const response = await signup({
        email: signupForm.email.trim(),
        password: signupForm.password,
        name: signupForm.name.trim(),
      });
      onAuthenticated(response);
      setSignupForm(defaultSignupForm);
    } catch (caught) {
      setError(toErrorResponse(caught, '회원가입할 수 없습니다.'));
    } finally {
      setSubmitting(null);
    }
  };

  return (
    <section className="auth-layout" aria-label="인증">
      <article className="panel auth-panel">
        <h2>로그인</h2>
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
              onChange={(event) => setLoginForm({ ...loginForm, password: event.target.value })}
              required
            />
          </label>
          <button className="primary-button" type="submit" disabled={submitting !== null}>
            {submitting === 'login' ? '로그인 중' : '로그인'}
          </button>
        </form>
      </article>

      <article className="panel auth-panel">
        <h2>회원가입</h2>
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
      </article>

      {error ? <ApiErrorMessage error={error} className="error-banner auth-error" /> : null}
    </section>
  );
}
