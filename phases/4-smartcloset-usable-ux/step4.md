# 단계 4: closet-quick-manage

범위: Must-have / MVP4 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/FRONTEND.md`
- `docs/API.md`
- `docs/DEMO_SCENARIO.md`
- `docs/design/mvp4/README.md`
- `frontend/src/features/clothes/ClosetPanel.tsx`
- `frontend/src/api/smartClosetApi.ts`
- `frontend/src/types/api.ts`
- `frontend/src/App.css`

이전 단계에서 만들어진 label/swatch/chip helper와 app shell을 확인한 뒤 작업하라.

## 작업
Closet view를 사용자가 첫 추천에 필요한 옷을 빠르게 등록하고, 기존 옷을 수정/보관할 수 있는 실사용 UX로 바꾼다.

## 변경 예상 파일
- `frontend/src/features/clothes/**`
- `frontend/src/api/smartClosetApi.ts`
- `frontend/src/types/api.ts`
- `frontend/src/App.css`

## 구현 메모
- 카테고리 filter는 전체, 상의, 하의, 아우터를 제공한다.
- 옷 목록은 활성 옷 중심으로 표시한다. API가 이미 `archived=false`를 반환하는 기준을 따른다.
- name을 가장 크게 표시하고, category/color/material은 한국어 라벨로 표시한다.
- color는 swatch와 라벨을 함께 표시한다.
- material은 chip으로 표시한다.
- min/max temperature와 rainSuitable은 보조 정보로 표시한다.
- 빠른 등록 form은 `ClothingRequest` shape를 그대로 사용한다.
- 계절/기온 프리셋은 UI helper로만 동작한다:
  - 한겨울: `-10..5`, rainSuitable false
  - 쌀쌀한 날: `0..12`, rainSuitable false
  - 간절기: `8..20`, rainSuitable false
  - 따뜻한 날: `17..28`, rainSuitable false
  - 비 오는 날: `5..24`, rainSuitable true
- 옷 수정은 `PUT /api/clothes/{clothingId}`로 전체 수정한다.
- 옷 보관은 `PATCH /api/clothes/{clothingId}/archive`로 처리하고, 성공 후 목록과 Today 체크리스트가 갱신될 수 있게 한다.
- 모바일에서도 수정/보관 액션이 hover 없이 항상 접근 가능해야 한다.

## 검증 절차
```bash
git diff --check
rg -n 'updateClothing|archiveClothing|한겨울|쌀쌀한 날|간절기|따뜻한 날|비 오는 날|swatch|chip' frontend/src
! rg -n 'image|upload|userId|recommendations/today' frontend/src/features/clothes frontend/src/api
(cd frontend && npm run build)
```

## 인수 기준
- Closet view에서 category filter, 빠른 등록, 수정, 보관 처리가 가능하다.
- 등록과 수정 request는 `ClothingRequest` 계약을 따른다.
- 프리셋은 서버 enum/DB schema를 바꾸지 않고 기온/비 적합성 입력값만 채운다.
- 옷 목록에서 한국어 category/color/material 라벨, color swatch, material chip이 보인다.
- 보관된 옷은 활성 목록과 Today 준비 상태에서 제외된다.
- 모바일에서 수정/보관 액션이 터치로 접근 가능하다.

## 금지사항
- 이미지 업로드 UI나 file metadata를 추가하지 마라. 이유: MVP4 제외 범위다.
- 서버 enum, DB schema, API request shape를 바꾸지 마라. 이유: 프리셋은 frontend helper다.
- archived 값을 등록/수정 request body에 넣지 마라. 이유: 보관은 별도 archive API로 처리한다.
- hover에만 의존하는 카드 액션을 만들지 마라. 이유: 모바일에서 접근할 수 없어야 한다.
- 추천 후보 필터링 규칙을 frontend에서 재구현하지 마라. 이유: 추천 domain rule은 backend 책임이고 frontend는 준비 상태만 안내한다.
