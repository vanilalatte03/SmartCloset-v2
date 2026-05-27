# 단계 4: account-hard-delete

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/adr/013-mvp8-account-stability.md`
- `src/main/java/com/smartcloset/user/**`
- `src/main/java/com/smartcloset/clothing/**`
- `src/main/java/com/smartcloset/recommendation/**`
- `src/main/java/com/smartcloset/auth/**`
- `src/test/java/com/smartcloset/**`

## 작업

- `DELETE /api/users/me` 보호 API를 추가한다.
- Password login enabled 계정은 현재 비밀번호를 검증한다.
- Google-only 계정은 `confirmation=DELETE`를 요구한다.
- 삭제 전 현재 사용자 clothing image stored filename을 수집한다.
- 현재 사용자 소유 wear histories, recommendation result items, recommendation results, clothing items, refresh sessions, account action tokens, social accounts, user row를 삭제한다.
- 이미지 파일은 `ClothingImageStorage.delete`로 삭제한다.
- 삭제는 현재 사용자 데이터만 대상으로 한다.
- 삭제 후 기존 access token으로 보호 resource를 조회할 수 없어야 한다.

## 인수 기준

```bash
./gradlew test --tests '*User*' --tests '*Auth*' --tests '*Clothing*' --tests '*Recommendation*'
git diff --check
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 아키텍처 체크리스트를 확인한다:
   - 다른 사용자 데이터가 삭제되지 않는가?
   - 이미지 파일 cleanup이 storage interface를 통해 수행되는가?
   - Password 계정은 현재 password를 검증하는가?
   - 삭제 후 stale access token으로 현재 사용자 resource를 읽을 수 없는가?
3. 결과에 따라 `phases/8-smartcloset-account-stability/index.json`의 해당 단계를 업데이트한다:
   - 성공 -> `"status": "completed"`, `"summary": "계정 hard delete API와 소유 데이터/이미지 cleanup을 추가했다."`
   - 수정 3회 시도 후에도 실패 -> `"status": "error"`, `"error_message": "구체적 에러 내용"`
   - 사용자 개입 필요 -> `"status": "blocked"`, `"blocked_reason": "구체적 사유"` 후 즉시 중단

검증 또는 리뷰가 통과하지 못하면 `issues/8-smartcloset-account-stability/issue-N.md`에 재현 명령, 핵심 에러, 수정 방향을 기록하고 fix step을 추가한다.

## 금지사항

- soft delete를 구현하지 마라. 이유: MVP8 결정은 즉시 hard delete다.
- S3 구현체를 추가하지 마라. 이유: MVP8은 `ClothingImageStorage` 경계만 사용한다.
- 관리자 삭제 API를 추가하지 마라. 이유: MVP8 범위는 현재 사용자 계정 삭제다.
- 기존 테스트를 깨뜨리지 마라.
