# Phase: SmartCloset 9차 UI/UX Redesign MVP

## 목표

MVP8 계정 안정성 완료 baseline 위에서 `tmp/design-preview`와 `docs/design/mvp9/` 시안을 강하게 참고해 프론트 화면 완성도를 높인다.

AWS 배포는 구현하지 않는다. 원래 MVP9 후보였던 AWS 배포, S3, SES/SMTP, Secrets Manager, CD 자동화는 후속 MVP로 연기한다.

## 작업 범위

- Must-have / MVP9 P0: MVP8 archive, MVP9 docs/ADR/agent 전환, 디자인 reference 문서화, app shell navigation 리디자인, Auth view 리디자인, 추천 dashboard 리디자인, 옷장 목록/등록 UX 리디자인, 취향/위치/기록 화면 리디자인, profile 기반 계정 설정 진입, 데스크톱/모바일 반응형 QA
- Should-have / MVP9 P1: microcopy polish, empty/loading/error 상태 시각 정리, focus/hover 상태 polish
- MVP9 제외: AWS 배포 구현, S3 구현체, SES/SMTP 실제 발송 구현체, Secrets Manager, CD 자동화, Redis, 백엔드 API/DTO 변경, DB schema 변경, 추천 규칙 변경, AI/GPT 추천, AI 자동 태깅

## Steps

| Step | Name | Range |
| ---: | --- | --- |
| 0 | mvp9-docs-archive | Must-have / MVP9 P0 |
| 1 | app-shell-auth-redesign | Must-have / MVP9 P0 |
| 2 | recommendation-dashboard | Must-have / MVP9 P0 |
| 3 | closet-list-form-images | Must-have / MVP9 P0 |
| 4 | preferences-location | Must-have / MVP9 P0 |
| 5 | history | Must-have / MVP9 P0 |
| 6 | account-settings | Must-have / MVP9 P0 |
| 7 | global-focus-hover-polish | Must-have / MVP9 P0 |
| 8 | docs-qa | Must-have / MVP9 P0 |

## 단계 진행 원칙

- Step 0은 문서 전환, MVP8 archive, ADR, phase 정의, 디자인 reference 정리만 다룬다.
- Step 1은 app shell navigation과 Auth view 리디자인만 다룬다.
- Step 2는 추천 dashboard와 추천 결과 표시만 다룬다.
- Step 3은 Closet 목록, 등록/수정 form, 이미지 표시 UX만 다룬다.
- Step 4는 Preferences와 Location 화면 리디자인만 다룬다.
- Step 5는 History 화면 리디자인만 다룬다.
- Step 6은 profile 기반 Account settings 진입과 계정 설정 화면만 다룬다.
- Step 7은 shell/shared focus-visible, hover, disabled, loading 상태 polish만 다룬다.
- Step 1-6은 각 담당 화면의 1440px 데스크톱과 390px 모바일 기본 겹침/잘림 점검을 포함한다.
- Step 8은 문서 동기화, 전체 화면 브라우저 수동 QA 기록, 최종 검증을 수행한다.

## 완료 기준

- 현재 baseline 문서가 MVP9 UI/UX 리디자인을 가리킨다.
- MVP8 계정 안정성은 `archive/mvp-8/`에 최소 요약으로만 남는다.
- `docs/design/mvp9/`에 디자인 reference와 사용 원칙이 문서화된다.
- 데스크톱 primary nav는 `추천`, `옷장`, `내 취향`, `위치`, `기록` 상단 탭이다.
- 모바일 primary nav는 같은 5개 탭의 하단 navigation이다.
- `계정 설정`은 primary nav가 아니라 profile pill/menu에서 진입한다.
- Auth, 추천, 옷장, 취향, 위치, 기록, 계정 설정 화면이 1440px 데스크톱과 390px 모바일에서 겹침/잘림 없이 동작한다.
- MVP8 세션 정책, 이메일 인증, 비밀번호 재설정, Google provider 상태, 계정 삭제 UX가 유지된다.
- 백엔드 HTTP API, DTO, DB schema, 추천 점수/필터/tie-break가 변경되지 않는다.
- AWS/S3/SES/Secrets Manager/CD/Redis 구현이 추가되지 않는다.

## 검증 명령

```bash
git diff --check
(cd frontend && npm run build)
python3 scripts/checks.py --docs-check-config phases/9-smartcloset-ui-ux-redesign/docs-checks.json --docs-check
```

각 step의 검증은 step 파일의 `## 인수 기준`을 기준으로 수행한다. Step 1-7 frontend 화면/공통 UI step은 `frontend-build` 중심으로 검증하고, backend test/build를 포함한 full local safety 검증은 docs-qa/final 단계에서 수행한다.

최종 step에서는 가능한 경우 브라우저에서 아래를 확인한다.

```text
desktop 1440px: Auth, 추천, 옷장, 내 취향, 위치, 기록, 계정 설정
mobile 390px: Auth, 추천, 옷장, 내 취향, 위치, 기록, 계정 설정
```

## 실행 예시

```bash
python3 scripts/execute.py 9-smartcloset-ui-ux-redesign --next-step-only
python3 scripts/execute.py 9-smartcloset-ui-ux-redesign
python3 scripts/autopilot.py 9-smartcloset-ui-ux-redesign --base main --max-review-fixes 2 --unsafe
```
