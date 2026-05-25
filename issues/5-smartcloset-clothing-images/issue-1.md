# Issue 1: 5-smartcloset-clothing-images step 2 자동 리뷰 실패 1

## 발생 위치
- Phase: 5-smartcloset-clothing-images
- Step: 2 `clothing-image-api`
- PR: https://github.com/vanilalatte03/SmartCloset-v2/pull/74

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
| 금지 범위 | 통과 | MVP 제외 범위와 금지 API 검색 |
| 자체 리뷰 | 실패 | Codex read-only review |

## 확인한 명령

```bash
python3 scripts/checks.py --stage manual
git diff --check origin/main...HEAD
```

## 발견사항
- src/test/java/com/smartcloset/clothing/ClothingControllerTest.java:334 adds upload metadata coverage and line 398 adds delete idempotency coverage, but there is no test that uploads a second image to the same clothing item and verifies replacement behavior. Step 2 implements PUT /api/clothes/{clothingId}/image, and the SmartCloset image rules define it as upload/replacement (.agents/skills/smartcloset-backend/SKILL.md:152) with required replacement test coverage (.agents/skills/smartcloset-backend/SKILL.md:249). Add a controller or service test that uploads image A, uploads image B to the same clothingId, verifies the response/GET bytes reflect image B, and ideally verifies the previous stored file is no longer used.

## 리뷰 결론
블로커가 있어 merge하지 않습니다.

## 수정 방향
- 같은 PR 브랜치에서 발견사항을 수정하고 같은 gate를 다시 통과시킨다.

## 완료 기준
- 로컬 검증, 금지 범위 검색, Codex 자체 리뷰를 모두 통과한다.
