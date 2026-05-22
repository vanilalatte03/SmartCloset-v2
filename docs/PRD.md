# PRD: SmartCloset 3차 MVP

## 한 줄 정의
SmartCloset 3차 MVP는 테스트 사용자 기반 서비스를 인증 사용자 기반 서비스로 전환하고, 사용자별 옷장·위치·추천 이력을 분리해 개인화 추천의 기반을 만든다.

## 배경
1차 MVP는 고정 날씨 기반 추천 도메인과 API 계약을 검증했다. 1.5차 MVP는 기상청 단기예보 조회서비스 `getVilageFcst` JSON 연동과 fallback 정책을 추가했다. 2차 MVP는 사용자별 위치 저장, 내장 대표 격자 catalog, React+Vite+TypeScript 프론트엔드 앱을 추가했다.

3차 MVP는 `userId=1` 테스트 흐름에서 벗어나 실제 사용자 단위 서비스처럼 보이는 단계다. 사용자는 회원가입과 로그인을 거쳐 자신의 옷장, 위치, 추천 이력, 착용 완료 이력, 선호도를 관리한다. 공개 API의 `userId` query parameter는 제거하고, 서버는 인증 principal에서 현재 사용자를 식별한다.

## 해결하려는 문제
- 2차까지는 테스트용 `userId` request parameter가 공개 API 계약에 노출되어 실제 사용자 서비스처럼 보이지 않았다.
- 프론트는 고정 `userId=1` 상태로 시작해 사용자별 세션과 로그아웃 흐름을 검증할 수 없었다.
- 옷장, 위치, 추천 결과, 착용 이력은 DB상 사용자별로 분리되어 있었지만 HTTP 계약상 인증 사용자 기준이 아니었다.
- 선호 색상/소재/스타일 태그를 저장할 사용자 프로필이 없어 개인화 추천을 시작할 기반이 부족했다.

## 핵심 사용자 시나리오
1. 사용자는 React 앱에 접속해 회원가입하거나 데모 계정으로 로그인한다.
2. 로그인 성공 시 프론트는 access token을 `sessionStorage`에 저장한다.
3. 프론트는 `GET /api/users/me`로 현재 사용자 정보를 복구하고 보호 API 요청에 `Authorization: Bearer {accessToken}`을 붙인다.
4. 신규 사용자는 기본 위치 서울특별시 `SEOUL`, `nx=60`, `ny=127`과 빈 선호도 배열을 가진다.
5. 사용자는 로그인 후 위치 catalog를 검색하고 자신의 위치로 저장한다.
6. 사용자는 선호 색상, 선호 소재, 스타일 태그를 저장한다.
7. 사용자는 자신의 옷장에 옷을 등록하고 목록을 확인한다.
8. 추천 생성 시 서비스는 인증 사용자의 위치 `nx`, `ny`로 KMA 예보를 조회한다.
9. 서비스는 날씨, 색상 조합, 착용 이력, 추천 이력, 선호 색상/소재를 반영해 코디를 추천한다.
10. 사용자는 추천 결과와 추천 이력을 최신순으로 확인한다.
11. 사용자가 추천 결과를 착용 완료 처리하면 인증 사용자 착용 이력에 반영된다.
12. 로그아웃 시 프론트는 `sessionStorage`의 access token과 사용자 상태를 제거한다.

## 3차 MVP 우선순위

### P0: 인증 사용자 기반 전환
- 회원가입/로그인
- Spring Security 적용
- JWT Bearer access token 인증
- 공개 API와 보호 API 분리
- 공개 HTTP API에서 `?userId=` query parameter 제거
- 현재 사용자 전용 response DTO에서 `userId` 필드 제거
- 옷장, 위치, 추천 생성, 추천 이력, 착용 완료를 인증 사용자 기준으로 전환
- 신규 사용자 기본 위치와 빈 선호도 배열 생성
- 추천 이력 조회 API 추가: `GET /api/recommendations?limit={limit}`
- 선호도 저장/조회 API 추가
- 기존 다양성 점수를 `preferenceScore`로 교체
- React 프론트 로그인/회원가입/로그아웃/토큰 복구 흐름
- Docker Compose 로컬 DB 초기화 권장 정책 문서화

