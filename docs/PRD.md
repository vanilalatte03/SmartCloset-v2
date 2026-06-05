# PRD: SmartCloset MVP10 AI 옷 등록 보조

## 문서 목적

이 문서는 SmartCloset MVP10의 확정 범위를 정의한다. MVP10은 MVP9 UI/UX 리디자인 완료 baseline 위에서 사진 업로드 기반 옷 등록 후보 체크를 추가한다.

현재 코드 출발점은 MVP9 구현 완료 상태다. MVP10 구현 기준은 이 문서와 `docs/` 아래 현재 문서, ADR-016이다.

MVP10의 AI는 옷차림 추천을 생성하지 않는다. AI는 옷 등록 form에 들어갈 후보값을 제안하고, 사용자가 확인/수정한 값만 기존 저장 API로 저장한다.

## 문서 책임

| 계약 영역 | Source of truth |
| --- | --- |
| HTTP endpoint, request/response DTO, 인증/에러 계약 | `docs/API.md` |
| 추천 후보, 점수, 추천 이유, 예보 시간대 입력 | `docs/RECOMMENDATION_RULES.md` |
| 백엔드 구조, transaction, adapter 정책 | `docs/ARCHITECTURE.md` |
| DB schema와 JPA/entity 기준 | `docs/ERD.md` |
| 프론트 API client, 타입, UX, 반응형 기준 | `docs/FRONTEND.md` |
| 데모/공유 검증 | `docs/DEMO_SCENARIO.md`, `docs/SHARING_GUIDE.md` |
| 결정 배경 | `docs/ADR.md`, `docs/adr/016-mvp10-ai-clothing-registration-assist.md` |

## MVP10 한 줄 정의

사용자가 옷 사진을 업로드하면 Spring AI와 `gpt-5.4-nano`가 옷 등록 후보를 제안하고, confidence가 낮은 필드는 사용자가 확인한 뒤 저장하게 한다.

## 목표

- 옷 등록 시 사용자가 모든 선택지를 직접 고르는 부담을 줄인다.
- 사진 기반으로 `name`, `category`, `color`, `material`, `minTemperature`, `maxTemperature`, `rainSuitable`, `styleTags` 후보를 빠르게 채운다.
- confidence가 낮은 후보는 흐리게 표시하고 확인 필요 상태로 둔다.
- AI 분석 결과를 저장하지 않고, 사용자가 수정/확인한 최종 값만 기존 옷 저장 API로 저장한다.
- OpenAI 비용이 무심코 증가하지 않도록 기능 비활성 기본값, 사용자 수동 호출, 일일 제한, 파일 fingerprint cache를 적용한다.
- 추천 생성은 계속 규칙 기반으로 유지한다.
- 데스크톱 1440px, 모바일 390px 기준으로 옷 등록 AI 후보 체크 UI가 겹치거나 잘리지 않게 한다.

## 현재 Baseline

- Spring Boot 4.0.6, Java 21, MySQL을 사용한다.
- Spring Security + JWT Bearer access token 인증을 사용한다.
- DB-backed refresh session과 HttpOnly refresh cookie를 사용한다.
- Frontend는 access token을 memory state에 저장하고 refresh cookie로 세션을 복구한다.
- Password signup은 이메일 인증 필요 상태를 반환하고 access token을 발급하지 않는다.
- 미인증 password 계정 login은 실패한다.
- Google provider 상태와 Google login flow를 유지한다.
- 계정 삭제는 현재 사용자 소유 데이터와 이미지 파일을 즉시 hard delete한다.
- 공개 HTTP API는 `userId` query parameter를 받지 않는다.
- 현재 사용자 전용 응답 DTO는 `userId`를 노출하지 않는다.
- 사용자 소유 옷장, 위치, 선호도, 추천 이력, 착용 이력, 추천 피드백은 인증 사용자별로 분리한다.
- 추천 생성 API는 `POST /api/recommendations`다.
- 추천 이력 조회 API는 `GET /api/recommendations?limit={limit}`이며 기본 20, 최소 1, 최대 50, 최신순이다.
- 현재 날씨 요약 API는 `GET /api/weather/current`이며 보호 API다.
- MVP5 이미지 API, MVP6 피드백/개인화, MVP7 위치/날씨 source snapshot, MVP8 계정 안정성, MVP9 UI/UX 리디자인 기능은 유지한다.
- Docker Compose local 공유 흐름을 유지한다.

