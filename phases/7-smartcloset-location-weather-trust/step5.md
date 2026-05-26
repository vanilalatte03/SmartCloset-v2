# 단계 5: frontend-location-weather-trust-ux

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/API.md`
- `docs/FRONTEND.md`
- `docs/DEMO_SCENARIO.md`
- `docs/COMMANDS.md`
- `frontend/src/types/api.ts`
- `frontend/src/api/smartClosetApi.ts`
- `frontend/src/features/**`
- Step 1-4에서 변경한 API response shape

## 작업

- `frontend/src/types/api.ts`를 MVP7 API 타입으로 갱신한다.
- `smartClosetApi`에 location search, location resolve, location update source, current weather, recommendation forecastPeriod 요청을 반영한다.
- Location view에 동네 검색 결과와 현재 위치로 찾기 흐름을 추가한다.
- 브라우저 Geolocation API 권한 허용/거부/미지원 상태를 처리한다.
- resolve 후보는 자동 저장하지 않고 후보 선택 후 저장하게 한다.
- Today view에 forecastPeriod 선택 control을 추가한다.
- 추천 결과와 현재 날씨 패널에 위치/날씨 source snapshot을 표시한다.
- History view에 추천별 위치/날씨 source snapshot을 표시한다.
- 기존 피드백, 착용 완료, 이미지 blob thumbnail 흐름을 유지한다.

## 인수 기준

```bash
cd frontend && npm run build
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 모바일 375px에서 위치 후보, 현재 위치 버튼, 시간대 선택, source snapshot이 겹치지 않는지 확인한다.
3. 가능하면 브라우저 수동 QA로 권한 허용/거부 흐름을 확인한다.
4. 결과에 따라 `phases/7-smartcloset-location-weather-trust/index.json`의 해당 단계를 업데이트한다.

## 금지사항

- 로그인 전에 보호 API를 호출하지 마라. 이유: 모든 MVP7 위치/날씨 API는 보호 API다.
- 위치 후보를 자동 저장하지 마라. 이유: 사용자가 후보를 확인해야 한다.
- 외부 지도 SDK나 주소 검색 SDK를 추가하지 마라. 이유: MVP7 P0 범위 밖이다.
- 보호 이미지 URL을 일반 public `<img src>`로 직접 참조하지 마라. 이유: 이미지 조회에는 Authorization header가 필요하다.
