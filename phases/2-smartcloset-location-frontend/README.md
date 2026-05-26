# Phase: SmartCloset 2차 Location + Frontend MVP

> 상태: 완료된 과거 phase 문서다. 현재 구현 source of truth는 루트 `README.md`와 `docs/` 아래 현재 문서이며, 이 phase/step의 과거 API 또는 범위 표현이 현재 문서와 충돌하면 현재 문서를 우선한다. 완료 phase를 재실행할 때만 당시 step-local 기준으로 참고한다.

## 목표
SmartCloset 1.5차 KMA weather provider 기반 추천 흐름에 사용자별 위치 저장, 내장 대표 격자 catalog API, 사용자 위치 기반 KMA 요청, React+Vite+TypeScript 프론트엔드 앱, Docker Compose 프론트 공유 흐름을 추가한다.

## 작업 범위
- Must-have / 2차 P0: 사용자 위치 컬럼과 기본값 보정, 내장 KMA 대표 격자 catalog, 위치 조회/선택 API, `LOCATION_NOT_FOUND`, 사용자 위치 `nx`, `ny` 기반 KMA 요청, React+Vite+TypeScript SPA, Docker Compose `frontend` 서비스, README와 docs 동기화

## 제외 범위
- 로그인/회원가입
- Spring Security
- 외부 주소/지도 API
- 사용자 현재 위치 자동 감지
- 위경도-KMA 격자 변환 API
- Weather source DB 저장
- 추천 결과의 위치 snapshot 저장
- 날씨 Redis/DB 캐싱
- AI/GPT 추천
- 옷 이미지 업로드
- AWS 배포
- CD 자동화

## Steps
| Step | Name | Range |
| ---: | --- | --- |
| 0 | location-domain-and-schema | Must-have / 2차 P0 |
| 1 | location-api | Must-have / 2차 P0 |
| 2 | kma-user-grid-integration | Must-have / 2차 P0 |
| 3 | frontend-scaffold-and-compose | Must-have / 2차 P0 |
| 4 | frontend-core-flows | Must-have / 2차 P0 |
| 5 | sharing-verification-and-doc-sync | Must-have / 2차 P0 |

## 완료 기준
- seed user 기본 위치는 서울특별시 `SEOUL`, `nx=60`, `ny=127`이다.
- 기존 사용자 row에 위치가 없으면 위치 조회 또는 추천 생성 전에 애플리케이션에서 서울 기본값으로 backfill한다.
- `users.location_code`, `users.location_name`, `users.location_nx`, `users.location_ny`는 DB nullable 컬럼이며 DB default를 강제하지 않는다.
- 내장 catalog는 최소 `SEOUL`, `BUSAN`, `DAEGU`, `INCHEON`, `GWANGJU`, `DAEJEON`, `ULSAN`, `SEJONG`, `JEJU`를 제공한다.
- `GET /api/locations?keyword={keyword}`, `GET /api/users/location?userId={userId}`, `PUT /api/users/location?userId={userId}`가 문서 계약대로 동작한다.
- 존재하지 않는 `locationCode`는 `LOCATION_NOT_FOUND`와 `{ field, message }` 형태의 `details`로 실패한다.
- 추천 생성은 사용자 위치의 `locationNx`, `locationNy`로 KMA `getVilageFcst`를 호출한다.
- `KMA_NX`, `KMA_NY`는 2차 사용자별 추천의 source of truth가 아니다.
- `POST /api/recommendations?userId={userId}` 계약은 유지된다.
- today 추천 GET 계약은 생기지 않는다.
- React+Vite+TypeScript frontend가 `frontend/` 아래 생성되고 TypeScript `strict` build가 통과한다.
- Docker Compose는 `mysql`, `app`, `frontend` 서비스를 함께 실행한다.

## 검증 명령
```bash
git diff --check
! rg -n 'GET /api/recommendations/(today)' . --glob '!archive/**'
./gradlew test
./gradlew build
cd frontend && npm run build
docker compose config
test -f .env || cp .env.example .env
docker compose up --build
docker compose down
```

## 실행 예시
```bash
python3 scripts/execute.py 2-smartcloset-location-frontend --next-step-only
python3 scripts/execute.py 2-smartcloset-location-frontend
python3 scripts/autopilot.py 2-smartcloset-location-frontend --base main --max-review-fixes 2 --unsafe
```

## 리스크
- 위치 컬럼을 DB `NOT NULL`로 바로 추가하면 기존 row가 있는 `ddl-auto=update` 환경에서 실패할 수 있다.
- KMA client가 계속 전역 `KMA_NX`, `KMA_NY`를 사용하면 사용자별 추천이 동작하지 않는다.
- 프론트 앱과 Compose `frontend` 서비스를 다시 분리하면 README 공유 흐름과 실제 실행 구성이 어긋날 수 있다.
- 백엔드 실패 응답의 `details` shape와 프론트 타입이 다르면 에러 화면이 깨질 수 있다.

## 운영 메모
- 프론트와 Compose 서비스는 2차 P0 공유 흐름에 포함되어 있으므로 `docs/COMMANDS.md`의 frontend build와 Compose 검증 기준을 유지한다.
- KMA 외부 호출 문제가 생기면 `WEATHER_FALLBACK_ENABLED=true` 기본값과 `StaticWeatherProvider` fallback으로 추천 생성 데모를 유지한다.