### P1: 인증/개인화 사용성 보강
- 인증 만료 시 프론트 로그인 화면 전환
- 추천 이력 목록 화면 polish
- 선호도 입력 편의 개선
- 보호 API의 401/403 상태 표시 정리
- 프론트와 백엔드 인증 테스트 보강

### P2: 3차 이후 후보
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

## 3차 포함 범위
- Java 21 기반 Spring Boot 4.0.6 백엔드 유지
- Spring Security 기반 인증/인가
- JWT Bearer access token 단일 구조
- BCrypt 비밀번호 hash 저장
- `users.email`, `users.password_hash`, `users.role` 추가
- 사용자 위치 저장과 내장 KMA 대표 격자 catalog 유지
- KMA `getVilageFcst` JSON weather provider 유지
- `StaticWeatherProvider` fallback 유지
- 인증 사용자 기준 옷 관리 API
- 인증 사용자 기준 위치 조회/선택 API
- 인증 사용자 기준 추천 생성/착용 완료 API
- 인증 사용자 기준 추천 이력 조회 API
- 사용자 선호도 저장/조회 API
- 선호 색상/소재 기반 `preferenceScore`
- styleTags 저장/조회/표시
- React+Vite+TypeScript SPA 인증 흐름
- Docker Compose 공유 방식 유지
- 2차 결과의 최소 archive 정리

## 3차 제외 범위
- Refresh token과 token rotation
- 소셜 로그인
- 이메일 인증
- 비밀번호 재설정
- Spring Security 관리자 권한 기능
- 외부 주소/지도 검색 API
- 사용자 현재 위치 자동 감지
- 위경도-KMA 격자 변환 API
- Weather source DB 저장
- 날씨 응답 Redis 캐싱
- 추천 결과 위치 source snapshot 저장
- 선호도 별도 테이블 정규화
- styleTags 기반 개인화 고도화
- 옷 이미지 업로드
- 이미지 자동 분석/태깅
- AI/GPT 추천
- 캘린더 연동
- 쇼핑몰 추천
- AWS 배포
- CD 자동화

## 인증 정책
3차 인증은 Spring Security와 JWT Bearer token으로 구현한다.

- 공개 API는 `POST /api/auth/signup`, `POST /api/auth/login`뿐이다.
- 보호 API는 `Authorization: Bearer {accessToken}` header가 필요하다.
- JWT는 access token 단일 구조로 시작한다.
- refresh token은 3차 범위에서 제외한다.
- 프론트 access token 저장 위치는 `sessionStorage`로 고정한다.
- 새로고침 시 `sessionStorage` token으로 `GET /api/users/me`를 호출해 로그인 상태를 복구한다.
- 로그아웃 시 `sessionStorage` token과 사용자 상태를 제거한다.
- JWT access token은 `HS256`으로 서명하고 `JWT_SECRET`을 사용한다.
- JWT subject는 현재 사용자 id 문자열이며 claims는 `email`, `role`만 둔다.
- JWT access token 만료 시간은 2시간으로 고정한다.
- 비밀번호는 BCrypt hash로 저장한다.
- 기본 role은 `USER`다.

## userId 제거 정책
3차에서 `userId` 제거는 두 가지를 분리한다.

1. Query parameter 제거
   - 공개 HTTP API에서 `?userId=`를 전부 제거한다.
   - Controller는 인증 principal에서 현재 사용자 id를 얻는다.
   - Application service와 repository 내부에서는 구현 편의를 위해 `Long userId`를 계속 사용할 수 있다.
2. Response DTO 제거
   - 현재 사용자 전용 응답에서 `userId` 필드를 제거한다.
   - 대상은 옷, 위치, 선호도, 추천 생성, 추천 이력, 착용 완료 응답이다.

