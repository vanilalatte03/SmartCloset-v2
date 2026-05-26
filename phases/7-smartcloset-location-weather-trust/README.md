# Phase: SmartCloset 7차 Location Weather Trust MVP

## 목표

MVP6 추천 피드백/개인화 완료 baseline 위에 KMA 행정구역 catalog, 브라우저 좌표 resolve, 예보 시간대 선택, 위치/날씨 source snapshot 저장과 표시를 추가한다.

## 작업 범위

- Must-have / MVP7 P0: MVP6 archive, MVP7 docs/ADR/agent 전환, KMA 읍/면/동 catalog, 위경도 -> KMA grid 변환, 위치 후보 resolve API, 위치 저장 source, weather source metadata, 추천 forecastPeriod, 추천 결과 위치/날씨 snapshot, Today/Location/History UX, Docker Compose 공유 검증
- Should-have / MVP7 P1: source 표시 문구 polish, 긴 위치명 모바일 wrap polish, fallback 안내 문구 polish
- MVP7 제외: 외부 지도/주소 API, 지도 렌더링, raw KMA 응답 JSON 저장, GPS 좌표 원문 DB 저장, KMA `getVilageFcst` 외 weather API, AI/GPT 추천, AI 자동 태깅, Redis, AWS/CD 자동화

## Steps

| Step | Name | Range |
| ---: | --- | --- |
| 0 | mvp7-scope-docs | Must-have / MVP7 P0 |
| 1 | kma-location-catalog | Must-have / MVP7 P0 |
| 2 | geolocation-resolve-api | Must-have / MVP7 P0 |
| 3 | weather-source-snapshot | Must-have / MVP7 P0 |
| 4 | recommendation-weather-snapshot | Must-have / MVP7 P0 |
| 5 | frontend-location-weather-trust-ux | Must-have / MVP7 P0 |
| 6 | compose-docs-qa | Must-have / MVP7 P0 |

## 단계 진행 원칙

- Step 0은 문서 전환, archive, ADR, phase 정의만 다룬다.
- Step 1은 KMA 행정구역 catalog resource, `LocationOption` 확장, 검색 API 응답 확장만 다룬다. 브라우저 좌표 resolve는 추가하지 않는다.
- Step 2는 위경도 -> KMA grid 변환, `POST /api/locations/resolve`, 위치 저장 source만 다룬다. weather provider는 바꾸지 않는다.
- Step 3은 `WeatherProvider`가 condition과 source metadata를 반환하도록 weather 계약을 확장한다. 추천 DB 저장은 Step 4에서 한다.
- Step 4는 추천 생성/이력의 `forecastPeriod`와 위치/날씨 source snapshot 저장/응답만 다룬다.
- Step 5는 frontend API type/client와 Location/Today/History UX를 다룬다.
- Step 6은 문서 동기화, Docker Compose, QA 기록, 최종 검증을 수행한다.

## 완료 기준

- `GET /api/locations?keyword=일산동`이 읍/면/동 후보를 반환한다.
- `POST /api/locations/resolve`가 좌표를 KMA grid와 가까운 위치 후보로 변환한다.
- 잘못된 좌표는 `400 INVALID_REQUEST`다.
- `PUT /api/users/me/location`이 `MANUAL_SEARCH` 또는 `BROWSER_GEOLOCATION` source를 저장한다.
- `GET /api/weather/current`가 location/source metadata를 반환한다.
- `POST /api/recommendations`가 optional `forecastPeriod`를 받고 기본값 `CURRENT`를 사용한다.
- 추천 결과와 추천 이력에 위치/날씨 source snapshot이 포함된다.
- KMA 사용 여부, fallback 여부, base date/time, forecast date/time을 확인할 수 있다.
- 현재 사용자 위치 변경 후에도 과거 추천 snapshot은 바뀌지 않는다.
- 기존 MVP6 피드백/개인화와 MVP5 이미지 업로드/썸네일 기능이 유지된다.
- 공개 `userId` query parameter와 today 추천 GET endpoint가 추가되지 않는다.

## 검증 명령

```bash
git diff --check
./gradlew test
./gradlew build
(cd frontend && npm run build)
docker compose config --quiet
```

최종 step에서는 아래를 추가로 실행한다.

```bash
python3 scripts/checks.py --docs-check-config phases/7-smartcloset-location-weather-trust/docs-checks.json --docs-check
docker compose down -v
test -f .env || cp .env.example .env
docker compose up --build -d
curl -fsS http://localhost:8080/v3/api-docs >/dev/null
curl -fsS http://localhost:5173 >/dev/null
docker compose down
```

## 실행 예시

```bash
python3 scripts/execute.py 7-smartcloset-location-weather-trust --next-step-only
python3 scripts/execute.py 7-smartcloset-location-weather-trust
python3 scripts/autopilot.py 7-smartcloset-location-weather-trust --base main --max-review-fixes 2 --unsafe
```
