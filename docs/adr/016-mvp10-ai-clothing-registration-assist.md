# MVP10을 AI 옷 등록 보조 MVP로 정의

## 상태

승인됨

## 맥락

MVP9에서 화면 구조와 반응형 UX를 정리했지만, 옷 등록은 여전히 사용자가 카테고리, 색상, 소재, 기온 범위, 비 적합성, style tag를 모두 직접 입력해야 한다. 이 입력은 추천 품질에 직접 영향을 주지만 사용자가 귀찮아하기 쉽고, 초기 옷장 등록 속도를 떨어뜨린다.

사용자 수가 늘면 모델 비용이 커질 수 있으므로 비싼 모델을 기본으로 쓰지 않고, 사용자가 직접 호출한 경우에만 저비용 모델을 호출해야 한다.

현재 백엔드는 Spring Boot 4.0.6이다. Spring AI stable 1.x는 Boot 3.x 중심이므로, MVP10에서는 Boot 4.0.6을 유지하고 Spring AI 2.0 preview 계열을 사용한다. 이 결정은 preview 의존성 리스크를 가진다.

## 결정

MVP10은 AI/GPT 추천이 아니라 AI-assisted clothing registration MVP다.

- 사진 분석은 옷 등록 form의 후보값만 제안한다.
- 사용자가 확인하거나 수정한 값만 기존 옷 저장 API로 저장한다.
- AI 분석 결과는 DB에 저장하지 않는다.
- 기존 `POST /api/clothes`, `PUT /api/clothes/{clothingId}`, `PUT /api/clothes/{clothingId}/image` 계약을 대체하지 않는다.
- 새 보호 API `POST /api/clothes/analyze-image`를 추가해 multipart image를 분석하고 JSON 후보를 반환한다.
- 분석 이미지는 기존 이미지 파일 검증 규칙을 재사용하지만 파일 저장소에 저장하지 않는다.
- 기본 모델은 `gpt-5.4-nano`다.
- Spring AI chat model은 기본 비활성으로 둔다. 실제 호출은 `CLOTHING_ANALYSIS_ENABLED=true`, `SPRING_AI_MODEL_CHAT=openai`, `OPENAI_API_KEY`가 설정된 경우에만 가능하다.
- Spring AI 설정은 `spring.ai.openai.api-key: ${OPENAI_API_KEY:}`와 `spring.ai.openai.chat.options.model: ${CLOTHING_ANALYSIS_MODEL:gpt-5.4-nano}` 형태로 env 값을 읽는다.
- GPT-5 계열은 temperature를 지원하지 않을 수 있으므로 MVP10 구현에서는 temperature를 강제로 설정하지 않는다.
- 비용 방어는 사용자 수동 호출, 기능 비활성 기본값, user별 일일 호출 제한, 짧은 structured output, 프론트 파일 fingerprint cache로 처리한다.
- 낮은 confidence는 다른 모델 자동 재시도가 아니라 사용자 확인 UX로 처리한다.
- Spring AI 멀티모달 샘플의 `MultipartFile -> Resource + MimeType -> ChatClient -> DTO` 흐름은 참고하되, SmartCloset에서는 `clothing` 도메인 안의 provider boundary로 적용한다.

## 결과

- 사용자는 사진을 먼저 넣고 후보값을 빠르게 채운 뒤 애매한 필드만 확인할 수 있다.
- 추천 품질에 필요한 옷장 데이터 입력 장벽이 낮아진다.
- AI 호출은 추천 계산과 분리되어 기존 규칙 기반 추천의 설명 가능성과 테스트 가능성을 유지한다.
- Boot 4.0.6 baseline은 유지되지만 Spring AI 2.0 preview 의존성 변화에 따른 follow-up이 필요할 수 있다.
- OpenAI API key가 없거나 분석 기능이 비활성인 로컬 Docker Compose 공유 흐름은 계속 동작한다.

## 범위 제외

- AI/GPT 옷차림 추천
- AI-generated 추천 이유
- 이미지 기반 추천 점수, 후보 필터링, tie-break
- 사용자 확인 없는 자동 저장
- 분석 결과 DB 저장
- DB schema 변경
- 다중 이미지 업로드
- 이미지 편집, cropping, resizing, compression pipeline
- 이미지 EXIF 분석
- 이미지 moderation
- 다른 모델 자동 재시도
- 쇼핑 추천
