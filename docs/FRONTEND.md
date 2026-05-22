# Frontend: SmartCloset 2차 MVP

## 목표
2차 프론트엔드는 Swagger나 Spring static Demo UI 없이도 사용자가 주요 제품 흐름을 확인할 수 있는 React+Vite+TypeScript SPA다.

`frontend/` 디렉터리는 React+Vite+TypeScript SPA이며 Docker Compose `frontend` 서비스와 함께 유지한다.

주요 흐름:

- 사용자 위치 확인
- 내장 위치 catalog 검색과 선택
- 옷 목록 조회
- 옷 등록
- 추천 생성
- 추천 결과 확인
- 추천 결과 착용 완료 처리

## 기술 기준
- React
- Vite
- TypeScript
- TypeScript `strict` 사용
- CSS는 기본 CSS module 또는 단일 앱 CSS로 시작한다.
- 대형 상태 관리 라이브러리는 사용하지 않는다.
- React Router는 2차에서 필수로 두지 않는다. 단일 화면 대시보드로 시작한다.

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
    │   ├── clothes
    │   ├── location
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

export type LocationOptionResponse = {
  code: string;
  name: string;
  nx: number;
  ny: number;
};

export type UserLocationResponse = {
  userId: number;
  code: string;
  name: string;
  nx: number;
  ny: number;
  updatedAt: string;
};
```

## 화면 구성
2차 첫 화면은 단일 작업 화면으로 구성한다.

### Header
- 앱 이름
- 현재 `userId`
- API 연결 상태

### Location Panel
- 현재 사용자 위치 표시
- 위치 keyword 입력
- 위치 catalog 목록
- 위치 선택 버튼
- 위치 변경 성공/실패 상태

### Closet Panel
- 활성 옷 목록
- 옷 등록 form
- category, color, material select
- min/max temperature number input
- rainSuitable checkbox
- 등록 성공 후 목록 자동 갱신

### Recommendation Panel
- 추천 생성 버튼
- 추천 결과 weather snapshot
- outfit top/bottom/outer 표시
- score breakdown
- 추천 이유 목록
- 착용 완료 버튼
- 추천 실패 코드와 메시지 표시

## 상태 관리
2차는 React state와 작은 custom hook만 사용한다.

권장 hook:

- `useUserLocation(userId)`
- `useLocations(keyword)`
- `useClothes(userId)`
- `useRecommendation(userId)`

서버 상태 캐시 라이브러리는 2차 필수 범위가 아니다.

## 에러 처리
프론트는 백엔드 실패 응답의 `code`와 `message`를 그대로 표시할 수 있어야 한다.

특히 아래 코드는 사용자가 이해할 수 있는 상태로 보여준다.

- `LOCATION_NOT_FOUND`
- `USER_NOT_FOUND`
- `CLOTHING_NOT_FOUND`
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
- 첫 화면에서 바로 위치, 옷장, 추천 작업을 수행할 수 있어야 한다.
- 버튼, 입력, select, checkbox는 native 접근성을 해치지 않는다.
- 모바일에서도 세 패널이 세로로 자연스럽게 쌓여야 한다.

## Backend 계약
프론트가 호출하는 2차 필수 API:

- `GET /api/locations?keyword={keyword}`
- `GET /api/users/location?userId={userId}`
- `PUT /api/users/location?userId={userId}`
- `GET /api/clothes?userId={userId}`
- `POST /api/clothes?userId={userId}`
- `POST /api/recommendations?userId={userId}`
- `PATCH /api/recommendations/{recommendationId}/worn?userId={userId}`

프론트는 today 추천 GET 경로를 호출하지 않는다.

## 검증 기준
프론트 구현 후 아래 명령이 통과해야 한다.

```bash
cd frontend
npm install
npm run build
```

권장 scripts:

```json
{
  "scripts": {
    "dev": "vite --host 0.0.0.0",
    "build": "tsc -b && vite build",
    "preview": "vite preview --host 0.0.0.0"
  }
}
```

수동 확인:

1. `userId=1`의 현재 위치가 서울로 표시된다.
2. 위치 검색 후 부산 또는 제주를 선택할 수 있다.
3. 옷 목록이 표시된다.
4. 새 옷을 등록하면 목록에 반영된다.
5. 추천을 생성하면 weather, outfit, score, reasons가 표시된다.
6. 착용 완료 버튼을 누르면 worn 상태가 반영된다.
7. API 실패 또는 추천 실패 코드가 화면에 표시된다.
