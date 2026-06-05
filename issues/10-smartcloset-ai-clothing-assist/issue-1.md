# Issue 1: 10-smartcloset-ai-clothing-assist step 2 자동 리뷰 실패 1

## 발생 위치
- Phase: 10-smartcloset-ai-clothing-assist
- Step: 2 `clothing-image-analyzer-adapter`
- PR: https://github.com/vanilalatte03/smart-closet/pull/145

## 재현 명령
```bash
./gradlew test
git diff --check origin/main...HEAD
```

## 핵심 에러
## 자체 리뷰

| 항목 | 결과 | 비고 |
| --- | --- | --- |
| 로컬 검증 | 통과 | step 인수 기준 명령 |
| diff 검사 | 통과 | git diff --check |
| 금지 범위 | 통과 | MVP 제외 범위와 금지 API 검색 |
| 자체 리뷰 | 실패 | Codex read-only review |

## 확인한 명령

```bash
./gradlew test
git diff --check origin/main...HEAD
```

## 발견사항
- src/main/java/com/smartcloset/clothing/infrastructure/analysis/ClothingImageAnalyzerConfig.java:17: The OpenAI analyzer bean is guarded by @ConditionalOnBean(ChatModel.class) inside a regular component-scanned @Configuration. Spring Boot evaluates these conditions before later auto-configuration beans are reliably available, so the enabled path can be skipped and the fallback DisabledClothingImageAnalyzer registered even when CLOTHING_ANALYSIS_ENABLED=true, SPRING_AI_MODEL_CHAT=openai, and OPENAI_API_KEY are set. That violates Step 2's requirement for an active Spring AI/OpenAI adapter behind the ClothingImageAnalyzer boundary, and the tests only cover disabled paths. Remove the fragile @ConditionalOnBean gate or move this into proper auto-configuration, inject the ChatClient.Builder/ChatModel via ObjectProvider, and add a positive config test that proves SpringAiClothingImageAnalyzer is selected when the enabled conditions are present.

## 리뷰 결론
블로커가 있어 merge하지 않습니다.

## 수정 방향
- 같은 PR 브랜치에서 발견사항을 수정하고 같은 gate를 다시 통과시킨다.

## 완료 기준
- 로컬 검증, 금지 범위 검색, Codex 자체 리뷰를 모두 통과한다.
