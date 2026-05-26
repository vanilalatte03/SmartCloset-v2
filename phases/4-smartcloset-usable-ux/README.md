# Phase: SmartCloset 4차 Usable UX MVP

> 상태: 완료된 과거 phase 문서다. 현재 구현 source of truth는 루트 `README.md`와 `docs/` 아래 현재 문서이며, 이 phase/step의 과거 API 또는 범위 표현이 현재 문서와 충돌하면 현재 문서를 우선한다. 완료 phase를 재실행할 때만 당시 step-local 기준으로 참고한다.

## 목표
SmartCloset MVP-3 인증 사용자 baseline을 유지하면서, React 웹앱을 "회원가입 또는 로그인 후 2분 안에 첫 추천 성공" 흐름으로 재구성한다. MVP4는 추천 규칙이나 DB schema를 확장하는 단계가 아니라, Today 중심 앱 셸, 첫 추천 준비 체크리스트, 한국어 라벨/swatch/chip, 옷장 관리, 추천 실패 CTA, 반응형 모바일 UX를 완성하는 단계다.

## 작업 범위
- Must-have / MVP4 P0: 보호 API `GET /api/weather/current`, Today 기본 화면, 현재 날씨 요약, 첫 추천 준비 체크리스트, 추천 생성 CTA, 추천 실패 한국어 CTA, 이유 중심 추천 결과, 옷 등록/수정/보관 UX, desktop sidebar, mobile bottom tab, sticky 주요 CTA
- Should-have / MVP4 P1: 인증 화면 한국어 정리, 선호도 swatch/chip UI, 위치 catalog 검색/선택 polish, 추천 이력과 착용 완료 polish, Today 첫 화면 시각 우선순위, 옷장/선호도/위치/이력 카드 polish, 로딩/빈 상태/저장 성공/인증 만료 문구 정리
- Later / MVP4 P2: 별도 승인 전까지 구현하지 않는다. 이미지 업로드, AI/GPT 추천, PWA/native app, refresh token, 외부 지도/주소 API, 브라우저 현재 위치 자동 감지, Redis, AWS/CD, 선호도 정규화, styleTags scoring

## 제외 범위
- 새 공개 API
- DB schema 변경, migration, backfill script
- 추천 scoring, tie-break, 실패 코드 변경
- KMA `getVilageFcst` 외 weather provider
- refresh token, 소셜 로그인, 이메일 인증, 비밀번호 재설정
- 외부 주소/지도 API, browser geolocation, 위경도-KMA 격자 변환 API
- 이미지 업로드, 쇼핑 추천, AI/GPT 추천
- native mobile app, PWA install/push notification
- Redis, AWS 배포, CD 자동화
- 선호도 별도 테이블 정규화
- `styleTags` 기반 점수 계산 또는 추천 이유

## Steps
| Step | Name | Range |
| ---: | --- | --- |
| 0 | current-weather-api | Must-have / MVP4 P0 |
| 1 | frontend-api-label-foundation | Must-have / MVP4 P0 |
| 2 | responsive-app-shell | Must-have / MVP4 P0 |
| 3 | today-readiness-weather | Must-have / MVP4 P0 |
| 4 | closet-quick-manage | Must-have / MVP4 P0 |
| 5 | recommendation-guidance-ux | Must-have / MVP4 P0 |
| 6 | responsive-polish-and-qa | Must-have / MVP4 P0 |
| 7 | demo-sharing-doc-sync | Must-have / MVP4 P0 |
| 8 | preferences-swatch-chip | Should-have / MVP4 P1 |
| 9 | location-catalog-ux | Should-have / MVP4 P1 |
| 10 | history-worn-ux | Should-have / MVP4 P1 |
| 11 | today-status-recommendation-visual-priority | Should-have / MVP4 P1 |
| 12 | closet-preferences-card-polish | Should-have / MVP4 P1 |
| 13 | location-history-summary-polish-and-qa | Should-have / MVP4 P1 |

