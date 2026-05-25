# MVP 4 Decisions

MVP 4의 상세 결정 기록은 현재 `docs/adr/`에 유지한다. 이 문서는 주요 결정만 요약한다.

## 주요 결정
- MVP4는 백엔드 추천 규칙 확장이 아니라 반응형 실사용 UX MVP로 정의했다.
- 자세한 내용은 ../../docs/adr/009-mvp4-usable-ux.md 를 따른다.
- 로그인 후 기본 화면은 Today view로 정했다.
- Today view에서 현재 사용자 위치 기준 날씨를 보여주기 위해 보호 API `GET /api/weather/current`를 추가했다.
- `GET /api/weather/current`는 추천 결과나 추천 이력을 생성하지 않는 조회 API로 고정했다.
- 데스크톱은 sidebar navigation, 모바일은 bottom tab navigation을 사용했다.
- 첫 추천 준비 체크리스트는 위치, 선호도 확인/저장, 상의, 하의, 아우터 상태를 보여주도록 했다.
- 추천 실패 코드는 API 계약으로 유지하고, 프론트에서 한국어 메시지와 CTA로 변환했다.
- 추천 결과는 점수 상세보다 옷 조합과 "오늘 입기 좋은 이유"를 먼저 보여주도록 했다.
- 옷 등록에는 UI helper인 계절/기온 프리셋을 추가하되 서버 enum이나 DB schema는 바꾸지 않았다.
- MVP4에서는 공개 API, DB schema, 추천 점수 계산, 새 weather provider를 추가하지 않았다.
- 이미지 업로드, AI/GPT 추천, PWA/native app, 외부 location provider는 MVP5 이후 후보로 넘겼다.
