# Use User Location Catalog and React TypeScript Frontend

## Status
Accepted

## Context
SmartCloset 1.5차까지는 기상청 단기예보 `getVilageFcst` JSON 연동을 제공했지만 위치는 앱 전역 환경변수 `KMA_NX`, `KMA_NY`에 묶여 있었다. 사용자마다 사는 지역이 다르므로 추천 생성에는 사용자별 위치가 필요하다.

또한 Swagger와 Spring static Demo UI만으로는 실제 제품 사용 흐름을 확인하기 어렵다. 2차 MVP는 위치 선택, 옷 관리, 추천 생성, 착용 완료를 한 화면에서 확인할 정식 프론트엔드 앱이 필요하다.

## Decision
2차 MVP는 사용자별 위치 저장과 React+Vite+TypeScript 프론트엔드 앱을 도입한다.

위치 선택은 외부 주소/지도 API 없이 서버 내장 대표 격자 catalog로 구현한다.

- 위치 catalog는 code, name, nx, ny를 가진다.
- seed user 기본 위치는 서울특별시 `SEOUL`, `nx=60`, `ny=127`이다.
- 사용자는 2차 당시 사용자 위치 선택 API로 catalog code를 선택한다. 3차 이후 현재 인증 사용자 기반 위치 API 계약은 ADR-008을 따른다.
- 추천 생성은 사용자 위치의 `nx`, `ny`로 KMA `getVilageFcst`를 호출한다.
- 존재하지 않는 위치 code는 `LOCATION_NOT_FOUND`로 실패한다.

프론트엔드는 `frontend/` 아래 React+Vite+TypeScript SPA로 구현한다.

- TypeScript `strict` 기준을 사용한다.
- API 요청/응답 DTO는 명시적 타입으로 관리한다.
- 대형 상태 관리 라이브러리 없이 React state와 작은 API client로 시작한다.
- 프론트 구현 기준은 `docs/FRONTEND.md`를 따른다.

## Consequences
- 사용자는 자신의 지역을 선택하고 해당 위치의 날씨 기준으로 추천을 받을 수 있다.
- 외부 위치 API key, 지도 SDK, 좌표 변환 복잡도를 2차 범위에서 제거한다.
- 내장 catalog에 없는 세부 지역은 후속 MVP에서 확장해야 한다.
- `users` 테이블에는 현재 위치 snapshot 컬럼이 추가된다.
- 추천 결과에는 위치 snapshot을 저장하지 않는다. 위치 기반 추천 이력 분석이 필요하면 후속 MVP에서 별도 컬럼을 추가한다.
- 프론트 타입과 백엔드 API 문서의 동기화가 중요해진다.

## Out of Scope
- 로그인/회원가입
- Spring Security
- 외부 주소/지도 검색 API
- 사용자 현재 위치 자동 감지
- 위경도-KMA 격자 변환 API
- Weather source DB 저장
- Redis 날씨 캐싱
- AI/GPT 추천
- 이미지 업로드
- AWS 배포
- CD 자동화
