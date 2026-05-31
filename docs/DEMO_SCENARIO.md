# Demo Scenario: SmartCloset MVP9

## 데모 목표

Docker Compose로 SmartCloset 백엔드, MySQL, React 프론트엔드를 실행한 뒤 브라우저에서 MVP8 계정 안정성 기능이 유지되는지와 MVP9 UI/UX 리디자인 화면이 데스크톱/모바일에서 겹침 없이 동작하는지 확인한다.

MVP9 데모의 핵심은 사용자가 SmartCloset을 실제 서비스처럼 느낄 수 있는 화면 완성도를 확인하는 것이다.

## MVP9 데모 범위

포함:

- Auth 화면 visual/form layout 확인
- 회원가입 후 이메일 인증 필요 상태 확인
- 로그인 이메일 저장 체크박스
- refresh cookie 기반 새로고침 세션 복구
- Google provider enabled/disabled 상태 확인
- 추천 dashboard와 추천 결과 확인
- 옷장 목록, 옷 이미지, 옷 추가/수정 form 확인
- 내 취향 swatch/chip/toggle 입력 확인
- 위치 검색과 현재 위치 후보 찾기 확인
- 기록 calendar/timeline과 위치/날씨 snapshot 확인
- profile pill/menu 기반 계정 설정 진입
- 계정 삭제와 데이터 삭제
- 데스크톱 1440px, 모바일 390px 반응형 확인

제외:

- AWS 배포
- S3 storage
- SES/SMTP 실제 발송
- Secrets Manager
- CD 자동화
- Redis
- 백엔드 API/DTO 변경
- DB schema 변경
- 추천 규칙 변경
- AI/GPT 추천

## 데모 전제

- `.env`는 `.env.example`을 복사해 만든다.
- 이메일 발송은 `ConsoleEmailSender` 기준이며 실제 메일은 발송하지 않는다.
- Google OAuth 설정이 없으면 provider disabled 상태로 데모한다.
- KMA key가 없어도 `WEATHER_FALLBACK_ENABLED=true`이면 fallback weather로 추천 데모 가능하다.
- Docker Compose reset 시 DB와 이미지 volume이 초기화된다.

## 실행

```bash
docker compose down -v
test -f .env || cp .env.example .env
docker compose up --build
```

Frontend:

```text
http://localhost:5173
```

Swagger UI:

```text
http://localhost:8080/swagger-ui/index.html
```

## React 앱 MVP9 데모 시나리오

### 1. Auth 화면과 회원가입

1. Frontend에 접속한다.
2. Auth 화면의 visual background와 중앙 form이 데스크톱/모바일에서 읽히는지 확인한다.
3. 새 이메일/password/name으로 회원가입한다.
4. 회원가입 성공 후 이메일 인증 필요 안내를 확인한다.

기대 결과:

- Auth form CTA와 입력값이 390px 모바일에서 잘리지 않는다.
- 가입 직후 자동 로그인되지 않는다.
- access token이 JSON 응답에 포함되지 않는다.
- 인증 안내와 재요청 진입점이 표시된다.

### 2. 이메일 인증과 로그인

1. backend console/log에서 인증 token 또는 인증 링크를 확인한다.
2. 앱의 이메일 인증 확인 화면에 token을 입력하거나 링크를 연다.
3. 인증된 이메일/password로 로그인한다.
4. 이메일 저장 체크박스를 선택한 뒤 로그인한다.
5. 앱을 새로고침한다.

기대 결과:

- 이메일 인증이 성공한다.
- login 응답은 access token을 반환한다.
- refresh token 값은 JSON body에 없다.
- 새로고침 후 `POST /api/auth/refresh`로 세션이 복구된다.
- 로그인 화면에는 저장된 이메일 주소만 복원된다.
- 비밀번호, access token, refresh token은 브라우저 저장소에 저장되지 않는다.

### 3. Navigation과 account entry

1. 로그인 후 `추천` 화면이 기본 view인지 확인한다.
2. 데스크톱에서 상단 탭 `추천`, `옷장`, `내 취향`, `위치`, `기록`을 확인한다.
3. 모바일에서 하단 탭 `추천`, `옷장`, `내 취향`, `위치`, `기록`을 확인한다.
4. 우측 상단 profile pill/menu에서 계정 설정으로 진입한다.

