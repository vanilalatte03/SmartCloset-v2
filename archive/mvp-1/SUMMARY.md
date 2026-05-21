# MVP 1 Summary

## 구현된 기능
- Spring Boot 4.0.6 기반 SmartCloset 백엔드
- Seed user 기준 옷 등록, 목록, 상세, 수정, 보관 처리 API
- `StaticWeatherProvider` 고정 날씨 기반 추천 생성
- TOP, BOTTOM, OUTER 조합 후보 생성과 100점 규칙 기반 점수 계산
- 추천 결과 저장, 추천 이유 반환, 착용 완료 처리
- Swagger/OpenAPI, Docker Compose 공유, Spring Boot static resource 기반 최소 Demo UI
- Harness step, 자동 PR 루프, GitHub Actions test/build 기준

## 제외된 기능
- 외부 Weather API 실제 연동
- 사용자별 위치 저장과 위치 변경 API
- 로그인/회원가입과 Spring Security
- AI/GPT 추천, 이미지 업로드, Redis, AWS 배포, CD 자동화
- 정식 프론트엔드 앱, 쇼핑몰 추천, 관리자 기능

## 데모 시나리오 요약
- Docker Compose로 애플리케이션과 MySQL을 함께 실행한다.
- Swagger 또는 최소 Demo UI에서 seed user `1` 기준으로 옷 목록을 확인한다.
- 새 옷을 등록하고 목록과 추천 후보에 반영되는지 확인한다.
- 추천을 생성해 날씨, outfit, 점수 breakdown, 추천 이유를 확인한다.
- 추천 결과를 착용 완료 처리하고 이후 추천 이력에 반영되는지 확인한다.
- 옷 보관이나 후보 부족 상태에서 추천 실패 코드가 반환되는지 확인한다.
