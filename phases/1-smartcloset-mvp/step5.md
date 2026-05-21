# 단계 5: recommendation-p0-api

범위: Must-have / P0

## 읽어야 할 파일
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/RECOMMENDATION_RULES.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `phases/1-smartcloset-mvp/step4.md`

## 작업
추천 생성 API와 추천 결과 착용 완료 API를 구현하고 `RecommendationResult`, `RecommendationResultItem`, `WearHistory` 저장 흐름을 완성한다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/recommendation/presentation/**`
- `src/main/java/com/smartcloset/recommendation/application/**`
- `src/main/java/com/smartcloset/recommendation/dto/**`
- `src/main/java/com/smartcloset/recommendation/repository/**`
- `src/test/java/com/smartcloset/recommendation/**`

## 구현 메모
- 추천 생성 API는 `POST /api/recommendations?userId={userId}`이며 `201 Created`를 반환한다.
- 추천 생성은 write transaction이다.
- 응답에는 weather snapshot, outfit top/bottom/outer, score breakdown, reasons, `worn=false`, `createdAt`을 포함한다.
- `reasonsJson`은 Application 계층 또는 converter에서 string list와 JSON string으로 변환한다.
- 추천 실패 5종은 HTTP `422 Unprocessable Entity`로 반환한다.
- 착용 완료 API는 `PATCH /api/recommendations/{recommendationId}/worn?userId={userId}`이며 idempotent하게 성공한다.
- 이미 worn인 추천 결과는 `WearHistory`를 중복 생성하지 않는다.

## 검증 절차
```bash
./gradlew test
./gradlew build
```

## 인수 기준
- 추천 생성 API가 `RecommendationResult`와 item들을 저장한다.
- 착용 완료 API가 `RecommendationResult.worn=true`와 `WearHistory` 생성을 처리한다.
- 추천 실패는 API 문서의 공통 실패 응답 형태와 422 status를 따른다.
- P0 추천 API controller/service 테스트 또는 통합 테스트가 있다.

## 금지사항
- today 추천 GET 경로를 만들지 마라. 이유: 추천 생성은 상태 변경이므로 POST 계약으로 확정되어 있다.
- 착용 완료 중복 호출에서 WearHistory를 중복 저장하지 마라. 이유: `/worn`은 idempotent 성공이어야 한다.
