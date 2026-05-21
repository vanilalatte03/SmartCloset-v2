# MVP 1 Changelog

## 2026-05-21
- 1차 MVP 문서를 1.5차 KMA 날씨 API 기준으로 전환했다.
- 1차 MVP 전체 문서 복사본을 남기지 않고, 과거 맥락 확인용 최소 archive 요약으로 정리했다.

## MVP 1 종료 시점 주요 변경
- Spring Boot 4.0.6, Java 21, Gradle 기반 백엔드 구조를 확정했다.
- User, Clothing, Recommendation, Weather 도메인 경계를 정리했다.
- 옷 등록/조회/상세/수정/보관 API를 제공했다.
- 고정 날씨 기반 추천 생성 API와 추천 결과 착용 완료 API를 제공했다.
- 추천 점수 100점 구조, 추천 실패 코드, 결정적 tie-break 규칙을 문서화했다.
- RecommendationResult, RecommendationResultItem, WearHistory 저장 구조를 정리했다.
- Swagger/OpenAPI, Docker Compose, 최소 Demo UI 공유 흐름을 마련했다.
