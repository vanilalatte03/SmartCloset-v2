# MVP6 추천 피드백/개인화 QA 기록

이 문서는 SmartCloset MVP6 최종 공유 기준에서 추천 상황, 옷별 `styleTags`, 추천 피드백 저장/clear, 추천 이력 상태 표시, 이미지 업로드/썸네일 유지 여부를 확인한 기록이다.

## 실행 환경

| 항목 | 값 |
| --- | --- |
| 기준 범위 | MVP6 Step 6 compose-docs-qa |
| 실행일 | 2026-05-26 |
| 실행 방식 | Docker Compose smoke, Playwright system Chrome browser QA |
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
| `python3 scripts/checks.py --docs-check-config phases/6-smartcloset-feedback-personalization/docs-checks.json --docs-check` | PASS |

## Docker Compose Smoke

| 확인 | 결과 |
| --- | --- |
| `docker compose down -v` | PASS |
| `test -f .env || cp .env.example .env` | PASS |
| `docker compose up --build -d` | PASS |
| `curl -fsS http://localhost:8080/v3/api-docs >/dev/null` | PASS |
| `curl -fsS http://localhost:5173 >/dev/null` | PASS |

## Browser QA

인앱 Browser/Chrome extension 연결은 현재 세션에서 사용할 수 없어, 임시 Playwright 스크립트와 system Chrome 채널로 로컬 앱을 확인했다. 아래 항목은 실제 브라우저 자동화로 확인한 항목만 기록한다.

| 시나리오 | 결과 | 확인 내용 |
| --- | --- | --- |
| 회원가입/로그인 | PASS | 새 QA 계정 회원가입 후 로그인, Today 화면 진입 |
| 옷 styleTags 저장 | PASS | Closet에서 `OFFICE`, `MINIMAL` tag 추가 후 옷 등록 |
| 이미지 업로드/썸네일 | PASS | PNG 이미지 업로드 후 옷 카드 썸네일 표시 |
| 상황 선택 추천 | PASS | Today에서 `출근` 선택 후 추천 생성, 결과 상황 표시 |
| 착용 완료 | PASS | 추천 결과에서 착용 완료 저장과 상태 표시 |
| 피드백 저장 | PASS | `마음에 들어요`, `추웠어요` 저장 상태 확인 |
| 피드백 clear | PASS | 피드백 지우기 후 clear 상태 확인 |
| 피드백 재저장 | PASS | `별로예요`, `더웠어요` 재저장 상태 확인 |
| History 상태 표시 | PASS | 이력에서 상황, 착용 완료, 피드백 상태 표시 확인 |

## 결론

PASS. MVP6 Step 6 공유 기준과 수동 QA 기준을 충족했다.
