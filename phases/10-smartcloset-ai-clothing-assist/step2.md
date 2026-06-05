# 단계 2: clothing-image-analyzer-adapter

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/SHARING_GUIDE.md`
- `docs/adr/016-mvp10-ai-clothing-registration-assist.md`
- `build.gradle`
- `src/main/resources/application.yml`
- `src/main/java/com/smartcloset/clothing/**`

## 작업

- `ClothingImageAnalyzer` interface를 `clothing` 도메인 provider boundary로 만든다.
- analyzer 입출력 model은 `MultipartFile`, controller, HTTP DTO와 직접 결합하지 않고 application/service에서 mapping할 수 있게 둔다.
- 분석 결과 model은 아래 정보를 표현할 수 있어야 한다:
  - `analyzable`
  - `suggestion`
  - `fieldConfidence`
  - `reviewRequiredFields`
  - `lowConfidenceThreshold`
- `suggestion`은 기존 옷 저장 후보 field인 `name`, `category`, `color`, `material`, `minTemperature`, `maxTemperature`, `rainSuitable`, `styleTags`를 담을 수 있어야 한다.
- disabled analyzer는 기능 비활성 상태를 명확히 표현하고, 다음 API step에서 `CLOTHING_ANALYSIS_DISABLED`로 변환할 수 있게 한다.
- OpenAI analyzer는 Spring AI `ChatClient`를 사용해 `image bytes/Resource + MimeType -> ChatClient -> structured output -> analyzer result` 흐름을 구현한다.
- 옷으로 보기 어려운 이미지는 예외가 아니라 `analyzable=false`, `suggestion=null` analyzer result로 표현한다.
- provider 장애, timeout, malformed structured output은 다음 API step에서 `CLOTHING_ANALYSIS_UNAVAILABLE`로 변환할 수 있게 analyzer-level exception 또는 result로 표준화한다.
- confidence가 `lowConfidenceThreshold`보다 낮은 field를 `reviewRequiredFields`에 포함한다.
- GPT-5 계열 temperature 미지원 가능성을 고려해 temperature를 강제로 설정하지 않는다.
- 이 step에서는 HTTP endpoint, user별 daily limit, controller validation을 만들지 않는다.

## 인수 기준

```bash
git diff --check
./gradlew test
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Analyzer adapter 체크리스트를 확인한다:
   - analyzer boundary가 `MultipartFile`, controller, HTTP DTO에 의존하지 않는가?
   - disabled analyzer와 OpenAI analyzer가 같은 interface 뒤에 있는가?
   - API key가 비어 있고 비활성 설정일 때 앱 시작과 테스트가 깨지지 않는가?
   - provider 장애/timeout/malformed output을 API step에서 표준 에러로 변환할 수 있는가?
   - analyzer가 추천 domain service에 의존하거나 추천 domain을 수정하지 않는가?
3. 결과에 따라 `phases/10-smartcloset-ai-clothing-assist/index.json`의 해당 단계를 업데이트한다.

## 금지사항

- `POST /api/clothes/analyze-image` endpoint를 추가하지 마라. 이유: 이 단계는 provider adapter boundary만 담당하고 HTTP API는 Step 3 범위다.
- user별 daily limit을 구현하지 마라. 이유: limit은 인증 사용자 기반 API use case인 Step 3에서 처리한다.
- 분석 결과를 DB에 저장하지 마라. 이유: MVP10은 저장 전 후보 제안만 다룬다.
- AI 분석을 이유로 추천 결과 DTO나 score DTO를 변경하지 마라. 이유: 분석은 추천에 관여하지 않는다.
- 사용자 확인 없이 옷을 자동 저장하지 마라. 이유: 사용자가 수정/확인한 값만 저장해야 한다.
