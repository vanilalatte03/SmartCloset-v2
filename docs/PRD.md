# PRD: SmartCloset MVP9 프론트 UI/UX 리디자인

## 문서 목적

이 문서는 SmartCloset MVP9의 확정 범위를 정의한다. MVP9는 MVP8 계정 안정성 완료 baseline 위에서 `tmp/design-preview`와 `docs/design/mvp9/` 화면 시안을 강하게 참고해 프론트 UI/UX 완성도를 높인다.

현재 코드 출발점은 MVP8 구현 완료 상태다. MVP9 구현 기준은 이 문서와 `docs/` 아래 현재 문서, ADR-014다.

원래 MVP9 후보였던 AWS 배포는 후속 MVP로 연기한다.

## 문서 책임

| 계약 영역 | Source of truth |
| --- | --- |
| HTTP endpoint, request/response DTO, 인증/에러 계약 | `docs/API.md` |
| 추천 후보, 점수, 추천 이유, 예보 시간대 입력 | `docs/RECOMMENDATION_RULES.md` |
| 백엔드 구조, transaction, adapter 정책 | `docs/ARCHITECTURE.md` |
| DB schema와 JPA/entity 기준 | `docs/ERD.md` |
| 프론트 API client, 타입, UX, 반응형 기준 | `docs/FRONTEND.md` |
| MVP9 디자인 reference | `docs/design/mvp9/README.md` |
| 데모/공유 검증 | `docs/DEMO_SCENARIO.md`, `docs/SHARING_GUIDE.md` |
| 결정 배경 | `docs/ADR.md`, `docs/adr/014-mvp9-ui-ux-redesign.md`, `docs/adr/015-closet-archive-restore.md` |

## MVP9 한 줄 정의

SmartCloset의 기능은 유지하면서 사용자가 실제 서비스처럼 느낄 수 있도록 Auth, 추천, 옷장, 내 취향, 위치, 기록, 계정 설정 화면을 리디자인한다.

## 목표

- 로그인 전후 첫인상을 완성도 있는 제품 화면으로 바꾼다.
- 추천 화면에서 날씨, 위치, 상황, 예보 시간대, 옷장 준비 상태, 최근 이력을 한 번에 스캔하게 한다.
- 옷장과 추천 결과에서 실제 옷 이미지를 중심으로 조합을 빠르게 이해하게 한다.
- 선호 색상, 소재, style tag 입력을 swatch/chip/toggle 중심으로 더 직관적으로 만든다.
- 위치 검색과 현재 위치 후보 찾기 흐름을 지도 없이도 명확하게 만든다.
- 기록 화면에서 과거 추천과 착용 피드백을 이미지 중심으로 확인하게 한다.
- 계정 설정은 primary nav가 아니라 profile pill/menu에서 자연스럽게 진입하게 한다.
- 데스크톱 1440px, 모바일 390px 기준으로 텍스트, CTA, 카드가 겹치거나 잘리지 않게 한다.

## 현재 Baseline

- Spring Security + JWT Bearer access token 인증을 사용한다.
- DB-backed refresh session과 HttpOnly refresh cookie를 사용한다.
- Frontend는 access token을 memory state에 저장하고 refresh cookie로 세션을 복구한다.
- Password signup은 이메일 인증 필요 상태를 반환하고 access token을 발급하지 않는다.
- 미인증 password 계정 login은 실패한다.
- Google provider 상태와 Google login flow를 유지한다.
- 계정 삭제는 현재 사용자 소유 데이터와 이미지 파일을 즉시 hard delete한다.
- 공개 HTTP API는 `userId` query parameter를 받지 않는다.
- 현재 사용자 전용 응답 DTO는 `userId`를 노출하지 않는다.
- 사용자 소유 옷장, 위치, 선호도, 추천 이력, 착용 이력, 추천 피드백은 인증 사용자별로 분리한다.
- 추천 생성 API는 `POST /api/recommendations`다.
- 추천 이력 조회 API는 `GET /api/recommendations?limit={limit}`이며 기본 20, 최소 1, 최대 50, 최신순이다.
- 현재 날씨 요약 API는 `GET /api/weather/current`이며 보호 API다.
- MVP5 이미지 API, MVP6 피드백/개인화, MVP7 위치/날씨 source snapshot, MVP8 계정 안정성 기능은 유지한다.
- Docker Compose local 공유 흐름을 유지한다.

