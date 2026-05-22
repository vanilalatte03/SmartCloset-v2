# 단계 2: clothing-current-user-api

범위: Must-have / 3차 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/COMMANDS.md`
- `src/main/java/com/smartcloset/security/**`
- `src/main/java/com/smartcloset/clothing/**`
- `src/test/java/com/smartcloset/clothing/**`

이전 단계에서 만들어진 코드를 꼼꼼히 읽고, 설계 의도를 이해한 뒤 작업하라.

## 작업
옷장 API를 인증 사용자 기준으로 전환한다. 공개 HTTP 계약에서 `userId` query parameter를 제거하고, Controller는 인증 principal에서 현재 사용자 id를 얻어 application service에 전달한다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/clothing/presentation/**`
- `src/main/java/com/smartcloset/clothing/application/**`
- `src/main/java/com/smartcloset/clothing/dto/**`
- `src/main/java/com/smartcloset/clothing/repository/**`
- `src/test/java/com/smartcloset/clothing/**`
- auth test helper 또는 fixture 파일

## 구현 메모
- 대상 API:
  - `POST /api/clothes`
  - `GET /api/clothes`
  - `GET /api/clothes/{clothingId}`
  - `PUT /api/clothes/{clothingId}`
  - `PATCH /api/clothes/{clothingId}/archive`
- 모든 대상 API는 Bearer token을 요구한다.
- request에서 `userId` query parameter를 제거한다.
- 현재 사용자 전용 `ClothingResponse`에는 `userId`를 넣지 않는다.
- service/repository 내부에서는 소유자 조건 조회를 위해 `Long userId`를 계속 사용할 수 있다.
- 다른 사용자의 옷 상세/수정/archive 요청은 `CLOTHING_NOT_FOUND`로 실패시킨다.
- archive는 idempotent해야 한다.
- 목록 조회는 현재 인증 사용자의 `archived=false` 옷만 `id` 오름차순으로 반환한다.

## 검증 절차
```bash
git diff --check
! rg -n '/api/clothes\\?userId|userId.*ClothingResponse|ClothingResponse.*userId' src/main/java src/test/java
./gradlew test
```

## 인수 기준
- token 없이 옷 API를 호출하면 401로 실패한다.
- 유효 token으로 옷 등록/목록/상세/수정/archive가 동작한다.
- 옷 API request mapping에 `userId` query parameter가 없다.
- 옷 응답 DTO와 프론트 계약용 DTO에 `userId`가 없다.
- 사용자 A token으로 사용자 B의 옷을 조회/수정/archive할 수 없다.
- archive 중복 호출은 성공하며 결과가 `archived=true`로 유지된다.

## 금지사항
- `userId` query parameter를 compatibility 명목으로 유지하지 마라. 이유: 3차 공개 HTTP 계약에서 제거됐다.
- Controller에서 repository를 직접 호출하지 마라. 이유: Controller는 HTTP, validation, principal extraction, DTO mapping만 담당한다.
- 다른 사용자의 옷에 대해 `FORBIDDEN`으로 리소스 존재를 드러내지 마라. 이유: 현재 계약은 소유자가 아니면 `CLOTHING_NOT_FOUND`다.
- Entity에 Lombok `@Data` 또는 무분별한 setter를 추가하지 마라. 이유: Entity mutation은 의도가 드러나는 method로 제한한다.
