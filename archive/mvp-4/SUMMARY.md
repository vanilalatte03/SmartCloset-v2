# MVP 4 Summary

## 구현된 기능
- 보호 API `GET /api/weather/current`
- 로그인 후 기본 Today view
- 현재 위치와 현재 날씨 요약 표시
- 첫 추천 준비 체크리스트
- 추천 실패 코드의 한국어 안내와 직접 CTA
- 추천 결과의 옷 조합과 "오늘 입기 좋은 이유" 우선 표시
- 추천 결과 착용 완료 처리
- Closet view 카테고리 필터, 빠른 등록, 계절/기온 프리셋
- 옷 수정과 보관 처리 UX
- Preferences view 색상 swatch, 소재 chip, style tag 입력/삭제
- Location view 내장 catalog 검색과 위치 선택 UX
- History view 최신순 이력 카드와 착용 완료 처리
- 데스크톱 sidebar navigation
- 모바일 bottom tab navigation
- Docker Compose 기반 공유 smoke 확인

## 유지된 기능
- Spring Boot 4.0.6, Java 21, MySQL 기반 백엔드
- Spring Security + JWT Bearer access token
- 공개 API `POST /api/auth/signup`, `POST /api/auth/login`
- 인증 사용자 기준 옷장/위치/선호도/추천 이력/착용 이력 격리
- KMA `getVilageFcst` JSON 기반 weather provider
- `StaticWeatherProvider` fallback
- 내장 KMA 대표 격자 catalog
- 규칙 기반 추천 점수 100점 체계
- `preferenceScore`
- React+Vite+TypeScript 프론트엔드
- `sessionStorage` token 저장
- Swagger/OpenAPI
- Docker Compose 공유 방식

## 제외된 기능
- 옷 이미지 업로드
- AI/GPT 추천
- AI 자동 태깅
- refresh token
- 소셜 로그인
- 이메일 인증
- 비밀번호 재설정
- 외부 지도/주소 API
- 브라우저 현재 위치 자동 감지
- Redis
- AWS 배포와 CD 자동화
- PWA/native app 출시
- 선호도 별도 테이블 정규화
- styleTags 점수 반영

## 데모 시나리오 요약
- Docker Compose로 MySQL, 백엔드, React 프론트엔드를 함께 실행한다.
- React 앱에서 회원가입 또는 로그인을 수행한다.
- Today view에서 위치, 현재 날씨, 첫 추천 준비 체크리스트를 확인한다.
- 선호도를 확인하거나 저장한다.
- TOP, BOTTOM, OUTER를 등록한다.
- 추천을 생성하고 옷 조합과 추천 이유를 확인한다.
- 추천 결과를 착용 완료 처리한다.
- History view에서 추천 이력과 착용 상태를 확인한다.
