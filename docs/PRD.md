# PRD: SmartCloset MVP4 Draft

## 문서 목적
이 문서는 SmartCloset MVP4 요구사항을 작성하기 위한 틀이다. MVP4 기능 범위는 아직 확정하지 않았으며, 아래 TBD 항목을 채우기 전까지 현재 구현 baseline을 바꾸지 않는다.

현재 구현 baseline은 MVP-3 완료 상태다. 인증 사용자 기반 API, JWT Bearer access token, 사용자별 옷장/위치/추천 이력/착용 이력 분리, 선호도 JSON 컬럼, `preferenceScore`, 추천 이력 조회, React `sessionStorage` 세션 흐름이 이미 구현되어 있다.

## 현재 baseline
- 공개 API는 `POST /api/auth/signup`, `POST /api/auth/login`뿐이다.
- 그 외 API는 `Authorization: Bearer {accessToken}` header를 요구한다.
- 공개 HTTP API는 `userId` query parameter를 받지 않는다.
- 현재 사용자 전용 응답 DTO는 `userId`를 노출하지 않는다.
- 추천 생성 API는 `POST /api/recommendations`다.
- 추천 이력 조회 API는 `GET /api/recommendations?limit={limit}`이며 기본 20, 최소 1, 최대 50, 최신순이다.
- 프론트 access token 저장 위치는 `sessionStorage`다.
- 선호도는 `users` 테이블의 `preferred_colors_json`, `preferred_materials_json`, `style_tags_json` JSON 문자열 컬럼에 저장한다.
- `preferredColors`와 `preferredMaterials`만 `preferenceScore`에 반영한다.
- `styleTags`는 저장/조회/표시만 하며 추천 점수와 추천 이유에는 반영하지 않는다.
- 외부 Weather API는 기상청 단기예보 `getVilageFcst` JSON 연동만 사용한다.
- 위치 선택은 외부 지도/주소 API 없이 서버 내장 대표 격자 catalog를 사용한다.
- Docker Compose 공유 방식을 유지한다.

## MVP4 한 줄 정의
TBD. MVP4가 사용자에게 추가로 해결할 일을 한 문장으로 작성한다.

## 배경
TBD. 현재 baseline에서 남은 사용자 문제, 운영 문제, 제품 검증 문제를 실제 관찰 기준으로 작성한다.

참고:
- MVP-3 완료 기록은 `archive/mvp-3/`에 최소 요약으로 보존한다.
- Harness 실행 기록은 `phases/3-smartcloset-auth-personalization/`에 유지한다.
- MVP4 결정은 새 ADR 또는 기존 ADR 갱신으로 남긴다.

## 해결하려는 문제
- TBD

## 핵심 사용자 시나리오
1. TBD
2. TBD
3. TBD

## MVP4 우선순위

### P0: TBD
- TBD

### P1: TBD
- TBD

### P2: 후보
아래 항목은 기존 문서에서 후속 후보로 남긴 것이며, MVP4 확정 범위가 아니다.

- Refresh token
- 소셜 로그인
- 이메일 인증
- 비밀번호 재설정
- 선호도 별도 테이블 정규화
- styleTags 기반 개인화 고도화
- 외부 주소/지도 검색 API
- 사용자 현재 위치 자동 감지
- 옷 이미지 업로드
- AI/GPT 추천
- Redis 캐싱
- AWS 배포와 CD 자동화

## 포함 범위
- TBD

## 제외 범위
아래 항목은 별도 승인 전까지 현재 baseline에서 제외한다.

- refresh token
- social login
- email verification
- password reset
- external address/map APIs
- browser/current-location auto detection
- latitude/longitude to KMA grid conversion APIs
- KMA `getVilageFcst` 외 weather APIs
- weather source DB persistence
- Redis
- AWS deployment
- CD automation
- AI/GPT recommendations
- image upload
- shopping recommendations
- preference normalization tables
- styleTags scoring
- styleTags recommendation reasons

## API 변경 계획
TBD. 공개 API와 보호 API를 분리해서 작성한다.

기본 원칙:
- 현재 공개 API 2종 외 새 공개 API를 추가하려면 이 섹션과 ADR에 이유를 남긴다.
- 보호 API는 계속 Bearer token을 요구한다.
- `userId` query parameter를 공개 HTTP 계약에 되살리지 않는다.
- 현재 사용자 전용 response DTO에 `userId`를 되살리지 않는다.

## 데이터/ERD 변경 계획
TBD. 새 테이블, 컬럼, migration, backfill 필요 여부를 작성한다.

기본 원칙:
- 운영 DB migration 정책을 문서화하기 전까지 로컬 공유/데모 기준은 Docker Compose volume 초기화다.
- 선호도 별도 테이블 정규화는 MVP4 확정 범위로 승인되기 전까지 구현하지 않는다.

## 프론트엔드 변경 계획
TBD. 화면, 상태, API client, 타입 변경을 작성한다.

기본 원칙:
- access token 저장 위치는 `sessionStorage`를 유지한다.
- 보호 API 호출 전 로그인 상태를 확인한다.
- 큰 state-management library는 MVP4에서 명시적으로 필요해질 때만 도입한다.

## 추천 규칙 변경 계획
TBD. 점수, tie-break, 추천 이유, 실패 코드 변경을 작성한다.

기본 원칙:
- 현재 총점은 100점이며 `preferenceScore`는 최대 10점이다.
- `styleTags`는 MVP4에서 별도 승인되기 전까지 추천 점수와 추천 이유에 반영하지 않는다.
- today 추천 GET 경로를 현재 API 계약으로 추가하지 않는다.

## 완료 기준
- TBD

## 테스트/검증 기준
- TBD

## 결정해야 할 사항
- MVP4 P0 범위
- API 계약 변경 여부
- DB migration 필요 여부
- 프론트 화면/상태 구조 변경 여부
- 추천 규칙 변경 여부
- Docker Compose 공유 기준 변경 여부
