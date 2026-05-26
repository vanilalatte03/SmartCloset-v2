# Demo Scenario: SmartCloset MVP7

## 데모 목표

Docker Compose로 SmartCloset 백엔드, MySQL, React 프론트엔드를 실행한 뒤 브라우저에서 동네 단위 위치 검색, 현재 위치 후보 찾기, 예보 시간대 선택, 추천 결과와 이력의 위치/날씨 source 표시를 확인한다.

MVP7 데모의 핵심은 사용자가 추천에 사용된 위치와 날씨 기준을 신뢰할 수 있는지 확인하는 것이다.

## MVP7 데모 범위

포함:

- 회원가입 또는 로그인
- KMA 행정구역 catalog 검색
- `일산동` 같은 동명이인 후보 확인
- 브라우저 현재 위치 권한 허용 또는 거부 흐름
- 좌표 resolve 후보 선택 후 위치 저장
- Today view 예보 시간대 선택
- 추천 생성
- 추천 결과의 위치, KMA/fallback 여부, base/forecast 시각 확인
- 착용 완료와 추천 피드백 저장/clear 유지
- History view에서 과거 추천의 위치/날씨 source snapshot 확인

제외:

- 외부 지도/주소 API
- 지도 화면 렌더링
- raw KMA 응답 JSON 저장/표시
- AI/GPT 추천
- AI 자동 태깅
- AWS 배포

## 데모 전제

- `.env`는 `.env.example`을 복사해 만든다.
- `KMA_SERVICE_KEY`가 없어도 `WEATHER_FALLBACK_ENABLED=true`이면 fallback weather로 데모 가능하다.
- KMA key가 없으면 source 표시에서 fallback 사용 여부를 확인한다.
- Docker Compose reset 시 DB와 이미지 volume이 초기화된다.

## 실행

```bash
docker compose down -v
test -f .env || cp .env.example .env
docker compose up --build
```

Frontend:

```text
http://localhost:5173
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

## React 앱 MVP7 데모 시나리오

### 1. 회원가입 또는 로그인

1. Frontend에 접속한다.
2. 새 계정으로 회원가입하거나 기존 계정으로 로그인한다.
3. 로그인 후 Today view가 보이는지 확인한다.
4. access token이 `sessionStorage`에 저장되는지 확인한다.

기대 결과:

- 기본 위치가 표시된다.
- 기본 옷 프리셋이 준비된다.
- Today, Closet, Preferences, Location, History view를 이동할 수 있다.

### 2. 동네 단위 위치 검색

1. Location view로 이동한다.
2. 검색창에 `일산동`을 입력한다.
3. 후보 목록을 확인한다.

기대 결과:

- KMA catalog 후보가 여러 개 표시될 수 있다.
- 각 후보는 `fullName`, `nx/ny`로 구분된다.
- 외부 지도나 주소 API 호출 없이 결과가 나온다.

### 3. 위치 저장

1. 후보 중 하나를 선택한다.
2. 위치 저장을 실행한다.

기대 결과:

- `PUT /api/users/me/location`이 호출된다.
- 저장된 위치 source는 `MANUAL_SEARCH`다.
- Today의 위치 표시가 선택한 동네로 갱신된다.

### 4. 현재 위치로 후보 찾기

1. Location view에서 현재 위치로 찾기를 누른다.
2. 브라우저 권한을 허용하거나 거부한다.

기대 결과:

- 허용 시 `POST /api/locations/resolve`가 호출되고 가까운 후보가 표시된다.
- 후보는 자동 저장되지 않는다.
- 후보 저장 시 source는 `BROWSER_GEOLOCATION`이다.
- 거부 시 수동 검색으로 돌아갈 수 있다.

### 5. 예보 시간대 선택 추천

1. Today view로 이동한다.
2. 상황을 `출근`으로 선택한다.
3. 예보 시간대를 `오후`로 선택한다.
4. 추천 만들기를 누른다.

기대 결과:

- `POST /api/recommendations`에 `situation=WORK`, `forecastPeriod=AFTERNOON`이 반영된다.
- 추천 결과에 출근 상황과 오후 예보 기준이 표시된다.
- 추천 이유와 점수는 기존 규칙 기반 구조를 유지한다.

### 6. 위치/날씨 source 확인

1. 추천 결과의 weather/source 영역을 확인한다.
2. KMA key가 있으면 KMA 사용 표시를 확인한다.
3. KMA key가 없거나 실패 조건이면 fallback 표시를 확인한다.

기대 결과:

- 위치 fullName 또는 name이 표시된다.
- KMA grid `nx/ny`가 표시된다.
- KMA 사용 여부와 fallback 여부가 표시된다.
- base date/time과 forecast date/time이 표시된다.

### 7. 착용 완료와 피드백 유지

1. 추천 결과에서 착용 완료하기를 누른다.
2. 마음에 들어요 또는 추웠어요 피드백을 저장한다.
3. 피드백을 clear한다.

기대 결과:

- MVP6 착용 완료와 피드백 흐름이 유지된다.
- source snapshot 표시는 피드백 저장/clear와 무관하게 유지된다.

### 8. 추천 이력 snapshot 확인

1. History view로 이동한다.
2. 최근 추천 카드들을 확인한다.
3. Location view에서 위치를 다른 후보로 바꾼 뒤 History를 다시 확인한다.

기대 결과:

- 각 추천에 생성 당시 위치/날씨 source snapshot이 표시된다.
- 사용자 현재 위치가 바뀌어도 과거 추천 카드의 snapshot은 바뀌지 않는다.

## API 실패 케이스 확인

- 인증 없이 `POST /api/locations/resolve`: `401 UNAUTHORIZED`
- 잘못된 latitude/longitude: `400 INVALID_REQUEST`
- 존재하지 않는 locationCode 저장: `400 INVALID_REQUEST`
- 잘못된 forecastPeriod enum: `400 INVALID_REQUEST`
- body 없는 `POST /api/recommendations`: `201 Created`, `situation=CASUAL`, `forecastPeriod=CURRENT`
- 인증 없이 feedback PUT: `401 UNAUTHORIZED`
- `{}` feedback PUT: `200 OK`, `feedback=null`

## 완료 기준

- 동네 단위 위치 검색과 저장이 가능하다.
- 브라우저 현재 위치로 후보를 찾고 선택 저장할 수 있다.
- 오전/오후/저녁 예보 기준을 선택해 추천을 만들 수 있다.
- 추천 결과와 추천 이력에서 위치, KMA/fallback 여부, base/forecast 시각을 확인할 수 있다.
- MVP6 피드백/개인화와 MVP5 이미지 업로드/썸네일이 유지된다.
- Docker Compose 환경에서 앱이 정상 실행된다.
