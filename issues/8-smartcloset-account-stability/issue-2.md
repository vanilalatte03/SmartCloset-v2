# Issue 2: 8-smartcloset-account-stability step 4 자동 리뷰 실패 2

## 발생 위치
- Phase: 8-smartcloset-account-stability
- Step: 4 `account-hard-delete`
- PR: https://github.com/vanilalatte03/smart-closet/pull/110

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
- BLOCKER: Deleted users' existing JWTs are still authenticated globally because src/main/java/com/smartcloset/security/JwtAuthenticationFilter.java:41-50 only parses the token and never verifies that the subject user still exists. The new test at src/test/java/com/smartcloset/user/CurrentUserControllerTest.java:192-195 only covers GET /api/users/me, but protected endpoints that do not check UserRepository still succeed after account deletion. For example, GET /api/recommendations calls RecommendationService.getRecommendationHistory at src/main/java/com/smartcloset/recommendation/application/RecommendationService.java:178-187 and returns an empty 200 response for the deleted user id, and GET /api/locations at src/main/java/com/smartcloset/location/presentation/LocationController.java:27-32 returns catalog data without any current-user validation. This violates Step 4's requirement that the old access token must no longer be able to read protected resources after deletion. Add a user-existence/auth invalidation guard for protected requests, or equivalent current-user validation on all protected endpoints, and add a stale-token regression test against a non-/users/me protected endpoint.

## 리뷰 결론
블로커가 있어 merge하지 않습니다.

## 수정 방향
- 같은 PR 브랜치에서 발견사항을 수정하고 같은 gate를 다시 통과시킨다.

## 완료 기준
- 로컬 검증, 금지 범위 검색, Codex 자체 리뷰를 모두 통과한다.

---

## 자동 수정 완료

같은 PR 브랜치에서 자동 수정 후 리뷰 gate를 통과했습니다.

## 자체 리뷰

| 항목 | 결과 | 비고 |
| --- | --- | --- |
| 로컬 검증 | 통과 | docs/COMMANDS.md 기준 명령 |
| diff 검사 | 통과 | git diff --check |
| 금지 범위 | 통과 | MVP 제외 범위와 금지 API 검색 |
| 자체 리뷰 | 통과 | Codex read-only review |

## 확인한 명령

```bash
python3 scripts/checks.py --stage manual
git diff --check origin/main...HEAD
```

## 발견사항
- 없음

## 리뷰 결론
블로커 없음. 이 step PR은 merge 가능합니다.
