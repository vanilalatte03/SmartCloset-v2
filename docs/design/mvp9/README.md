# MVP9 Design References

이 디렉터리는 MVP9 프론트 UI/UX 리디자인 구현 시 참고할 디자인 시안을 보관한다.

원본은 `tmp/design-preview`에서 가져왔고, 구현 기준으로 유지하기 위해 이 디렉터리에 복사했다.

디자인 시안은 구현 참고 자료이며 최종 source of truth는 `docs/PRD.md`, `docs/FRONTEND.md`, `docs/API.md`, `docs/RECOMMENDATION_RULES.md`, `docs/adr/014-mvp9-ui-ux-redesign.md`다. 시안에 API 계약, 추천 규칙, DB 구조, 금지 범위가 문서와 충돌하면 문서를 우선한다.

## 사용 원칙

- 화면 구조, 정보 밀도, 주요 CTA 배치, 이미지 중심 카드 리듬을 강하게 참고한다.
- 백엔드 HTTP API, DTO, DB schema, 추천 규칙은 바꾸지 않는다.
- MVP8 세션 정책과 계정 안정성 UX는 유지한다.
- 데스크톱은 상단 탭, 모바일은 하단 탭 navigation을 사용한다.
- primary nav는 `추천`, `옷장`, `내 취향`, `위치`, `기록`으로 고정한다.
- `계정 설정`은 우측 상단 profile pill/menu에서 진입한다.
- 시안의 mock data, 날짜, 이메일, 특정 지역명은 예시일 뿐이다.
- "코디 추천" 같은 사용자 친화적 표현은 사용할 수 있지만 AI/GPT 추천처럼 보이게 하지 않는다.
- 지도, 외부 주소 API, AWS/S3/SES 운영 UI는 추가하지 않는다.

## References

| File | Usage |
| --- | --- |
| `smartcloset-auth-mockup.png` | Auth view의 full-bleed editorial image, 중앙 form, Google disabled/enabled 상태 참고 |
| `smartcloset-recommend-mockup.png` | 추천 dashboard의 hero, 상황/예보 선택, 추천 결과, 점수, 준비 상태, 최근 이력 참고 |
| `smartcloset-closet-list-mockup.png` | 옷장 목록의 이미지 카드, 필터 chip, count summary, 모바일 list 참고 |
| `smartcloset-closet-add-mockup.png` | 옷 등록/수정 form, 이미지 업로드, 온도 범위, style tag 입력 참고 |
| `smartcloset-preferences-mockup.png` | 색상 swatch, 소재 toggle, style tag chip, 추천 영향 표시 참고 |
| `smartcloset-location-mockup.png` | 위치 검색, 현재 위치 후보 찾기, 좌표 미저장 안내 참고 |
| `smartcloset-history-mockup.png` | 기록 calendar/timeline, outfit image grouping, feedback tag 참고 |
| `smartcloset-account-mockup.png` | Profile 진입 계정 설정, 세션 상태, 로그인 방법, 계정 삭제 layout 참고 |
| `auth-london-editorial.png` | Auth 배경 이미지 후보 |
| `smartcloset-mvp8-complete-mockup.html` | 전체 화면 흐름을 한 HTML에서 확인하는 참고 자료 |
| `smartcloset-other-screens-mockups.html` | 보조 화면 HTML 참고 자료 |
| `smartcloset-ux-redesign-mockup.html` | 초기 UX redesign concept 참고 자료 |

## 구현 시 반영할 수정 결정

- 추천 화면은 로그인 후 기본 view다.
- 화면 상단의 개발용 status bar 노출을 줄이고, 사용자에게 필요한 위치/세션/계정 상태만 profile 또는 보조 panel로 이동한다.
- 추천 결과는 점수표보다 "오늘 입기 좋은 이유"와 옷 조합을 먼저 보여준다.
- 옷 이미지가 있으면 목록, 추천 결과, 이력에서 우선 표시하고, 없으면 기존 fallback visual을 사용한다.
- 색상은 swatch, 소재는 toggle 또는 chip, style tag는 chip 입력으로 표시한다.
- 위치 화면은 지도 없이 동네 검색과 브라우저 좌표 resolve 후보 선택만 제공한다.
- 계정 삭제는 위험 영역으로 분리하고 확인 문구와 현재 비밀번호 입력 조건을 유지한다.
- 모든 button/card/control은 390px 모바일 폭에서 텍스트가 겹치거나 잘리지 않아야 한다.
