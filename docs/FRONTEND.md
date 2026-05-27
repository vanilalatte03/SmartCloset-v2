# Frontend: SmartCloset MVP8 Account Stability

## 목표

MVP8 프론트엔드는 MVP7 반응형 웹앱 위에 세션 복구, 이메일 인증, 비밀번호 재설정, Google login, 세션 만료 안내, 계정 삭제 UI를 추가한다.

사용자는 access token 만료나 새로고침 상황에서도 가능한 한 자연스럽게 세션을 복구하고, 복구가 불가능하면 명확한 안내를 받아야 한다.

## 기술 기준

- React
- Vite
- TypeScript strict
- 기존 CSS 체계 우선
- 큰 상태 관리 라이브러리 추가 금지
- API 요청/응답 DTO는 `src/types/api.ts`에 명시
- API 함수는 `src/api/smartClosetApi.ts`에서 정의
- Access token은 memory state에 저장
- Refresh token은 HttpOnly cookie로만 관리
- refresh cookie를 사용하는 요청은 credentials 포함
- 로그인 이메일 저장 체크박스는 이메일 주소 문자열만 브라우저 저장소에 저장할 수 있다.

## 인증 상태 기준

- 앱 시작 시 access token이 없으면 `POST /api/auth/refresh`를 호출해 세션 복구를 시도한다.
- refresh 성공 시 access token과 current user를 memory state에 저장한다.
- refresh 실패 시 anonymous 상태로 전환한다.
- 보호 API가 `401`을 반환하면 refresh를 한 번 시도하고 원 요청을 한 번만 재시도한다.
- retry 이후에도 실패하면 access token을 제거하고 세션 만료 안내를 표시한다.
- 로그아웃은 `POST /api/auth/logout`을 호출한 뒤 local auth state를 초기화한다.

## API Client 기준

MVP8에서 추가/변경할 함수:

- `signup(body)`
- `login(body)`
- `refreshSession()`
- `logout()`
- `requestEmailVerification(body)`
- `confirmEmailVerification(body)`
- `requestPasswordReset(body)`
- `confirmPasswordReset(body)`
- `getOAuthProviders()`
- `deleteAccount(accessToken, body)`
- 보호 API 공통 request는 401 retry-once를 지원할 수 있어야 한다.

Refresh cookie 요청:

- `fetch`에 `credentials: 'include'`를 사용한다.
- `AuthResponse`에 refresh token string이 없음을 전제로 한다.

이미지 blob fetch:

- MVP5와 동일하게 Authorization header를 붙인다.
- object URL은 cleanup한다.

## 타입 기준

```ts
export type AuthProvider = 'PASSWORD' | 'GOOGLE';

export type CurrentUserResponse = {
  email: string;
  name: string;
  role: 'USER';
  emailVerified: boolean;
  passwordLoginEnabled: boolean;
  authProviders: AuthProvider[];
  createdAt: string;
  updatedAt: string;
};

export type SignupResponse = {
  email: string;
  emailVerificationRequired: boolean;
  message: string;
};

export type AuthResponse = {
  accessToken: string;
  tokenType: 'Bearer';
  user: CurrentUserResponse;
};

export type EmailVerificationRequest = {
  email: string;
};

export type EmailVerificationConfirmRequest = {
  token: string;
};

export type PasswordResetRequest = {
  email: string;
};

export type PasswordResetConfirmRequest = {
  token: string;
  newPassword: string;
};

export type OAuthProvidersResponse = {
  google: {
    enabled: boolean;
    loginUrl: string | null;
  };
};

export type AccountDeletionRequest = {
  confirmation: 'DELETE';
  password?: string;
};
```

MVP5/MVP6/MVP7 위치, 날씨, 옷, 추천 타입은 유지한다.

## Auth View

Auth view에는 아래 flow를 제공한다.

- 로그인
- 로그인 이메일 저장 체크박스
- 회원가입
- 이메일 인증 안내와 인증 재요청
- 인증 token 확인
- 비밀번호 재설정 요청
- 비밀번호 재설정 확인
- Google login button

UX 기준:

- 회원가입 성공 후 "이메일 인증 후 로그인할 수 있습니다" 상태를 보여준다.
- MVP8 local 개발에서는 console/log email sender를 사용한다는 문구는 문서/개발 안내에만 두고, 앱 UI는 사용자 친화적인 인증 안내를 표시한다.
- 미인증 계정 로그인 실패는 인증 재요청으로 이어질 수 있어야 한다.
- 비밀번호 재설정 요청은 계정 존재 여부를 노출하지 않는 중립 성공 메시지를 보여준다.
- Google provider disabled 상태면 button을 비활성화하고 설정 필요 상태를 작게 표시한다.
- 이메일 저장 체크박스를 선택하면 로그인 성공 후 이메일 입력값만 저장하고, 다음 Auth view 진입 시 이메일 input에 복원한다.
- 이메일 저장 체크박스를 해제한 상태로 로그인하면 저장된 이메일 값을 제거한다.
- 이메일 저장 storage key는 앱 전용 prefix를 사용한다.

## Session UX

상태:

- `restoring`: refresh session 확인 중
- `anonymous`: 로그인 필요
- `authenticated`: access token 보유
- `expired`: refresh 실패 후 세션 만료 안내

규칙:

- `restoring` 화면은 layout shift가 작아야 한다.
- 보호 API retry는 한 요청당 한 번만 수행한다.
- refresh가 실패하면 남은 access token state를 제거한다.
- 로그아웃 후에는 refresh cookie 만료 요청을 보내고 로그인 화면으로 이동한다.

## Account Settings UX

Authenticated shell에 account/settings 진입점을 추가한다.

표시 항목:

- 이메일
- 이메일 인증 상태
- 연결된 로그인 제공자: password, Google
- password login 가능 여부
- 계정 삭제 control

계정 삭제:

- 삭제 전 확인 문구를 요구한다.
- Password login enabled 계정은 현재 비밀번호 입력을 요구한다.
- Google-only 계정은 confirmation만 요구한다.
- 삭제 성공 후 local auth state를 초기화하고 로그인 화면으로 이동한다.
- 삭제 실패 시 공통 error banner를 사용한다.

## 기존 UX 유지

- Location view의 동네 검색, 현재 위치 후보 찾기, source 표시를 유지한다.
- Today view의 상황/예보 시간대 선택과 추천 결과 source 표시를 유지한다.
- History view의 위치/날씨 snapshot 표시를 유지한다.
- Closet image blob fetch와 object URL cleanup을 유지한다.
- Feedback UX를 유지한다.

## 금지사항

- Access token을 `localStorage`나 `sessionStorage`에 저장하지 마라. 이유: MVP8은 memory state와 refresh cookie 기준이다.
- Refresh token 값을 JavaScript state나 JSON body에 저장하지 마라. 이유: refresh token은 HttpOnly cookie 전용이다.
- 이메일 저장 기능으로 비밀번호, access token, refresh token, current user object를 저장하지 마라. 이유: 편의 기능은 이메일 주소 문자열에만 한정한다.
- 큰 state-management library를 추가하지 마라. 이유: 현재 앱은 React state와 작은 hook으로 충분하다.
- AWS/S3/SES 전용 UI를 추가하지 마라. 이유: AWS 구현은 MVP9 범위다.
