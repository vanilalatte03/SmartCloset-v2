# MVP 3 Summary

## 구현된 기능
- 회원가입/로그인 API
- Spring Security 기반 보호 API 경계
- JWT Bearer access token 발급과 검증
- 현재 사용자 조회 API
- 공개 HTTP API의 `userId` query parameter 제거
- 현재 사용자 전용 응답 DTO의 `userId` 필드 제거
- 사용자별 옷장, 위치, 추천 이력, 착용 이력 격리
- `users` 테이블 JSON 문자열 컬럼 기반 선호도 저장
- 선호도 조회/저장 API
- 기존 다양성 점수를 `preferenceScore`로 교체
- 추천 이력 조회 API와 `limit` 정책
- React 인증 세션, `sessionStorage` token 저장, 새로고침 후 세션 복구
- React 위치/선호도/옷장/추천/추천 이력 흐름 연결
- Docker Compose `mysql`, `app`, `frontend` 공유 smoke 검증

## 유지된 기능
- Spring Boot 4.0.6, Java 21, MySQL 기반 백엔드
- KMA `getVilageFcst` JSON 기반 weather provider
- `StaticWeatherProvider` fallback
- 내장 KMA 대표 격자 catalog
- 규칙 기반 추천 점수 100점 체계
- 추천 실패 코드 5종
- 추천 결과 저장과 착용 완료 처리
- React+Vite+TypeScript 프론트엔드
- Swagger/OpenAPI
- Docker Compose 공유 방식

## 제외된 기능
- refresh token
- 소셜 로그인
- 이메일 인증
- 비밀번호 재설정
- 외부 주소/지도 API
- 사용자 현재 위치 자동 감지
- Redis 캐싱
- 이미지 업로드
- AI/GPT 추천
- AWS 배포와 CD 자동화
- 선호도 별도 테이블 정규화
- styleTags 점수 반영

## 데모 시나리오 요약
- Docker Compose로 MySQL, 백엔드, React 프론트엔드를 함께 실행한다.
- React 앱에서 회원가입 또는 로그인을 수행한다.
- 로그인 성공 후 access token이 `sessionStorage`에 저장되는지 확인한다.
- 현재 사용자 위치를 조회하고 내장 catalog에서 지역을 선택한다.
- 선호 색상, 선호 소재, styleTags를 저장한다.
- 옷을 등록하고 현재 인증 사용자 기준 목록을 확인한다.
- 추천을 생성해 weather snapshot, outfit, score breakdown, 추천 이유를 확인한다.
- 추천 이력을 최신순으로 조회한다.
- 추천 결과를 착용 완료 처리하고 중복 처리되지 않는지 확인한다.