## 해결하려는 문제

- 옷 등록 form은 추천 품질에 필요한 정보가 많아 사용자가 빠르게 등록하기 어렵다.
- 사진을 이미 올려도 카테고리, 색상, 소재, 날씨 적합 범위를 다시 수동으로 골라야 한다.
- 잘못 입력된 옷 속성은 추천 후보 필터와 점수에 영향을 주므로, 빠른 입력과 사용자 확인이 함께 필요하다.
- 비싼 모델이나 자동 호출을 쓰면 사용자 수 증가에 따라 비용이 예측 불가능하게 커질 수 있다.
- AI를 추천에 직접 섞으면 현재 규칙 기반 추천의 설명 가능성과 테스트 가능성이 흔들린다.

## 핵심 사용자 시나리오

1. 사용자가 로그인 후 `옷장` 화면에서 새 옷 등록 또는 기존 옷 수정을 연다.
2. 사용자가 옷 사진을 선택하고 preview를 확인한다.
3. 사용자가 `AI 후보 체크` 버튼을 누른다.
4. 백엔드는 보호 API `POST /api/clothes/analyze-image`로 이미지를 분석한다.
5. 프론트는 반환된 후보값을 form에 채우고 confidence가 낮은 필드를 흐리게 표시한다.
6. 사용자가 확인 필요 필드를 수정하거나 확인 처리한다.
7. 저장 시 기존 `POST /api/clothes` 또는 `PUT /api/clothes/{clothingId}` JSON API로 옷 정보를 저장한다.
8. 선택 이미지가 있으면 기존 `PUT /api/clothes/{clothingId}/image`로 이미지를 저장한다.
9. 이후 추천은 기존 규칙 기반 추천 엔진이 저장된 옷 정보를 사용한다.

## MVP10 우선순위

### P0: 문서 전환과 MVP9 archive

- MVP9는 `archive/mvp-9/`에 최소 요약으로 이동한다.
- 현재 baseline은 MVP10 AI 옷 등록 보조로 전환한다.
- ADR-016을 추가하고 `docs/ADR.md`에 연결한다.
- MVP10 phase 문서와 docs-check 규칙을 추가한다.

### P0: Spring AI analyzer boundary

- Spring Boot 4.0.6을 유지한다.
- Spring AI는 2.0 preview 계열을 사용한다.
- OpenAI chat model starter를 사용한다.
- 기본 모델은 `gpt-5.4-nano`다.
- `ClothingImageAnalyzer` provider boundary를 둔다.
- 기본 설정은 AI 분석 비활성이다.
- OpenAI API key가 없거나 분석 기능이 꺼져 있어도 local 실행과 기존 기능은 동작해야 한다.

### P0: Protected clothing analysis API

- `POST /api/clothes/analyze-image` 보호 API를 추가한다.
- multipart part 이름은 `image`다.
- 기존 이미지 검증 규칙을 재사용한다.
- 분석 이미지는 저장하지 않는다.
- 응답은 `suggestion`, `fieldConfidence`, `reviewRequiredFields`, `lowConfidenceThreshold`를 포함한다.
- 옷으로 보기 어려운 이미지는 실패가 아니라 `analyzable=false` 성공 응답으로 처리한다.

### P0: Closet form AI assist UX

- 이미지 선택 후 preview와 `AI 후보 체크` 버튼을 제공한다.
- 분석은 사용자가 버튼을 눌렀을 때만 실행한다.
- 같은 파일은 프론트 파일 fingerprint 기준으로 마지막 분석 결과를 재사용할 수 있다.
- confidence가 낮은 필드는 흐리게 표시하고 `확인 필요` 상태를 붙인다.
- 사용자가 수정하거나 확인하면 해당 필드는 normal 상태가 된다.
- 확인 필요 필드가 남은 저장 시 한 번 더 확인한다.

### P0: Cost guard와 QA

- user별 in-memory daily limit 기본값은 20회다.
- OpenAI 호출은 짧은 structured output을 사용한다.
- GPT-5 계열 temperature 미지원 가능성을 고려해 temperature를 강제로 설정하지 않는다.
- 다른 모델로 자동 재시도하지 않는다.
- desktop 1440px, mobile 390px에서 옷 등록 AI 후보 체크 UX를 확인한다.

