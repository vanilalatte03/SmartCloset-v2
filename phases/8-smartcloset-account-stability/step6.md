# 단계 6: aws-ready-local-profile-boundaries

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/ARCHITECTURE.md`
- `docs/SHARING_GUIDE.md`
- `docs/COMMANDS.md`
- `docs/adr/013-mvp8-account-stability.md`
- `.env.example`
- `docker-compose.yml`
- `src/main/resources/application.yml`

## 작업

- local Docker Compose 실행 흐름이 MVP8 변경 후에도 유지되는지 확인한다.
- refresh cookie, CORS allowed origins/credentials, OAuth redirect/base URL이 properties/env로 분리되어 있는지 확인한다.
- `EmailSender`가 interface이고 MVP8 구현체가 `ConsoleEmailSender`인지 확인한다.
- 계정 삭제가 `ClothingImageStorage` interface를 통해 이미지 cleanup을 수행하는지 확인한다.
- `.env.example`, `docs/SHARING_GUIDE.md`, `docs/COMMANDS.md`의 local 실행 값과 MVP9 AWS-ready boundary 설명을 동기화한다.
- 필요하면 `local` profile과 future `prod` profile의 분리 기준을 문서와 설정에 반영한다.

## 인수 기준

```bash
docker compose config --quiet
git diff --check
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 아키텍처 체크리스트를 확인한다:
   - AWS SDK, S3, SES, Secrets Manager 구현이 추가되지 않았는가?
   - local Docker Compose가 기본 실행 경로로 남아 있는가?
   - MVP9에서 prod profile을 추가해도 local profile을 유지할 수 있는가?
3. 결과에 따라 `phases/8-smartcloset-account-stability/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "local 실행 유지와 MVP9 AWS-ready adapter/profile 경계를 정리했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

검증 또는 리뷰가 통과하지 못하면 `issues/8-smartcloset-account-stability/issue-N.md`에 재현 명령, 핵심 에러, 수정 방향을 기록하고 fix step을 추가한다.

## 금지사항

- AWS 배포를 구현하지 마라. 이유: AWS 배포는 MVP9 범위다.
- S3 구현체를 추가하지 마라. 이유: MVP8은 storage interface 경계만 준비한다.
- SES/SMTP 실제 발송 구현체를 추가하지 마라. 이유: MVP8은 `ConsoleEmailSender` 기준이다.
- local Docker Compose 실행을 깨뜨리지 마라. 이유: MVP8 공유 경로는 local Compose다.
