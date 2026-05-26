# Demo Scenario: SmartCloset MVP6

## 데모 목표

Docker Compose로 SmartCloset 백엔드, MySQL, React 프론트엔드를 실행한 뒤 브라우저에서 옷별 styleTags, 상황 선택 추천, 착용 완료, 추천 피드백 저장/clear, 추천 이력 상태 표시를 확인한다.

MVP6 데모의 핵심은 사용자가 추천 후 실제 경험을 남기고 다음 추천 개인화에 반영되는 흐름이다.

## MVP6 데모 범위

포함:

- 회원가입 또는 로그인
- 기본 옷 프리셋과 옷 이미지 확인
- 옷 등록/수정에서 styleTags 저장
- Today view 상황 선택
- 추천 생성
- 추천 결과의 상황, 이유, 점수 확인
- 착용 완료
- 마음에 들어요, 별로예요, 추웠어요, 더웠어요 피드백 저장
- 피드백 clear
- History view에서 상황, 착용 여부, 착용 시각, 피드백 확인

제외:

- AI/GPT 추천
- AI 자동 태깅
- 외부 지도/주소 API
- 피드백 analytics dashboard
- AWS 배포

## 데모 전제

- `.env`는 `.env.example`을 복사해 만든다.
- `KMA_SERVICE_KEY`가 없어도 `WEATHER_FALLBACK_ENABLED=true`이면 fallback weather로 데모 가능하다.
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

## React 앱 MVP6 데모 시나리오

### 1. 회원가입 또는 로그인

1. Frontend에 접속한다.
2. 새 계정으로 회원가입하거나 기존 계정으로 로그인한다.
3. 로그인 후 Today view가 보이는지 확인한다.
4. access token이 `sessionStorage`에 저장되는지 확인한다.

기대 결과:

- 기본 위치가 표시된다.
- 기본 옷 프리셋이 준비된다.
- Today, Closet, Preferences, Location, History view를 이동할 수 있다.

### 2. 옷별 styleTags 확인과 수정

1. Closet view로 이동한다.
2. 기본 프리셋 옷의 styleTags chip이 보이는지 확인한다.
3. 새 옷을 등록하며 `OFFICE`, `MINIMAL`, `출근` tag를 추가한다.
4. 등록된 옷을 수정해 styleTags를 변경한다.

기대 결과:

- `ClothingResponse.styleTags`가 항상 배열로 표시된다.
- blank tag는 저장되지 않는다.
- 옷 이미지 업로드/교체/삭제 흐름은 기존처럼 동작한다.

### 3. 상황 선택 추천 생성

1. Today view로 이동한다.
2. 상황을 `출근`으로 선택한다.
3. 추천 만들기를 누른다.

기대 결과:

- `POST /api/recommendations`에 `situation=WORK`가 반영된다.
- 추천 결과에 `출근` 상황이 표시된다.
- 추천 이유에 상황 또는 styleTags 반영 문구가 포함될 수 있다.

### 4. 착용 완료

1. 추천 결과에서 착용 완료하기를 누른다.
2. 같은 추천에서 다시 착용 완료 버튼 상태를 확인한다.

기대 결과:

- 착용 완료 상태가 표시된다.
- `wornAt`이 표시된다.
- 중복 착용 이력이 생기지 않는다.

### 5. 추천 피드백 저장

1. 추천 결과에서 마음에 들어요를 선택한다.
2. 추웠어요를 함께 선택한다.
3. 저장 상태가 반영되는지 확인한다.

기대 결과:

- `PUT /api/recommendations/{recommendationId}/feedback`가 호출된다.
- 응답의 `feedback.sentiment=LIKED`, `feedback.thermal=TOO_COLD`가 UI에 반영된다.
- `feedback.updatedAt`이 표시되거나 상태 갱신에 사용된다.

### 6. 추천 피드백 전체 교체와 clear

1. 마음에 들어요 대신 별로예요를 선택한다.
2. 추웠어요를 해제하거나 더웠어요로 바꾼다.
3. 피드백 지우기를 누른다.

기대 결과:

- PUT은 전체 상태를 교체한다.
- 누락 필드 또는 명시적 `null`은 해당 필드를 clear한다.
- 양쪽 `null`이면 `feedback=null` 상태가 된다.

### 7. 최근 피드백 반영 확인

1. 같은 상황 또는 다른 상황으로 추천을 다시 생성한다.
2. 점수 상세에서 `preferenceScore`가 표시되는지 확인한다.
3. 이유 목록에서 최근 피드백 반영 또는 회피 문구가 나오는지 확인한다.

기대 결과:

- 최근 14일 피드백이 추천 점수와 이유에 반영된다.
- 이미지 존재 여부는 점수와 이유를 바꾸지 않는다.

### 8. 추천 이력 확인

1. History view로 이동한다.
2. 최근 추천 카드들을 확인한다.

기대 결과:

- 추천별 상황이 표시된다.
- 착용 전/착용 완료와 `wornAt`을 확인할 수 있다.
- 피드백 없음 또는 저장된 피드백을 확인할 수 있다.
- 추천 outfit thumbnail 또는 fallback이 표시된다.

## API 실패 케이스 확인

- 인증 없이 feedback PUT: `401 UNAUTHORIZED`
- 다른 사용자 추천 feedback PUT: `404 RECOMMENDATION_NOT_FOUND`
- 잘못된 situation enum: `400 INVALID_REQUEST`
- 잘못된 feedback enum: `400 INVALID_REQUEST`
- body 없는 `POST /api/recommendations`: `201 Created`, `situation=CASUAL`
- `{}` feedback PUT: `200 OK`, `feedback=null`

## 완료 기준

- 상황 선택 추천이 가능하다.
- 옷별 styleTags가 저장/표시된다.
- 착용 완료와 피드백 저장/clear가 가능하다.
- 추천 이력에서 상황, 착용 여부, 착용 시각, 피드백이 보인다.
- 기존 이미지 업로드와 썸네일 표시가 유지된다.
- Docker Compose 환경에서 앱이 정상 실행된다.
