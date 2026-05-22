# MVP 2 Decisions

MVP 2의 상세 결정 기록은 현재 `docs/adr/`에 유지한다. 이 문서는 주요 결정만 요약한다.

## 주요 결정
- 사용자별 위치 저장과 위치 catalog 선택을 2차 핵심 범위로 확정했다. 자세한 내용: ../../docs/adr/007-mvp2-user-location-and-react-frontend.md
- 위치 선택은 외부 주소/지도 API 없이 서버 내장 대표 격자 catalog로 구현했다.
- seed user 기본 위치는 서울특별시 `SEOUL`, `nx=60`, `ny=127`로 정했다.
- 추천 생성은 사용자 위치의 `nx`, `ny`로 KMA `getVilageFcst`를 호출하도록 정했다.
- `KMA_NX`, `KMA_NY`는 2차 추천 source of truth가 아니라 기존 환경변수 호환용으로 남겼다.
- 프론트엔드는 `frontend/` 아래 React+Vite+TypeScript SPA로 구현했다.
- 2차 공유 방식은 Docker Compose에 `frontend` 서비스를 포함하는 방식으로 유지했다.
- 로그인/회원가입, Spring Security, 개인화 선호도는 3차 MVP 후보로 넘겼다.
