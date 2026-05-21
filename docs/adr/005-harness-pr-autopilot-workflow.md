# Harness PR Autopilot Workflow

## Status
Accepted

## Context
SmartCloset 구현은 Codex가 Harness step 단위로 작게 변경하고 리뷰받는 흐름을 사용한다. phase 전체를 한 PR로 만들면 변경 범위가 커지고 자체 리뷰 코멘트가 사람이 읽기 어려워진다.

## Decision
`scripts/autopilot.py`를 Harness 상위 runner로 둔다.

- 단일 step 실행은 `scripts/execute.py --step N`이 담당한다.
- PR 생성, 자체 리뷰 gate, GitHub Issue 및 로컬 issue 기록, 같은 PR 브랜치의 자동 수정 재시도, 자동 병합은 `scripts/autopilot.py`가 담당한다.
- 자동 병합은 해당 step PR의 로컬 검증과 자체 리뷰가 모두 통과한 경우에만 허용한다.
- 리뷰 실패는 같은 PR의 자체 리뷰 코멘트, GitHub Issue, `issues/{phase}/issue-N.md`에 함께 기록한 뒤 같은 브랜치에서 자동 수정과 재리뷰를 최대 2회 진행한다.
- 자동 수정 후에도 실패하면 PR과 Issue를 열어둔 채 루프를 중단한다.
- 자동 병합 방식은 squash merge로 고정한다.

## Consequences
- 구현 작업은 `codex/{phase}-step{N}-{name}` 브랜치와 작은 PR 중심으로 추적된다.
- 실패 원인은 GitHub와 로컬 파일 양쪽에 남아 재시도 맥락이 보존되며, 새 fix PR이 중복으로 쌓이지 않는다.
- `gh auth status`가 유효하지 않으면 자동 PR 루프는 시작할 수 없다.
- GitHub Actions가 생기기 전에는 로컬 gate를 기준으로 판정하고, CI가 생기면 `gh pr checks`를 gate에 추가한다.
