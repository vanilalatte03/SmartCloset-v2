# 단계 3: recommendation-feedback-api

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
- `src/main/java/com/smartcloset/recommendation/repository/RecommendationResultRepository.java`

## 작업

추천 피드백 snapshot API와 History response 상태를 구현한다.

- `RecommendationFeedbackSentiment` enum을 추가한다: `LIKED`, `DISLIKED`.
- `RecommendationThermalFeedback` enum을 추가한다: `TOO_COLD`, `TOO_HOT`.
- `RecommendationFeedbackRequest`, nested 상태 DTO `RecommendationFeedbackStateResponse`, PUT 응답 wrapper `RecommendationFeedbackResponse`를 추가한다.
- `recommendation_results.sentiment_feedback`, `thermal_feedback`, `feedback_updated_at`을 추가한다.
- `PUT /api/recommendations/{recommendationId}/feedback`를 추가한다.
- 현재 사용자 소유 추천만 수정 가능하게 한다.
- PUT은 전체 교체로 처리한다.
- 누락 필드는 `null`로 간주한다.
- 양쪽 `null`이면 feedback clear로 처리한다.
- `RecommendationResponse`에 nullable `RecommendationFeedbackStateResponse feedback`과 nullable `wornAt`을 포함한다.
- 착용 완료 API의 idempotent 동작을 유지한다.

## 인수 기준

```bash
./gradlew test --tests '*RecommendationControllerTest'
./gradlew test --tests '*Recommendation*'
git diff --check
```

## 검증 절차

1. `LIKED` + `TOO_COLD` 저장 성공을 확인한다.
2. `{}` 요청이 `feedback=null` clear로 처리되는지 확인한다.
3. `{ "sentiment": null, "thermal": null }` 요청이 clear로 처리되는지 확인한다.
4. 누락 필드가 기존 값을 유지하지 않고 `null`로 교체되는지 확인한다.
5. 다른 사용자 추천은 `RECOMMENDATION_NOT_FOUND`로 실패하는지 확인한다.
6. 추천 이력 response에 `wornAt`과 `feedback`이 포함되는지 확인한다.

## 금지사항

- 피드백 이벤트 로그 테이블을 만들지 마라. 이유: MVP6는 최신 snapshot 모델이다.
- 개인화 점수 계산을 이 단계에서 변경하지 마라. 이유: Step 4 범위다.
- 착용 완료 API를 피드백 API로 대체하지 마라. 이유: worn과 feedback은 별도 상태다.
