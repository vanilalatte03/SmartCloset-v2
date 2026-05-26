# MVP 6 Summary

## 구현된 기능

- 추천 상황 선택: `WORK`, `CASUAL`, `WORKOUT`, `DATE`, `FORMAL`
- body 없는 `POST /api/recommendations`의 기본 상황 `CASUAL`
- 추천 결과의 situation snapshot 저장과 응답
- 옷 등록/수정/조회 DTO의 `styleTags`
- 기본 옷 프리셋 styleTags
- 추천 결과 outfit item styleTags 반환
- 추천 피드백 PUT API
- 피드백 전체 교체, 누락 필드 null 처리, clear
- 추천 이력의 `wornAt`과 `feedback`
- 최근 14일 피드백 기반 `preferenceScore`
- 사용자 선호/옷별/상황별 styleTags 점수 반영
- 추천 이유의 상황/styleTags/피드백 문구
- Today/Closet/History 프론트 UX

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
- Docker Compose 공유 흐름

## 제외된 기능

- AI/GPT 추천
- AI 자동 태깅
- 피드백 이벤트 로그 분석 플랫폼
- preference normalization table 분리
- 쇼핑 추천
- refresh token
- social login
- Redis
- 외부 지도/주소 API
- AWS 배포와 CD 자동화

## 데모 시나리오 요약

- Docker Compose로 MySQL, 백엔드, React 프론트엔드를 함께 실행한다.
- React 앱에서 회원가입 또는 로그인을 수행한다.
- Closet에서 옷별 styleTags를 저장하고 확인한다.
- Today에서 상황을 선택해 추천을 생성한다.
- 추천 결과에서 착용 완료와 피드백 저장/clear를 확인한다.
- History에서 상황, 착용 여부, 착용 시각, 피드백 상태를 확인한다.
