# 단계 4: closet-image-ux

## 읽어야 할 파일

- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/FRONTEND.md`
- `docs/design/mvp5/README.md`
- `frontend/src/types/api.ts`
- `frontend/src/api/client.ts`
- `frontend/src/api/smartClosetApi.ts`
- `frontend/src/features/clothes/ClosetPanel.tsx`
- `frontend/src/App.css`
- `frontend/src/index.css`

## 작업

Closet view에 이미지 관리 UX를 추가한다.

- frontend type에 `ClothingImageResponse`와 nullable `image`를 추가한다.
- 이미지 업로드/삭제/blob 조회 API 함수를 추가한다.
- authenticated blob fetch 후 object URL로 썸네일을 표시한다.
- object URL cleanup을 구현한다.
- 옷 카드에 썸네일 또는 fallback visual을 표시한다.
- 등록 후 선택 파일이 있으면 생성된 clothing id로 이미지 업로드를 이어서 호출한다.
- 수정 중 이미지 교체와 삭제를 제공한다.
- 업로드 실패는 옷 정보 저장 실패와 분리해 안내한다.

## 인수 기준

```bash
(cd frontend && npm run build)
git diff --check
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 모바일 375px에서 옷 카드, 파일 입력, 수정/삭제 버튼이 겹치지 않는지 확인한다.
3. 성공하면 phase index의 Step 4를 completed로 갱신한다.

## 금지사항

- 보호 이미지 URL을 일반 `<img src>`로 직접 사용하지 마라. 이유: Authorization header를 붙일 수 없다.
- 큰 상태 관리 라이브러리를 추가하지 마라. 이유: 현재 frontend rule은 React state와 작은 hook 기준이다.
- drag-and-drop 전용 UX만 제공하지 마라. 이유: 기본 파일 선택 흐름이 필요하다.
