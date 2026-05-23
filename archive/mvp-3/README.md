# SmartCloset MVP 3 Archive

이 문서는 SmartCloset 3차 MVP를 이해하기 위한 최소 요약이다.

- 아카이브 정리 시점: 2026-05-24
- 완료 시점: 2026-05-22T20:38:12+0900
- 상태: 인증 사용자 기반 API와 개인화 최소 버전 완료 후 MVP4 문서 전환 준비
- 목표: 테스트용 `userId=1` 흐름을 벗어나 회원가입/로그인, JWT Bearer 인증, 현재 사용자 기준 API, 사용자 선호도, 추천 이력, React 세션 흐름을 검증한다.
- 최종 범위: Spring Security, JWT access token, current-user API, 사용자별 옷장/위치/추천 이력/착용 이력 분리, 선호도 JSON 컬럼 저장, `preferenceScore`, 추천 이력 조회, React `sessionStorage` 세션, Docker Compose 공유 검증

현재 구현 기준 문서는 루트 `README.md`와 `docs/` 아래 문서다. 이 archive는 과거 MVP 맥락 확인용이며 구현 source of truth가 아니다.

## 관련 링크
- 현재 PRD/MVP4 틀: ../../docs/PRD.md
- 현재 API 문서: ../../docs/API.md
- 현재 프론트 문서: ../../docs/FRONTEND.md
- 현재 추천 규칙: ../../docs/RECOMMENDATION_RULES.md
- ADR 인덱스: ../../docs/ADR.md
- MVP 3 phase 기록: ../../phases/3-smartcloset-auth-personalization/README.md
- MVP 3 issue 기록: ../../issues/3-smartcloset-auth-personalization/
- MVP 3 요약: SUMMARY.md
- MVP 3 결정: DECISIONS.md
- MVP 3 변경 이력: CHANGELOG.md
