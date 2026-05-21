# Harness PR Autopilot Workflow

## Status
Accepted

## Context
SmartCloset 구현은 Codex가 phase 단위로 브랜치를 만들고 커밋하는 Harness 흐름을 사용한다. 기존 실행기는 step 실행, 커밋, push까지만 담당하므로 PR 생성, 자체 리뷰, 이슈 기록, fix PR 반복, 병합이 자동 운영 규칙으로 닫혀 있지 않았다.

## Decision
`scripts/autopilot.py`를 Harness 상위 runner로 둔다.

- phase 실행은 `scripts/execute.py`가 담당한다.
- PR 생성, 자체 리뷰 gate, GitHub Issue 및 로컬 issue 기록, fix PR 재시도, 자동 병합은 `scripts/autopilot.py`가 담당한다.
- 자동 병합은 로컬 검증과 자체 리뷰가 모두 통과한 PR에만 허용한다.
- 리뷰 실패는 GitHub Issue와 `issues/{phase}/issue-N.md`에 함께 기록한다.
- 자동 병합 방식은 squash merge로 고정한다.

## Consequences
- 구현 작업은 `codex/{phase}` 브랜치와 PR 중심으로 추적된다.
- 실패 원인은 GitHub와 로컬 파일 양쪽에 남아 재시도 맥락이 보존된다.
- `gh auth status`가 유효하지 않으면 자동 PR 루프는 시작할 수 없다.
- GitHub Actions가 생기기 전에는 로컬 gate를 기준으로 판정하고, CI가 생기면 `gh pr checks`를 gate에 추가한다.
