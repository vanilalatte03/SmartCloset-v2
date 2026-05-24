# MVP4 브라우저 사용성 테스트 기록

## 문서 목적
이 문서는 SmartCloset MVP4의 핵심 사용자 흐름을 브라우저에서 수동으로 확인하기 위한 QA 기록지다.

이 문서는 구현 기준 문서가 아니다. 제품 범위와 API 계약은 `docs/PRD.md`, `docs/API.md`, `docs/FRONTEND.md`, `docs/DEMO_SCENARIO.md`, `docs/SHARING_GUIDE.md`를 우선한다.

## 메타정보

| 항목 | 값 |
| --- | --- |
| 확인일 | 2026-05-24 |
| 기준 브랜치 | `main` |
| 관련 PR | `#66` |
| 기준 범위 | MVP4 완료 이후 추천 이력 fetch 개선 반영 상태 |
| 테스트 유형 | 브라우저 기반 수동 사용성 확인 + 반응형 자동 점검 |

## 테스트 환경

| 항목 | 값 |
| --- | --- |
| 실행 방식 | Docker Compose |
| Frontend | `http://localhost:5173` |
| Backend | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| 테스트 당시 조건 | Docker Compose 실행 중인 로컬 환경에서 고유 QA 계정을 생성해 확인 |

실제 API key, JWT, 비밀번호, private key는 이 문서에 기록하지 않는다.

## 사전 준비

```bash
docker compose down -v
test -f .env || cp .env.example .env
docker compose up --build
```

이번 실제 확인은 이미 실행 중인 Docker Compose 환경을 사용했다. 기존 데이터 삭제를 피하기 위해 `docker compose down -v`는 수행하지 않았고, 고유 QA 계정으로 사용자 데이터 격리를 확인했다.

확인 전제:

- `http://localhost:5173`에서 React 앱에 접속할 수 있다.
- `http://localhost:8080/v3/api-docs` 또는 Swagger UI에 접속할 수 있다.
- 신규 사용자는 기본 위치 서울특별시 `SEOUL`로 시작한다.
- 프론트 access token 저장 위치는 `sessionStorage`다.
- 현재 날씨 요약 조회는 추천 결과를 생성하거나 추천 이력을 저장하지 않는다.

실제 테스트 계정:

- 주 흐름: `mvp4-qa-20260524-1208@example.com`
- 사용자 격리 확인: `mvp4-qa-isolation-20260524-1427@example.com`
- 반응형 점검: `mvp4-responsive-<timestamp>@example.com`

비밀번호와 JWT는 기록하지 않는다.

## 사용성 체크리스트