## 해결하려는 문제

- 기능은 갖췄지만 화면 구조와 시각 위계가 아직 프로덕트처럼 단단하게 느껴지지 않는다.
- 데스크톱 sidebar와 상태 bar 중심 구조가 사용자의 핵심 작업보다 개발/상태 정보를 먼저 보여준다.
- 모바일에서 화면별 핵심 CTA와 카드 정보가 더 명확해야 한다.
- 추천 결과에서 옷 조합, 추천 이유, 점수 상세, 준비 상태가 더 자연스럽게 연결되어야 한다.
- 옷장, 취향, 위치, 기록 화면의 입력 control과 정보 밀도가 화면마다 다르게 느껴진다.
- 계정 설정이 primary nav에 들어가 있어 핵심 사용 흐름과 설정 흐름의 위계가 분리되지 않는다.
- AWS 배포보다 먼저 사용자 경험의 완성도를 올리는 편이 서비스 품질에 더 직접적이다.

## 핵심 사용자 시나리오

1. 사용자가 Auth 화면에 접속해 완성도 있는 visual background와 명확한 form을 본다.
2. 사용자가 로그인하거나 회원가입/이메일 인증/비밀번호 재설정/Google provider 상태를 확인한다.
3. 로그인 후 `추천` 화면에서 오늘 날씨, 위치, 상황, 예보 시간대, 추천 조합을 한 화면에서 본다.
4. 사용자가 상황과 예보 시간대를 바꿔 추천을 다시 생성한다.
5. 사용자가 추천 결과의 옷 이미지와 "오늘 입기 좋은 이유"를 먼저 확인하고 필요하면 점수 상세를 본다.
6. 사용자가 `옷장`에서 이미지 중심 목록을 보고 새 옷을 등록한다.
7. 사용자가 `내 취향`에서 색상 swatch, 소재 toggle, style tag chip을 수정한다.
8. 사용자가 `위치`에서 동네 검색 또는 현재 위치 후보 찾기로 저장 위치를 바꾼다.
9. 사용자가 `기록`에서 과거 추천과 착용 피드백을 이미지 중심으로 확인한다.
10. 사용자가 우측 상단 profile pill/menu에서 계정 설정으로 이동해 세션, 로그인 방법, 계정 삭제를 관리한다.

## MVP9 우선순위

### P0: 문서 전환과 디자인 기준 고정

- MVP8은 `archive/mvp-8/`에 최소 요약으로 이동한다.
- 현재 baseline은 MVP9 UI/UX 리디자인으로 전환한다.
- ADR-014를 추가하고 `docs/ADR.md`에 연결한다.
- `tmp/design-preview` 이미지를 `docs/design/mvp9/`에 보관하고 사용 원칙을 문서화한다.

### P0: App shell과 Auth redesign

- 데스크톱 primary nav는 상단 탭으로 둔다.
- 모바일 primary nav는 하단 탭으로 둔다.
- primary nav는 `추천`, `옷장`, `내 취향`, `위치`, `기록`으로 고정한다.
- `계정 설정`은 우측 상단 profile pill/menu에서 진입한다.
- Auth view는 full-bleed visual과 중앙 form 구조를 참고하되 기존 MVP8 auth flow를 유지한다.

### P0: Recommendation dashboard

- 추천 화면은 `smartcloset-recommend-mockup.png`를 강하게 참고한다.
- 추천 생성 조건, 날씨/위치, 준비 상태, 추천 결과, 점수 상세, 최근 이력을 dashboard로 구성한다.
- 추천 결과는 옷 이미지와 추천 이유를 먼저 보여주고 점수 상세는 보조 정보로 둔다.
- 추천 상황과 예보 시간대는 명확한 segmented/card control로 표시한다.

### P0: Closet, Preferences, Location, History redesign

