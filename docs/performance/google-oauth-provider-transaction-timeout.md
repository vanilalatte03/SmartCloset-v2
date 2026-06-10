# Google OAuth Provider Transaction/Timeout 성능 기록

## 문서 목적
이 문서는 Google OAuth callback에서 외부 provider 호출이 DB transaction을 오래 붙잡을 수 있던 문제와 timeout 설정 정리를 기록한다.

이 문서는 ADR이 아니며 OAuth 공개 API, refresh token 원문 저장 정책, Google profile 검증 계약을 변경하지 않는다. 관련 GitHub Issue는 `#158`이다.

## 문제
OAuth callback은 Google authorization code를 access token/profile로 교환한 뒤 SmartCloset user, social account, refresh session을 저장한다.

기존 `GoogleOAuthService.callback`은 메서드 전체가 `@Transactional`이었다. 따라서 `GoogleOAuthClient.fetchUserProfile`이 Google token/userinfo endpoint와 통신하는 동안에도 Spring transaction이 열린 상태가 될 수 있었다.

또한 `GoogleOAuthClient`는 기본 `RestClient.create()`를 사용했다. 이 구조에서는 Google token/userinfo 호출의 connect/read timeout이 서비스 설정으로 드러나지 않아 provider 지연이나 network hang 상황에서 API 응답 시간을 예측하기 어려웠다.

## Before
기존 구조의 위험 요소는 다음과 같았다.

- OAuth callback 진입 직후 transaction이 열렸다.
- Google token 교환과 userinfo 조회가 transaction 안에서 수행됐다.
- provider 지연 시 DB connection과 transaction resource 점유 시간이 provider 응답 시간만큼 늘어날 수 있었다.
- `RestClient` request factory에 명시적인 connect/read timeout이 없었다.
- timeout 값을 환경변수로 조정할 수 없어 장애 상황별 운영 조정 지점이 부족했다.

## After
개선 후 callback 흐름은 provider I/O와 DB write를 분리한다.

1. Google OAuth provider 활성 상태를 확인한다.
2. Google authorization code 교환과 profile 조회를 transaction 밖에서 수행한다.
3. profile의 `sub`, `email`, `emailVerified`를 transaction 밖에서 검증한다.
4. 검증된 profile만 `TransactionTemplate` 안으로 전달한다.
5. write transaction 안에서 user/social account를 조회하거나 생성하고 refresh session을 발급한다.
6. 새 Google user의 기본 옷 seed는 transaction commit 이후 신규 계정 온보딩 경계에서 별도로 실행한다.
7. transaction commit 이후 callback response에 필요한 access token과 refresh cookie 원문을 반환한다.

Google provider HTTP client는 `smartcloset.security.oauth2.google.connect-timeout`, `smartcloset.security.oauth2.google.read-timeout`을 사용한다.

- 기본 connect timeout: `3s`
- 기본 read timeout: `5s`
- 환경변수: `GOOGLE_OAUTH_CONNECT_TIMEOUT`, `GOOGLE_OAUTH_READ_TIMEOUT`

provider timeout이나 HTTP transport 장애는 기존과 같이 `OAUTH2_PROVIDER_UNAVAILABLE`로 변환한다.

## 성능 영향
Google provider가 느려져도 provider 호출 시간은 DB transaction 보유 시간에 더해지지 않는다.

DB write transaction은 verified profile을 받은 뒤 user/social account upsert와 refresh session 발급 구간에만 열린다. 기본 옷 seed는 Issue `#180` 이후 commit 이후 온보딩 경계로 분리됐다. 따라서 provider 지연과 preset image/storage 비용은 OAuth DB write transaction 보유 시간에 직접 더해지지 않는다.

timeout 값은 Duration property로 바인딩되므로 운영 환경에서 provider 품질과 네트워크 특성에 맞게 조정할 수 있다. timeout이 발생하면 callback은 기존 OAuth provider unavailable 오류 계약으로 빠르게 실패한다.

## 회귀 방지 기준
Google OAuth callback에서는 다음 기준을 지킨다.

- Google token/userinfo provider 호출 중에는 Spring transaction이 열려 있지 않다.
- user/social account upsert와 refresh session 발급은 하나의 write transaction 안에서 수행된다.
- provider timeout과 transport 장애는 `OAUTH2_PROVIDER_UNAVAILABLE` 오류 계약을 유지한다.
- Google profile의 verified email 검증은 유지한다.
- refresh token 원문은 DB나 JSON response에 저장 또는 노출하지 않는다.

## 확인한 테스트
이번 변경은 다음 테스트를 추가하거나 갱신해 회귀를 막는다.

- `GoogleOAuthTransactionBoundaryTest`: fake Google client에서 provider 호출 시 transaction inactive를 기록하고, refresh 발급 시 transaction active를 확인한다.
- `GoogleOAuthClientTest`: slow local token endpoint가 read timeout을 유발할 때 `OAUTH2_PROVIDER_UNAVAILABLE`로 변환되는지 확인한다.
- `GoogleOAuthPropertiesTest`: Google OAuth connect/read timeout 기본값과 property binding을 확인한다.
- 기존 `GoogleOAuthServiceTest`: Google-only user 생성, 기존 이메일 계정 연결, unverified email 거부 계약을 유지한다.