## 위치 정책
3차 위치 선택은 2차와 동일하게 외부 위치 API 없이 서버 내장 대표 격자 catalog를 사용한다. 단, `GET /api/locations`는 보호 API다.

회원가입 화면에서는 위치 catalog를 호출하지 않는다. 신규 사용자는 기본 위치 `SEOUL`로 생성되고, 로그인 후 위치 패널에서 변경한다. `GET /api/locations`의 `401`은 위치 검색 실패가 아니라 인증 만료로 처리한다.

최소 catalog는 아래 지역을 포함한다.

| Code | Name | nx | ny |
| --- | --- | ---: | ---: |
| `SEOUL` | 서울특별시 | 60 | 127 |
| `BUSAN` | 부산광역시 | 98 | 76 |
| `DAEGU` | 대구광역시 | 89 | 90 |
| `INCHEON` | 인천광역시 | 55 | 124 |
| `GWANGJU` | 광주광역시 | 58 | 74 |
| `DAEJEON` | 대전광역시 | 67 | 100 |
| `ULSAN` | 울산광역시 | 102 | 84 |
| `SEJONG` | 세종특별자치시 | 66 | 103 |
| `JEJU` | 제주특별자치도 | 52 | 38 |

## 선호도 정책
선호도는 `users` 테이블의 JSON 문자열 컬럼으로 저장한다.

- `preferred_colors_json`
- `preferred_materials_json`
- `style_tags_json`

API 계약에서는 배열로 주고받는다.

- `preferredColors: []`
- `preferredMaterials: []`
- `styleTags: []`

신규 사용자의 기본 선호도는 모두 빈 배열이다. 선호도 별도 테이블 정규화는 4차 이후 후보로 남긴다.

`styleTags`는 3차에서 저장/조회/표시만 한다. `preferenceScore`와 추천 이유에는 반영하지 않는다.

## 추천 정책
추천 생성 API는 `POST /api/recommendations`만 사용한다. today 추천 GET 경로는 API 계약으로 사용하지 않는다.

추천 총점은 100점 기준을 유지한다.

| Score | Max |
| --- | ---: |
| `weatherScore` | 35 |
| `colorScore` | 25 |
| `wearHistoryScore` | 20 |
| `recommendationHistoryScore` | 10 |
| `preferenceScore` | 10 |

`preferenceScore`는 최대 10점이다.

- 선호 색상/소재가 모두 비어 있으면 0점
- 추천 후보 옷 중 `preferredColors`와 일치하는 색상이 하나 이상 있으면 5점
- 추천 후보 옷 중 `preferredMaterials`와 일치하는 소재가 하나 이상 있으면 5점

## 주요 API

공개 API:

- `POST /api/auth/signup`: 회원가입
- `POST /api/auth/login`: 로그인

보호 API:

- `GET /api/users/me`: 현재 사용자 조회
- `GET /api/locations?keyword={keyword}`: 내장 위치 catalog 조회
- `GET /api/users/me/location`: 현재 사용자 위치 조회
- `PUT /api/users/me/location`: 현재 사용자 위치 선택
- `GET /api/users/me/preferences`: 현재 사용자 선호도 조회
- `PUT /api/users/me/preferences`: 현재 사용자 선호도 저장
- `POST /api/clothes`: 옷 등록
- `GET /api/clothes`: 옷 목록 조회
- `GET /api/clothes/{clothingId}`: 옷 상세 조회
- `PUT /api/clothes/{clothingId}`: 옷 수정
- `PATCH /api/clothes/{clothingId}/archive`: 옷 보관 처리
- `POST /api/recommendations`: 추천 생성
- `GET /api/recommendations?limit={limit}`: 추천 이력 조회
- `PATCH /api/recommendations/{recommendationId}/worn`: 추천 결과 착용 완료 처리

추천 이력 조회 `limit` 정책:

- 기본값 `20`
- 최소 `1`
- 최대 `50`
- 최신순 정렬
- 범위 밖 또는 숫자가 아닌 값은 `400 INVALID_REQUEST`

