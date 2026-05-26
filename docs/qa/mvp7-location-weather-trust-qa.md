# MVP7 위치/날씨 신뢰도 QA 기록

이 문서는 SmartCloset MVP7 최종 공유 기준에서 KMA catalog 위치 검색, 브라우저 현재 위치 후보, 예보 시간대 선택, 위치/날씨 source snapshot, Docker Compose smoke 여부를 확인한 기록이다.

## 실행 환경

| 항목 | 값 |
| --- | --- |
| 기준 범위 | MVP7 Step 6 compose-docs-qa |
| 실행일 | 2026-05-27 |
| 실행 방식 | 로컬 자동 검증, Docker Compose smoke, 브라우저 QA |
| Frontend | `http://localhost:5173` |
| Backend OpenAPI | `http://localhost:8080/v3/api-docs` |

## 자동 검증

| 명령 | 결과 |
| --- | --- |
| `python3 -m compileall scripts` | PASS |
| `git diff --check` | PASS |
| `./gradlew test` | PASS |
| `./gradlew build` | PASS |
| `(cd frontend && npm run build)` | PASS |
| `docker compose config --quiet` | PASS |
| `python3 scripts/checks.py --docs-check-config phases/7-smartcloset-location-weather-trust/docs-checks.json --docs-check` | PASS |

## Docker Compose Smoke

| 확인 | 결과 | 기록 |
| --- | --- | --- |
| `docker compose down -v` | PASS | 기존 compose container, network, volume 정리 완료 |
| `test -f .env || cp .env.example .env` | PASS | 기존 `.env` 사용 |
| `docker compose up --build -d` | PASS | app image build, mysql healthy, app/frontend container 기동 완료 |
| `curl -fsS http://localhost:8080/v3/api-docs >/dev/null` | PASS | app 기동 직후 connection reset 재시도 후 성공 |
| `curl -fsS http://localhost:5173 >/dev/null` | PASS | frontend 응답 확인 |
| `docker compose down` | PASS | smoke 종료 후 container/network 정리 완료 |

## Browser QA

Docker Compose 환경에서 QA용 임시 계정으로 로그인해 아래 항목을 확인했다. 민감정보와 JWT는 기록하지 않는다.

| 시나리오 | 결과 | 확인 내용 |
| --- | --- | --- |
| 동네 검색 | PASS | Location view에서 `일산동` 검색 시 18개 후보와 `KMA_4128751000` 일산1동 후보 확인 |
| 위치 저장 | PASS | 일산1동을 선택해 현재 위치가 `KMA_4128751000`, `nx=56`, `ny=129`, `직접 선택`으로 갱신됨 |
| 현재 위치 후보 | PARTIAL | in-app browser가 Geolocation 권한을 거부해 권한 거부 안내를 확인했다. 같은 좌표 resolve API는 `nx=56`, `ny=129`, nearest `KMA_4128751000`, 후보 5개를 반환했다. |
| 예보 시간대 선택 | PASS | Today view에서 `저녁` forecastPeriod 선택 후 추천 생성, 추천 결과에 `저녁` 표시 |
| KMA/fallback/base/forecast 표시 | PASS | Today/Location/Recommendation/History에서 위치, KMA 격자, 위치 source, 날씨 source, KMA 사용 여부, fallback 여부, 발표 기준, 예보 대상 표시 확인 |
| History snapshot | PASS | History detail에서 추천 생성 당시 일산1동 위치 snapshot, `저녁` forecastPeriod, KMA/fallback/base/forecast metadata 유지 확인 |

## 결론

PASS with note. 로컬 자동 검증, Docker Compose smoke, 주요 브라우저 QA를 통과했다. in-app browser의 Geolocation 권한 거부로 현재 위치 후보 UI의 성공 목록은 직접 확인하지 못했고, 해당 경로는 resolve API 응답과 브라우저 권한 거부 fallback으로 보완 확인했다.