기대 결과:

- primary nav에 계정 탭이 없다.
- 계정 설정은 profile에서 자연스럽게 열린다.
- 상단 탭과 하단 탭의 텍스트가 잘리지 않는다.

### 4. 추천 dashboard

1. `추천` 화면에서 날씨, 위치, 상황, 예보 시간대, 준비 상태를 확인한다.
2. 상황과 예보 시간대를 선택해 추천을 생성한다.
3. 추천 결과의 옷 이미지, 추천 이유, 점수 상세를 확인한다.
4. 착용 완료와 피드백 저장/clear를 수행한다.

기대 결과:

- 추천 생성은 `POST /api/recommendations`를 사용한다.
- 추천 점수/필터/tie-break는 기존 규칙을 유지한다.
- 옷 조합과 "오늘 입기 좋은 이유"가 점수표보다 먼저 읽힌다.
- 이미지가 없는 옷은 fallback visual로 식별 가능하다.

### 5. 옷장과 내 취향

1. `옷장`에서 이미지 중심 목록과 filter chip을 확인한다.
2. 새 옷 추가 화면에서 이미지, 이름, 카테고리, 색상, 소재, 기온 범위, 비 적합성, style tag를 입력한다.
3. `내 취향`에서 색상 swatch, 소재 toggle, style tag chip을 수정한다.

기대 결과:

- 옷 이미지 blob fetch와 object URL cleanup 흐름이 유지된다.
- 보호 이미지는 public `<img src>` 직접 참조가 아니다.
- 옷 등록/수정 JSON API가 multipart로 대체되지 않는다.
- swatch/chip/toggle control이 모바일에서 겹치지 않는다.

### 6. 위치와 기록

1. `위치`에서 `일산동` 검색과 현재 위치 후보 찾기를 확인한다.
2. 후보를 선택해 계정 위치를 저장한다.
3. `기록`에서 과거 추천의 위치/날씨 요약이 현재 위치 변경과 독립적으로 유지되는지 확인한다.

기대 결과:

- 위치 검색은 내부 KMA catalog 기준이다.
- 브라우저 GPS 원문 좌표를 DB에 저장하지 않는다.
- 선택된 동네만 사용자 위치로 저장된다.
- 과거 추천 snapshot은 현재 위치 변경과 독립적으로 유지된다.

### 7. 계정 설정과 계정 삭제

1. Profile pill/menu에서 계정 설정으로 이동한다.
2. 이메일 인증 상태, 로그인 제공자, 세션 상태를 확인한다.
3. confirmation `DELETE`를 입력한다.
4. Password login enabled 계정이면 현재 비밀번호를 입력한다.
5. 삭제를 실행한다.

기대 결과:

- 계정 삭제 위험 영역이 다른 설정과 분리되어 보인다.
- 계정 삭제가 성공한다.
- 로그인 상태가 초기화된다.
- 기존 refresh cookie는 더 이상 사용할 수 없다.
- 삭제된 계정의 옷장, 추천 이력, 이미지 파일이 남아 보호 API에서 조회되지 않는다.

## API 실패 케이스 확인

- 미인증 password 계정 login: `403 EMAIL_VERIFICATION_REQUIRED`
- 만료/사용 완료 인증 token confirm: `400 ACCOUNT_TOKEN_INVALID`
- reset token 재사용: `400 ACCOUNT_TOKEN_INVALID`
- refresh cookie 없음: `401 UNAUTHORIZED`
- revoked refresh token 사용: `401 INVALID_TOKEN`
- Google provider 설정 없음: provider status `enabled=false`
- 계정 삭제 confirmation 누락: `400 INVALID_REQUEST`
- 계정 삭제 password 불일치: `401 UNAUTHORIZED`

## 완료 기준

- MVP9 navigation 계약이 유지된다.
- Auth, 추천, 옷장, 내 취향, 위치, 기록, 계정 설정 화면이 데스크톱 1440px과 모바일 390px에서 겹침/잘림 없이 동작한다.
- MVP8 세션 복구, 이메일 인증, 비밀번호 재설정, Google provider 상태, 계정 삭제 UX가 유지된다.
- 백엔드 HTTP API, DTO, DB schema, 추천 규칙이 변경되지 않는다.
- Docker Compose local 환경에서 앱이 정상 실행된다.
