# AI 분석 malformed output 장애 처리 기록

## 문서 목적

이 문서는 AI 옷 등록 보조 provider가 잘못된 structured output을 반환했을 때 사용자 입력 오류가 아니라 provider 장애로 처리하도록 정리한 안정성 개선 기록이다.

이 문서는 ADR이 아니며 공개 API shape, DB schema, 추천 계산, 옷 저장 flow를 변경하지 않는다. 관련 GitHub Issue는 `#177`이고, 구현은 PR `#185`에서 merge했다.

## 문제

`POST /api/clothes/analyze-image`는 사용자 업로드 이미지 검증 오류와 Spring AI/OpenAI provider 장애를 분리해야 한다.

기존 `SpringAiClothingImageAnalyzer`는 missing `analyzable`, missing `suggestion`, missing `fieldConfidence`, unknown `reviewRequiredFields` 같은 일부 malformed output을 `ClothingImageAnalysisUnavailableException`으로 변환했다. 그러나 analyzable response의 `suggestion` 변환 중 발생하는 일부 예외는 그대로 남을 수 있었다.

대표 예시는 다음과 같다.

- provider가 `category`, `color`, `material`에 알 수 없는 enum 문자열을 반환한다.
- provider가 blank `name`을 반환한다.
- provider가 `minTemperature > maxTemperature` 값을 반환한다.
- provider가 0.0 이상 1.0 이하 범위를 벗어난 confidence 값을 반환한다.

이 예외가 `IllegalArgumentException`으로 controller advice까지 전파되면 `ILLEGAL_ARGUMENT` `400 Bad Request`로 응답될 수 있다. 이는 provider 출력 품질 문제를 사용자 업로드 오류처럼 보이게 만들고, API 계약의 `CLOTHING_ANALYSIS_UNAVAILABLE` `503 Service Unavailable` 경계와 어긋난다.

## 변경

`SpringAiClothingImageAnalyzer.toResult(...)`에서 analyzable provider response를 내부 `ClothingAnalysisResult`로 변환하는 구간을 provider adapter 경계로 명확히 감쌌다.

변환 중 이미 `ClothingImageAnalysisUnavailableException`으로 분류되는 malformed output은 그대로 유지한다. 그 외 enum 변환, `ClothingAnalysisSuggestion`, `ClothingAnalysisResult` 검증에서 발생하는 `IllegalArgumentException` 또는 `NullPointerException`은 `malformedOutput("Invalid analyzable response", cause)`로 변환한다.

서비스 계층은 기존처럼 `ClothingImageAnalysisUnavailableException`을 `SmartClosetException(ErrorCode.CLOTHING_ANALYSIS_UNAVAILABLE)`로 번역한다. 따라서 provider structured output 오류는 최종 HTTP 응답에서 `503 CLOTHING_ANALYSIS_UNAVAILABLE`로 수렴한다.

## 보존한 계약

- 잘못된 multipart part, MIME type, size 등 사용자 업로드 이미지 validation 실패는 기존처럼 `400 INVALID_REQUEST`다.
- 분석 기능 비활성은 `503 CLOTHING_ANALYSIS_DISABLED`다.
- provider timeout과 호출 실패는 `503 CLOTHING_ANALYSIS_UNAVAILABLE`이다.
- 옷으로 보기 어려운 이미지는 기존처럼 `analyzable=false` 성공 응답이다.
- 분석 이미지는 DB나 파일 저장소에 저장하지 않는다.
- provider raw output, 이미지 bytes, request body, Authorization header, cookie, query string은 응답이나 로그에 노출하지 않는다.
- AI 분석 결과는 추천 점수, 후보 필터링, tie-break, 추천 이유에 사용하지 않는다.

## 운영 영향

provider가 schema에 맞지 않는 값을 반환해도 클라이언트는 사용자 입력 오류가 아니라 provider 일시 장애로 해석할 수 있다. 프론트는 기존 provider 장애 안내를 사용할 수 있고, 운영 로그도 `CLOTHING_ANALYSIS_UNAVAILABLE`로 묶여 provider 품질 이슈와 사용자 업로드 검증 이슈를 분리해서 볼 수 있다.

이 변경은 provider 호출 횟수, daily limit 정책, 이미지 저장 정책, 추천 domain에는 영향을 주지 않는다.

## 회귀 기준

AI 분석 malformed output 처리는 다음 기준을 지킨다.

- unknown `category`, `color`, `material` enum 값은 `CLOTHING_ANALYSIS_UNAVAILABLE`로 수렴해야 한다.
- blank `name`, 잘못된 temperature range, 잘못된 confidence 값은 `CLOTHING_ANALYSIS_UNAVAILABLE`로 수렴해야 한다.
- missing `analyzable`, missing `suggestion`, missing `fieldConfidence`, blank/unknown `reviewRequiredFields`는 계속 `CLOTHING_ANALYSIS_UNAVAILABLE`로 수렴해야 한다.
- 사용자 업로드 이미지 validation 실패를 provider 장애로 바꾸면 안 된다.
- `IllegalArgumentException` 400은 provider structured output 오류로 노출되면 안 된다.
- provider output 원문이나 이미지 정보를 로그/응답에 추가하면 안 된다.

## 검증

PR `#185`에서 다음 검증을 통과했다.

- `git diff --check`
- `git diff --check origin/main...HEAD`
- `python3 scripts/checks.py --docs-check --include-final-docs`
- `./gradlew test --tests com.smartcloset.clothing.infrastructure.analysis.SpringAiClothingImageAnalyzerTest --tests com.smartcloset.clothing.application.ClothingAnalysisServiceTest --tests com.smartcloset.clothing.ClothingControllerTest.returnsInvalidRequestWhenAnalysisImageValidationFails`
- `./gradlew test`
- GitHub Actions: `test-build`
- 커밋 훅: `python3 -m compileall scripts`
- 커밋 훅: `./gradlew build`
- 커밋 훅: `cd frontend && npm run build`
- Codex CLI read-only review: `pass=true`, findings 없음

추가된 테스트는 unknown enum, blank name, invalid temperature range, missing `fieldConfidence`가 `ClothingImageAnalysisUnavailableException`으로 변환되는지 확인한다. 기존 service/controller 테스트는 해당 exception이 `CLOTHING_ANALYSIS_UNAVAILABLE`로 매핑되고, invalid image 입력은 `INVALID_REQUEST`로 유지되는지 확인한다.
