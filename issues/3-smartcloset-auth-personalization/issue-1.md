# Issue 1: 3-smartcloset-auth-personalization step 0 자동 리뷰 실패 1

## 발생 위치
- Phase: 3-smartcloset-auth-personalization
- Step: 0 `user-account-schema-and-token-infra`
- PR: https://github.com/vanilalatte03/SmartCloset-v2/pull/28

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
| 자체 리뷰 | 통과 | Codex read-only review |

## 확인한 명령

```bash
python3 scripts/checks.py --stage manual
git diff --check origin/main...HEAD
```

## 발견사항
- phases/3-smartcloset-auth-personalization/index.json:10 - 로그인/회원가입/Spring Security 범위가 추가되었습니다.

## 리뷰 결론
블로커가 있어 merge하지 않습니다.

## 수정 방향
- 같은 PR 브랜치에서 발견사항을 수정하고 같은 gate를 다시 통과시킨다.

## 완료 기준
- 로컬 검증, 금지 범위 검색, Codex 자체 리뷰를 모두 통과한다.

---

## 재시도 1 리뷰 실패

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
- BLOCKER: src/main/java/com/smartcloset/security/SecurityConfig.java:20 permits all requests, and src/test/java/com/smartcloset/security/TemporaryPermitAllSecurityConfigTest.java:32 asserts unauthenticated GET /api/locations returns 200. AGENTS.md, docs/API.md, docs/PRD.md, docs/ARCHITECTURE.md, and ADR-008 require only POST /api/auth/signup and POST /api/auth/login to be public; GET /api/locations and all other APIs must require Authorization: Bearer {accessToken}.

## 리뷰 결론
블로커가 있어 merge하지 않습니다.

---

## 재시도 2 리뷰 실패

## 자체 리뷰

| 항목 | 결과 | 비고 |
| --- | --- | --- |
| 로컬 검증 | 실패 | docs/COMMANDS.md 기준 명령 |
| diff 검사 | 통과 | git diff --check |
| 금지 범위 | 통과 | MVP 제외 범위와 금지 API 검색 |
| 자체 리뷰 | 실패 | Codex read-only review |

## 확인한 명령

```bash
python3 scripts/checks.py --stage manual
git diff --check origin/main...HEAD
```

## 발견사항
- `python3 scripts/checks.py --stage manual` 실패: $ python3 -m compileall scripts  # lint, docs/COMMANDS.md
Listing 'scripts'...
$ ./gradlew test  # test, docs/COMMANDS.md
> Task :compileJava UP-TO-DATE
> Task :processResources UP-TO-DATE
> Task :classes UP-TO-DATE
> Task :compileTestJava UP-TO-DATE
> Task :processTestResources UP-TO-DATE
> Task :testClasses UP-TO-DATE

> Task :test

SmartClosetApplicationTests > contextLoads() FAILED
    java.lang.IllegalStateException at DefaultCacheAwareContextLoaderDelegate.java:195
        Caused by: org.springframework.beans.factory.UnsatisfiedDependencyException at ConstructorResolver.java:804
            Caused by: org.springframework.beans.factory.NoSuchBeanDefinitionException at DefaultListableBeanFactory.java:2297

ClothingControllerTest > createsClothingWithArchivedFalse() FAILED
    java.lang.IllegalStateException at DefaultCacheAwareContextLoaderDelegate.java:157

ClothingControllerTest > updatesClothingDetailsWithoutChangingArchived() FAILED
    java.lang.IllegalStateException at DefaultCacheAwareContextLoaderDelegate.java:157

ClothingControllerTest > returnsClothingNotFoundWhenUpdatingOrArchivingOtherUsersClothing() FAILED
    java.lang.IllegalStateException at DefaultCacheAwareCo
... output truncated ...
- BLOCKER: `src/main/java/com/smartcloset/security/SecurityConfig.java:28`-`30` now requires authentication for every request except `POST /api/auth/signup` and `POST /api/auth/login`, but those auth endpoints are not implemented in this branch and the existing frontend client still sends no Bearer token (`frontend/src/api/smartClosetApi.ts:19`-`70`). This makes the current Docker/React/API smoke flow return 401 before a documented login path exists.
- BLOCKER: Protected controllers still accept caller-controlled `?userId=` instead of deriving the user from `CurrentUserPrincipal`: `src/main/java/com/smartcloset/clothing/presentation/ClothingController.java:34`, `42`, `49`, `57`, `66`; `src/main/java/com/smartcloset/user/presentation/UserLocationController.java:26`, `32`; `src/main/java/com/smartcloset/recommendation/presentation/RecommendationController.java:27`, `35`. With any valid token, a caller can pass another user's id and access or mutate that user's closet/location/recommendations, violating the MVP principal-based isolation rules.
- BLOCKER: Browser preflight from the Vite frontend is likely rejected because Security is enabled but CORS is not enabled in the filter chain (`src/main/java/com/smartcloset/security/SecurityConfig.java:21`-`34`). MVC CORS exists only in `src/main/java/com/smartcloset/common/config/WebConfig.java:12`-`15`; without `http.cors(...)`, protected API calls with Authorization/JSON headers from `localhost:5173` can fail at OPTIONS before reaching controllers.
- API CONTRACT: Invalid bearer tokens are hardcoded to return `UNAUTHORIZED` in `src/main/java/com/smartcloset/security/JwtAuthenticationEntryPoint.java:16`-`35`, and the new test asserts that in `src/test/java/com/smartcloset/security/ProtectedApiSecurityConfigTest.java:53`-`56`. `docs/API.md:553`-`560` defines `INVALID_TOKEN` for invalid token cases, so missing-token and invalid-token failures are not contract-compliant.

## 리뷰 결론
블로커가 있어 merge하지 않습니다.
