# 단계 1: spring-ai-config-boundary

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/API.md`
- `docs/SHARING_GUIDE.md`
- `docs/adr/016-mvp10-ai-clothing-registration-assist.md`
- `build.gradle`
- `src/main/resources/application.yml`
- `.env.example`
- `docker-compose.yml`
- `src/main/java/com/smartcloset/**/config/**`

## 작업

- Spring Boot 4.0.6을 유지하고 Spring AI 2.0 preview 계열 BOM과 OpenAI chat model starter를 추가한다.
- `application.yml`에 Spring AI chat model과 OpenAI API key/model 설정을 추가하되 기본은 비활성으로 둔다:
  - `spring.ai.model.chat: ${SPRING_AI_MODEL_CHAT:none}`
  - `spring.ai.openai.api-key: ${OPENAI_API_KEY:}`
  - `spring.ai.openai.chat.options.model: ${CLOTHING_ANALYSIS_MODEL:gpt-5.4-nano}`
- `smartcloset.clothing.analysis` properties를 추가하고 기본값을 비활성/저비용으로 둔다:
  - `enabled: ${CLOTHING_ANALYSIS_ENABLED:false}`
  - `low-confidence-threshold: ${CLOTHING_ANALYSIS_LOW_CONFIDENCE_THRESHOLD:0.75}`
  - `daily-limit: ${CLOTHING_ANALYSIS_DAILY_LIMIT:20}`
  - `timeout-seconds: ${CLOTHING_ANALYSIS_TIMEOUT_SECONDS:10}`
- `.env.example`과 `docker-compose.yml`에 같은 env key를 추가하되 실제 API key 값은 비워 둔다.
- 필요한 경우 analyzer 설정용 properties class를 추가하고, binding/context-load 테스트로 기본 비활성 상태를 검증한다.
- 이 step에서는 Spring AI/OpenAI 호출 adapter, `ClothingImageAnalyzer` 구현, HTTP endpoint를 만들지 않는다.
- GPT-5 계열 temperature 미지원 가능성을 고려해 temperature를 강제로 설정하지 않는다.

## 인수 기준

```bash
git diff --check
./gradlew test
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. Spring AI config 체크리스트를 확인한다:
   - Boot 4.0.6 baseline이 유지되는가?
   - Spring AI 2.0 preview 의존성과 기본 비활성 설정이 반영됐는가?
   - API key가 비어 있고 `CLOTHING_ANALYSIS_ENABLED=false`일 때 앱 시작이 깨지지 않는가?
   - `.env.example`과 Compose가 실제 secret 없이 실행 가능한가?
   - analyzer adapter, API endpoint, 추천 domain 변경이 이 step에 섞이지 않았는가?
3. 결과에 따라 `phases/10-smartcloset-ai-clothing-assist/index.json`의 해당 단계를 업데이트한다.

## 금지사항

- Spring Boot 버전을 내리지 마라. 이유: 현재 baseline은 Spring Boot 4.0.6이다.
- `ClothingImageAnalyzer` adapter나 `POST /api/clothes/analyze-image` endpoint를 구현하지 마라. 이유: Step 1은 의존성/설정 경계만 담당한다.
- 추천 domain service를 수정하지 마라. 이유: MVP10 AI는 옷 등록 후보만 돕는다.
- API key를 코드, 문서 예시 값, 테스트 fixture에 실제 값으로 넣지 마라. 이유: 민감정보 커밋 금지다.
- temperature를 강제로 설정하지 마라. 이유: GPT-5 계열에서 지원되지 않아 호출 오류가 날 수 있다.
