# MVP 3 Decisions

MVP 3의 상세 결정 기록은 현재 `docs/adr/`에 유지한다. 이 문서는 주요 결정만 요약한다.

## 주요 결정
- 인증은 Spring Security + JWT Bearer access token 단일 구조로 구현했다. 자세한 내용: ../../docs/adr/008-mvp3-authenticated-user-personalization.md
- 공개 API는 `POST /api/auth/signup`, `POST /api/auth/login`만 두고, 그 외 API는 보호 API로 고정했다.
- JWT access token은 `HS256` + `JWT_SECRET`으로 서명하고 만료 시간을 2시간으로 고정했다.
- refresh token, 소셜 로그인, 이메일 인증, 비밀번호 재설정은 범위에서 제외했다.
- 프론트 access token 저장 위치는 `sessionStorage`로 고정했다.
- 공개 HTTP API에서 `userId` query parameter를 제거하고 인증 principal에서 현재 사용자를 식별하도록 했다.
- 현재 사용자 전용 response DTO에서는 `userId` 필드를 제거했다.
- 사용자 선호도는 `users` 테이블의 `preferred_colors_json`, `preferred_materials_json`, `style_tags_json` JSON 문자열 컬럼에 저장하기로 했다.
- `preferredColors`와 `preferredMaterials`만 `preferenceScore`에 반영하고, `styleTags`는 저장/조회/표시만 하기로 했다.
- 기존 다양성 점수는 `preferenceScore` 10점으로 교체했다.
- 추천 이력 조회 API는 `GET /api/recommendations?limit={limit}`로 두고 기본 20, 최소 1, 최대 50, 최신순으로 고정했다.
- MVP4 기능 범위는 이 archive에서 확정하지 않는다. MVP4 변경은 현재 `docs/PRD.md`와 새 ADR에서 별도로 결정한다.
