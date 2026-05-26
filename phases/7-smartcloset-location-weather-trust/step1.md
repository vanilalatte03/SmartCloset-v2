# 단계 1: kma-location-catalog

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/COMMANDS.md`
- `src/main/java/com/smartcloset/location/**`
- `src/main/java/com/smartcloset/user/**`
- `phases/7-smartcloset-location-weather-trust/README.md`

## 작업

- KMA 단기예보 격자 위경도 엑셀을 application resource 또는 생성된 source data로 변환한다.
- `LocationOption`을 MVP7 계약에 맞게 확장한다: code, name, fullName, region1, region2, region3, nx, ny, nullable latitude, nullable longitude.
- `LocationCatalog` 검색을 대표 도시 9개에서 KMA 행정구역 catalog 검색으로 바꾼다.
- `GET /api/locations?keyword={keyword}` 응답을 `LocationOptionResponse` MVP7 shape로 확장한다.
- `일산동` 검색이 복수 후보를 반환하는 테스트를 추가한다.
- 기존 사용자 기본 위치 `SEOUL`, `nx=60`, `ny=127` 동작을 유지한다.

## 인수 기준

```bash
./gradlew test --tests '*Location*'
./gradlew test
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. API 문서의 `LocationOptionResponse`와 실제 DTO가 일치하는지 확인한다.
3. 결과에 따라 `phases/7-smartcloset-location-weather-trust/index.json`의 해당 단계를 업데이트한다.

## 금지사항

- 외부 지도/주소 API client를 추가하지 마라. 이유: MVP7 P0 위치 검색은 내부 KMA catalog 기반이다.
- 브라우저 좌표 resolve API를 이 단계에서 추가하지 마라. 이유: Step 2 범위다.
- 위치 catalog를 사용자별 mutable DB table로 만들지 마라. 이유: MVP7은 application resource로 시작한다.
