# MVP5 옷 이미지 QA 기록

## 문서 목적

이 문서는 SmartCloset MVP5 최종 공유 기준에서 옷 이미지 업로드, 교체, 삭제, 썸네일 표시, Docker Compose volume 유지 여부를 확인한 기록이다.

제품 범위와 API 계약은 `docs/PRD.md`, `docs/API.md`, `docs/FRONTEND.md`, `docs/DEMO_SCENARIO.md`, `docs/SHARING_GUIDE.md`를 우선한다.

## 메타정보

| 항목 | 값 |
| --- | --- |
| 확인일 | 2026-05-25 |
| 기준 브랜치 | `codex/5-smartcloset-clothing-images-step6-compose-docs-qa` |
| 기준 범위 | MVP5 Step 6 compose-docs-qa |
| 테스트 유형 | Docker Compose smoke + API 흐름 검증 + Headless Chrome 렌더링 확인 |

실제 API key, JWT, 비밀번호, private key는 이 문서에 기록하지 않는다.

## 실행 환경

| 항목 | 값 |
| --- | --- |
| 실행 방식 | Docker Compose |
| Frontend | `http://localhost:5173` |
| Backend | `http://localhost:8080` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| 이미지 저장 경로 | app container `/data/smartcloset/clothing-images` |
| 이미지 volume | `clothing-image-data` |

## 자동 검증

| 명령 | 결과 |
| --- | --- |
| `git diff --check` | PASS |
| `python3 -m compileall scripts` | PASS |
| `./gradlew test` | PASS |
| `./gradlew build` | PASS |
| `(cd frontend && npm run build)` | PASS |
| `docker compose config` | PASS |
| `docker compose down -v` | PASS |
| `test -f .env || cp .env.example .env` | PASS |
| `docker compose up --build -d` | PASS |
| `curl -fsS http://localhost:8080/v3/api-docs >/dev/null` | PASS |
| `curl -fsS http://localhost:5173 >/dev/null` | PASS |

## QA 계정과 데이터

| 항목 | 값 |
| --- | --- |
| QA 계정 | `mvp5-qa-20260525205823@example.com` |
| TOP | `MVP5 레드 상의` |
| BOTTOM | `MVP5 블루 하의` |
| OUTER | `MVP5 그레이 아우터` |

비밀번호와 access token은 기록하지 않는다.

## 확인 결과

| 단계 | 확인 항목 | 기대 결과 | 결과 | 메모 |
| --- | --- | --- | --- | --- |
| 1 | Compose volume 생성 | app service에 이미지 volume이 연결된다. | PASS | `clothing-image-data`가 `/data/smartcloset/clothing-images`에 mount됨. |
| 2 | 회원가입 | 신규 사용자가 생성되고 보호 API token을 받는다. | PASS | QA 계정으로 확인. |
| 3 | 옷 등록 | TOP, BOTTOM, OUTER를 JSON API로 등록한다. | PASS | 기존 `POST /api/clothes` 계약 유지. |
| 4 | 이미지 업로드 | `PUT /api/clothes/{id}/image`가 image metadata를 반환한다. | PASS | TOP 이미지 업로드 성공. |
| 5 | 이미지 교체 | 같은 API로 교체 후 기존 옷 정보는 유지된다. | PASS | TOP 이미지 metadata 유지, bytes 조회 `200 image/png`. |
| 6 | 이미지 삭제 | `DELETE /api/clothes/{id}/image` 후 `image=null`이다. | PASS | BOTTOM 삭제 성공. |
| 7 | 이미지 삭제 idempotency | 이미지가 없어도 삭제 API가 성공한다. | PASS | BOTTOM 두 번째 삭제도 `image=null`. |
| 8 | 추천 생성 | 이미지 metadata가 outfit item에 포함된다. | PASS | TOP image metadata 포함, BOTTOM은 `null`. |
| 9 | 추천 이력 | 이력 outfit item도 최신 image metadata를 반환한다. | PASS | TOP image metadata 포함, BOTTOM은 `null`. |
| 10 | 옷장 썸네일 렌더링 | 이미지가 있는 옷은 인증 blob 썸네일로 표시된다. | PASS | Headless Chrome에서 Closet `blob:` 이미지 1개 확인. |
| 11 | 추천 결과 썸네일 렌더링 | Today 추천 결과에서 썸네일과 fallback이 함께 표시된다. | PASS | Today `blob:` 이미지 1개 확인. |
| 12 | 이력 썸네일 렌더링 | History 추천 이력에서 썸네일이 표시된다. | PASS | History `blob:` 이미지 2개 확인. |
| 13 | app 재시작 후 이미지 유지 | app container 재시작 후 기존 이미지가 조회된다. | PASS | `docker compose restart app` 후 `GET /api/clothes/{id}/image`가 `200 image/png` 반환. |

## 최종 판정

PASS. MVP5 Step 6 공유 기준과 수동 QA 기준을 충족했다.
