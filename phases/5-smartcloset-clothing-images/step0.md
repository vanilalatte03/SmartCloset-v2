# 단계 0: mvp5-scope-docs

## 읽어야 할 파일

- `.agents/skills/smartcloset-backend/SKILL.md`
- `AGENTS.md`
- `README.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/ARCHITECTURE.md`
- `docs/FRONTEND.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/ERD.md`
- `docs/DEMO_SCENARIO.md`
- `docs/SHARING_GUIDE.md`
- `docs/COMMANDS.md`
- `docs/adr/010-mvp5-clothing-images.md`

## 작업

문서가 MVP5 옷 이미지 업로드 범위를 일관되게 설명하는지 검토하고 필요한 누락을 보완한다.

- MVP4 archive는 `archive/mvp-4/` 최소 요약으로만 유지한다.
- 현재 source of truth는 루트 `README.md`와 `docs/` 아래 MVP5 문서다.
- 이미지 API 형태는 기존 JSON 옷 API 유지 + 별도 보호 이미지 API로 고정한다.
- 이미지 접근은 보호 API로 고정한다.
- 파일 제한은 5MB, jpg/jpeg/png/webp로 고정한다.
- AI 자동 태깅, 다중 이미지, S3/CDN, 이미지 기반 추천 점수는 제외 범위로 유지한다.

## 인수 기준

```bash
git diff --check
python3 -m compileall scripts
rg -n 'MVP5|이미지 업로드|/api/clothes/.*/image|CLOTHING_IMAGE_STORAGE_DIR' README.md docs AGENTS.md .agents/skills/smartcloset-backend/SKILL.md phases/5-smartcloset-clothing-images
! rg -n 'GET /api/recommendations/(today)' README.md docs AGENTS.md .agents/skills/smartcloset-backend/SKILL.md frontend/src
! rg -n -F -e 'POST /api/recommendations''?userId' -e '/api/clothes''?userId' -e '/api/users/location''?userId' README.md docs AGENTS.md .agents/skills/smartcloset-backend/SKILL.md frontend/src
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. 문서 충돌 시 `docs/PRD.md`, `docs/API.md`, `docs/RECOMMENDATION_RULES.md`를 우선한다.
3. 성공하면 `phases/5-smartcloset-clothing-images/index.json`의 Step 0을 completed로 갱신한다.
4. 실패하면 `issues/5-smartcloset-clothing-images/issue-N.md`에 원인을 기록한다.

## 금지사항

- 코드 구현을 시작하지 마라. 이유: Step 0은 문서 전환 검증 단계다.
- 전체 MVP4 문서 복사본을 archive에 넣지 마라. 이유: archive는 최소 요약만 보관한다.
- 이미지 업로드를 공개 API로 문서화하지 마라. 이유: 사용자 소유 이미지가 인증 밖으로 노출된다.