## Docker Compose DB 전환 정책
MVP 3 전환 시 로컬 Docker Compose DB는 기존 2차 schema/seed data와 충돌할 수 있으므로 초기화를 권장한다.

```bash
docker compose down -v
docker compose up --build
```

운영 DB migration은 3차 문서 범위에서 다루지 않는다. 로컬 공유/데모 기준은 volume 초기화로 정리한다.

## 완료 기준
- 2차 MVP 결과가 `archive/mvp-2/`에 최소 요약으로 정리된다.
- 공개 API와 보호 API가 문서에서 분리된다.
- 공개 API 계약에서 `?userId=`가 제거된다.
- 현재 사용자 전용 response DTO에서 `userId` 필드가 제거된다.
- Spring Security와 JWT Bearer access token 기준이 문서화된다.
- 프론트 access token 저장 위치가 `sessionStorage`로 문서화된다.
- `GET /api/locations`가 보호 API와 로그인 후 위치 선택 흐름으로 문서화된다.
- 기존 다양성 점수가 `preferenceScore`로 교체된다.
- `preferenceScore` 계산 규칙이 5점 색상 + 5점 소재로 고정된다.
- `styleTags`가 점수/추천 이유에 반영되지 않는다고 문서화된다.
- 선호도 저장 방식이 `users` JSON 문자열 컬럼으로 고정된다.
- 추천 이력 조회 limit 정책이 문서화된다.
- DB 초기화 권장 명령이 README, 공유, 데모, 명령 문서에 반영된다.

## 테스트/검증 기준
- 회원가입 성공 시 기본 위치와 빈 선호도 배열이 생성된다.
- 이메일 중복 가입은 실패한다.
- 로그인 성공 시 access token이 반환된다.
- 토큰 없이 보호 API를 호출하면 `401`로 실패한다.
- 유효한 토큰으로 `GET /api/users/me`가 현재 사용자를 반환한다.
- 보호 API는 query parameter `userId` 없이 현재 사용자 기준으로 동작한다.
- 현재 사용자 전용 응답에는 `userId`가 포함되지 않는다.
- 사용자 A token으로 사용자 B의 옷, 위치, 추천, 착용 이력을 조회/수정할 수 없다.
- `GET /api/recommendations`는 기본 20개를 최신순으로 반환한다.
- `GET /api/recommendations?limit=50`은 성공하고, `limit=51`은 `INVALID_REQUEST`로 실패한다.
- 선호 색상/소재가 비어 있으면 `preferenceScore=0`이다.
- 추천 후보에 선호 색상이 하나 이상 있으면 `preferenceScore`에 5점이 반영된다.
- 추천 후보에 선호 소재가 하나 이상 있으면 `preferenceScore`에 5점이 반영된다.
- `styleTags` 변경은 추천 점수와 추천 이유를 바꾸지 않는다.
- Docker Compose 로컬 전환 시 `docker compose down -v` 후 `docker compose up --build`로 데모를 시작할 수 있다.

## 결정된 사항
- Spring Boot 버전은 `4.0.6`으로 고정한다.
- 인증은 Spring Security + JWT Bearer access token으로 구현한다.
- refresh token은 3차 범위에서 제외한다.
- 프론트 access token 저장 위치는 `sessionStorage`로 고정한다.
- 추천 생성 API는 `POST /api/recommendations`를 사용한다.
- today 추천 GET 경로는 사용하지 않는다.
- 외부 Weather API는 기상청 단기예보 `getVilageFcst` JSON만 사용한다.
- 위치 선택은 외부 지도/주소 API 없이 서버 내장 대표 격자 catalog로 구현한다.
- `GET /api/locations`는 보호 API로 유지한다.
- 기존 다양성 점수는 `preferenceScore`로 교체한다.
- `styleTags`는 저장/조회/표시만 하고 추천 점수와 추천 이유에는 반영하지 않는다.
- Docker Compose 공유 방식을 유지한다.
