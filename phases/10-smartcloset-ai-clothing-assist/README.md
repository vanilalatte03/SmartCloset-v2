# Phase: SmartCloset 10차 AI Clothing Registration Assist MVP

## 목표

MVP9 UI/UX 리디자인 완료 baseline 위에서 사진 기반 AI 옷 등록 보조를 추가한다.

사용자는 옷 등록/수정 화면에서 사진을 선택하고 `AI 후보 체크`를 실행한다. 백엔드는 Spring AI와 OpenAI `gpt-5.4-nano`로 카테고리, 색상, 소재, 기온 범위, 비 적합성, style tag 후보와 confidence를 반환한다. 프론트는 confidence가 낮은 필드를 흐리게 표시하고 확인 필요 상태로 둔다. 저장은 사용자가 확인/수정한 값만 기존 옷 저장 API로 진행한다.

## 작업 범위

- Must-have / MVP10 P0: MVP9 archive, MVP10 docs/ADR/agent 전환, Spring AI 2.0 preview 기반 analyzer boundary, 비활성 기본값, 보호 분석 API, confidence/review DTO, 옷 등록 form AI 후보 체크 UX, 비용 제한, 최종 QA
- Should-have / MVP10 P1: 분석 결과 microcopy polish, low-confidence field confirm UX polish, duplicate file fingerprint cache polish
- MVP10 제외: AI/GPT 옷차림 추천, AI-generated 추천 이유, 이미지 기반 추천 점수/필터/tie-break, 사용자 확인 없는 자동 저장, 분석 결과 DB 저장, DB schema 변경, 다중 이미지, 이미지 편집/cropping/resizing/compression pipeline, EXIF 분석, image moderation, 다른 모델 자동 재시도, 쇼핑 추천

## Steps

| Step | Name | Range |
| ---: | --- | --- |
| 0 | mvp10-docs-archive | Must-have / MVP10 P0 |
| 1 | spring-ai-config-boundary | Must-have / MVP10 P0 |
| 2 | clothing-image-analyzer-adapter | Must-have / MVP10 P0 |
| 3 | clothing-analysis-api | Must-have / MVP10 P0 |
| 4 | closet-form-ai-assist | Must-have / MVP10 P0 |
| 5 | docs-qa-final | Must-have / MVP10 P0 |

## 단계 진행 원칙

- Step 0은 MVP10 문서 전환, MVP9 archive, ADR, phase 정의만 확인한다. `build.gradle`, `application.yml`, `.env.example`, `docker-compose.yml`, `src/**`, `frontend/**` 변경은 이 step에 포함하지 않는다.
- Step 1은 Spring AI 의존성, 기본 비활성 설정, env/Compose 노출, analyzer properties binding만 다룬다.
- Step 2는 `ClothingImageAnalyzer` provider boundary, disabled analyzer, Spring AI/OpenAI adapter, structured output mapping만 다룬다.
- Step 3은 `POST /api/clothes/analyze-image` 보호 API, DTO, 이미지 검증, 호출 제한, 에러 처리를 다룬다.
- Step 4는 프론트 타입/API client와 옷 등록/수정 form의 AI 후보 체크 UX만 다룬다.
- Step 5는 문서 동기화, 브라우저 QA, 최종 검증을 수행한다.
- 추천 생성, 추천 점수, 후보 필터링, tie-break, 추천 이유는 어떤 step에서도 변경하지 않는다.

## 완료 기준

- 현재 baseline 문서가 MVP10 AI 옷 등록 보조와 ADR-016을 가리킨다.
- MVP9 UI/UX 리디자인은 `archive/mvp-9/`에 최소 요약으로만 남는다.
- Spring AI 2.0 preview 계열을 사용하되 Boot 4.0.6 baseline을 유지한다.
- `CLOTHING_ANALYSIS_ENABLED=false`와 `SPRING_AI_MODEL_CHAT=none` 상태에서 기존 local 실행과 테스트가 통과한다.
- `POST /api/clothes/analyze-image`는 보호 API이며 multipart image를 받아 저장하지 않는 후보 분석 결과만 반환한다.
- 분석 응답은 `suggestion`, `fieldConfidence`, `reviewRequiredFields`, `lowConfidenceThreshold`를 포함한다.
- confidence가 낮은 필드는 프론트에서 흐리게 표시되고 확인 필요 상태가 된다.
- 사용자가 수정/확인한 값만 기존 JSON 옷 저장 API와 기존 이미지 업로드 API로 저장된다.
- AI 분석 결과는 DB schema, 추천 점수, 추천 후보, 추천 이유, 추천 이력에 영향을 주지 않는다.
- 비용 방어가 수동 호출, user별 일일 제한, 파일 fingerprint cache, 짧은 structured output으로 문서화되고 구현된다.

## 검증 명령

```bash
git diff --check
./gradlew test
./gradlew build
(cd frontend && npm run build)
docker compose config --quiet
python3 scripts/checks.py --docs-check-config phases/10-smartcloset-ai-clothing-assist/docs-checks.json --docs-check --include-final-docs
```

Autopilot 자체 리뷰 gate는 각 step 파일의 `## 인수 기준` fenced command를 실행한다. `execute.py --step` 또는 `--next-step-only`가 마지막 pending step을 완료하면 phase 완료 metadata를 기록하기 전에 `python3 scripts/checks.py --stage final`을 실행하므로, 마지막 step PR은 merge 전에 final gate를 통과해야 한다.

최종 Step 5에서는 Codex Browser를 우선 사용하고, 필요하면 Chrome 또는 Computer Use로 대체해 아래 흐름을 확인한다. 결과는 `docs/qa/mvp10-ai-clothing-assist-qa.md`에 viewport, 화면명, 결과 `PASS`, 확인 도구, 확인 메모가 있는 행으로 기록한다. Final docs-check는 이 QA 기록이 없으면 실패한다.

```text
desktop 1440px: 옷장 AI 후보 체크, confidence 확인, 저장 흐름
mobile 390px: 옷장 AI 후보 체크, confidence 확인, 저장 흐름
backend API: disabled, invalid image, limit exceeded, provider unavailable, success
recommendation: AI 분석 전후 추천 점수/이유 불변
```

## 실행 예시

```bash
python3 scripts/execute.py 10-smartcloset-ai-clothing-assist --next-step-only
python3 scripts/execute.py 10-smartcloset-ai-clothing-assist
python3 scripts/autopilot.py 10-smartcloset-ai-clothing-assist --base main --max-review-fixes 2 --unsafe
```
