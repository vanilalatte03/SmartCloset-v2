# MVP 3 Changelog

## 2026-05-24
- 3차 MVP 문맥을 MVP4 문서 작성 준비를 위해 archive로 정리했다.
- 3차 MVP 전체 문서 복사본을 남기지 않고, 과거 맥락 확인용 최소 archive 요약으로 정리했다.

## MVP 3 종료 시점 주요 변경
- 회원가입/로그인 API를 추가했다.
- Spring Security와 JWT Bearer access token 인증을 도입했다.
- auth 2종 외 API를 보호 API로 전환했다.
- 공개 HTTP API에서 `userId` query parameter를 제거했다.
- 현재 사용자 전용 응답 DTO에서 `userId` 필드를 제거했다.
- 옷장, 위치, 추천 이력, 착용 이력을 현재 인증 사용자 기준으로 격리했다.
- 사용자 선호도 JSON 문자열 컬럼과 선호도 API를 추가했다.
- 기존 다양성 점수를 `preferenceScore`로 교체했다.
- 추천 이력 조회 API와 limit 정책을 추가했다.
- React 프론트엔드에 인증 세션, `sessionStorage` token 저장, 세션 복구 흐름을 추가했다.
- React 위치/선호도/옷장/추천/추천 이력 화면을 인증 사용자 API에 연결했다.
- Docker Compose 공유 smoke를 통과시키고 문서를 동기화했다.