## 단계 진행 원칙
- Step 0은 MVP4의 유일한 backend API 추가인 `GET /api/weather/current`만 다룬다. 새 공개 API나 DB 변경을 섞지 않는다.
- Step 1은 frontend API client, 타입, 라벨/swatch/chip/failure CTA mapping foundation만 준비한다. 대규모 화면 재배치는 Step 2 이후에 한다.
- Step 2는 앱 셸과 view 전환 구조를 만든다. Today/Closet 등 각 view의 상세 기능은 후속 step에서 채운다.
- Step 3은 Today 화면의 날씨 요약과 첫 추천 준비 체크리스트를 만든다. 옷 등록 폼 자체를 확장하지 않는다.
- Step 4는 Closet view의 빠른 등록, 수정, 보관 처리만 다룬다.
- Step 5는 추천 생성, 추천 결과 표시, 추천 실패 CTA를 Today 흐름에 연결한다. 추천 도메인 score나 실패 코드는 바꾸지 않는다.
- Step 6은 P0 첫 추천 성공 흐름을 모바일 375px과 desktop 1280px 이상에서 검증하고 CSS polish에 집중한다.
- Step 7은 P0 release candidate 기준 문서, 데모, 공유 검증만 마무리한다. UX 기능을 뒤늦게 새로 추가하지 않는다.
- Step 7까지가 MVP4 P0 release cut이다. Step 8-13은 P1 polish tail이며, P0 완료를 막는 blocker로 보지 않는다.
- Step 8-13은 각 화면 polish를 분리한다. 한 화면의 부족을 다른 화면 step에서 선행 구현해서 해결하지 않는다.
- Step 11은 Today 첫 화면의 시각 우선순위와 상태 바 정보 정리에 집중한다. 추천 규칙, 추천 API, 날씨 API 계약은 변경하지 않는다.
- Step 12는 Closet과 Preferences의 카드형 조작감을 보강한다. 이미지 업로드, 새 enum, 새 API를 추가하지 않는다.
- Step 13은 Location과 History의 정보 위계를 보강하고 최종 반응형 QA를 수행한다. 외부 지도/주소 API나 브라우저 위치 권한 요청을 추가하지 않는다.
- P1 step에서 문서와 실제 구현이 달라지면 해당 step 안에서 필요한 문서만 함께 동기화한다.

## Step PR 리뷰 원칙
- 각 step PR은 해당 step 파일의 작업, 인수 기준, 금지사항을 우선한다.
- 미래 step의 화면이나 polish가 아직 없다는 이유만으로 현재 step PR을 blocker 처리하지 않는다.
- 현재 step이 미래 step 범위를 선행 구현하면 blocker로 본다.
- 리뷰 실패 수정은 현재 step 범위 안에서 해결한다.
- MVP4 P0 기준은 Step 6과 Step 7에서 전체 회귀 기준으로 다시 검증한다.
- Step 8 이후 step은 P1 화면 polish 기준으로 리뷰한다.

## 완료 기준

### P0 Release Cut: Step 7
- 로그인 후 기본 view가 `오늘`이고, 신규 사용자가 다음 행동을 즉시 알 수 있다.
- `GET /api/weather/current`는 보호 API이며 현재 인증 사용자 위치 기준 `WeatherResponse`만 반환하고 추천 결과/이력을 생성하지 않는다.
- 첫 추천 준비 체크리스트가 위치, 선호도 확인/저장, TOP/BOTTOM/OUTER 활성 옷 등록 상태를 보여준다.
- 추천 실패 시 내부 코드만 노출하지 않고 한국어 메시지와 직접 CTA를 보여준다.
- 추천 성공 시 옷 조합과 "오늘 입기 좋은 이유"가 점수 상세보다 먼저 보인다.
- 옷 목록에서 category/color/material이 한국어 라벨, swatch, chip으로 표시된다.
- 옷 등록 폼에 계절/기온 프리셋이 있고, 옷 수정과 보관 처리가 프론트에서 가능하다.
- 데스크톱은 sidebar navigation과 top status bar를 사용한다.
- 모바일 375px에서 하단 탭 `오늘`, `옷장`, `선호도`, `위치`, `이력`과 sticky CTA가 겹치지 않는다.
- 프론트는 `sessionStorage` token 흐름과 현재 API 계약을 유지하고, `userId` query parameter나 today 추천 GET 경로를 호출하지 않는다.

