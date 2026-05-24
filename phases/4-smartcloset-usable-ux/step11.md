# 단계 11: today-status-recommendation-visual-priority

범위: Should-have / MVP4 P1

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/FRONTEND.md`
- `docs/API.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/design/mvp4/README.md`
- `docs/design/mvp4/desktop/today.png`
- `docs/design/mvp4/mobile/today.png`
- `frontend/src/App.tsx`
- `frontend/src/features/today/TodayPanel.tsx`
- `frontend/src/features/recommendation/RecommendationPanel.tsx`
- `frontend/src/components/DisplayTokens.tsx`
- `frontend/src/utils/displayMappings.ts`
- `frontend/src/App.css`

이전 P0/P1 구현과 현재 미커밋 프론트 변경이 있으면 먼저 diff를 읽고, 사용자 변경을 되돌리지 말고 그 위에서 작업하라.

## 작업
Today 첫 화면의 시각 우선순위를 레퍼런스처럼 현재 날씨 요약과 추천 생성/결과 중심으로 재배치하고, 제품 화면에서 개발 상태 정보가 과하게 보이지 않도록 정리한다.

## 변경 예상 파일
- `frontend/src/App.tsx`
- `frontend/src/features/today/TodayPanel.tsx`
- `frontend/src/features/recommendation/RecommendationPanel.tsx`
- `frontend/src/components/DisplayTokens.tsx`
- `frontend/src/App.css`

## 구현 메모
- 데스크톱 top status bar에서 `MVP4` 라벨과 `http://localhost:8080` 같은 API base URL이 주 시각 요소가 되지 않게 한다.
- API 상태는 기본적으로 `API 연결됨`, `확인 중`, `API 오류` 정도의 짧은 badge로 보이게 하고, base URL은 작은 `details/summary`나 보조 텍스트로 낮춘다.
- 모바일 app bar도 제품명, 현재 view, 위치 요약을 우선하고 개발용 API URL을 노출하지 않는다.
- Today layout은 상단에 현재 위치/날씨 요약과 추천 생성/추천 결과 영역이 먼저 오도록 재배치한다.
- 첫 추천 체크리스트는 추천 CTA 아래 또는 우측/하단 보조 카드로 낮춘다.
- 추천이 아직 없을 때는 한 줄 텍스트 대신 상의/하의/아우터 슬롯 3개를 보여준다.
- 빈 추천 슬롯은 이미지 업로드 없이 category label/icon-like glyph, 색상 swatch placeholder, 소재 chip placeholder, 날씨 badge를 조합해 카드처럼 표현한다.
- 추천 성공 결과에서도 상의/하의/아우터가 행 목록처럼 보이지 않고 slot card grid처럼 보이게 한다.
- weather badge는 `WeatherLabel`, rainy/windy text, temperature를 조합한 presentational UI로 만들고 새 API field를 요구하지 않는다.
- 추천 생성 API는 계속 `POST /api/recommendations`만 사용한다.
- 현재 날씨 요약 API는 계속 `GET /api/weather/current`만 사용하고 추천 결과/이력을 만들지 않는다.

## 검증 절차
```bash
git diff --check
! rg -n 'recommendations/today|userId|navigator\.geolocation|mapbox|kakao|naver.*map|google.*map' frontend/src
rg -n 'API 연결됨|오늘 추천|현재 날씨|추천 만들기|상의|하의|아우터' frontend/src/App.tsx frontend/src/features/today frontend/src/features/recommendation frontend/src/App.css
(cd frontend && npm run build)
```

가능하면 로컬 프론트에서 데스크톱 1366px와 모바일 375px 화면을 확인한다:
- Today 첫 화면에서 날씨 요약과 추천 생성/결과가 체크리스트보다 먼저 보인다.
- API base URL이 화면의 주 요소로 보이지 않는다.
- 추천 빈 상태와 추천 결과가 slot card grid로 보이고 텍스트가 버튼/카드 밖으로 넘치지 않는다.
- 모바일에서 sticky 추천 CTA와 하단 탭이 겹치지 않는다.

## 인수 기준
- 제품 화면에서 `MVP4` 라벨과 큰 API URL 노출이 제거되거나 보조/접힘 정보로 낮아져 있다.
- Today 첫 화면의 상단 우선순위가 현재 날씨 요약 + 추천 생성/결과 중심이다.
- 추천 빈 상태가 상의/하의/아우터 slot card, 색상 swatch, 소재 chip, 날씨 badge를 사용한다.
- 추천 성공 결과도 slot card 형식으로 옷 조합을 빠르게 파악할 수 있다.
- 기존 API 계약, DTO, 추천 규칙, sessionStorage 인증 흐름이 유지된다.
- `cd frontend && npm run build`가 통과한다.

## 금지사항
- 추천 scoring, tie-break, 실패 코드를 변경하지 마라. 이유: 이 step은 UI 시각 우선순위 polish다.
- 새 backend API나 DB schema를 추가하지 마라. 이유: MVP4는 기존 API 계약 위에서 동작한다.
- 이미지 업로드 UI, image URL, file metadata를 추가하지 마라. 이유: 이미지 업로드는 MVP4 제외 범위다.
- AI/GPT 추천처럼 보이는 문구를 추가하지 마라. 이유: 추천은 규칙 기반이다.
- API enum 값을 한국어 request 값으로 보내지 마라. 이유: API 계약은 대문자 enum을 유지한다.
