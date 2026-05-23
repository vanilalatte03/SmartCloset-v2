# 단계 6: responsive-polish-and-qa

범위: Must-have / MVP4 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/FRONTEND.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `docs/design/mvp4/README.md`
- `frontend/src/App.tsx`
- `frontend/src/App.css`
- `frontend/src/index.css`
- `frontend/src/features/**`

이전 단계에서 만들어진 P0 frontend 화면과 첫 추천 성공 흐름을 꼼꼼히 확인한 뒤 작업하라.

## 작업
MVP4 P0 반응형 UX를 desktop과 mobile에서 마감한다. 기능 추가보다 layout, text overflow, CTA 접근성, loading/empty/error/success 상태, 화면 간 상태 갱신을 검증하고 polish한다.

## 변경 예상 파일
- `frontend/src/App.css`
- `frontend/src/index.css`
- `frontend/src/App.tsx`
- `frontend/src/features/**`
- `frontend/src/components/**`

## 구현 메모
- desktop 1280px 이상에서 sidebar, top status bar, 주요 콘텐츠 grid가 안정적으로 보여야 한다.
- mobile 375px에서 top app bar, single column content, bottom tab, sticky CTA가 겹치지 않아야 한다.
- 버튼, chip, swatch, card 텍스트가 부모 요소를 넘치지 않게 한다.
- 하단 탭 5개 텍스트가 줄바꿈/축약 없이 전문적으로 보이도록 조정한다.
- Today, Closet, 추천 성공/실패 흐름의 loading/empty/error/success 상태 문구를 한국어로 정리한다.
- Preferences, Location, History는 하단 탭과 mount point가 깨지지 않는지 확인하되, 화면별 polish는 Step 8, 9, 10에서 수행한다.
- 인증 만료 시 보호 API 화면들이 auth 흐름으로 일관되게 돌아가야 한다.
- 카드 안에 카드를 중첩한 구조가 있으면 정리한다.
- 과도한 단색 팔레트, 랜딩 hero, 기능 과장 문구가 있으면 제거한다.
- 브라우저 또는 Playwright로 375px와 1280px 이상 screenshot을 반드시 확인하고 step summary에 결과를 남긴다.

## 검증 절차
```bash
git diff --check
! rg -n 'AI|98%|스타일 예보|소셜|비밀번호 찾기|이메일 인증|이미지 업로드|recommendations/today|userId' frontend/src
rg -n '오늘|옷장|선호도|위치|이력|오늘 입기 좋은 이유|상의를 등록|하의를 등록|아우터' frontend/src
(cd frontend && npm run build)
```

수동 확인:

1. desktop 1280px 이상에서 `오늘`, `옷장`, `선호도`, `위치`, `이력`을 탐색한다.
2. mobile 375px에서 하단 탭과 sticky CTA가 겹치지 않는지 확인한다.
3. 신규 사용자로 위치 확인, 선호도 저장, TOP/BOTTOM/OUTER 등록, 추천 생성을 완료한다.
4. TOP/BOTTOM/OUTER 부족 상태에서 추천 실패 CTA가 올바른 view로 이동하는지 확인한다.
5. 옷 수정/보관 후 Today 체크리스트와 추천 후보 상태가 갱신되는지 확인한다.
6. 375px와 1280px 이상 screenshot 확인 결과를 step summary에 남긴다.

## 인수 기준
- desktop과 mobile에서 주요 화면의 UI 요소와 텍스트가 겹치지 않는다.
- 모바일 하단 탭 5개가 항상 접근 가능하다.
- 주요 CTA가 화면 맥락에 맞게 보이고 터치 가능하다.
- 로딩, 빈 상태, 저장 성공, 인증 만료 상태가 한국어 문장으로 표시된다.
- 첫 추천 성공 흐름이 React 앱에서 끊기지 않는다.
- 375px와 1280px 이상 screenshot 확인이 완료되어 있다.
- frontend build가 통과한다.

## 금지사항
- 반응형 polish 단계에서 새 backend API나 DB 변경을 추가하지 마라. 이유: 기능 계약은 이전 step에서 끝나야 한다.
- visual polish를 위해 랜딩 페이지를 만들지 마라. 이유: MVP4 첫 화면은 Today 작업 화면이다.
- 텍스트 크기를 viewport width로 스케일하지 마라. 이유: 작은 화면에서 layout 예측 가능성이 떨어진다.
- UI 문구에 실제 기능보다 큰 기대를 주는 표현을 넣지 마라. 이유: MVP4는 규칙 기반 추천이다.
