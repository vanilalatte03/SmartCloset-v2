# 단계 8: preferences-swatch-chip

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
- `frontend/src/features/preferences/PreferencesPanel.tsx`
- `frontend/src/api/smartClosetApi.ts`
- `frontend/src/types/api.ts`
- `frontend/src/App.css`

이전 P0 release candidate와 label/swatch/chip helper를 확인한 뒤 작업하라.

## 작업
Preferences view를 선호 색상 swatch multi-select, 선호 소재 chip multi-select, style tag 입력/삭제, 저장 CTA 중심으로 정리한다.

## 변경 예상 파일
- `frontend/src/features/preferences/**`
- `frontend/src/App.css`
- `frontend/src/types/api.ts`

## 구현 메모
- `GET /api/users/me/preferences`로 현재 값을 불러온다.
- `PUT /api/users/me/preferences`로 `preferredColors`, `preferredMaterials`, `styleTags` 배열을 저장한다.
- 선호 색상은 한국어 라벨과 swatch로 multi-select한다.
- 선호 소재는 한국어 라벨 chip으로 multi-select한다.
- style tag는 문자열 입력/삭제를 제공한다.
- 저장 성공, 저장 실패, 인증 만료 상태를 한국어 문장으로 표시한다.
- `styleTags`는 저장/조회/표시만 하며 추천 점수/이유에 반영된다는 뉘앙스를 피한다.
- Preferences 저장 또는 확인이 Today 체크리스트에 반영될 수 있게 상위 상태 hook을 연결한다.

## 검증 절차
```bash
git diff --check
rg -n 'preferredColors|preferredMaterials|styleTags|swatch|chip|저장' frontend/src/features/preferences frontend/src
! rg -n 'styleTags.*점수|styleTags.*이유|styleTags.*추천.*반영|preferenceScore.*styleTags' frontend/src
(cd frontend && npm run build)
```

## 인수 기준
- Preferences view에서 색상과 소재를 시각적으로 선택할 수 있다.
- style tag 입력/삭제와 저장이 가능하다.
- 저장 payload는 API 계약의 배열 필드를 사용한다.
- 저장 성공/실패/인증 만료 상태가 사용자 문장으로 보인다.
- Today 체크리스트의 선호도 항목과 흐름이 어긋나지 않는다.

## 금지사항
- 선호도 별도 테이블이나 새 API를 요구하지 마라. 이유: MVP4는 existing preferences API만 사용한다.
- `styleTags`를 score, tie-breaker, recommendation reason과 연결하지 마라. 이유: 현재 baseline에서 저장/조회/표시만 한다.
- 색상/소재 enum 값을 한국어 request 값으로 보내지 마라. 이유: API는 대문자 enum을 유지한다.
- 큰 form library나 state management library를 추가하지 마라. 이유: React state와 작은 hook 기준이다.
