# 단계 0: location-domain-and-schema

범위: Must-have / 2차 P0

## 읽어야 할 파일
먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/PRD.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/API.md`
- `docs/COMMANDS.md`
- `docs/adr/007-mvp2-user-location-and-react-frontend.md`
- `src/main/java/com/smartcloset/user/domain/User.java`
- `src/main/java/com/smartcloset/user/repository/UserRepository.java`
- `src/main/resources/application.yml`
- `src/main/resources/data.sql` 또는 seed user를 만드는 현재 파일

## 작업
사용자별 위치 저장의 도메인과 schema 기반을 추가한다. 이 단계는 Entity, 내장 catalog, 기본값 보정, seed data까지만 다루고 HTTP API와 KMA 호출 변경은 이후 step에서 한다.

## 변경 예상 파일
- `src/main/java/com/smartcloset/user/domain/User.java`
- `src/main/java/com/smartcloset/user/repository/UserRepository.java`
- `src/main/java/com/smartcloset/location/**`
- `src/main/resources/application.yml`
- seed data 또는 test fixture 파일
- `src/test/java/com/smartcloset/user/**`
- `src/test/java/com/smartcloset/location/**`

## 구현 메모
- `users` 위치 컬럼은 DB nullable로 추가한다.
  - `location_code`
  - `location_name`
  - `location_nx`
  - `location_ny`
- DB default는 두지 않는다. 이유: 후속 migration 도구 도입 전까지 기존 row가 있는 `ddl-auto=update` 환경을 안전하게 유지해야 한다.
- 애플리케이션 기본 위치는 `SEOUL`, `서울특별시`, `60`, `127`이다.
- `User`에는 의도가 드러나는 메서드를 둔다.
  - `updateLocation(LocationOption location)`
  - `ensureDefaultLocation()`
- 내장 catalog는 DB 테이블이 아니라 애플리케이션 코드의 value object와 catalog component로 시작한다.
- 최소 catalog는 문서의 9개 대표 지역을 모두 포함한다.
- keyword 검색은 code 또는 name 기준이며 대소문자 차이는 구현자가 읽기 쉬운 방식으로 처리한다.
- seed user는 생성 시 서울 기본 위치를 가진다.

## 검증 절차
```bash
git diff --check
! rg -n 'GET /api/recommendations/(today)' . --glob '!archive/**'
./gradlew test
```

## 인수 기준
- `User`가 위치 snapshot 필드 4개를 가지며 기존 사용자 name 필드와 관계가 깨지지 않는다.
- 위치가 비어 있는 `User`에 `ensureDefaultLocation()`을 호출하면 서울 기본값이 채워진다.
- 이미 위치가 있는 `User`에 `ensureDefaultLocation()`을 호출해도 선택 위치가 덮어써지지 않는다.
- `LocationCatalog`가 9개 대표 지역을 반환하고 code/name 검색을 지원한다.
- 이 단계가 끝난 뒤에도 공개 위치 API는 아직 추가되지 않아도 된다.
- 기존 추천, 옷 관리, KMA fallback 테스트가 계속 통과한다.

## 금지사항
- 위치 컬럼을 DB `NOT NULL` 또는 DB default로 강제하지 마라. 이유: 기존 row가 있는 개발 DB에서 마이그레이션 실패 위험이 있다.
- 별도 `locations` DB 테이블을 만들지 마라. 이유: 2차 위치 선택지는 서버 내장 catalog로 결정됐다.
- 외부 주소/지도 API를 추가하지 마라. 이유: 2차 제외 범위다.
- KMA client 요청 파라미터를 아직 변경하지 마라. 이유: 사용자 grid 적용은 별도 step에서 테스트와 함께 다룬다.
- 공개 HTTP 위치 API를 이 단계에 섞지 마라. 이유: schema/domain 기반과 API 계층을 분리해 리뷰 가능하게 유지한다.
