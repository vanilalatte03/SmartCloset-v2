# 단계 4: preferences-api-and-storage

범위: Must-have / 3차 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/COMMANDS.md`
- `src/main/java/com/smartcloset/user/**`
- `src/main/java/com/smartcloset/clothing/domain/**`
- `src/main/java/com/smartcloset/common/exception/**`

이전 단계에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업
현재 인증 사용자의 선호도 저장/조회 API와 `users` JSON 문자열 저장 로직을 구현한다. 이 단계는 추천 점수 반영 전까지 저장/조회/검증만 다룬다.

이 단계에서 보호 API로 추가 잠그는 범위는 `GET/PUT /api/users/me/preferences`다. 추천 API의 HTTP 전환과 최종 security boundary는 각각 Step 6, Step 7에서 다룬다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/user/application/**`
- `src/main/java/com/smartcloset/user/dto/**`
- `src/main/java/com/smartcloset/user/presentation/**`
- `src/main/java/com/smartcloset/user/domain/User.java`
- `src/main/java/com/smartcloset/user/domain/PreferenceJsonMapper.java`
- `src/test/java/com/smartcloset/user/**`

## 구현 메모
- 대상 API:
  - `GET /api/users/me/preferences`
  - `PUT /api/users/me/preferences`
- 선호도 API는 보호 API다.
- request/response field:
  - `preferredColors`
  - `preferredMaterials`
  - `styleTags`
- 저장 컬럼:
  - `preferred_colors_json`
  - `preferred_materials_json`
  - `style_tags_json`
- 신규 사용자의 기본값은 모두 빈 배열이다.
- JSON 변환은 string concat보다 Jackson 같은 구조화 API를 우선한다.
- `preferredColors`는 `ClothingColor` enum 배열이다.
- `preferredMaterials`는 `ClothingMaterial` enum 배열이다.
- `styleTags`는 문자열 배열이며 blank 불가, 각 항목 최대 30자 기준을 따른다.
- 중복 제거는 허용하지만 순서 정책이 있다면 테스트로 고정한다.
- 이 단계에서는 `styleTags`를 추천 점수, tie-breaker, 추천 이유에 연결하지 않는다.
- `SecurityConfig`는 선호도 API를 Bearer token 필수로 만들고, 아직 전환하지 않은 추천 API의 임시 허용은 Step 7 제거 대상으로 유지한다.

## 검증 절차
```bash
git diff --check
rg -n 'preferred_colors_json|preferred_materials_json|style_tags_json' src/main/java src/test/java docs/ERD.md docs/API.md
! rg -n '/api/recommendations.*401' src/test/java
./gradlew test
```

## 인수 기준
- token 없이 선호도 API를 호출하면 401로 실패한다.
- 신규 사용자의 선호도 조회는 빈 배열 3개를 반환한다.
- 선호 색상/소재/styleTags 저장 후 같은 값을 다시 조회할 수 있다.
- 다른 사용자의 선호도는 조회/수정할 수 없다.
- 선호도 응답에는 `userId`가 없다.
- 잘못된 enum 값 또는 invalid styleTags는 `INVALID_REQUEST`로 실패한다.
- 저장 로직은 JSON array string을 사용하며 별도 preference table을 만들지 않는다.
- 아직 전환하지 않은 추천 API를 Step 4에서 새로 401 회귀 테스트 대상으로 만들지 않는다.

## 금지사항
- 선호도 별도 테이블을 만들지 마라. 이유: 3차 저장 방식은 `users` JSON 문자열 컬럼이다.
- `styleTags`를 추천 점수나 추천 이유에 반영하지 마라. 이유: 3차에서는 저장/조회/표시만 한다.
- JSON 배열을 취약한 문자열 이어붙이기로 만들지 마라. 이유: escaping과 invalid JSON 위험이 있다.
- 선호도 응답에 `userId`를 넣지 마라. 이유: 현재 사용자 전용 response DTO에서는 `userId`를 제거한다.
- `/api/**` 전체 인증 정책을 이 단계에 적용하지 마라. 이유: 추천 API 전환과 최종 보안 경계 검증이 아직 남아 있다.
