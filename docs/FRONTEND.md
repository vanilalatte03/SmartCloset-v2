# Frontend: SmartCloset Current Baseline

## 목표
현재 프론트엔드는 고정 `userId=1` 대시보드에서 벗어나 회원가입/로그인 후 인증 사용자 기준으로 옷장, 위치, 선호도, 추천, 추천 이력을 확인하는 React+Vite+TypeScript SPA다.

## MVP4 작성 메모
MVP4 프론트 범위는 아직 확정되지 않았다. 새 화면, 라우팅, 상태 관리 라이브러리, API client 변경은 `docs/PRD.md`에서 범위를 정한 뒤 이 문서에 반영한다.

`frontend/` 디렉터리는 React+Vite+TypeScript SPA이며 Docker Compose `frontend` 서비스와 함께 유지한다.

주요 흐름:

- 회원가입
- 로그인
- `sessionStorage` access token 저장
- 새로고침 후 로그인 상태 복구
- 현재 사용자 정보 조회
- 사용자 위치 확인/변경
- 선호도 저장/조회/표시
- 옷 목록 조회와 등록
- 추천 생성
- 추천 이력 조회
- 추천 결과 착용 완료 처리
- 로그아웃

## 기술 기준
- React
- Vite
- TypeScript
- TypeScript `strict` 사용
- CSS는 기본 CSS module 또는 단일 앱 CSS로 시작한다.
- 대형 상태 관리 라이브러리는 사용하지 않는다.
- React Router는 현재 baseline에서 필수로 두지 않는다. 로그인 전/후 상태 전환은 React state로 시작할 수 있다.

## 인증 상태 기준
프론트 access token 저장 위치는 `sessionStorage`로 고정한다.

권장 key:

```text
smartcloset.accessToken
```

흐름:

1. 앱 시작 시 `sessionStorage`에서 access token을 읽는다.
2. token이 있으면 `GET /api/users/me`를 호출해 현재 사용자 정보를 복구한다.
3. token이 없거나 `GET /api/users/me`가 `401`이면 로그인 화면을 보여준다.
4. 로그인 성공 시 access token을 `sessionStorage`에 저장하고 사용자 정보를 상태에 보관한다.
5. 모든 보호 API 요청에는 `Authorization: Bearer {accessToken}`을 붙인다.
6. 로그아웃 시 `sessionStorage` token과 사용자 상태를 제거한다.

Refresh token은 현재 baseline 범위가 아니다.

## 디렉터리 기준
```text
frontend
├── package.json
├── tsconfig.json
├── vite.config.ts
└── src
    ├── api
    │   ├── client.ts
    │   └── smartClosetApi.ts
    ├── components
    ├── features
    │   ├── auth
    │   ├── clothes
    │   ├── location
    │   ├── preferences
    │   └── recommendation
    ├── types
    │   └── api.ts
    ├── App.tsx
    └── main.tsx
```

## 환경변수
프론트는 Vite 환경변수로 백엔드 API base URL을 받는다.

```env
VITE_API_BASE_URL=http://localhost:8080
```

Docker Compose에서는 프론트 컨테이너가 브라우저에서 접근 가능한 API base URL을 사용해야 한다. 로컬 기본값은 `http://localhost:8080`이다.

## API Client 기준
- 모든 요청/응답 DTO는 `src/types/api.ts`에 명시한다.
- API 함수는 `src/api/smartClosetApi.ts`에서만 정의한다.
- 컴포넌트에서 `fetch`를 직접 호출하지 않는다.
- 성공 응답은 `{ data: T }` 형태로 파싱한다.
- 실패 응답은 `{ code, message, details }` 형태로 파싱해 화면 상태로 전달한다.
- 보호 API는 access token을 받아 `Authorization: Bearer ...` header를 붙인다.
- `401`은 인증 만료로 취급하고 로그인 화면으로 전환한다.

필수 타입:

```ts
export type ApiResponse<T> = {
  data: T;
};

export type ErrorResponse = {
  code: string;
  message: string;
  details: Array<{
    field: string;
    message: string;
  }>;
};

export type AuthResponse = {
  accessToken: string;
  tokenType: 'Bearer';
  user: CurrentUserResponse;
};

export type CurrentUserResponse = {
  email: string;
  name: string;
  role: 'USER';
  createdAt: string;
  updatedAt: string;
};

export type UserLocationResponse = {
  code: string;
  name: string;
  nx: number;
  ny: number;
  updatedAt: string;
};

export type UserPreferencesResponse = {
  preferredColors: ClothingColor[];
  preferredMaterials: ClothingMaterial[];
  styleTags: string[];
};
```

로그인 응답의 `user`는 `GET /api/users/me` 응답과 같은 `CurrentUserResponse` 타입을 사용한다.

현재 사용자 전용 응답 타입에는 `userId` 필드를 두지 않는다.

## 화면 구성
현재 첫 화면은 인증 상태에 따라 나뉜다.

### Logged-out View
- 로그인 form
- 회원가입 form
- API 연결 실패 상태

회원가입 화면에서는 위치 catalog를 호출하지 않는다. 신규 사용자는 서버에서 기본 위치 `SEOUL`과 빈 선호도 배열로 생성된다.

### Logged-in App Shell
- 앱 이름
- 현재 사용자 이름/email
- API 연결 상태
- 로그아웃 버튼

### Location Panel
- 현재 사용자 위치 표시
- 위치 keyword 입력
- 위치 catalog 목록
- 위치 선택 버튼
- 위치 변경 성공/실패 상태

`GET /api/locations`는 보호 API다. 이 API에서 `401`이 발생하면 위치 검색 실패가 아니라 인증 만료로 처리한다.

