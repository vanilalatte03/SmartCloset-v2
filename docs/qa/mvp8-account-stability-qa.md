# MVP8 계정 안정성 QA 기록

이 문서는 SmartCloset MVP8 최종 공유 기준에서 계정 안정성 API, 기존 MVP5/MVP6/MVP7 핵심 흐름, Docker Compose smoke, 문서 동기화 검증 여부를 확인한 기록이다.

## 실행 환경

| 항목 | 값 |
| --- | --- |
| 기준 범위 | MVP8 Step 7 compose-docs-qa |
| 실행일 | 2026-05-27 |
| 실행 방식 | 로컬 자동 검증, Docker Compose smoke, API smoke |
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
| `python3 scripts/checks.py --docs-check-config phases/8-smartcloset-account-stability/docs-checks.json --docs-check` | PASS |

## Docker Compose Smoke

| 확인 | 결과 | 기록 |
| --- | --- | --- |
| `docker compose down -v` | PASS | 기존 compose container, network, volume 정리 완료 |
| `test -f .env || cp .env.example .env` | PASS | 기존 `.env` 사용 |
| `docker compose up --build -d` | PASS | app image build, mysql healthy, app/frontend container 기동 완료 |
| `curl -fsS http://localhost:8080/v3/api-docs >/dev/null` | PASS | app 기동 직후 connection reset이 1회 있었고 readiness 후 재시도 성공 |
| `curl -fsS http://localhost:5173 >/dev/null` | PASS | frontend 응답 확인 |
| `docker compose down` | PASS | smoke 종료 후 container/network 정리 완료 |

## API Smoke

Docker Compose 환경에서 QA용 임시 계정을 생성해 아래 항목을 확인했다. 민감정보, action token 원문, JWT, refresh cookie 값은 기록하지 않는다.

| 시나리오 | 결과 | 확인 내용 |
| --- | --- | --- |
| 회원가입 | PASS | `POST /api/auth/signup`이 `emailVerificationRequired=true`를 반환하고 access token을 반환하지 않음 |
| 미인증 로그인 차단 | PASS | 이메일 인증 전 login이 `403 EMAIL_VERIFICATION_REQUIRED`로 실패 |
| 이메일 인증 | PASS | `ConsoleEmailSender` local outbox token으로 confirm 성공, 같은 token 재사용은 `400 ACCOUNT_TOKEN_INVALID` |
| 로그인과 refresh | PASS | 인증 후 login이 access token과 HttpOnly refresh cookie를 발급, JSON body에는 refresh token 없음 |
| refresh rotation | PASS | `POST /api/auth/refresh`가 새 access token을 반환하고 refresh token 값을 JSON에 노출하지 않음 |
| 비밀번호 재설정 | PASS | reset request는 중립 성공 응답, confirm 성공 후 이전 refresh session은 `401 INVALID_TOKEN` |
| Google provider status | PASS | local 기본 설정에서 `google.enabled=false`, `loginUrl=null` |
| 현재 사용자 조회 | PASS | `GET /api/users/me`가 이메일 인증 상태, password login 가능 상태를 반환 |
| 위치 검색 | PASS | `GET /api/locations?keyword=일산동`이 KMA catalog 후보를 반환 |
| 현재 날씨 | PASS | `GET /api/weather/current`가 위치 snapshot과 weather source metadata를 반환 |
| 옷 이미지 | PASS | 옷 등록, `PUT /api/clothes/{id}/image`, 이미지 bytes 조회, 이미지 삭제 확인 |
| 추천 생성/이력 | PASS | `POST /api/recommendations`가 `forecastPeriod=CURRENT`와 위치/날씨 source snapshot을 포함한 결과를 저장하고, 이력 조회로 확인 |
| 계정 삭제 | PASS | `DELETE /api/users/me` 성공 후 기존 access token으로 사용자 조회 불가, 삭제 후 refresh도 `401` |

브라우저 수동 QA는 별도로 수행하지 않았고, Step 7의 "가능한 범위의 API smoke" 기준으로 계정 안정성과 기존 핵심 기능을 확인했다.

## 아키텍처 체크

| 항목 | 결과 | 확인 내용 |
| --- | --- | --- |
| MVP8 포함/제외 범위 | PASS | refresh, 이메일 인증, reset, Google provider status, 계정 삭제는 구현/문서화되어 있고 AWS/S3/SES/Redis/admin/soft delete는 MVP8 구현에 포함되지 않음 |
| 기존 MVP 기능 유지 | PASS | 위치 검색, 현재 날씨, 이미지 업로드/조회/삭제, 추천 생성/이력 source snapshot API smoke 통과 |
| token 원문 저장/노출 방지 | PASS | API 응답에 refresh token이 없고, action token은 local outbox로만 확인해 confirm에 사용. DB 저장 원문은 smoke에서 직접 노출하지 않음 |
| AWS-ready 경계 | PASS | `ConsoleEmailSender`, `ClothingImageStorage`, cookie/CORS/OAuth env 경계 문서 기준 유지 |
| 문서 회귀 신호 | PASS | docs-check가 공개 `userId` query, today 추천 GET, refresh token JSON 노출, MVP8 제외 구현 표현을 검사하고 통과 |

## 결론

PASS. 로컬 자동 검증, Docker Compose smoke, API smoke, docs-check를 통과했다. UI 클릭 기반 수동 QA는 수행하지 않았으며, 계정 안정성 핵심 흐름은 API smoke로 대체 확인했다.
