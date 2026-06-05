# MVP10 AI Clothing Registration Assist QA

## 검증 환경

- 일시: 2026-06-05
- 도구: Codex Browser, Playwright with local Chrome, curl
- 실행: Spring Boot `http://localhost:8081`, Vite frontend `http://localhost:5175`, UI success-flow QA proxy `http://localhost:8082`
- 기준: 데스크톱 1440px와 모바일 390px에서 옷장 AI 후보 체크, confidence 확인 필요 표시, 저장 전 확인 흐름을 확인한다.

## 결과

| Viewport | 화면명 | 결과 | 확인 도구 | 확인 메모 |
| --- | --- | --- | --- | --- |
| desktop 1440px | 옷장 AI 후보 체크 | PASS | Codex Browser | 이미지 붙여넣기 후 `AI 후보 체크`가 활성화됐고, 후보값 적용, 확인 필요 4개 표시, 저장 전 확인 modal, 현재 값 저장 완료를 확인했다. 성공 분석 응답은 로컬 QA proxy의 고정 응답을 사용했다. |
| mobile 390px | 옷장 AI 후보 체크 | PASS | Codex Browser + Playwright with local Chrome | 하단 탭과 `옷 추가` 모바일 진입을 Codex Browser로 확인했고, 390px Playwright에서 파일 선택, AI 후보 체크 활성화, 확인 필요 표시, 저장 전 확인 modal, 현재 값 저장 완료를 확인했다. 성공 분석 응답은 로컬 QA proxy의 고정 응답을 사용했다. |
| backend API | analysis cases | PASS | curl + Gradle tests | 실제 백엔드 `POST /api/clothes/analyze-image`는 기본 비활성에서 `503 CLOTHING_ANALYSIS_DISABLED`, invalid gif에서 `400 INVALID_REQUEST`를 반환했다. Provider unavailable, daily limit, success/not-analyzable DTO mapping은 `ClothingAnalysisServiceTest`와 controller tests가 검증한다. |
| recommendation | AI 분석 전후 추천 불변 | PASS | curl + source review | 분석 API 호출 전후 `GET /api/recommendations?limit=5` 응답이 동일했고, 분석 결과는 DB/추천 이력/추천 점수/추천 이유에 저장하거나 반영하지 않는 구현 경계를 확인했다. |

## 확인 메모

- AI 분석 성공 UI는 OpenAI key 없이 로컬에서 반복 가능하도록 QA proxy로만 대체했다. 실제 백엔드는 기본값 `CLOTHING_ANALYSIS_ENABLED=false`, `SPRING_AI_MODEL_CHAT=none`, 빈 API key 상태로 실행했다.
- 분석 이미지는 기존 이미지 검증을 통과한 뒤 저장 전 후보 제안에만 사용되며, 옷 저장은 기존 JSON 저장 API와 이미지 업로드 API를 그대로 사용했다.
- 낮은 confidence 필드는 `확인 필요` badge와 field confirm button으로 표시됐다.
- 확인 필요 항목이 남은 상태에서 저장하면 `아직 확인 필요 항목이 있습니다` modal이 표시되고, 사용자가 현재 값 저장을 선택해야 저장이 진행됐다.
- 추천 API, 추천 규칙, DB schema 변경은 Step 5에서 추가하지 않았다.