### Preferences Panel
- 선호 색상 multi-select 또는 checkbox group
- 선호 소재 multi-select 또는 checkbox group
- styleTags 입력/목록 표시
- 저장 버튼
- 저장 성공/실패 상태

`styleTags`는 현재 baseline에서 저장/조회/표시만 하며 추천 점수와 추천 이유에 반영하지 않는다.

### Closet Panel
- 활성 옷 목록
- 옷 등록 form
- category, color, material select
- min/max temperature number input
- rainSuitable checkbox
- 등록 성공 후 목록 자동 갱신
- 옷 상세 조회
- 옷 전체 수정
- 옷 보관 처리

### Recommendation Panel
- 추천 생성 버튼
- 추천 결과 weather snapshot
- outfit top/bottom/outer 표시
- score breakdown
- `preferenceScore` 표시
- 추천 이유 목록
- 착용 완료 버튼
- 추천 실패 코드와 메시지 표시

### Recommendation History Panel
- `GET /api/recommendations?limit=20` 기본 호출
- 최신순 추천 이력 목록
- limit 선택 또는 고정 입력
- worn 상태 표시

Limit 정책:

- 기본값 `20`
- 최소 `1`
- 최대 `50`
- 범위 밖 또는 숫자가 아닌 값은 `400 INVALID_REQUEST`

## 상태 관리
현재 baseline은 React state와 작은 custom hook만 사용한다.

권장 hook:

- `useAuthSession()`
- `useCurrentUser(accessToken)`
- `useUserLocation(accessToken)`
- `useLocations(accessToken, keyword)`
- `useUserPreferences(accessToken)`
- `useClothes(accessToken)`
- `useRecommendation(accessToken)`
- `useRecommendationHistory(accessToken, limit)`

서버 상태 캐시 라이브러리는 현재 baseline 필수 범위가 아니다.

## 에러 처리
프론트는 백엔드 실패 응답의 `code`와 `message`를 그대로 표시할 수 있어야 한다.

특히 아래 코드는 사용자가 이해할 수 있는 상태로 보여준다.

- `UNAUTHORIZED`
- `INVALID_TOKEN`
- `EMAIL_ALREADY_EXISTS`
- `LOCATION_NOT_FOUND`
- `USER_NOT_FOUND`
- `CLOTHING_NOT_FOUND`
- `RECOMMENDATION_NOT_FOUND`
- `NO_TOP_AVAILABLE`
- `NO_BOTTOM_AVAILABLE`
- `NO_WEATHER_SUITABLE_ITEM`
- `OUTER_REQUIRED_BUT_NOT_AVAILABLE`
- `INSUFFICIENT_CLOSET_ITEMS`
- `INTERNAL_SERVER_ERROR`

네트워크 실패나 JSON 파싱 실패는 프론트 공통 오류 상태로 처리한다.

## UX 기준
- 운영 도구처럼 조용하고 밀도 있는 화면으로 구성한다.
- 과도한 랜딩 페이지나 마케팅 hero를 만들지 않는다.
- 로그인 후 첫 화면에서 위치, 선호도, 옷장, 추천 작업을 수행할 수 있어야 한다.
- 버튼, 입력, select, checkbox는 native 접근성을 해치지 않는다.
- 모바일에서도 주요 패널이 세로로 자연스럽게 쌓여야 한다.

## Backend 계약

공개 API:

- `POST /api/auth/signup`
- `POST /api/auth/login`

보호 API:

- `GET /api/users/me`
- `GET /api/locations?keyword={keyword}`
- `GET /api/users/me/location`
- `PUT /api/users/me/location`
- `GET /api/users/me/preferences`
- `PUT /api/users/me/preferences`
- `GET /api/clothes`
- `POST /api/clothes`
- `GET /api/clothes/{clothingId}`
- `PUT /api/clothes/{clothingId}`
- `PATCH /api/clothes/{clothingId}/archive`
- `POST /api/recommendations`
- `GET /api/recommendations?limit={limit}`
- `PATCH /api/recommendations/{recommendationId}/worn`

프론트는 `userId` query parameter를 붙이지 않는다. 프론트 타입에도 현재 사용자 전용 response의 `userId` 필드를 두지 않는다.

프론트는 today 추천 GET 경로를 호출하지 않는다.

## 검증 기준
프론트 변경 후 아래 명령이 통과해야 한다.

```bash
cd frontend
npm install
npm run build
```

수동 확인:

1. 회원가입 후 자동 또는 수동 로그인할 수 있다.
2. 로그인 성공 후 access token이 `sessionStorage`에 저장된다.
3. 새로고침 후 `GET /api/users/me`로 로그인 상태가 복구된다.
4. 로그인 전에는 위치 catalog를 호출하지 않는다.
5. 로그인 후 현재 위치가 서울로 표시된다.
6. 위치 검색 후 부산 또는 제주를 선택할 수 있다.
7. 선호 색상/소재/styleTags를 저장하고 다시 조회할 수 있다.
8. styleTags를 바꿔도 추천 점수와 추천 이유가 변하지 않는다.
9. 새 옷을 등록하면 목록에 반영된다.
10. 추천을 생성하면 weather, outfit, score, reasons가 표시된다.
11. score에는 `preferenceScore`가 표시되고 기존 다양성 점수 필드는 표시되지 않는다.
12. 추천 이력을 최신순으로 확인할 수 있다.
13. 착용 완료 버튼을 누르면 worn 상태가 반영된다.
14. 로그아웃 시 `sessionStorage` token이 제거되고 로그인 화면으로 돌아간다.
