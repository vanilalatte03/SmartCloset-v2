# 단계 3: clothing-analysis-api

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/adr/016-mvp10-ai-clothing-registration-assist.md`
- `src/main/java/com/smartcloset/common/exception/ErrorCode.java`
- `src/main/java/com/smartcloset/clothing/controller/ClothingController.java`
- `src/main/java/com/smartcloset/clothing/application/ClothingService.java`
- `src/main/java/com/smartcloset/clothing/infrastructure/file/ClothingImageValidator.java`
- `src/main/java/com/smartcloset/clothing/**/ClothingImageAnalyzer*`

## 작업

- 보호 API `POST /api/clothes/analyze-image`를 추가한다.
- Request는 `multipart/form-data`이며 part 이름은 `image`다.
- 인증 principal의 현재 사용자 id를 기준으로 호출 제한을 적용한다.
- 기존 `ClothingImageValidator` 검증을 재사용하되 이미지 bytes를 DB나 파일 저장소에 저장하지 않는다.
- Step 2의 `ClothingImageAnalyzer`를 호출하고 analyzer result를 HTTP response DTO로 mapping한다.
- 응답은 `{ "data": ... }` envelope를 유지하고 아래 필드를 포함한다:
  - `analyzable`
  - `suggestion`
  - `fieldConfidence`
  - `reviewRequiredFields`
  - `lowConfidenceThreshold`
- `suggestion`은 기존 `ClothingRequest`와 같은 후보 field를 포함한다: `name`, `category`, `color`, `material`, `minTemperature`, `maxTemperature`, `rainSuitable`, `styleTags`.
- 옷으로 보기 어려운 사진은 실패가 아니라 `analyzable=false`, `suggestion=null` 성공 응답으로 처리한다.
- 새 error code를 추가한다:
  - `CLOTHING_ANALYSIS_DISABLED` -> `503 Service Unavailable`
  - `CLOTHING_ANALYSIS_UNAVAILABLE` -> `503 Service Unavailable`
  - `CLOTHING_ANALYSIS_LIMIT_EXCEEDED` -> `429 Too Many Requests`
- user별 in-memory daily limit 기본값은 20회다.

## 인수 기준

```bash
git diff --check
./gradlew test
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. API 체크리스트를 확인한다:
   - 인증 없이 호출하면 기존 인증 실패 shape를 반환하는가?
   - invalid image는 기존 `INVALID_REQUEST` image detail로 실패하는가?
   - disabled/API key 없음/provider unavailable/limit exceeded가 문서화된 code로 실패하는가?
   - 성공 응답 DTO가 프론트 타입과 일치하는가?
   - 분석 결과가 DB, 파일 저장소, 추천 이력에 저장되지 않는가?
3. 결과에 따라 `phases/10-smartcloset-ai-clothing-assist/index.json`의 해당 단계를 업데이트한다.

## 금지사항

- `POST /api/clothes` 또는 `PUT /api/clothes/{clothingId}`를 multipart로 바꾸지 마라. 이유: 기존 JSON 옷 저장 계약을 유지해야 한다.
- 분석 결과를 DB에 저장하지 마라. 이유: MVP10은 저장 전 후보 제안만 다룬다.
- AI 분석을 이유로 추천 결과 DTO나 score DTO를 변경하지 마라. 이유: 분석은 추천에 관여하지 않는다.
- 사용자 확인 없이 옷을 자동 저장하지 마라. 이유: 사용자가 수정/확인한 값만 저장해야 한다.
