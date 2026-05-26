# 단계 0: mvp7-scope-docs

## 읽어야 할 파일

먼저 아래 파일들을 읽고 프로젝트의 아키텍처와 설계 의도를 파악하라:

- `AGENTS.md`
- `.agents/skills/smartcloset-backend/SKILL.md`
- `docs/MVP_CHANGE_CHECKLIST.md`
- `docs/PRD.md`
- `docs/API.md`
- `docs/RECOMMENDATION_RULES.md`
- `docs/ARCHITECTURE.md`
- `docs/ERD.md`
- `docs/FRONTEND.md`
- `docs/ADR.md`
- `docs/adr/012-mvp7-location-weather-trust.md`
- `phases/7-smartcloset-location-weather-trust/README.md`

## 작업

- MVP7 문서 전환 결과가 현재 문서 기준과 충돌하지 않는지 확인한다.
- `archive/mvp-6/` 최소 요약이 전체 문서 복사본이 아닌지 확인한다.
- `README.md`, `AGENTS.md`, `.agents/skills/smartcloset-backend/SKILL.md`, `.codex/agents/*.toml`의 현재 baseline이 MVP7과 ADR-012를 가리키는지 확인한다.
- `phases/7-smartcloset-location-weather-trust/docs-checks.json`이 MVP7 핵심 회귀 신호만 검사하는지 확인한다.
- 누락된 MVP7 문서 마커가 있으면 문서만 수정한다.

## 인수 기준

```bash
git diff --check
python3 scripts/checks.py --docs-check-config phases/7-smartcloset-location-weather-trust/docs-checks.json --docs-check
```

## 검증 절차

1. 위 AC 커맨드를 실행한다.
2. `docs/MVP_CHANGE_CHECKLIST.md`의 항상 확인할 파일 목록을 기준으로 문서 누락을 확인한다.
3. 결과에 따라 `phases/7-smartcloset-location-weather-trust/index.json`의 해당 단계를 업데이트한다.

## 금지사항

- 백엔드 또는 프론트 구현 코드를 수정하지 마라. 이유: 이 단계는 MVP7 문서 전환 확인만 담당한다.
- `archive/mvp-6/`에 MVP6 전체 문서를 복사하지 마라. 이유: archive는 최소 요약만 유지한다.
- MVP7에서 제외한 외부 지도/주소 API를 포함 범위로 바꾸지 마라. 이유: ADR-012 결정과 충돌한다.
