# 단계 2: geolocation-resolve-api

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/COMMANDS.md`
- `src/main/java/com/smartcloset/location/**`
- `src/main/java/com/smartcloset/user/**`
- Step 1에서 수정한 위치 catalog 파일

## 작업

- KMA 공식 변환식 기반 `KmaGridConverter`를 추가한다.
- `POST /api/locations/resolve` 보호 API를 추가한다.
- `LocationResolveRequest`와 `LocationResolveResponse`를 API 문서와 일치하게 구현한다.
- latitude/longitude validation을 추가한다.
- resolve 결과는 KMA grid, nearest, candidates를 반환하고 DB에 아무것도 저장하지 않는다.
- `LocationSource` enum을 추가한다: `MANUAL_SEARCH`, `BROWSER_GEOLOCATION`.
- `PUT /api/users/me/location` request에 optional `source`를 추가하고 사용자 위치 source를 저장한다.
- 신규 사용자와 기존 기본 위치 보정의 source 기본값은 `MANUAL_SEARCH`다.

## 인수 기준

```bash
./gradlew test --tests '*Location*'
./gradlew test
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 잘못된 좌표가 `400 INVALID_REQUEST`로 실패하는지 확인한다.
3. resolve API가 사용자 위치를 저장하지 않는지 확인한다.
4. 결과에 따라 `phases/7-smartcloset-location-weather-trust/index.json`의 해당 단계를 업데이트한다.

## 금지사항

- 브라우저 GPS 원문 latitude/longitude를 DB에 저장하지 마라. 이유: 좌표는 후보 찾기 입력으로만 사용한다.
- weather provider를 이 단계에서 수정하지 마라. 이유: Step 3 범위다.
- 위치 후보를 자동 저장하지 마라. 이유: 사용자가 후보를 확인해야 한다.
