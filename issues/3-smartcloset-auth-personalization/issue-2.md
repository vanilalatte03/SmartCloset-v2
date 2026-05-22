# Issue 2: 3-smartcloset-auth-personalization step 1 자동 리뷰 실패 2

## 발생 위치
- Phase: 3-smartcloset-auth-personalization
- Step: 1 `auth-api-and-session-contract`
- PR: https://github.com/vanilalatte03/SmartCloset-v2/pull/33

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
| 금지 범위 | 실패 | MVP 제외 범위와 금지 API 검색 |
| 자체 리뷰 | 실패 | Codex read-only review |

## 확인한 명령

```bash
python3 scripts/checks.py --stage manual
git diff --check origin/main...HEAD
```

## 발견사항
- phases/3-smartcloset-auth-personalization/index.json:18 - 로그인/회원가입/Spring Security 범위가 추가되었습니다.
- src/main/java/com/smartcloset/security/SecurityConfig.java:18-24 and 56-63 keep /api/locations, /api/users/location, /api/clothes/**, /api/recommendations/** and any other request permitAll. This violates AGENTS.md, docs/PRD.md, docs/API.md, and ADR-008: only POST /api/auth/signup and POST /api/auth/login may be public; all other APIs must require Authorization: Bearer {accessToken}.
- src/test/java/com/smartcloset/security/TemporarySecurityPermitAllTest.java:47-50 now codifies the contract violation by expecting GET /api/locations without a bearer token to return 200, even though docs/API.md and ADR-008 require GET /api/locations to be a protected API and return 401 when unauthenticated.
- The old public userId-based controllers remain reachable because of the permitAll security config: src/main/java/com/smartcloset/clothing/presentation/ClothingController.java:32-67, src/main/java/com/smartcloset/user/presentation/UserLocationController.java:25-35, and src/main/java/com/smartcloset/recommendation/presentation/RecommendationController.java:26-37 still accept @RequestParam Long userId. This violates the MVP 3 rule to remove public ?userId= and identify the current user from the authenticated principal.

## 리뷰 결론
블로커가 있어 merge하지 않습니다.

## 수정 방향
- 같은 PR 브랜치에서 발견사항을 수정하고 같은 gate를 다시 통과시킨다.

## 완료 기준
- 로컬 검증, 금지 범위 검색, Codex 자체 리뷰를 모두 통과한다.
