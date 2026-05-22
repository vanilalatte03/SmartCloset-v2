# MVP 2 Summary

## 구현된 기능
- 사용자별 위치 snapshot 저장
- 내장 KMA 대표 격자 catalog 조회와 검색
- 사용자 위치 조회/선택 API
- 추천 생성 시 사용자 위치 `nx`, `ny` 기반 KMA `getVilageFcst` JSON 호출
- 위치가 없는 기존 사용자 row의 서울특별시 기본 위치 backfill
- React+Vite+TypeScript SPA
- 프론트 위치 선택, 옷 목록/등록, 추천 생성, 착용 완료 흐름
- Docker Compose `frontend` 서비스 포함 공유 흐름
- README, API, ERD, 아키텍처, 데모, 공유 문서 동기화

## 유지된 기능
- Spring Boot 4.0.6, Java 21, MySQL 기반 백엔드
- 옷 등록/목록/상세/수정/보관 API
- KMA `getVilageFcst` JSON 기반 weather provider
- `StaticWeatherProvider` fallback
- 규칙 기반 추천 점수 100점 체계
- 추천 실패 코드 5종
- 추천 결과 저장과 착용 완료 처리
- Swagger/OpenAPI
- Docker Compose 공유 방식

## 제외된 기능
- 회원가입/로그인과 Spring Security
- 외부 주소/지도 API
- 사용자 현재 위치 자동 감지
- 위경도-KMA 격자 변환 API
- Weather source DB 저장
- Redis 날씨 캐싱
- AI/GPT 추천
- 이미지 업로드
- AWS 배포와 CD 자동화

## 데모 시나리오 요약
- Docker Compose로 MySQL, 백엔드, React 프론트엔드를 함께 실행한다.
- React 앱에서 seed user `1`의 기본 위치 서울특별시를 확인한다.
- 내장 위치 catalog에서 지역을 검색하고 사용자 위치로 저장한다.
- 옷 목록을 확인하고 새 옷을 등록한다.
- 추천을 생성해 weather snapshot, outfit, score breakdown, 추천 이유를 확인한다.
- 추천 결과를 착용 완료 처리하고 이후 추천 이력에 반영되는지 확인한다.
