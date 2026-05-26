# Phase: SmartCloset 6차 Feedback Personalization MVP

## 목표

MVP5 옷 이미지 업로드 완료 baseline 위에 추천 상황 선택, 추천 피드백 저장, 최근 피드백 기반 개인화, 옷별 `styleTags` 점수 반영, 추천 이력의 착용/피드백 표시를 추가한다.

## 작업 범위

- Must-have / MVP6 P0: MVP5 archive, MVP6 docs/ADR/agent 전환, 옷별 styleTags API/DB, 추천 상황 request/snapshot, 추천 피드백 PUT API, 최근 피드백과 styleTags 기반 `preferenceScore`, 추천 이유 보강, Today/Closet/History UX, Docker Compose 공유 검증
- Should-have / MVP6 P1: 피드백 저장 문구 polish, History 카드 compact layout polish, 모바일 버튼 wrap polish
- MVP6 제외: AI/GPT 추천, AI 자동 태깅, feedback event log analytics, preference normalization table 분리, 쇼핑 추천, refresh token, social login, Redis, 외부 지도/주소 API, AWS/CD 자동화

## Steps

| Step | Name | Range |
| ---: | --- | --- |
| 0 | mvp6-scope-docs | Must-have / MVP6 P0 |
| 1 | clothing-style-tags | Must-have / MVP6 P0 |
| 2 | recommendation-situation | Must-have / MVP6 P0 |
| 3 | recommendation-feedback-api | Must-have / MVP6 P0 |
| 4 | personalized-scoring-reasons | Must-have / MVP6 P0 |
| 5 | frontend-feedback-personalization-ux | Must-have / MVP6 P0 |
| 6 | compose-docs-qa | Must-have / MVP6 P0 |

## 단계 진행 원칙

- Step 0은 문서 전환, archive, ADR, phase 정의만 다룬다.
- Step 1은 옷별 `styleTags` API/DB/backend DTO와 backend 테스트만 다룬다. 추천 점수는 바꾸지 않는다.
- Step 2는 추천 상황 request/snapshot/response와 기본값 `CASUAL`만 다룬다. 피드백 API는 추가하지 않는다.
- Step 3은 추천 피드백 PUT API, full replacement/clear, History response의 `feedback`/`wornAt`만 다룬다. 점수 반영은 Step 4에서 한다.
- Step 4는 `preferenceScore`, 최근 피드백 snapshot, situation style tag mapping, 추천 이유만 다룬다.
- Step 5는 frontend API type/client와 Today/Closet/History UX를 다룬다.
- Step 6은 문서 동기화, Docker Compose, QA 기록, 최종 검증을 수행한다.

## 완료 기준

- `ClothingRequest`/`ClothingResponse`에 `styleTags`가 포함된다.
- body 없는 `POST /api/recommendations`가 `CASUAL`로 성공한다.
- 상황 선택 추천이 저장되고 `RecommendationResponse.situation`으로 반환된다.
- `PUT /api/recommendations/{recommendationId}/feedback`가 최신 feedback snapshot을 전체 교체하거나 clear한다.
- `RecommendationResponse`에 nullable `wornAt`과 nullable `feedback`이 포함된다.
- 최근 14일 피드백과 styleTags가 `preferenceScore`와 추천 이유에 반영된다.
- 추천 이력에서 상황, 착용 여부, 착용 시각, 피드백을 확인할 수 있다.
- 기존 MVP5 이미지 업로드/썸네일 기능이 유지된다.
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
python3 scripts/checks.py --docs-check-config phases/6-smartcloset-feedback-personalization/docs-checks.json --docs-check
docker compose down -v
test -f .env || cp .env.example .env
docker compose up --build -d
curl -fsS http://localhost:8080/v3/api-docs >/dev/null
curl -fsS http://localhost:5173 >/dev/null
docker compose down
```

## 실행 예시

```bash
python3 scripts/execute.py 6-smartcloset-feedback-personalization --next-step-only
python3 scripts/execute.py 6-smartcloset-feedback-personalization
python3 scripts/autopilot.py 6-smartcloset-feedback-personalization --base main --max-review-fixes 2 --unsafe
```
