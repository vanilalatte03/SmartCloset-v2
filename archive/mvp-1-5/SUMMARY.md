# MVP 1.5 Summary

## 구현된 기능
- 기상청 단기예보 조회서비스 `getVilageFcst` JSON 연동
- `KmaVilageForecastWeatherProvider` 기본 weather provider 구성
- `StaticWeatherProvider` fallback 유지
- `KMA_SERVICE_KEY`, `KMA_NX`, `KMA_NY`, `KMA_BASE_URL`, `WEATHER_FALLBACK_ENABLED` 환경변수 기준 정리
- KMA base date/time 계산
- 현재 KST 이후 가장 가까운 forecast group 선택
- `TMP`, `SKY`, `PTY`, `PCP`, `WSD` category를 내부 `WeatherCondition`으로 매핑
- `WEATHER_FALLBACK_ENABLED=false` strict KMA mode
- 추천 생성 API 계약 유지: `POST /api/recommendations?userId={userId}`
- KMA/fallback 통합 테스트와 문서 동기화

## 유지된 기능
- 옷 등록/목록/상세/수정/보관 API
- 규칙 기반 추천 점수 100점 체계
- 추천 실패 코드 5종
- 추천 결과 저장과 착용 완료 처리
- Swagger/OpenAPI
- Spring Boot static resource 기반 최소 Demo UI
- Docker Compose 공유 방식

## 제외된 기능
- 사용자별 위치 저장
- 위치 선택 API
- 정식 프론트엔드 앱
- 외부 주소/지도 API
- Weather source DB 저장
- Redis 날씨 캐싱
- 로그인/회원가입
- AI/GPT 추천
- 이미지 업로드
- AWS 배포와 CD 자동화
