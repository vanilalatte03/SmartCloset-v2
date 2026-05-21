import subprocess
import sys
from pathlib import Path

import pytest

sys.path.insert(0, str(Path(__file__).parent))
import autopilot as ap


def cp(cmd=None, returncode=0, stdout="", stderr=""):
    return subprocess.CompletedProcess(cmd or [], returncode, stdout, stderr)


@pytest.fixture
def tmp_repo(tmp_path):
    (tmp_path / "issues").mkdir()
    return tmp_path


@pytest.fixture
def runner(tmp_repo):
    return ap.AutopilotRunner("1-smartcloset-mvp", root=tmp_repo)


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


def test_phase_success_creates_draft_pr_and_merges(runner):
    gh_calls = []
    phase_branches = []

    runner._ensure_preconditions = lambda: None
    runner._run_phase = lambda branch: phase_branches.append(branch)
    runner._run_review_gate = lambda: ap.ReviewResult(True, [], "ok")

    def fake_gh(*args, check=True):
        gh_calls.append(args)
        if args[:2] == ("pr", "create"):
            return cp(stdout="https://github.com/org/repo/pull/7\n")
        return cp()

    runner._gh = fake_gh

    pr_url = runner.run()

    assert phase_branches == ["codex/1-smartcloset-mvp"]
    assert pr_url == "https://github.com/org/repo/pull/7"
    assert ("pr", "create", "--base", "main", "--head", "codex/1-smartcloset-mvp",
            "--title", "[codex] 1-smartcloset-mvp 자동 구현") == gh_calls[0][:8]
    assert "--draft" in gh_calls[0]
    assert ("pr", "ready", "https://github.com/org/repo/pull/7") in gh_calls
    assert ("pr", "merge", "https://github.com/org/repo/pull/7", "--squash", "--delete-branch") in gh_calls


def test_review_fail_creates_local_and_github_issue_and_closes_pr(runner, tmp_repo):
    gh_calls = []
    runner.max_review_fixes = 0
    runner._ensure_preconditions = lambda: None
    runner._run_phase = lambda branch: None
    runner._run_review_gate = lambda: ap.ReviewResult(False, ["테스트 실패"], "fail")

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
    assert "테스트 실패" in issue_path.read_text(encoding="utf-8")
    assert any(call[:2] == ("issue", "create") for call in gh_calls)
    assert ("pr", "comment", "https://github.com/org/repo/pull/8", "--body",
            "자동 리뷰 gate가 실패했습니다.\n\nfail\n\n- 테스트 실패") in gh_calls
    assert any(call[:3] == ("pr", "close", "https://github.com/org/repo/pull/8") for call in gh_calls)


def test_review_fix_branch_retries_and_merges(runner):
    gh_calls = []
    fix_branches = []
    pushed = []
    review_results = [
        ap.ReviewResult(False, ["리뷰 실패"], "fail"),
        ap.ReviewResult(True, [], "ok"),
    ]

    runner._ensure_preconditions = lambda: None
    runner._run_phase = lambda branch: None
    runner._run_review_gate = lambda: review_results.pop(0)
    runner._prepare_fix_branch = lambda fix_branch, source_branch: fix_branches.append((fix_branch, source_branch))
    runner._invoke_codex_fix = lambda issue, fix_branch: None
    runner._commit_dirty_fix = lambda issue: None
    runner._push_branch = lambda branch: pushed.append(branch)

    def fake_gh(*args, check=True):
        gh_calls.append(args)
        if args[:2] == ("pr", "create"):
            pr_number = len([call for call in gh_calls if call[:2] == ("pr", "create")])
            return cp(stdout=f"https://github.com/org/repo/pull/{pr_number}\n")
        if args[:2] == ("issue", "create"):
            return cp(stdout="https://github.com/org/repo/issues/1\n")
        return cp()

    runner._gh = fake_gh

    pr_url = runner.run()

    assert fix_branches == [("codex/1-smartcloset-mvp-fix-1-1", "codex/1-smartcloset-mvp")]
    assert pushed == ["codex/1-smartcloset-mvp-fix-1-1"]
    assert pr_url == "https://github.com/org/repo/pull/2"
    assert ("pr", "merge", "https://github.com/org/repo/pull/2", "--squash", "--delete-branch") in gh_calls


def test_review_stops_after_max_retry(runner):
    runner.max_review_fixes = 1
    runner._ensure_preconditions = lambda: None
    runner._run_phase = lambda branch: None
    runner._run_review_gate = lambda: ap.ReviewResult(False, ["계속 실패"], "fail")
    runner._prepare_fix_branch = lambda fix_branch, source_branch: None
    runner._invoke_codex_fix = lambda issue, fix_branch: None
    runner._commit_dirty_fix = lambda issue: None
    runner._push_branch = lambda branch: None

    def fake_gh(*args, check=True):
        if args[:2] == ("pr", "create"):
            return cp(stdout="https://github.com/org/repo/pull/9\n")
        if args[:2] == ("issue", "create"):
            return cp(stdout="https://github.com/org/repo/issues/9\n")
        return cp()

    runner._gh = fake_gh

    with pytest.raises(ap.AutopilotError) as exc_info:
        runner.run()

    assert "최대 횟수" in str(exc_info.value)


def test_parse_codex_review_json(runner):
    result = runner._parse_review_result('{"pass": true, "summary": "ok", "findings": []}')

    assert result.passed is True
    assert result.summary == "ok"
    assert result.findings == []


def test_forbidden_diff_ignores_negated_docs_and_flags_added_scope(runner):
    def fake_git(*args, check=True):
        assert args[:2] == ("diff", "--unified=0")
        return cp(
            stdout="\n".join([
                "+외부 Weather API는 구현하지 않는다.",
                "+Redis 캐싱을 구현한다.",
                "+GET /api/recommendations/today",
            ])
        )

    runner._git = fake_git

    findings = runner._scan_forbidden_diff()

    assert "Redis 범위가 추가되었습니다." in findings
    assert "금지 API `GET /api/recommendations/today`가 추가되었습니다." in findings
    assert not any("외부 Weather API" in finding for finding in findings)
