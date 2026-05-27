# Issue 3: 8-smartcloset-account-stability step 5 자동 리뷰 실패 3

## 발생 위치
- Phase: 8-smartcloset-account-stability
- Step: 5 `frontend-account-stability-ux`
- PR: https://github.com/vanilalatte03/SmartCloset-v2/pull/112

## 재현 명령
```bash
python3 scripts/checks.py --stage manual
git diff --check origin/main...HEAD
```

## 핵심 에러
## 자체 리뷰

| 항목 | 결과 | 비고 |
| --- | --- | --- |
| 로컬 검증 | 통과 | docs/COMMANDS.md 기준 명령 |
| diff 검사 | 통과 | git diff --check |
| 금지 범위 | 통과 | MVP 제외 범위와 금지 API 검색 |
| 자체 리뷰 | 실패 | Codex read-only review |

## 확인한 명령

```bash
python3 scripts/checks.py --stage manual
git diff --check origin/main...HEAD
```

## 발견사항
- BLOCKER: `frontend/src/api/client.ts:77` and `frontend/src/api/client.ts:134` call `refreshAccessTokenHandler()` independently for every protected request that receives 401. Because MVP8 refresh rotates the HttpOnly refresh token on every call, concurrent 401s can race: the first refresh revokes/replaces the session while later refresh calls still use the old cookie and fail with 401, causing `handleAuthExpired()` in `frontend/src/App.tsx:110` to clear an otherwise recoverable session. This is likely in normal app use because Today loads weather, preferences/clothes, and history concurrently from `frontend/src/features/today/TodayPanel.tsx:166`, `frontend/src/features/today/TodayPanel.tsx:185`, and `frontend/src/features/today/TodayPanel.tsx:222`. Add a single-flight/in-flight refresh promise so concurrent 401 retries share one refresh result and only retry their original request once.

## 리뷰 결론
블로커가 있어 merge하지 않습니다.

## 수정 방향
- 같은 PR 브랜치에서 발견사항을 수정하고 같은 gate를 다시 통과시킨다.

## 완료 기준
- 로컬 검증, 금지 범위 검색, Codex 자체 리뷰를 모두 통과한다.
