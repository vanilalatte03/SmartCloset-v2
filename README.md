# SmartCloset

SmartCloset은 날씨, 위치, 개인 옷장, 취향, 착용 이력을 바탕으로 오늘 입을 옷 조합을 추천하는 백엔드 중심 웹 서비스입니다.

Spring Boot 백엔드 프로젝트로서 단순 CRUD를 넘어 인증/세션, 사용자별 데이터 격리, 외부 날씨 API fallback, 규칙 기반 추천 도메인, 이미지 저장소 경계, Docker Compose 공유 실행까지 하나의 서비스 흐름 안에서 다룹니다. 프론트엔드는 백엔드 API를 실제 사용 흐름으로 검증하고 보여주기 위한 React SPA로 함께 둡니다.

## Overview

SmartCloset의 핵심 질문은 "오늘 날씨와 내 옷장 기준으로 왜 이 조합을 추천하는가?"입니다.

추천은 AI/GPT가 아니라 설명 가능하고 테스트 가능한 규칙 기반 로직으로 생성합니다. 사용자는 옷을 등록하고, 위치와 취향을 저장하고, 상황과 예보 시간대를 선택해 추천을 생성한 뒤, 착용 여부와 피드백을 남겨 다음 추천에 반영할 수 있습니다.

```text
회원가입/로그인
  -> 위치, 취향, 옷장 설정
  -> KMA 날씨 또는 fallback weather 조회
  -> 규칙 기반 추천 생성
  -> 착용 기록과 피드백 저장
  -> 추천 이력 확인
```

## Tech Stack

- Backend: Java 21, Spring Boot 4.0.6, Spring Security, JPA
- Database: MySQL
- Frontend: React, Vite, TypeScript
- Auth: JWT bearer access token, DB-backed refresh session, HttpOnly refresh cookie
- Weather: KMA `getVilageFcst`, local fallback provider
- Storage: local file system, Docker Compose volume
- Tooling: Gradle, Docker Compose, project docs-check scripts

## Backend Highlights

| 영역 | 구현 포인트 | 설계에서 드러나는 것 |
| --- | --- | --- |
| 인증/계정 | JWT access token, DB-backed refresh session, HttpOnly refresh cookie, 이메일 인증, 비밀번호 재설정, Google OAuth | 세션 안정성, token 원문 비저장, 계정 복구/삭제 흐름 |
| 사용자 데이터 격리 | 공개 `userId` query parameter 제거, 인증 principal 기준 보호 API | 실제 서비스형 multi-user API 설계 |
| 추천 도메인 | 날씨, 색상, 착용 이력, 추천 이력, 선호도, 피드백 기반 scoring | Controller/Repository 밖에 둔 테스트 가능한 domain logic |
| 날씨 연동 | KMA `getVilageFcst` provider, `StaticWeatherProvider` fallback, source snapshot | 외부 API 장애를 흡수하는 provider boundary |
| 위치 도메인 | KMA 행정구역 catalog 검색, 브라우저 좌표 resolve, GPS 원문 미저장 | 외부 지도 API 없이 생활권 위치를 다루는 방식 |
| 옷 이미지 | 별도 보호 이미지 API, 파일 검증, 로컬 파일 저장소, DB metadata 분리 | 파일 저장소와 소유권 검증 경계 |
| 운영 공유 | MySQL, backend, frontend, image volume을 Docker Compose로 실행 | 로컬 재현성과 데모 가능성 |

## Domain Structure

```text
com.smartcloset
├── auth              # signup/login, refresh session, email/password reset, Google OAuth
├── user              # current user profile, preferences, location ownership
├── clothing          # closet CRUD, archive/unarchive, image metadata and storage
├── location          # KMA catalog search and browser coordinate resolve
├── weather           # weather provider abstraction, KMA adapter, fallback weather
├── recommendation    # outfit candidate generation, scoring, history, feedback
├── security          # JWT, authentication filter, security configuration
└── common            # response envelope, exception, shared entity/config
```

프론트엔드는 React/Vite/TypeScript SPA입니다. 백엔드 API를 실제 화면에서 검증하기 위한 클라이언트이기도 하며, 현재 MVP9에서는 UI/UX 완성도를 높이는 단계입니다.

## Architecture

SmartCloset은 HTTP, use case orchestration, domain rule, persistence/provider 책임을 분리합니다.

```text
Controller -> Application Service -> Domain Service -> Repository / Provider
```

- Controller는 HTTP 요청/응답, validation, 인증 principal 추출, DTO mapping을 담당합니다.
- Application Service는 use case 흐름과 transaction 경계를 조율합니다.
- Domain Service는 추천 후보 생성, 점수 계산, tie-break, 추천 이유 생성 같은 비즈니스 규칙을 담당합니다.
- Repository는 persistence만 담당하고, Provider는 KMA weather나 image storage처럼 외부/인프라 경계를 감쌉니다.

이 구조를 택한 이유는 추천 점수나 날씨 fallback 같은 핵심 규칙이 Controller 또는 Repository에 섞이면 테스트와 변경이 어려워지기 때문입니다. 현재 상세 구조는 `docs/ARCHITECTURE.md`를 source of truth로 둡니다.