### P1 Polish Tail: Step 8-13
- Preferences view에서 색상 swatch, 소재 chip, style tag 입력/삭제, 저장 상태가 사용자 문장으로 정리되어 있다.
- Location view에서 현재 위치, catalog 검색, 선택 CTA, 인증 만료 처리가 지도/브라우저 현재 위치 없이 동작한다.
- History view에서 최신 추천 이력, 추천 옷 조합, weather snapshot, 착용 여부, 착용 완료 처리가 모바일에서 넘치지 않는다.
- Today view 첫 화면에서 현재 날씨 요약과 추천 생성/결과가 먼저 보이고, 체크리스트는 보조 정보로 낮아져 있다.
- 제품 화면에서 API base URL과 MVP4 라벨이 과하게 노출되지 않고, 개발 정보는 작거나 접힌 형태로 확인할 수 있다.
- Closet view는 옷 카드 그리드와 빠른 등록 패널로 분리되어 있고, 옷 카드는 이미지 없이 category icon/색상 블록/소재 chip/날씨 badge로 식별된다.
- Preferences view는 원형 색상 swatch, 소재 chip, style tag 입력 영역이 분리되어 있고 저장 CTA가 데스크톱/모바일에서 명확히 보인다.
- Location view는 선택된 위치 카드, 현재 날씨 요약 카드, 검색 결과 카드 구조를 사용한다.
- History view는 추천 요약 카드를 먼저 보여주고 상세 이유/날씨/점수는 펼침 영역으로 낮춘다.
- P1 step에서 문서와 실제 구현이 달라진 경우 해당 step 안에서 필요한 문서가 동기화되어 있다.

## 검증 명령
```bash
git diff --check
! rg -n 'GET /api/recommendations/(today)' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
! rg -n -F -e 'POST /api/recommendations?userId' -e '/api/clothes?userId' -e '/api/users/location?userId' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/RECOMMENDATION_RULES.md docs/ERD.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md frontend/src
! rg -n 'navigator\.geolocation|mapbox|kakao|naver.*map|google.*map|recommendations/today' frontend/src
rg -n 'GET /api/weather/current' README.md docs/PRD.md docs/API.md docs/ARCHITECTURE.md docs/FRONTEND.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md docs/COMMANDS.md AGENTS.md .agents/skills/smartcloset-backend/SKILL.md
rg -n '2분 안에 첫 추천 성공|오늘 입기 좋은 이유|하단 탭|색상 swatch|소재 chip' README.md docs/PRD.md docs/FRONTEND.md docs/DEMO_SCENARIO.md docs/SHARING_GUIDE.md
./gradlew test
./gradlew build
(cd frontend && npm run build)
docker compose config
```

Docker Compose smoke까지 확인하는 최종 step에서는 아래를 추가로 실행한다.

```bash
docker compose down -v
test -f .env || cp .env.example .env
docker compose up --build -d
curl -fsS http://localhost:8080/v3/api-docs >/dev/null
curl -fsS http://localhost:5173 >/dev/null
docker compose down
```

## 실행 예시
```bash
python3 scripts/execute.py 4-smartcloset-usable-ux --next-step-only
python3 scripts/execute.py 4-smartcloset-usable-ux
python3 scripts/autopilot.py 4-smartcloset-usable-ux --base main --max-review-fixes 2 --unsafe
```

## 리스크
- MVP4 문서가 이미 UI 목표를 크게 잡고 있어 한 PR에 화면 전체를 갈아엎기 쉽다. step별로 shell, Today, Closet, Recommendation, Preferences, Location, History를 분리한다.
- `GET /api/weather/current`를 추천 생성 shortcut처럼 구현하면 추천 이력/착용 이력이 오염될 수 있다.
- fallback 날씨 `temperature=12`에서는 OUTER가 필요하므로 체크리스트와 데모 seed 흐름에서 OUTER 안내가 빠지면 첫 추천 성공률이 낮아진다.
- 기존 3차 프론트는 panel-grid 중심이라 모바일 앱 셸로 전환할 때 CSS가 크게 흔들릴 수 있다. Step 6에서 375px와 desktop 검증을 별도 수행한다.
- 한국어 라벨 mapping을 컴포넌트마다 중복하면 enum 추가/변경 시 화면별 불일치가 생긴다. Step 1에서 공통 mapping을 먼저 둔다.
- `styleTags`를 사용자 친화적으로 보이게 하다가 추천 점수에 영향을 주는 것처럼 문구를 쓰면 MVP4 범위를 넘는다.
- Step 11-13은 이미 완성된 MVP4 기능 위의 시각 우선순위 polish다. 부족한 시각 표현을 해결하기 위해 새 backend 기능, 이미지 업로드, 지도, AI 문구를 끌어오면 범위를 넘는다.

## 운영 메모
- `archive/`는 구현 source of truth가 아니다.
- 4차 구현 기준은 루트 `README.md`, `docs/`, `docs/adr/009-mvp4-usable-ux.md`, `.agents/skills/smartcloset-backend/SKILL.md`다.
- 문서 충돌 시 `docs/PRD.md`, `docs/API.md`, `docs/RECOMMENDATION_RULES.md`를 우선한다.
- 디자인 시안은 `docs/design/mvp4/README.md`의 사용 원칙에 따라 참고만 한다.
