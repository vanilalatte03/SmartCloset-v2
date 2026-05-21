# 단계 7: clothing-p1-api-ci

범위: Should-have / P1

## 읽어야 할 파일
- `docs/API.md`
- `docs/PRD.md`
- `docs/SHARING_GUIDE.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `phases/1-smartcloset-mvp/step6.md`

## 작업
P0 완료 후 Clothing 상세/수정/보관 API와 GitHub Actions test/build를 추가한다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/clothing/**`
- `src/test/java/com/smartcloset/clothing/**`
- `.github/workflows/**`
- 필요 시 `README.md`, `docs/API.md`

## 구현 메모
- `GET /api/clothes/{clothingId}?userId={userId}`는 본인 옷만 조회한다.
- `PUT /api/clothes/{clothingId}?userId={userId}`는 등록과 같은 필드를 받는 전체 수정이다.
- `PATCH /api/clothes/{clothingId}/archive?userId={userId}`는 idempotent하게 `archived=true`로 처리한다.
- archive된 옷은 추천 후보와 기본 목록 조회에서 제외된다.
- GitHub Actions는 test/build를 실행한다.
- P0 API나 Docker Compose 공유 흐름을 깨뜨리지 않는다.

## 검증 절차
```bash
./gradlew test
./gradlew build
```

## 인수 기준
- P1 Clothing API가 API 문서 계약을 따른다.
- 다른 사용자 옷 접근은 `CLOTHING_NOT_FOUND`로 처리된다.
- archive API는 중복 호출해도 성공한다.
- GitHub Actions test/build workflow가 추가되어 있다.

## 금지사항
- 로그인/회원가입을 추가하지 마라. 이유: 1차 MVP는 `userId` request parameter 기반이다.
- P0 완료 전 이 단계를 진행하지 마라. 이유: P1은 공유 품질 강화 범위다.