## Key Decisions

**인증 사용자 API로 전환**

초기 MVP의 테스트용 `userId` query parameter를 제거하고, Spring Security principal 기준으로 현재 사용자를 식별합니다. 이 결정은 사용자별 옷장, 위치, 취향, 추천 이력을 실제 서비스처럼 격리하기 위한 기반입니다. 관련 결정은 ADR-008에 정리되어 있습니다.

**Access token + HttpOnly refresh cookie**

Access token은 bearer token으로 유지하되, refresh token은 HttpOnly cookie로만 전달하고 DB에는 hash만 저장합니다. 새로고침 복구, token rotation, logout revoke, 만료 UX를 다루기 위해 MVP8에서 DB-backed refresh session을 추가했습니다. 관련 결정은 ADR-013을 따릅니다.

**AI가 아니라 규칙 기반 추천**

추천은 날씨, 색상, 착용/추천 이력, 선호도, 최근 피드백을 기반으로 계산합니다. 추천 이유도 template 기반으로 생성합니다. 이 프로젝트에서는 "그럴듯한 결과"보다 점수와 이유를 테스트할 수 있는 구조를 더 중요하게 봤습니다.

**KMA provider와 fallback 분리**

실제 날씨 추천을 위해 KMA 단기예보 `getVilageFcst`를 사용하지만, API key 미설정, 네트워크 실패, NODATA, 응답 누락이 있어도 Docker Compose 데모가 깨지지 않도록 fallback provider를 유지합니다. 관련 결정은 ADR-006, ADR-012에 있습니다.

**이미지는 DB가 아니라 저장소에 둠**

옷 JSON API를 multipart로 바꾸지 않고 이미지 업로드/조회/삭제를 별도 보호 API로 분리했습니다. 파일 bytes는 파일 시스템 또는 volume에 저장하고 DB에는 metadata만 둡니다. 이후 S3로 옮겨도 application service가 storage interface에만 의존하도록 경계를 뒀습니다.

**배포보다 제품 완성도를 먼저 정리**

원래 MVP9 후보였던 AWS 배포는 후속 MVP로 미루고, 현재는 UI/UX 리디자인을 진행합니다. 이유는 계정/추천/날씨/이미지 기능이 갖춰진 상태에서 사용자가 실제 서비스처럼 이해할 수 있는 화면 완성도가 먼저 필요했기 때문입니다. 관련 결정은 ADR-014입니다.

## Current MVP

현재 문서 기준은 **MVP9: 프론트 UI/UX 리디자인 MVP**입니다.

MVP9는 MVP8 계정 안정성 완료 상태 위에서 Auth, 추천, 옷장, 내 취향, 위치, 기록, 계정 설정 화면의 완성도를 높입니다. MVP9 자체는 백엔드 HTTP API, DTO, DB schema, 추천 점수/필터/tie-break를 변경하지 않습니다.

MVP가 바뀔 때 README에서 주로 갱신하는 위치는 이 섹션과 `Documentation`의 MVP-specific 링크입니다. 상세 제품 범위는 `docs/PRD.md`, API 계약은 `docs/API.md`, 추천 규칙은 `docs/RECOMMENDATION_RULES.md`를 우선합니다.

## Getting Started

로컬 백엔드:

```bash
./gradlew bootRun
```

로컬 프론트엔드:

```bash
cd frontend
npm run dev
```

Docker Compose:

```bash
test -f .env || cp .env.example .env
docker compose up --build
```

자세한 명령과 Docker Compose smoke 절차는 `docs/COMMANDS.md`를 따릅니다.

## Verification

문서 변경:

```bash
git diff --check
python3 scripts/checks.py --docs-check
```

백엔드 변경:

```bash
./gradlew test
./gradlew build
```

프론트 변경:

```bash
cd frontend
npm run build
```

## Documentation

| 영역 | Source of truth |
| --- | --- |
| 제품 목표, MVP 범위, 포함/제외 | `docs/PRD.md` |
| HTTP API, 인증 경계, DTO, 에러 코드 | `docs/API.md` |
| 추천 후보, 점수, tie-break, 추천 이유 | `docs/RECOMMENDATION_RULES.md` |
| 백엔드 구조, 저장소, 트랜잭션 | `docs/ARCHITECTURE.md` |
| DB schema, entity/JPA 기준 | `docs/ERD.md` |
| 프론트 타입, API client, UX, 반응형 기준 | `docs/FRONTEND.md` |
| MVP9 디자인 reference | `docs/design/mvp9/README.md` |
| 데모와 수동 검증 | `docs/DEMO_SCENARIO.md` |
| Docker Compose 공유와 환경변수 | `docs/SHARING_GUIDE.md` |
| 실행 명령과 검증 명령 | `docs/COMMANDS.md` |
| MVP 변경 체크리스트 | `docs/MVP_CHANGE_CHECKLIST.md` |
| 결정 기록과 변경 이력 | `docs/ADR.md`, `docs/adr/` |
