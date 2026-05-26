# 단계 1: clothing-style-tags

## 읽어야 할 파일

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/API.md`
- `docs/ERD.md`
- `docs/ARCHITECTURE.md`
- `docs/RECOMMENDATION_RULES.md`
- `src/main/java/com/smartcloset/clothing/domain/ClothingItem.java`
- `src/main/java/com/smartcloset/clothing/dto/ClothingRequest.java`
- `src/main/java/com/smartcloset/clothing/dto/ClothingResponse.java`
- `src/main/java/com/smartcloset/clothing/application/ClothingService.java`

## 작업

옷별 `styleTags` 저장과 API 응답을 구현한다.

- `clothing_items.style_tags_json`을 추가한다.
- `ClothingRequest`에 `styleTags`를 추가하되 요청 누락은 빈 배열로 처리한다.
- `ClothingResponse`와 추천 outfit item DTO에 `styleTags` 배열을 포함한다.
- tag는 trim하고 blank tag는 저장하지 않는다.
- 중복 tag는 제거한다.
- 단일 tag 최대 길이는 30자로 검증한다.
- 기본 옷 프리셋에 MVP6 API 문서의 style tag 값을 부여한다.
- 옷 등록/수정/조회 controller/service 테스트를 추가하거나 수정한다.

## 인수 기준

```bash
./gradlew test --tests '*Clothing*'
./gradlew test --tests '*Recommendation*'
git diff --check
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 옷 등록 요청에서 `styleTags` 누락 시 응답이 `[]`인지 확인한다.
3. blank/중복 tag normalization을 확인한다.
4. 30자 초과 tag가 validation 실패하는지 확인한다.
5. 추천 outfit item response에 `styleTags`가 포함되는지 확인한다.

## 금지사항

- 추천 점수 계산을 이 단계에서 변경하지 마라. 이유: 개인화 점수 반영은 Step 4 범위다.
- 이미지 API를 수정하지 마라. 이유: MVP5 이미지 계약은 유지한다.
- 공개 `userId` query parameter를 추가하지 마라. 이유: 현재 사용자 식별은 JWT principal 기준이다.
