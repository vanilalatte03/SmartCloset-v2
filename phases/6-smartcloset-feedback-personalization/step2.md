# 단계 2: recommendation-situation

## 읽어야 할 파일

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/API.md`
- `docs/ERD.md`
- `docs/ARCHITECTURE.md`
- `docs/RECOMMENDATION_RULES.md`
- `src/main/java/com/smartcloset/recommendation/presentation/RecommendationController.java`
- `src/main/java/com/smartcloset/recommendation/application/RecommendationService.java`
- `src/main/java/com/smartcloset/recommendation/domain/RecommendationResult.java`
- `src/main/java/com/smartcloset/recommendation/dto/RecommendationResponse.java`

## 작업

추천 상황 request/snapshot/response를 구현한다.

- `RecommendationSituation` enum을 추가한다: `WORK`, `CASUAL`, `WORKOUT`, `DATE`, `FORMAL`.
- `RecommendationRequest` DTO를 추가한다.
- `POST /api/recommendations`는 body 없이 호출 가능해야 한다.
- body가 없거나 `situation`이 누락되면 `CASUAL`을 사용한다.
- `recommendation_results.situation` snapshot을 저장한다.
- `RecommendationResponse.situation`을 반환한다.
- controller/service/domain 테스트를 추가하거나 수정한다.

## 인수 기준

```bash
./gradlew test --tests '*RecommendationControllerTest'
./gradlew test --tests '*Recommendation*'
git diff --check
```

## 검증 절차

1. body 없는 추천 생성이 `CASUAL`로 성공하는지 확인한다.
2. `situation=WORK` 요청이 response와 DB snapshot에 반영되는지 확인한다.
3. 잘못된 enum은 `400 INVALID_REQUEST`로 실패하는지 확인한다.

## 금지사항

- 추천 피드백 API를 이 단계에서 추가하지 마라. 이유: Step 3 범위다.
- 개인화 점수 계산을 이 단계에서 변경하지 마라. 이유: Step 4 범위다.
- today 추천 GET endpoint를 추가하지 마라. 이유: 현재 추천 생성 계약은 POST다.
