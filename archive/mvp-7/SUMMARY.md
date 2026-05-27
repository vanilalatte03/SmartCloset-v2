# MVP 7 Summary

## 구현된 기능

- KMA 행정구역 catalog 기반 동네 단위 위치 검색
- `일산동` 같은 동명이인 후보 구분 표시
- 브라우저 Geolocation 좌표를 서버에서 KMA grid와 가까운 후보로 resolve
- 브라우저 GPS 원문 좌표 DB 미저장
- 위치 저장 source: `MANUAL_SEARCH`, `BROWSER_GEOLOCATION`
- 추천 요청 `forecastPeriod`: `CURRENT`, `MORNING`, `AFTERNOON`, `EVENING`
- `GET /api/weather/current`의 location/source metadata
- 추천 결과와 이력의 위치/날씨 source snapshot 저장
- KMA 사용 여부, fallback 여부, base date/time, forecast date/time 표시
- Location/Today/History 프론트 UX
- Docker Compose smoke와 브라우저 QA 기록

## 유지된 기능

- Spring Boot 4.0.6, Java 21, MySQL
- Spring Security + JWT Bearer access token
- 공개 API `POST /api/auth/signup`, `POST /api/auth/login`
- 인증 사용자 기준 옷장/위치/선호도/추천 이력/착용 이력 격리
- KMA `getVilageFcst` JSON weather provider와 fallback weather
- React+Vite+TypeScript 프론트엔드
- `sessionStorage` token 저장
- 규칙 기반 추천 점수 100점 체계
- MVP5 이미지 업로드/교체/조회/삭제와 썸네일 표시
- MVP6 추천 상황, 옷별 `styleTags`, 추천 피드백, 최근 피드백 기반 `preferenceScore`
- Docker Compose 공유 흐름

## 제외된 기능

- refresh token
- social login
- email verification
- password reset
- Redis
- AWS 배포와 CD 자동화
- S3/CDN 이미지 hosting
- 외부 지도/주소 API
- raw KMA 응답 JSON 저장
- GPS 좌표 원문 DB 저장
- AI/GPT 추천
- AI 자동 태깅

## 데모 시나리오 요약

- Docker Compose로 MySQL, 백엔드, React 프론트엔드를 함께 실행한다.
- React 앱에서 회원가입 또는 로그인을 수행한다.
- Location에서 `일산동` 검색과 현재 위치 후보 찾기를 확인한다.
- Today에서 상황과 예보 시간대를 선택해 추천을 생성한다.
- 추천 결과에서 위치/source snapshot, KMA/fallback, base/forecast 시각을 확인한다.
- History에서 과거 추천의 위치/날씨 snapshot이 현재 위치 변경과 독립적으로 유지되는지 확인한다.
