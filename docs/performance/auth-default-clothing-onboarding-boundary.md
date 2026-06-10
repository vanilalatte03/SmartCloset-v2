# 인증 기본 옷 온보딩 경계 분리 기록

## 문서 목적

이 문서는 인증 login/refresh/OAuth session 발급 경로에서 기본 옷 preset seed 책임을 신규 계정 온보딩 경계로 분리한 안정성 및 성능 개선 기록이다.

이 문서는 ADR이 아니며 공개 API shape, DB schema, refresh token 저장 정책, Google verified email 계약을 변경하지 않는다. 관련 GitHub Issue는 `#180`이고, 구현은 PR `#188`에서 merge했다.

## 문제

`DefaultClothingPresetSeeder.seedIfEmpty`는 사용자 옷장 row 수 조회, preset image resource read, `ClothingImageStorage.store`, rollback cleanup 등록, 기본 옷 row 저장, legacy temperature range 보정을 수행한다.

기존 인증 흐름은 password login, refresh, Google OAuth callback session 발급에서 이 seeder를 직접 호출했다. 그 결과 빈번한 인증 경로와 refresh token rotation이 preset 이미지 파일 I/O 및 옷장 migration 성격의 작업에 묶였다.

특히 `POST /api/auth/refresh`는 세션 복구와 token rotation만 책임져야 하는 경로인데, 기본 옷 seed 실패나 storage 비용이 refresh/session rotation 장애처럼 보일 수 있었다.

## 변경

`AccountOnboardingService`를 추가해 신규 계정 생성 이후 기본 옷 seed를 인증 핵심 흐름 밖으로 분리했다.

- password signup은 user/action token 생성 transaction 안에서 user를 저장하고, commit 이후 신규 계정 기본 옷 온보딩을 예약한다.
- 새 Google-only user 생성 branch만 기본 옷 온보딩을 예약한다.
- 온보딩은 user id를 기준으로 user를 다시 조회한 뒤 `REQUIRES_NEW` transaction에서 `DefaultClothingPresetSeeder.seedIfEmpty`를 실행한다.
- 온보딩 실패는 warning log로 남기고 auth response, refresh rotation, OAuth session 발급 결과를 rollback하지 않는다.
- password login과 `POST /api/auth/refresh`는 기본 옷 seed 또는 legacy 보정을 수행하지 않는다.
- 기존 email user에 Google social account를 link하는 경로와 OAuth unique 충돌 retry 경로는 신규 계정 온보딩을 예약하지 않는다.

## 보존한 계약

- Password signup은 이메일 인증 필요 상태를 반환하고 access token을 발급하지 않는다.
- 신규 password user와 새 Google-only user는 기본 위치 `SEOUL`, 위치 source `MANUAL_SEARCH`, 빈 선호도 계약을 유지한다.
- 신규 계정의 기본 옷 preset은 signup 또는 새 Google user commit 이후 준비된다.
- Login과 refresh response shape, refresh cookie, access token bearer response 계약은 변경하지 않는다.
- Refresh token 원문은 DB, 로그, JSON response에 저장하거나 노출하지 않는다.
- Google provider 호출은 DB transaction 밖에서 수행한다.
- Google OAuth user/social account upsert unique 충돌은 provider 재호출 없이 새 transaction에서 재조회해 로그인으로 수렴한다.
- local/demo profile의 `SeedDataInitializer` profile/property gate는 변경하지 않는다.
- 공개 `userId` query parameter 또는 현재 사용자 전용 DTO `userId` 노출을 추가하지 않는다.

## 운영 영향

`POST /api/auth/refresh`는 더 이상 preset 이미지 읽기, storage 저장, 옷장 row 생성, legacy 보정 비용을 부담하지 않는다. refresh 경로는 refresh session rotation과 access token 재발급에 집중한다.

Password login도 기존 사용자 옷장 보정 비용을 수행하지 않는다. 따라서 로그인 요청의 지연과 실패 원인은 인증 상태, 비밀번호 검증, refresh session issue에 더 가깝게 해석할 수 있다.

Google OAuth DB write transaction은 provider profile 수신 후 user/social account upsert와 refresh session 발급 구간에 집중한다. 새 Google-only user의 기본 옷 seed는 commit 이후 온보딩 경계에서 별도로 처리된다.

온보딩은 아직 비동기 worker가 아니라 요청 thread의 after-commit callback에서 실행된다. 따라서 신규 계정 생성 요청 직후 thread가 seed 비용을 일부 부담할 수 있지만, 인증 write transaction 보유 시간과 rollback 범위에서는 분리된다.

## 회귀 기준

인증 기본 옷 온보딩 경계는 다음 기준을 지킨다.

- `AuthService`와 `GoogleOAuthService`가 `DefaultClothingPresetSeeder`에 직접 의존하면 안 된다.
- Password login과 `POST /api/auth/refresh`는 `AccountOnboardingService`를 호출하면 안 된다.
- Password signup과 새 Google-only user 생성은 commit 이후 기본 옷 온보딩을 예약해야 한다.
- 온보딩은 `REQUIRES_NEW` transaction에서 user를 재조회해 seed해야 한다.
- 온보딩 실패가 signup/login/refresh/OAuth response 실패로 전파되면 안 된다.
- 기존 email user에 Google social account를 link하는 경로는 새 기본 옷 seed를 예약하면 안 된다.
- OAuth provider fetch outside transaction, known unique 충돌 retry, refresh token 원문 비노출 계약을 유지해야 한다.
- 신규 계정 온보딩과 local/demo seed initializer profile gate를 혼동하면 안 된다.

## 검증

PR `#188`에서 다음 검증을 통과했다.

- `git diff --check`
- `python3 scripts/checks.py --docs-check --include-final-docs`
- `./gradlew test --tests com.smartcloset.auth.application.AuthServiceOnboardingBoundaryTest --tests com.smartcloset.auth.application.AccountOnboardingServiceTest --tests com.smartcloset.auth.AuthControllerTest --tests com.smartcloset.auth.AuthSignupConcurrencyTest --tests com.smartcloset.auth.application.GoogleOAuthServiceTest --tests com.smartcloset.auth.application.GoogleOAuthConcurrencyTest --tests com.smartcloset.auth.application.GoogleOAuthTransactionBoundaryTest --tests com.smartcloset.auth.application.AuthServiceUniqueViolationTest`
- `./gradlew test`
- `./gradlew build`
- `cd frontend && npm run build`
- GitHub Actions: `test-build`
- 커밋 훅: `python3 -m compileall scripts`
- 커밋 훅: `./gradlew test`
- 커밋 훅: `./gradlew build`
- 커밋 훅: `cd frontend && npm run build`
- Codex CLI read-only review: `pass=true`, findings 없음

추가된 테스트는 signup after-commit 기본 옷 seed, seed 실패 non-propagation, login/refresh no-seed, 새 Google-only user 온보딩 예약, OAuth unique 충돌 retry의 no-onboarding 회귀를 확인한다.