## 포함 범위

- `archive/mvp-9/` 최소 요약
- ADR-016
- MVP10 phase 문서와 docs-check 규칙
- Spring AI 2.0 preview 계열 의존성 도입
- OpenAI `gpt-5.4-nano` 기반 옷 사진 분석 provider
- 분석 기능 비활성 기본값과 env 설정
- `POST /api/clothes/analyze-image` 보호 API
- `ClothingAnalysisResponse` 계열 DTO
- confidence 기반 확인 필요 UX
- 사용자 수동 호출과 daily limit 비용 방어
- 프론트 파일 fingerprint cache
- MVP10 수동 QA 문서화

## 제외 범위

- AI/GPT 옷차림 추천
- AI-generated 추천 이유
- 이미지 기반 추천 점수, 후보 필터링, tie-break
- 사용자 확인 없는 자동 저장
- 분석 결과 DB 저장
- DB schema 변경
- 추천 점수/필터/tie-break 변경
- 추천 이력 schema 변경
- 다중 이미지 업로드
- 이미지 편집/cropping/resizing/compression pipeline
- 이미지 EXIF 분석
- image moderation
- 다른 모델 자동 재시도
- 쇼핑 추천
- AWS 배포 구현
- S3 storage 구현체
- SES/SMTP 실제 발송 구현체
- Secrets Manager
- CD 자동화
- Redis
- native mobile app 또는 PWA 배포

## 완료 기준

- 현재 문서 baseline이 MVP10 AI 옷 등록 보조와 ADR-016을 가리킨다.
- MVP9 UI/UX 리디자인은 archive에 최소 요약으로만 남는다.
- Spring AI 2.0 preview 계열과 OpenAI `gpt-5.4-nano` 사용 결정이 문서화된다.
- `CLOTHING_ANALYSIS_ENABLED=false`, `SPRING_AI_MODEL_CHAT=none`, API key 없음 상태에서 기존 local 실행이 깨지지 않는다.
- `POST /api/clothes/analyze-image`는 인증 사용자 보호 API다.
- 분석 API는 이미지를 저장하지 않고 후보값과 confidence만 반환한다.
- confidence가 낮은 필드는 프론트에서 흐림/확인 필요 상태로 보인다.
- 사용자가 확인/수정한 값만 기존 옷 JSON 저장 API로 저장된다.
- 기존 이미지 저장은 `PUT /api/clothes/{clothingId}/image`로 유지된다.
- 추천 생성, 추천 점수, 후보 필터링, tie-break, 추천 이유가 변경되지 않는다.
- DB schema가 변경되지 않는다.
- desktop 1440px과 mobile 390px에서 옷 등록 AI 후보 체크 UI가 겹치거나 잘리지 않는다.

## 테스트/검증 기준

문서 전환 검증:

- `git diff --check`
- `python3 scripts/checks.py --docs-check-config phases/10-smartcloset-ai-clothing-assist/docs-checks.json --docs-check`

MVP10 구현 phase 검증:

- `git diff --check`
- `./gradlew test`
- `./gradlew build`
- `cd frontend && npm run build`
- `docker compose config --quiet`
- `python3 scripts/checks.py --docs-check-config phases/10-smartcloset-ai-clothing-assist/docs-checks.json --docs-check --include-final-docs`

수동 QA:

- 데스크톱 1440px: 옷장 AI 후보 체크, confidence 확인, 저장 흐름
- 모바일 390px: 옷장 AI 후보 체크, confidence 확인, 저장 흐름
- Backend API: disabled, invalid image, limit exceeded, provider unavailable, success
- Recommendation: AI 분석 전후 추천 점수/이유 불변

## 결정 완료 사항

- MVP10 범위: AI-assisted clothing registration suggestion
- AI provider: Spring AI 2.0 preview 계열 + OpenAI chat model
- 기본 모델: `gpt-5.4-nano`
- 분석 endpoint: `POST /api/clothes/analyze-image`
- 분석 결과 저장: 저장하지 않음
- 사용자 확인: confidence 낮은 필드 확인 필요
- 저장 flow: 기존 옷 JSON API와 기존 이미지 API 유지
- 추천: 규칙 기반 유지, AI 분석 결과 비개입
- DB schema: 변경 없음
- fallback model: 자동 재시도 없음
