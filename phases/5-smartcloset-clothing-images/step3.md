# 단계 3: image-response-dtos

## 읽어야 할 파일

- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/API.md`
- `docs/RECOMMENDATION_RULES.md`
- `src/main/java/com/smartcloset/clothing/dto/ClothingImageResponse.java`
- `src/main/java/com/smartcloset/recommendation/dto/OutfitItemResponse.java`
- `src/main/java/com/smartcloset/recommendation/dto/RecommendationOutfitResponse.java`
- `src/main/java/com/smartcloset/recommendation/application/RecommendationService.java`
- `src/test/java/com/smartcloset/recommendation/RecommendationControllerTest.java`

## 작업

추천 응답 DTO에 nullable image metadata를 반영한다.

- `OutfitItemResponse.image`를 nullable로 추가한다.
- 추천 생성과 추천 이력 조회 응답에 image metadata가 포함되게 한다.
- 이미지가 없는 경우 `image=null`로 반환한다.
- 추천 점수와 추천 이유는 변경하지 않는다.

## 인수 기준

```bash
./gradlew test --tests com.smartcloset.recommendation.RecommendationControllerTest
./gradlew test --tests com.smartcloset.recommendation.domain.RecommendationScorerTest
git diff --check
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 현재 사용자 전용 응답에 `userId`가 추가되지 않았는지 확인한다.
3. 이미지 metadata가 추천 점수에 영향을 주지 않는지 확인한다.
4. 성공하면 phase index의 Step 3을 completed로 갱신한다.

## 금지사항

- 추천 결과에 이미지 bytes를 JSON으로 넣지 마라. 이유: DTO는 metadata와 보호 image URL만 제공한다.
- 이미지 업로드 여부로 score나 reason을 바꾸지 마라. 이유: 추천 규칙 변경은 MVP5 범위가 아니다.
