# Issue 4: 9-smartcloset-ui-ux-redesign step 7 자동 리뷰 실패 4

## 발생 위치
- Phase: 9-smartcloset-ui-ux-redesign
- Step: 7 `global-focus-hover-polish`
- PR: https://github.com/vanilalatte03/SmartCloset-v2/pull/129

## 재현 명령
```bash
(cd frontend && npm run build)
git diff --check origin/main...HEAD
```

## 핵심 에러
## 자체 리뷰

| 항목 | 결과 | 비고 |
| --- | --- | --- |
| 로컬 검증 | 통과 | step 인수 기준 명령 |
| diff 검사 | 통과 | git diff --check |
| 금지 범위 | 통과 | MVP 제외 범위와 금지 API 검색 |
| 자체 리뷰 | 실패 | Codex read-only review |

## 확인한 명령

```bash
(cd frontend && npm run build)
git diff --check origin/main...HEAD
```

## 발견사항
- frontend/src/App.css:3722-3765: The new desktop hover media query overrides active style-tag suggestion chips but does not restore an `.suggestion-chip.active:hover` state. Because `.suggestion-chip:hover:not(:disabled)` is later than `.suggestion-chip.active`, selected style-tag chips in both Preferences and Closet change from the active green state to the generic blue hover state while hovered, making the selected chip look unselected/ambiguous. Step 7 explicitly covers common chip hover/active polish, so add an active hover override for `.suggestion-chip.active:hover:not(:disabled)` that preserves the selected styling.

## 리뷰 결론
블로커가 있어 merge하지 않습니다.

## 수정 방향
- 같은 PR 브랜치에서 발견사항을 수정하고 같은 gate를 다시 통과시킨다.

## 완료 기준
- 로컬 검증, 금지 범위 검색, Codex 자체 리뷰를 모두 통과한다.
