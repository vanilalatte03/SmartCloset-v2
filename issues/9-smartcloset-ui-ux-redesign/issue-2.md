# Issue 2: 9-smartcloset-ui-ux-redesign step 1 자동 리뷰 실패 2

## 발생 위치
- Phase: 9-smartcloset-ui-ux-redesign
- Step: 1 `app-shell-auth-redesign`
- PR: https://github.com/vanilalatte03/SmartCloset-v2/pull/121

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
| 금지 범위 | 실패 | MVP 제외 범위와 금지 API 검색 |
| 자체 리뷰 | 실패 | Codex read-only review |

## 확인한 명령

```bash
(cd frontend && npm run build)
git diff --check origin/main...HEAD
```

## 발견사항
- frontend/src/features/auth/AuthPanel.tsx:34 - 이메일 인증 범위가 추가되었습니다.
- frontend/src/features/auth/AuthPanel.tsx:35 - 비밀번호 재설정 범위가 추가되었습니다.
- BLOCKER: frontend/src/App.tsx:28 imports the Auth background from ../../docs/design/mvp9/auth-london-editorial.png, but docker-compose.yml:80-82 mounts only ./frontend into the frontend container at /app. Inside Compose, that relative import resolves outside the mounted app and the Vite dev server cannot resolve the asset, breaking the documented Docker Compose sharing flow for MVP9. Move the runtime image under frontend/src/assets or frontend/public and import it from there.

## 리뷰 결론
블로커가 있어 merge하지 않습니다.

## 수정 방향
- 같은 PR 브랜치에서 발견사항을 수정하고 같은 gate를 다시 통과시킨다.

## 완료 기준
- 로컬 검증, 금지 범위 검색, Codex 자체 리뷰를 모두 통과한다.