- Closet 목록과 등록/수정은 이미지 중심 카드와 form preview를 사용한다.
- Preferences는 색상 swatch, 소재 toggle, style tag chip 중심으로 구성한다.
- Location은 동네 검색, 현재 위치 후보 찾기, 좌표 미저장 안내를 분리한다.
- History는 calendar/timeline과 outfit image grouping을 사용한다.

### P0: Account settings와 responsive polish

- Account settings는 profile에서 열린 설정 화면처럼 보이게 한다.
- 이메일 인증 상태, 로그인 제공자, 세션 상태, 계정 삭제 위험 영역을 분리한다.
- 1440px 데스크톱과 390px 모바일에서 화면 겹침, CTA 잘림, 입력 overflow를 확인한다.

## 포함 범위

- `archive/mvp-8/` 최소 요약
- ADR-014
- `docs/design/mvp9/` reference
- MVP9 phase 문서와 docs-check 규칙
- Frontend app shell navigation 변경
- Auth view visual redesign
- Recommendation dashboard redesign
- Closet list/add/edit UX redesign
- Preferences swatch/chip/toggle UX redesign
- Location search/current-location UX redesign
- History timeline/calendar UX redesign
- Account settings profile entry와 responsive polish
- MVP9 수동 QA 문서화

## 제외 범위

- AWS 배포 구현
- S3 storage 구현체
- SES/SMTP 실제 발송 구현체
- Secrets Manager
- CD 자동화
- Redis
- 백엔드 API/DTO 변경
- DB schema 변경
- 추천 점수/필터/tie-break 변경
- AI/GPT 추천
- AI 자동 태깅
- native mobile app 또는 PWA 배포
- 외부 지도/주소 API
- 다중 이미지 업로드
- 이미지 편집/cropping/resizing pipeline

## 완료 기준

- 현재 문서 baseline이 MVP9 UI/UX 리디자인과 ADR-014를 가리킨다.
- MVP8 계정 안정성은 archive에 최소 요약으로만 남는다.
- `docs/design/mvp9/` reference가 구현 기준으로 문서화된다.
- 데스크톱 primary nav는 `추천`, `옷장`, `내 취향`, `위치`, `기록` 상단 탭이다.
- 모바일 primary nav는 같은 5개 탭의 하단 navigation이다.
- 계정 설정은 profile pill/menu에서 진입한다.
- Auth, 추천, 옷장, 내 취향, 위치, 기록, 계정 설정 화면이 디자인 reference 방향을 따른다.
- MVP8 세션 복구, 이메일 인증, 비밀번호 재설정, Google provider 상태, 계정 삭제 UX가 유지된다.
- MVP9 자체에서는 백엔드 HTTP API, DTO, DB schema, 추천 점수/필터/tie-break가 변경되지 않는다. 이후 옷장 보관함 복원 API 확장은 ADR-015를 따른다.
- 1440px 데스크톱과 390px 모바일에서 텍스트, CTA, 카드가 겹치거나 잘리지 않는다.
- AWS/S3/SES/Secrets Manager/CD/Redis 구현이 포함되지 않는다.

## 테스트/검증 기준

문서 전환 검증:

- `git diff --check`
- `python3 scripts/checks.py --docs-check-config phases/9-smartcloset-ui-ux-redesign/docs-checks.json --docs-check`

MVP9 구현 phase 검증:

- `git diff --check`
- `cd frontend && npm run build`
- `python3 scripts/checks.py --docs-check-config phases/9-smartcloset-ui-ux-redesign/docs-checks.json --docs-check`

수동 QA:

- 데스크톱 1440px: Auth, 추천, 옷장, 내 취향, 위치, 기록, 계정 설정
- 모바일 390px: Auth, 추천, 옷장, 내 취향, 위치, 기록, 계정 설정

## 결정 완료 사항

- MVP9 범위: 프론트 UI/UX 리디자인
- AWS 배포: 후속 MVP로 연기
- 디자인 기준: `tmp/design-preview`와 `docs/design/mvp9/`
- Primary nav: `추천`, `옷장`, `내 취향`, `위치`, `기록`
- 계정 설정 진입: 우측 상단 profile pill/menu
- API/DB/추천 규칙: MVP8 계약 유지, MVP9에서 변경하지 않음