| 단계 | 확인 항목 | 기대 결과 | 결과(PASS·FAIL) | 메모 |
| --- | --- | --- | --- | --- |
| 1 | 회원가입 화면 진입 | 한국어 인증 화면이 보이고 소셜 로그인, 비밀번호 찾기, 이메일 인증 UI가 없다. | PASS | 로그인/회원가입 카드만 표시됨. |
| 2 | 신규 사용자 회원가입 | 회원가입 성공 후 access token이 `sessionStorage`에 저장되고 로그인 상태가 된다. | PASS | `MVP4 QA` 계정 생성 후 Today 화면으로 진입. |
| 3 | 로그인 | 기존 계정으로 로그인하면 Today 화면으로 이동한다. | PASS | 로그아웃 후 같은 QA 계정으로 재로그인 성공. |
| 4 | 새로고침 후 세션 복구 | 저장된 token으로 `GET /api/users/me`를 호출해 로그인 상태가 유지된다. | PASS | 브라우저 새로고침 후 사용자/위치/날씨 영역 복구. |
| 5 | Today 기본 진입 | 로그인 후 기본 view가 `오늘`이며 현재 위치, 날씨 요약, 첫 추천 준비 체크리스트가 보인다. | PASS | 신규 사용자는 서울특별시 `SEOUL`, 선호도 0개, 옷장 미등록 상태로 표시됨. |
| 6 | 위치 화면 이동 | 현재 위치가 표시되고 외부 지도나 브라우저 위치 권한 요청 없이 내장 catalog 검색을 사용할 수 있다. | PASS | 브라우저 위치 권한 요청 없음. `부산` 검색으로 catalog 결과 확인. |
| 7 | 위치 선택 | catalog에서 위치를 선택하면 현재 위치와 Today 위치 요약이 갱신된다. | PASS | `부산광역시`, `BUSAN`, `nx=98, ny=76`으로 갱신. 문구 polish 이슈 별도 기록. |
| 8 | 선호도 화면 이동 | 색상은 swatch, 소재와 style tag는 chip 형태로 확인할 수 있다. | PASS | 블랙/네이비 swatch, 면 chip, style tag 입력 UI 확인. |
| 9 | 선호도 저장 | `preferredColors`, `preferredMaterials`, `styleTags` 저장 후 화면에 저장 상태가 반영된다. | PASS | `preferredColors=[BLACK,NAVY]`, `preferredMaterials=[COTTON]`, `styleTags=[캐주얼]` 저장 후 체크리스트 반영. |
| 10 | 옷장 화면 이동 | 현재 사용자 옷 목록만 보이고 category, color, material이 사용자 친화적인 한국어 라벨로 표시된다. | PASS | 상의/하의/아우터 수량과 한국어 라벨, 색상 swatch, 소재 chip 확인. |
| 11 | 상의 등록 | TOP 옷을 등록하면 목록에 추가되고 첫 추천 체크리스트의 상의 항목이 충족된다. | PASS | `그레이 후드` 등록 후 상의 1개 표시. |
| 12 | 하의 등록 | BOTTOM 옷을 등록하면 목록에 추가되고 첫 추천 체크리스트의 하의 항목이 충족된다. | PASS | `블랙 팬츠` 등록 후 하의 1개 표시. |
| 13 | 아우터 등록 | OUTER 옷을 등록하면 목록에 추가되고 첫 추천 체크리스트의 아우터 항목이 충족된다. | PASS | `네이비 코트` 등록 후 아우터 1개 표시. |
| 14 | 옷 수정 | 등록한 옷의 이름, 색상, 소재, 기온 범위 등을 수정할 수 있다. | PASS | 상의 이름을 `그레이 후드 집업`으로 수정 저장했고 카드에 즉시 반영됨. |
| 15 | 옷 보관 | 옷 archive 처리가 가능하고 보관된 옷은 활성 추천 후보에서 제외된다. | PASS | `그레이 후드 집업` 보관 후 상의 활성 수량 0개, 추천 준비 체크리스트 미충족으로 변경. |
| 16 | 추천 실패 CTA 확인 | 부족한 옷이 있을 때 내부 실패 코드 대신 한국어 안내와 직접 CTA가 표시된다. | PASS | 상의 부족 상태에서 `추천을 만들기 전에 해결할 항목이 있어요`, `빠른 등록하기` CTA 표시. 내부 실패 코드는 노출되지 않음. |
| 17 | 추천 생성 | `POST /api/recommendations`로 추천이 생성되고 옷 조합과 오늘 입기 좋은 이유가 먼저 보인다. | PASS | `그레이 셔츠 / 블랙 팬츠 / 네이비 코트` 조합과 이유 목록 표시. 날씨 표시 불일치 이슈 별도 기록. |
| 18 | 추천 점수 확인 | 점수는 보조 정보로 확인 가능하며 `preferenceScore`가 포함되고 `diversityScore`는 보이지 않는다. | PASS | 점수 상세 펼침 후 `선호 반영 10` 확인. `diversityScore` 문구 미노출. |
| 19 | 착용 완료 | `PATCH /api/recommendations/{recommendationId}/worn` 후 착용 상태와 착용 시간이 표시된다. | PASS | `착용 완료 · 5월 24일 오후 02:26` 표시, 최근 추천 미리보기에도 착용 완료 반영. |
| 20 | 착용 완료 재시도 | 같은 추천을 다시 착용 완료 처리해도 중복 착용 이력이 생기지 않는다. | PASS | 착용 완료 후 버튼이 disabled 상태로 바뀌고 이력 목록은 1건 유지. |
| 21 | 추천 이력 조회 | `GET /api/recommendations?limit=20` 기준 최신순 추천 이력이 보인다. | PASS | 이력 화면에 `1개 · 최신순`, 추천 `#6` 표시. |
| 22 | 추천 이력 outfit 확인 | 각 이력 카드에서 TOP, BOTTOM, OUTER 조합과 착용 여부를 확인할 수 있다. | PASS | 상의/하의/아우터 이름, 색상, 소재와 `착용 완료` 상태 확인. |
| 23 | 사용자 격리 | 다른 사용자로 로그인하면 이전 사용자의 옷장, 추천 이력, 착용 이력이 보이지 않는다. | PASS | `격리 QA` 신규 계정에서 옷장 0/0/0, 이력 0개, 기본 선호도 0개 확인. |

## 반응형 확인

| Viewport | 확인 항목 | 기대 결과 | 결과(PASS·FAIL) | 메모 |
| --- | --- | --- | --- | --- |
| 375px | 모바일 하단 탭 | `오늘`, `옷장`, `선호도`, `위치`, `이력` 탭이 겹치지 않는다. | PASS | CDP 점검: Today, Closet, Preferences, Location, History 모두 horizontal overflow 0. |
| 375px | 모바일 주요 CTA | 추천 생성, 옷 등록, 저장 버튼이 화면 밖으로 밀리지 않는다. | PASS | 화면 밖 버튼 0개. 추천 생성, 옷 등록, 선호도 저장 계열 CTA가 viewport 안에 유지됨. |
| 375px | 모바일 카드 액션 | hover 없이 수정, 보관, 착용 완료 액션에 접근할 수 있다. | PASS | 옷장 카드의 수정/보관 버튼이 hover 없이 표시됨. 착용 완료는 추천 수동 흐름에서 disabled 상태까지 확인. |
| 1280px | 데스크톱 앱 셸 | 사이드바와 상단 상태 영역이 콘텐츠와 겹치지 않는다. | PASS | CDP 점검: 5개 화면 모두 horizontal overflow 0, 화면 밖 버튼 0개. |
| 1280px | 데스크톱 작업 흐름 | Today, Closet, Preferences, Location, History 화면 전환이 자연스럽다. | PASS | 실제 수동 테스트와 CDP 화면 전환 모두 정상. |

## 결과 기록

| 항목 | 값 |
| --- | --- |
| 전체 판정 | 조건부 PASS |
| 발견 이슈 | 1. 추천 생성 직후 현재 날씨 패널은 `23°C / 맑음`으로 갱신됐지만 추천 카드와 이력 스냅샷은 `12°C / 흐림`으로 표시되어 동일 사용 흐름 안에서 날씨 정보가 불일치했다. 2. 위치 저장 성공 문구가 `부산광역시(으)로 저장했습니다.`처럼 `로/으로` 조사 선택 표기를 화면에 그대로 노출한다. |
| 후속 작업 | `GET /api/weather/current`와 `POST /api/recommendations`가 사용하는 날씨 소스와 fallback 타이밍을 맞춘다. 위치 저장 성공 문구는 `일산으로`, `서울로`, `부산광역시로`처럼 실제 조사로 확정해 표시한다. |
