import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).parent))
import autopilot as ap

REPO_ROOT = Path(__file__).resolve().parent.parent


def cp(cmd=None, returncode=0, stdout="", stderr=""):
    return subprocess.CompletedProcess(cmd or [], returncode, stdout, stderr)


def _install_phase_scope_rules(tmp_repo, phase):
    """실제 저장소의 phase scope-rules.json을 테스트 repo에 복사한다."""
    src = REPO_ROOT / "phases" / phase / "scope-rules.json"
    dst_dir = tmp_repo / "phases" / phase
    dst_dir.mkdir(parents=True, exist_ok=True)
    shutil.copy(src, dst_dir / "scope-rules.json")


@pytest.fixture
def tmp_repo(tmp_path):
    (tmp_path / "issues").mkdir()
    codex_dir = tmp_path / ".codex"
    codex_dir.mkdir()
    # 전역 금지 규칙은 실제 저장소 파일을 그대로 사용해 동작 일치를 검증한다.
    shutil.copy(REPO_ROOT / ".codex" / "scope-rules.json", codex_dir / "scope-rules.json")
    phase_dir = tmp_path / "phases" / "1-smartcloset-mvp"
    phase_dir.mkdir(parents=True)
    (phase_dir / "index.json").write_text(
        json.dumps(
            {
                "project": "SmartCloset",
                "phase": "1-smartcloset-mvp",
                "steps": [
                    {"step": 0, "name": "project-scaffold", "status": "pending"},
                    {"step": 1, "name": "clothing-p0-api", "status": "pending"},
                ],
            },
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )
    (phase_dir / "step0.md").write_text("# 단계 0\n\n## 작업\n프로젝트 골격을 만든다.\n", encoding="utf-8")
    (phase_dir / "step1.md").write_text("# 단계 1\n\n## 작업\nClothing P0 API를 만든다.\n", encoding="utf-8")
    return tmp_path


@pytest.fixture
def runner(tmp_repo):
    return ap.AutopilotRunner("1-smartcloset-mvp", root=tmp_repo)


def _mark_step_complete(tmp_repo, step_num, summary="완료"):
    index_path = tmp_repo / "phases" / "1-smartcloset-mvp" / "index.json"
    index = json.loads(index_path.read_text(encoding="utf-8"))
    for step in index["steps"]:
        if step["step"] == step_num:
            step["status"] = "completed"
            step["summary"] = summary
    index_path.write_text(json.dumps(index, ensure_ascii=False), encoding="utf-8")


def test_preconditions_stop_on_dirty_worktree(runner):
    def fake_git(*args, check=True):
        if args == ("status", "--short", "--untracked-files=all"):
            return cp(stdout=" M README.md\n")
        return cp()

    runner._git = fake_git

    with pytest.raises(ap.AutopilotError) as exc_info:
        runner._ensure_preconditions()

    assert "작업트리가 clean 상태가 아닙니다" in str(exc_info.value)
    assert "README.md" in str(exc_info.value)


def test_preconditions_stop_on_gh_auth_failure(runner):
    runner._git = lambda *args, check=True: cp()

    def fake_gh(*args, check=True):
        raise ap.AutopilotError("gh auth failed")

    runner._gh = fake_gh

    with pytest.raises(ap.AutopilotError) as exc_info:
        runner._ensure_preconditions()

    assert "gh auth failed" in str(exc_info.value)


def test_preconditions_run_base_manual_checks_after_sync(runner):
    git_calls = []
    shell_calls = []

    def fake_git(*args, check=True):
        git_calls.append(args)
        return cp()

    def fake_run_shell(command, check=True, timeout=None):
        shell_calls.append((command, check, timeout))
        return cp()

    runner._git = fake_git
    runner._gh = lambda *args, check=True: cp()
    runner._run_shell = fake_run_shell

    runner._ensure_preconditions()

    assert ("fetch", "origin", "main") in git_calls
    assert ("checkout", "main") in git_calls
    assert ("pull", "--ff-only", "origin", "main") in git_calls
    assert shell_calls == [(ap.FALLBACK_REVIEW_CHECK_COMMAND, False, 1800)]


def test_preconditions_stop_when_base_manual_checks_fail(runner):
    runner._git = lambda *args, check=True: cp()
    runner._gh = lambda *args, check=True: cp()
    runner._run_shell = lambda command, check=True, timeout=None: cp(
        returncode=1,
        stderr="backend test failed",
    )

    with pytest.raises(ap.AutopilotError) as exc_info:
        runner._ensure_preconditions()

    message = str(exc_info.value)
    assert "base 브랜치 `main`의 manual 검증이 이미 실패" in message
    assert "backend test failed" in message


def test_runner_rejects_xhigh_without_allow_flag(tmp_repo):
    with pytest.raises(ValueError, match="--allow-xhigh"):
        ap.AutopilotRunner("1-smartcloset-mvp", root=tmp_repo, step_effort="xhigh")


def test_run_step_passes_default_effort_to_execute(runner):
    calls = []
    step = {"step": 0, "name": "project-scaffold"}

    def fake_run(cmd, check=True, timeout=None):
        calls.append((cmd, timeout))
        return cp()

    runner._run = fake_run
    runner._run_step("codex/branch", step)

    cmd, timeout = calls[0]
    assert timeout == 1800
    assert cmd[:3] == [sys.executable, "scripts/execute.py", "1-smartcloset-mvp"]
    assert "--codex-effort" in cmd
    assert cmd[cmd.index("--codex-effort") + 1] == "medium"
    assert "--allow-xhigh" not in cmd


def test_run_step_passes_xhigh_allow_flag_to_execute(tmp_repo):
    runner = ap.AutopilotRunner(
        "1-smartcloset-mvp",
        root=tmp_repo,
        step_effort="xhigh",
        allow_xhigh=True,
    )
    calls = []

    runner._run = lambda cmd, check=True, timeout=None: calls.append(cmd) or cp()
    runner._run_step("codex/branch", {"step": 0, "name": "project-scaffold"})

    cmd = calls[0]
    assert cmd[cmd.index("--codex-effort") + 1] == "xhigh"
    assert "--allow-xhigh" in cmd


def test_codex_review_uses_high_effort(runner):
    calls = []

    def fake_run(cmd, check=True, timeout=None, input=None):
        calls.append((cmd, check, timeout, input))
        return cp(stdout='{"pass": true, "summary": "ok", "findings": []}')

    runner._run = fake_run

    result = runner._run_codex_review({"step": 1, "name": "clothing-p0-api"})

    cmd, check, timeout, prompt = calls[0]
    assert result.passed is True
    assert check is False
    assert timeout == 1800
    assert cmd[:3] == ["codex", "exec", "--json"]
    assert 'model_reasoning_effort="high"' in cmd
    assert "-" in cmd
    assert "-o" in cmd
    assert "Read-only review only" in prompt


def test_codex_fix_uses_medium_effort(runner, tmp_repo):
    calls = []
    issue = ap.IssueRecord(
        1,
        "리뷰 실패",
        "## Issue\n- 실패",
        tmp_repo / "issues" / "1-smartcloset-mvp" / "issue-1.md",
        "",
    )
    review = ap.ReviewResult(False, ["실패"], "fail")

    runner._run = lambda cmd, check=True, timeout=None, input=None: calls.append((cmd, timeout, input)) or cp()
    runner._invoke_codex_fix(
        issue,
        "codex/branch",
        {"step": 0, "name": "project-scaffold"},
        review,
        1,
    )

    cmd, timeout, prompt = calls[0]
    assert timeout == 1800
    assert cmd[:3] == ["codex", "exec", "--json"]
    assert 'model_reasoning_effort="medium"' in cmd
    assert cmd[-1] == "-"
    assert "자동 리뷰 수정 담당자" in prompt


def test_step_success_creates_draft_pr_comments_and_merges(runner, tmp_repo):
    gh_calls = []
    executed = []

    runner._ensure_preconditions = lambda: None
    runner._sync_base = lambda: None

    def fake_run_step(branch, step):
        executed.append((branch, step["step"]))
        _mark_step_complete(tmp_repo, step["step"], f"{step['name']} 완료")

    runner._run_step = fake_run_step
    runner._run_review_gate = lambda step: ap.ReviewResult(True, [], "ok", commands=("cmd",))

    def fake_gh(*args, check=True, timeout=None):
        gh_calls.append(args)
        if args[:2] == ("pr", "create"):
            return cp(stdout=f"https://github.com/org/repo/pull/{len([c for c in gh_calls if c[:2] == ('pr', 'create')])}\n")
        return cp()

    runner._gh = fake_gh

    pr_urls = runner.run()

    assert executed == [
        ("codex/1-smartcloset-mvp-step0-project-scaffold", 0),
        ("codex/1-smartcloset-mvp-step1-clothing-p0-api", 1),
    ]
    assert ("pr", "checks", "https://github.com/org/repo/pull/1", "--watch") in gh_calls
    assert "https://github.com/org/repo/pull/1" in pr_urls
    assert "https://github.com/org/repo/pull/2" in pr_urls
    assert gh_calls[0][:8] == (
        "pr",
        "create",
        "--base",
        "main",
        "--head",
        "codex/1-smartcloset-mvp-step0-project-scaffold",
        "--title",
        "feat: 1-smartcloset-mvp 0단계 project-scaffold 구현",
    )
    assert "--draft" in gh_calls[0]
    assert any(call[:2] == ("pr", "comment") and "## 자체 리뷰" in call[4] for call in gh_calls)
    assert ("pr", "ready", "https://github.com/org/repo/pull/1") in gh_calls
    assert ("pr", "merge", "https://github.com/org/repo/pull/2", "--squash", "--delete-branch") in gh_calls


def test_pr_body_uses_step_task_as_change_reason(runner):
    runner._changed_files = lambda: ["src/main/java/com/smartcloset/Clothing.java"]

    body = runner._pr_body("codex/branch", {"step": 1, "name": "clothing-p0-api"})

    assert "## 변경 이유\n- Clothing P0 API를 만들기 위해 필요한 변경을 반영했습니다.\n\n" in body
    assert "SmartCloset 구현을 작은 step 단위로 리뷰하고 안전하게 병합하기 위해 분리했습니다." not in body
    assert "- Draft PR로 생성하며 자체 리뷰 gate 통과 시 ready 전환 후 squash merge합니다." in body


def test_step_change_reason_uses_first_sentence_only(runner, tmp_repo):
    step_path = tmp_repo / "phases" / "1-smartcloset-mvp" / "step1.md"
    step_path.write_text(
        "# 단계 1\n\n"
        "## 작업\n"
        "KMA provider 구현에 필요한 설정 계약을 코드에 준비한다. 이 단계는 설정 바인딩만 다룬다.\n",
        encoding="utf-8",
    )

    reason = runner._step_change_reason({"step": 1, "name": "clothing-p0-api"})

    assert reason == "KMA provider 구현에 필요한 설정 계약을 코드에 준비하기 위해 필요한 변경을 반영했습니다."


def test_run_executes_final_gate_when_no_pending_steps(runner, tmp_repo):
    _mark_step_complete(tmp_repo, 0, "0 완료")
    _mark_step_complete(tmp_repo, 1, "1 완료")
    calls = []

    runner._ensure_preconditions = lambda: calls.append("preconditions")
    runner._sync_base = lambda: calls.append("sync")
    runner._run_final_gate = lambda: calls.append("final-gate")

    result = runner.run()

    assert result == "No pending steps for 1-smartcloset-mvp."
    assert calls == ["preconditions", "final-gate"]


def test_review_fail_records_issue_and_leaves_pr_open(runner, tmp_repo):
    gh_calls = []
    runner.max_review_fixes = 0
    runner._ensure_preconditions = lambda: None
    runner._sync_base = lambda: None
    runner._run_step = lambda branch, step: _mark_step_complete(tmp_repo, step["step"], "완료")
    runner._run_review_gate = lambda step: ap.ReviewResult(
        False,
        ["src/main/java/App.java:10 - 테스트 실패"],
        "fail",
        checks_passed=False,
    )

    def fake_gh(*args, check=True):
        gh_calls.append(args)
        if args[:2] == ("pr", "create"):
            return cp(stdout="https://github.com/org/repo/pull/8\n")
        if args[:2] == ("issue", "create"):
            return cp(stdout="https://github.com/org/repo/issues/1\n")
        return cp()

    runner._gh = fake_gh

    with pytest.raises(ap.AutopilotError):
        runner.run()

    issue_path = tmp_repo / "issues" / "1-smartcloset-mvp" / "issue-1.md"
    assert issue_path.exists()
    issue_text = issue_path.read_text(encoding="utf-8")
    assert "Step: 0 `project-scaffold`" in issue_text
    assert "src/main/java/App.java:10 - 테스트 실패" in issue_text
    assert any(call[:2] == ("issue", "create") for call in gh_calls)
    assert any(call[:2] == ("pr", "comment") and "## 자체 리뷰" in call[4] for call in gh_calls)
    assert not any(call[:2] == ("pr", "close") for call in gh_calls)
    assert not any(call[:2] == ("pr", "merge") for call in gh_calls)


def test_review_fail_fixes_same_pr_then_merges_and_continues(runner, tmp_repo):
    gh_calls = []
    fix_calls = []
    dirty_commits = []
    pushed = []
    runner._ensure_preconditions = lambda: None
    runner._sync_base = lambda: None
    runner._run_step = lambda branch, step: _mark_step_complete(tmp_repo, step["step"], "완료")
    reviews = [
        ap.ReviewResult(False, ["src/App.java:1 - 실패"], "fail"),
        ap.ReviewResult(True, [], "ok"),
        ap.ReviewResult(True, [], "ok"),
    ]
    runner._run_review_gate = lambda step: reviews.pop(0)
    runner._invoke_codex_fix = lambda issue, branch, step, review, attempt: fix_calls.append(
        (issue.number, branch, step["step"], attempt)
    )
    runner._commit_dirty_fix = lambda step: dirty_commits.append(step["step"])
    runner._push_branch = lambda branch: pushed.append(branch)

    def fake_gh(*args, check=True, timeout=None):
        gh_calls.append(args)
        if args[:2] == ("pr", "create"):
            pr_count = len([call for call in gh_calls if call[:2] == ("pr", "create")])
            return cp(stdout=f"https://github.com/org/repo/pull/{pr_count}\n")
        if args[:2] == ("issue", "create"):
            return cp(stdout="https://github.com/org/repo/issues/1\n")
        return cp()

    runner._gh = fake_gh

    pr_urls = runner.run()

    assert fix_calls == [(1, "codex/1-smartcloset-mvp-step0-project-scaffold", 0, 1)]
    assert dirty_commits == [0]
    assert pushed == ["codex/1-smartcloset-mvp-step0-project-scaffold"]
    issue_text = (tmp_repo / "issues" / "1-smartcloset-mvp" / "issue-1.md").read_text(encoding="utf-8")
    assert "## 자동 수정 완료" in issue_text
    assert any(call[:2] == ("issue", "close") and call[2] == "https://github.com/org/repo/issues/1" for call in gh_calls)
    assert ("pr", "merge", "https://github.com/org/repo/pull/1", "--squash", "--delete-branch") in gh_calls
    assert ("pr", "merge", "https://github.com/org/repo/pull/2", "--squash", "--delete-branch") in gh_calls
    assert "https://github.com/org/repo/pull/1" in pr_urls
    assert "https://github.com/org/repo/pull/2" in pr_urls


def test_review_stops_after_max_fix_attempts_without_closing_pr(runner, tmp_repo):
    gh_calls = []
    fix_calls = []
    runner.max_review_fixes = 1
    runner._ensure_preconditions = lambda: None
    runner._sync_base = lambda: None
    runner._run_step = lambda branch, step: _mark_step_complete(tmp_repo, step["step"], "완료")
    reviews = [
        ap.ReviewResult(False, ["첫 실패"], "fail"),
        ap.ReviewResult(False, ["재시도 실패"], "fail again"),
    ]
    runner._run_review_gate = lambda step: reviews.pop(0)
    runner._invoke_codex_fix = lambda issue, branch, step, review, attempt: fix_calls.append(attempt)
    runner._commit_dirty_fix = lambda step: None
    runner._push_branch = lambda branch: None

    def fake_gh(*args, check=True):
        gh_calls.append(args)
        if args[:2] == ("pr", "create"):
            return cp(stdout="https://github.com/org/repo/pull/8\n")
        if args[:2] == ("issue", "create"):
            return cp(stdout="https://github.com/org/repo/issues/1\n")
        return cp()

    runner._gh = fake_gh

    with pytest.raises(ap.AutopilotError) as exc_info:
        runner.run()

    issue_path = tmp_repo / "issues" / "1-smartcloset-mvp" / "issue-1.md"
    assert "재시도 1 리뷰 실패" in issue_path.read_text(encoding="utf-8")
    assert fix_calls == [1]
    assert "최대 횟수" in str(exc_info.value)
    assert any(call[:2] == ("issue", "comment") for call in gh_calls)
    assert not any(call[:2] == ("pr", "close") for call in gh_calls)
    assert not any(call[:2] == ("pr", "merge") for call in gh_calls)


def test_parse_codex_review_json(runner):
    result = runner._parse_review_result('{"pass": true, "summary": "ok", "findings": []}')

    assert result.passed is True
    assert result.summary == "ok"
    assert result.findings == []


def test_parse_codex_review_json_from_event_stream(runner):
    stdout = "\n".join([
        '{"type":"started"}',
        '{"type":"message","message":{"content":[{"type":"output_text","text":"{\\"pass\\": true, \\"summary\\": \\"ok\\", \\"findings\\": []}"}]}}',
    ])

    result = runner._parse_review_result(stdout)

    assert result.passed is True
    assert result.summary == "ok"
    assert result.findings == []


def test_parse_codex_review_json_from_nested_item_event(runner):
    stdout = "\n".join([
        '{"type":"started"}',
        '{"type":"event","item":{"type":"message","content":[{"type":"output_text","text":"```json\\n{\\"pass\\": true, \\"summary\\": \\"ok\\", \\"findings\\": []}\\n```"}]}}',
        '{"type":"result","status":"success"}',
    ])

    result = runner._parse_review_result(stdout)

    assert result.passed is True
    assert result.summary == "ok"
    assert result.findings == []


def test_parse_codex_review_returns_none_when_json_missing(runner):
    assert runner._parse_review_result("plain text only") is None


def test_codex_review_prompt_excludes_issue_records_and_uses_step_contract(runner):
    prompt = runner._codex_review_prompt({"step": 1, "name": "clothing-p0-api"})

    assert "issues/**" in prompt
    assert "audit logs" in prompt
    assert "not implementation changes" in prompt
    assert "phases/1-smartcloset-mvp/README.md" in prompt
    assert "phases/1-smartcloset-mvp/step1.md" in prompt
    assert "Current Harness step is Step 1 `clothing-p0-api`" in prompt
    assert "Missing functionality assigned to future steps is not a blocker" in prompt
    assert "Implementing future-step scope inside the current step is a blocker" in prompt


def test_review_gate_passes_current_step_to_review_components(runner):
    step = {"step": 1, "name": "clothing-p0-api"}
    seen = {}

    def fake_run(cmd, check=True, timeout=None):
        if cmd == ["git", "diff", "--check", "origin/main...HEAD"]:
            return cp()
        raise AssertionError(cmd)

    def fake_run_shell(command, check=True, timeout=None):
        assert command == ap.FALLBACK_REVIEW_CHECK_COMMAND
        return cp()

    def fake_scan(current_step):
        seen["scan"] = current_step
        return []

    def fake_codex(current_step):
        seen["codex"] = current_step
        return ap.ReviewResult(True, [], "ok")

    runner._run = fake_run
    runner._run_shell = fake_run_shell
    runner._scan_forbidden_diff = fake_scan
    runner._run_codex_review = fake_codex

    review = runner._run_review_gate(step)

    assert review.passed is True
    assert seen == {"scan": step, "codex": step}
    assert review.commands == (
        ap.FALLBACK_REVIEW_CHECK_COMMAND,
        "git diff --check origin/main...HEAD",
    )


def test_step_acceptance_commands_read_step_fenced_block(tmp_repo):
    step_path = tmp_repo / "phases" / "1-smartcloset-mvp" / "step1.md"
    step_path.write_text(
        "\n".join([
            "# 단계 1",
            "",
            "## 작업",
            "Clothing P0 API를 만든다.",
            "",
            "## 인수 기준",
            "",
            "```bash",
            "(cd frontend && npm run build)",
            "python3 scripts/checks.py --docs-check",
            "```",
            "",
            "## 검증 절차",
            "1. 인수 기준을 실행한다.",
        ]),
        encoding="utf-8",
    )
    runner = ap.AutopilotRunner("1-smartcloset-mvp", root=tmp_repo)

    commands = runner._step_acceptance_commands({"step": 1, "name": "clothing-p0-api"})

    assert commands == (
        "(cd frontend && npm run build)",
        "python3 scripts/checks.py --docs-check",
    )
    assert runner._review_commands({"step": 1, "name": "clothing-p0-api"}) == (
        "(cd frontend && npm run build)",
        "python3 scripts/checks.py --docs-check",
        "git diff --check origin/main...HEAD",
    )


def test_review_gate_runs_step_acceptance_commands_for_step_review(tmp_repo):
    step_path = tmp_repo / "phases" / "1-smartcloset-mvp" / "step1.md"
    step_path.write_text(
        "\n".join([
            "# 단계 1",
            "",
            "## 작업",
            "Clothing P0 API를 만든다.",
            "",
            "## 인수 기준",
            "",
            "```bash",
            "(cd frontend && npm run build)",
            "```",
        ]),
        encoding="utf-8",
    )
    runner = ap.AutopilotRunner("1-smartcloset-mvp", root=tmp_repo)
    step = {"step": 1, "name": "clothing-p0-api"}
    shell_calls = []

    def fake_run_shell(command, check=True, timeout=None):
        shell_calls.append(command)
        return cp()

    def fake_run(cmd, check=True, timeout=None):
        if cmd == ["git", "diff", "--check", "origin/main...HEAD"]:
            return cp()
        raise AssertionError(cmd)

    runner._run_shell = fake_run_shell
    runner._run = fake_run
    runner._scan_forbidden_diff = lambda current_step: []
    runner._run_codex_review = lambda current_step: ap.ReviewResult(True, [], "ok")

    review = runner._run_review_gate(step)

    assert review.passed is True
    assert shell_calls == ["(cd frontend && npm run build)"]
    assert review.commands == (
        "(cd frontend && npm run build)",
        "git diff --check origin/main...HEAD",
    )


def test_review_gate_falls_back_to_manual_when_step_ac_missing(runner):
    step = {"step": 1, "name": "clothing-p0-api"}
    shell_calls = []

    def fake_run_shell(command, check=True, timeout=None):
        shell_calls.append(command)
        return cp()

    def fake_run(cmd, check=True, timeout=None):
        if cmd == ["git", "diff", "--check", "origin/main...HEAD"]:
            return cp()
        raise AssertionError(cmd)

    runner._run_shell = fake_run_shell
    runner._run = fake_run
    runner._scan_forbidden_diff = lambda current_step: []
    runner._run_codex_review = lambda current_step: ap.ReviewResult(True, [], "ok")

    review = runner._run_review_gate(step)

    assert review.passed is True
    assert shell_calls == [ap.FALLBACK_REVIEW_CHECK_COMMAND]


def test_review_commands_replace_step_diff_check_with_branch_diff(tmp_repo):
    step_path = tmp_repo / "phases" / "1-smartcloset-mvp" / "step1.md"
    step_path.write_text(
        "\n".join([
            "# 단계 1",
            "",
            "## 인수 기준",
            "",
            "```bash",
            "git diff --check",
            "python3 scripts/checks.py --docs-check",
            "```",
        ]),
        encoding="utf-8",
    )
    runner = ap.AutopilotRunner("1-smartcloset-mvp", root=tmp_repo)

    assert runner._review_commands({"step": 1, "name": "docs"}) == (
        "python3 scripts/checks.py --docs-check",
        "git diff --check origin/main...HEAD",
    )


def test_forbidden_diff_ignores_negated_docs_and_flags_added_scope(runner):
    forbidden_today_get = "GET " + "/api/recommendations/today"

    def fake_git(*args, check=True):
        assert args[:2] == ("diff", "--unified=0")
        return cp(
            stdout="\n".join([
                "diff --git a/phases/1-smartcloset-mvp/scope-rules.json b/phases/1-smartcloset-mvp/scope-rules.json",
                "+++ b/phases/1-smartcloset-mvp/scope-rules.json",
                "@@ -1,0 +1,1 @@",
                f'+    "anySubstrings": ["{forbidden_today_get}", "Redis"],',
                "diff --git a/issues/1-smartcloset-mvp/issue-1.md b/issues/1-smartcloset-mvp/issue-1.md",
                "+++ b/issues/1-smartcloset-mvp/issue-1.md",
                "@@ -1,0 +1,1 @@",
                "+- Redis 범위가 추가되었습니다.",
                "diff --git a/phases/4-smartcloset-usable-ux/step6-output.json b/phases/4-smartcloset-usable-ux/step6-output.json",
                "+++ b/phases/4-smartcloset-usable-ux/step6-output.json",
                "@@ -1,0 +1,1 @@",
                '+{"stdout": "AI/GPT 추천을 구현한다. Redis 캐싱을 구현한다."}',
                "diff --git a/docs/AUTH.md b/docs/AUTH.md",
                "+++ b/docs/AUTH.md",
                "@@ -1,0 +1,1 @@",
                "+refresh token을 반환하지 않는다.",
                "diff --git a/README.md b/README.md",
                "+++ b/README.md",
                "@@ -1,0 +1,5 @@",
                "+MVP4에서도 제외되는 범위:",
                "+- 이미지 업로드",
                "+- AI/GPT 추천",
                "+## 실행 전 요구사항",
                "+Docker",
                "diff --git a/docs/PRD.md b/docs/PRD.md",
                "+++ b/docs/PRD.md",
                "@@ -1,0 +1,12 @@",
                "+## 1차 MVP 제외 범위",
                "+- AI/GPT 추천",
                "+- Redis 캐싱",
                "+## P2: 1차 이후 또는 시간이 남을 경우",
                "+- 외부 Weather API 연동",
                "+- AWS 수동 배포",
                "+## 구현 범위",
                "+외부 Weather API는 구현하지 않는다.",
                "+외부 Weather API는 기상청 단기예보 getVilageFcst JSON 연동만 허용한다.",
                "+AWS 배포는 제공하지 않는다.",
                "+회원가입/로그인은 구현하지 않는다.",
                "+SmartCloset 1차 MVP의 추천은 AI/GPT 추천이 아니라 규칙 기반 추천이다.",
                '+rg -n "recommendations/today" .',
                "+! rg -n 'AI|이미지 업로드|이메일 인증' frontend/src",
                "+Spring Security와 회원가입을 구현한다.",
                "+OpenWeather 외부 Weather API 연동을 구현한다.",
                "+AWS 배포를 구현한다.",
                "+AI/GPT 추천을 구현한다.",
                "+refresh token을 구현한다.",
                "+소셜 로그인 기능을 구현한다.",
                "+이메일 인증을 구현한다.",
                "+비밀번호 재설정을 구현한다.",
                "+CD 자동화를 구현한다.",
                "+이미지 업로드를 구현한다.",
                "+다중 이미지 업로드를 구현한다.",
                "+AI 자동 태깅을 구현한다.",
                "+S3/CDN을 구현한다.",
                "+이미지 기반 추천 점수를 반영한다.",
                "+이미지 기반 추천 이유를 생성한다.",
                "+외부 지도 API를 구현한다.",
                "+Redis 캐싱을 구현한다.",
                f"+{forbidden_today_get}",
            ])
        )

    runner._git = fake_git

    findings = runner._scan_forbidden_diff()

    assert any("docs/PRD.md:" in finding and "Redis 범위" in finding for finding in findings)
    assert any("docs/PRD.md:" in finding and forbidden_today_get in finding for finding in findings)
    assert any("refresh token" in finding for finding in findings)
    assert any("소셜 로그인" in finding for finding in findings)
    assert any("이메일 인증" in finding for finding in findings)
    assert any("비밀번호 재설정" in finding for finding in findings)
    assert any("외부 Weather API" in finding for finding in findings)
    assert any("AWS 배포" in finding for finding in findings)
    assert any("CD 자동화" in finding for finding in findings)
    assert any("AI/GPT" in finding for finding in findings)
    assert any("다중 이미지" in finding for finding in findings)
    assert any("AI 자동 태깅" in finding for finding in findings)
    assert any("S3/CDN" in finding for finding in findings)
    assert any("이미지 기반 추천 점수/이유" in finding for finding in findings)
    assert any("외부 주소/지도 API" in finding for finding in findings)
    assert not any("이미지 업로드 범위" in finding for finding in findings)
    assert not any("docs/AUTH.md" in finding for finding in findings)
    assert not any("README.md" in finding for finding in findings)
    assert not any("step6-output.json" in finding for finding in findings)
    assert not any("scope-rules.json" in finding for finding in findings)
    assert not any("getVilageFcst" in finding for finding in findings)
    assert not any("로그인/회원가입" in finding for finding in findings)
    assert not any("Spring Security" in finding for finding in findings)
    assert len(findings) == len(set(findings))


def test_forbidden_diff_allows_mvp8_step0_account_scope(tmp_repo):
    _install_phase_scope_rules(tmp_repo, "8-smartcloset-account-stability")
    runner = ap.AutopilotRunner("8-smartcloset-account-stability", root=tmp_repo)

    def fake_git(*args, check=True):
        assert args[:2] == ("diff", "--unified=0")
        return cp(
            stdout="\n".join([
                "diff --git a/docs/PRD.md b/docs/PRD.md",
                "+++ b/docs/PRD.md",
                "@@ -1,0 +1,5 @@",
                "+refresh token을 구현한다.",
                "+이메일 인증을 구현한다.",
                "+비밀번호 재설정을 구현한다.",
                "+Google social login을 구현한다.",
                "+Redis 캐싱을 구현한다.",
            ])
        )

    runner._git = fake_git

    findings = runner._scan_forbidden_diff({"step": 0, "name": "mvp8-scope-docs-archive"})

    assert not any("refresh token 범위" in finding for finding in findings)
    assert not any("이메일 인증 범위" in finding for finding in findings)
    assert not any("비밀번호 재설정 범위" in finding for finding in findings)
    assert not any("소셜 로그인 범위" in finding for finding in findings)
    assert any("Redis 범위" in finding for finding in findings)


def test_forbidden_diff_keeps_mvp8_future_step_scope_blocked(tmp_repo):
    _install_phase_scope_rules(tmp_repo, "8-smartcloset-account-stability")
    runner = ap.AutopilotRunner("8-smartcloset-account-stability", root=tmp_repo)

    def fake_git(*args, check=True):
        assert args[:2] == ("diff", "--unified=0")
        return cp(
            stdout="\n".join([
                "diff --git a/docs/API.md b/docs/API.md",
                "+++ b/docs/API.md",
                "@@ -1,0 +1,4 @@",
                "+refresh token을 구현한다.",
                "+이메일 인증을 구현한다.",
                "+Google social login을 구현한다.",
                "+소셜 로그인 기능을 구현한다.",
            ])
        )

    runner._git = fake_git

    findings = runner._scan_forbidden_diff({"step": 1, "name": "refresh-token-session"})

    assert not any("refresh token 범위" in finding for finding in findings)
    assert any("이메일 인증 범위" in finding for finding in findings)
    assert any("소셜 로그인 범위" in finding for finding in findings)


def test_forbidden_diff_allows_mvp9_account_maintenance_context(tmp_repo):
    _install_phase_scope_rules(tmp_repo, "9-smartcloset-ui-ux-redesign")
    runner = ap.AutopilotRunner("9-smartcloset-ui-ux-redesign", root=tmp_repo)

    def fake_git(*args, check=True):
        assert args[:2] == ("diff", "--unified=0")
        return cp(
            stdout="\n".join([
                "diff --git a/phases/9-smartcloset-ui-ux-redesign/step1.md b/phases/9-smartcloset-ui-ux-redesign/step1.md",
                "+++ b/phases/9-smartcloset-ui-ux-redesign/step1.md",
                "@@ -1,0 +1,4 @@",
                "+Auth 기능은 MVP8 계약을 유지한다: 로그인, 이메일 인증, 비밀번호 재설정, Google social login provider 상태.",
                "+비밀번호 재설정 버튼과 기존 UX를 유지한다.",
                "+refresh token 원문은 JSON에 노출하지 않는다.",
                "+이메일 인증 상태와 로그인 제공자를 표시한다.",
            ])
        )

    runner._git = fake_git

    findings = runner._scan_forbidden_diff({"step": 1, "name": "app-shell-auth-redesign"})

    assert findings == []


def test_forbidden_diff_blocks_mvp9_account_maintenance_context_outside_account_steps(tmp_repo):
    _install_phase_scope_rules(tmp_repo, "9-smartcloset-ui-ux-redesign")
    runner = ap.AutopilotRunner("9-smartcloset-ui-ux-redesign", root=tmp_repo)

    def fake_git(*args, check=True):
        assert args[:2] == ("diff", "--unified=0")
        return cp(
            stdout="\n".join([
                "diff --git a/phases/9-smartcloset-ui-ux-redesign/step3.md b/phases/9-smartcloset-ui-ux-redesign/step3.md",
                "+++ b/phases/9-smartcloset-ui-ux-redesign/step3.md",
                "@@ -1,0 +1,3 @@",
                "+이메일 인증 상태를 표시한다.",
                "+비밀번호 재설정 버튼 UX를 유지한다.",
                "+Google social login provider 상태를 표시한다.",
            ])
        )

    runner._git = fake_git

    findings = runner._scan_forbidden_diff({"step": 3, "name": "closet-list-form-images"})

    assert any("이메일 인증 범위" in finding for finding in findings)
    assert any("비밀번호 재설정 범위" in finding for finding in findings)
    assert any("소셜 로그인 범위" in finding for finding in findings)


def test_forbidden_diff_blocks_mvp9_account_scope_expansion(tmp_repo):
    _install_phase_scope_rules(tmp_repo, "9-smartcloset-ui-ux-redesign")
    runner = ap.AutopilotRunner("9-smartcloset-ui-ux-redesign", root=tmp_repo)

    def fake_git(*args, check=True):
        assert args[:2] == ("diff", "--unified=0")
        return cp(
            stdout="\n".join([
                "diff --git a/docs/PRD.md b/docs/PRD.md",
                "+++ b/docs/PRD.md",
                "@@ -1,0 +1,4 @@",
                "+refresh token을 발급한다.",
                "+이메일 인증을 구현한다.",
                "+비밀번호 재설정을 새로 도입한다.",
                "+Google social login을 추가한다.",
            ])
        )

    runner._git = fake_git

    findings = runner._scan_forbidden_diff({"step": 1, "name": "app-shell-auth-redesign"})

    assert any("refresh token 범위" in finding for finding in findings)
    assert any("이메일 인증 범위" in finding for finding in findings)
    assert any("비밀번호 재설정 범위" in finding for finding in findings)
    assert any("소셜 로그인 범위" in finding for finding in findings)


def test_forbidden_diff_does_not_treat_mvp5_as_safe_context(runner):
    def fake_git(*args, check=True):
        assert args[:2] == ("diff", "--unified=0")
        return cp(
            stdout="\n".join([
                "diff --git a/docs/PRD.md b/docs/PRD.md",
                "+++ b/docs/PRD.md",
                "@@ -1,0 +1,1 @@",
                "+MVP5에서 AI/GPT 추천을 구현한다.",
            ])
        )

    runner._git = fake_git

    findings = runner._scan_forbidden_diff()

    assert any("AI/GPT 추천 범위" in finding for finding in findings)


def test_forbidden_diff_allows_mvp5_image_upload_scope(runner):
    def fake_git(*args, check=True):
        assert args[:2] == ("diff", "--unified=0")
        return cp(
            stdout="\n".join([
                "diff --git a/docs/PRD.md b/docs/PRD.md",
                "+++ b/docs/PRD.md",
                "@@ -1,0 +1,1 @@",
                "+MVP5에서 이미지 업로드를 구현한다.",
            ])
        )

    runner._git = fake_git

    findings = runner._scan_forbidden_diff()

    assert findings == []


def test_forbidden_diff_uses_file_context_for_safe_sections(runner, tmp_repo):
    docs_dir = tmp_repo / "docs"
    docs_dir.mkdir(exist_ok=True)
    (docs_dir / "PRD.md").write_text(
        "\n".join([
            "# PRD",
            "",
            "## 제외 범위",
            "",
            "- AI 자동 태깅",
            "- S3/CDN",
        ]),
        encoding="utf-8",
    )

    def fake_git(*args, check=True):
        assert args[:2] == ("diff", "--unified=0")
        return cp(
            stdout="\n".join([
                "diff --git a/docs/PRD.md b/docs/PRD.md",
                "+++ b/docs/PRD.md",
                "@@ -0,0 +5,2 @@",
                "+- AI 자동 태깅",
                "+- S3/CDN",
            ])
        )

    runner._git = fake_git

    findings = runner._scan_forbidden_diff()

    assert findings == []


def test_review_markdown_is_table_and_dedupes_findings():
    review = ap.ReviewResult(
        False,
        ["a.java:1 - 실패", "a.java:1 - 실패"],
        "블로커가 있어 merge하지 않습니다.",
        forbidden_passed=False,
        commands=("python3 scripts/checks.py --stage manual",),
    )

    markdown = review.to_markdown()

    assert "| 금지 범위 | 실패 |" in markdown
    assert markdown.count("a.java:1 - 실패") == 1
    assert "## 리뷰 결론" in markdown


def test_main_rejects_xhigh_without_allow_flag():
    with pytest.raises(SystemExit) as exc_info:
        ap.main(["1-smartcloset-mvp", "--step-effort", "xhigh"])

    assert exc_info.value.code == 2


def test_main_rejects_zero_max_steps():
    with pytest.raises(SystemExit) as exc_info:
        ap.main(["1-smartcloset-mvp", "--max-steps", "0"])

    assert exc_info.value.code == 2


def test_review_gate_blocks_dangerous_acceptance_command(tmp_repo):
    step_path = tmp_repo / "phases" / "1-smartcloset-mvp" / "step1.md"
    step_path.write_text(
        "\n".join([
            "# 단계 1",
            "",
            "## 인수 기준",
            "",
            "```bash",
            "rm -r -f build",
            "```",
        ]),
        encoding="utf-8",
    )
    runner = ap.AutopilotRunner("1-smartcloset-mvp", root=tmp_repo)
    shell_calls = []

    runner._run_shell = lambda command, check=True, timeout=None: shell_calls.append(command) or cp()
    runner._run = lambda cmd, check=True, timeout=None: cp()
    runner._scan_forbidden_diff = lambda current_step: []
    runner._run_codex_review = lambda current_step: ap.ReviewResult(True, [], "ok")

    review = runner._run_review_gate({"step": 1, "name": "clothing-p0-api"})

    assert review.passed is False
    assert review.checks_passed is False
    assert shell_calls == []
    assert any("위험 명령 정책" in finding for finding in review.findings)


def test_scope_rules_config_extends_forbidden_and_allows_messages(tmp_repo):
    phase_dir = tmp_repo / "phases" / "1-smartcloset-mvp"
    (phase_dir / "scope-rules.json").write_text(
        json.dumps(
            {
                "extraForbidden": [
                    {
                        "message": "GraphQL 범위가 추가되었습니다.",
                        "anyLowered": ["graphql"],
                    }
                ],
                "allowedScopeMessages": [
                    {
                        "message": "Redis 범위가 추가되었습니다.",
                        "steps": [1],
                    }
                ],
            },
            ensure_ascii=False,
        ),
        encoding="utf-8",
    )
    runner = ap.AutopilotRunner("1-smartcloset-mvp", root=tmp_repo)

    def fake_git(*args, check=True):
        assert args[:2] == ("diff", "--unified=0")
        return cp(
            stdout="\n".join([
                "diff --git a/docs/PRD.md b/docs/PRD.md",
                "+++ b/docs/PRD.md",
                "@@ -1,0 +1,2 @@",
                "+GraphQL API를 구현한다.",
                "+Redis 캐싱을 구현한다.",
            ])
        )

    runner._git = fake_git

    findings = runner._scan_forbidden_diff({"step": 1, "name": "clothing-p0-api"})

    assert any("GraphQL 범위" in finding for finding in findings)
    assert not any("Redis 범위" in finding for finding in findings)

    blocked = runner._scan_forbidden_diff({"step": 0, "name": "project-scaffold"})
    assert any("Redis 범위" in finding for finding in blocked)


def test_merge_waits_for_pr_checks_and_blocks_on_failure(runner):
    gh_calls = []

    def fake_gh(*args, check=True, timeout=None):
        gh_calls.append(args)
        if args[:2] == ("pr", "checks"):
            return cp(returncode=1, stdout="build  fail  1m  https://ci")
        return cp()

    runner._gh = fake_gh

    with pytest.raises(ap.AutopilotError) as exc_info:
        runner._mark_ready_and_merge("https://github.com/org/repo/pull/3")

    assert "원격 체크" in str(exc_info.value)
    assert not any(call[:2] == ("pr", "merge") for call in gh_calls)


def test_merge_proceeds_when_no_checks_after_grace(runner, monkeypatch):
    gh_calls = []
    sleeps = []
    monkeypatch.setattr(ap, "NO_CHECKS_GRACE_SECONDS", 0)
    monkeypatch.setattr(ap.time, "sleep", lambda seconds: sleeps.append(seconds))

    def fake_gh(*args, check=True, timeout=None):
        gh_calls.append(args)
        if args[:2] == ("pr", "checks"):
            return cp(returncode=1, stderr="no checks reported on the 'codex/x' branch")
        return cp()

    runner._gh = fake_gh

    runner._mark_ready_and_merge("https://github.com/org/repo/pull/3")

    assert any(call[:2] == ("pr", "merge") for call in gh_calls)
    assert sleeps == []


def test_no_checks_grace_retries_until_checks_appear(runner, monkeypatch):
    gh_calls = []
    sleeps = []
    checks_results = [
        cp(returncode=1, stderr="no checks reported on the 'codex/x' branch"),
        cp(returncode=0, stdout="build  pass  1m  https://ci"),
    ]
    monkeypatch.setattr(ap.time, "sleep", lambda seconds: sleeps.append(seconds))

    def fake_gh(*args, check=True, timeout=None):
        gh_calls.append(args)
        if args[:2] == ("pr", "checks"):
            return checks_results.pop(0)
        return cp()

    runner._gh = fake_gh

    runner._mark_ready_and_merge("https://github.com/org/repo/pull/3")

    checks_calls = [call for call in gh_calls if call[:2] == ("pr", "checks")]
    assert len(checks_calls) == 2
    assert sleeps == [ap.NO_CHECKS_POLL_SECONDS]
    assert any(call[:2] == ("pr", "merge") for call in gh_calls)


def test_no_checks_failure_during_grace_blocks_merge(runner, monkeypatch):
    gh_calls = []
    checks_results = [
        cp(returncode=1, stderr="no checks reported on the 'codex/x' branch"),
        cp(returncode=1, stdout="build  fail  1m  https://ci"),
    ]
    monkeypatch.setattr(ap.time, "sleep", lambda seconds: None)

    def fake_gh(*args, check=True, timeout=None):
        gh_calls.append(args)
        if args[:2] == ("pr", "checks"):
            return checks_results.pop(0)
        return cp()

    runner._gh = fake_gh

    with pytest.raises(ap.AutopilotError, match="원격 체크"):
        runner._mark_ready_and_merge("https://github.com/org/repo/pull/3")

    assert not any(call[:2] == ("pr", "merge") for call in gh_calls)


def test_allow_no_checks_skips_grace_wait(tmp_repo, monkeypatch):
    runner = ap.AutopilotRunner("1-smartcloset-mvp", root=tmp_repo, allow_no_checks=True)
    gh_calls = []

    def fail_sleep(seconds):
        raise AssertionError("--allow-no-checks must not wait")

    monkeypatch.setattr(ap.time, "sleep", fail_sleep)

    def fake_gh(*args, check=True, timeout=None):
        gh_calls.append(args)
        if args[:2] == ("pr", "checks"):
            return cp(returncode=1, stderr="no checks reported on the 'codex/x' branch")
        return cp()

    runner._gh = fake_gh

    runner._mark_ready_and_merge("https://github.com/org/repo/pull/3")

    checks_calls = [call for call in gh_calls if call[:2] == ("pr", "checks")]
    assert len(checks_calls) == 1
    assert any(call[:2] == ("pr", "merge") for call in gh_calls)


def test_run_converts_timeout_to_autopilot_error(runner, monkeypatch):
    def fake_subprocess_run(*args, **kwargs):
        raise subprocess.TimeoutExpired(cmd="slow", timeout=5)

    monkeypatch.setattr(ap.subprocess, "run", fake_subprocess_run)

    with pytest.raises(ap.AutopilotError, match="끝나지 않아"):
        runner._run(["slow"], timeout=5)
    with pytest.raises(ap.AutopilotError, match="끝나지 않아"):
        runner._run_shell("slow", timeout=5)


def test_dry_run_lists_pending_steps_without_side_effects(tmp_repo):
    runner = ap.AutopilotRunner("1-smartcloset-mvp", root=tmp_repo, dry_run=True, max_steps=1)
    runner._git = lambda *args, **kwargs: (_ for _ in ()).throw(AssertionError("git should not run"))
    runner._gh = lambda *args, **kwargs: (_ for _ in ()).throw(AssertionError("gh should not run"))

    summary = runner.run()

    assert "[dry-run]" in summary
    assert "Step 0 `project-scaffold`" in summary
    assert "Step 1" not in summary  # max_steps=1
    assert not (tmp_repo / ".codex" / "autopilot.lock").exists()


def test_lock_blocks_concurrent_runs(tmp_repo):
    lock_path = tmp_repo / ".codex" / "autopilot.lock"
    lock_path.parent.mkdir(parents=True, exist_ok=True)
    lock_path.write_text(str(os.getpid()), encoding="utf-8")
    runner = ap.AutopilotRunner("1-smartcloset-mvp", root=tmp_repo)

    with pytest.raises(ap.AutopilotError, match="이미 실행 중"):
        runner.run()

    assert lock_path.exists()


def test_stale_lock_is_replaced_and_released(tmp_repo):
    lock_path = tmp_repo / ".codex" / "autopilot.lock"
    lock_path.parent.mkdir(parents=True, exist_ok=True)
    lock_path.write_text("999999999", encoding="utf-8")
    runner = ap.AutopilotRunner("1-smartcloset-mvp", root=tmp_repo)
    runner._run_loop = lambda: "done"

    assert runner.run() == "done"
    assert not lock_path.exists()


def test_max_steps_stops_loop_after_limit(runner, tmp_repo):
    runner.max_steps = 1
    runner._ensure_preconditions = lambda: None
    runner._sync_base = lambda: None
    runner._run_step = lambda branch, step: _mark_step_complete(tmp_repo, step["step"], "완료")
    runner._run_review_gate = lambda step: ap.ReviewResult(True, [], "ok")
    runner._run_final_gate = lambda: (_ for _ in ()).throw(AssertionError("final gate must not run"))

    def fake_gh(*args, check=True, timeout=None):
        if args[:2] == ("pr", "create"):
            return cp(stdout="https://github.com/org/repo/pull/1\n")
        return cp()

    runner._gh = fake_gh

    result = runner.run()

    assert "https://github.com/org/repo/pull/1" in result
    assert "--max-steps 1 도달" in result
