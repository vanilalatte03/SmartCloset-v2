import json
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

    def fake_gh(*args, check=True):
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

    def fake_gh(*args, check=True):
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
        if cmd == ["python3", "scripts/checks.py", "--stage", "manual"]:
            return cp()
        if cmd == ["git", "diff", "--check", "origin/main...HEAD"]:
            return cp()
        raise AssertionError(cmd)

    def fake_scan(current_step):
        seen["scan"] = current_step
        return []

    def fake_codex(current_step):
        seen["codex"] = current_step
        return ap.ReviewResult(True, [], "ok")

    runner._run = fake_run
    runner._scan_forbidden_diff = fake_scan
    runner._run_codex_review = fake_codex

    review = runner._run_review_gate(step)

    assert review.passed is True
    assert seen == {"scan": step, "codex": step}


def test_forbidden_diff_ignores_negated_docs_and_flags_added_scope(runner):
    forbidden_today_get = "GET " + "/api/recommendations/today"

    def fake_git(*args, check=True):
        assert args[:2] == ("diff", "--unified=0")
        return cp(
            stdout="\n".join([
                "diff --git a/scripts/autopilot.py b/scripts/autopilot.py",
                "+++ b/scripts/autopilot.py",
                "@@ -1,0 +1,1 @@",
                f'+            if "{forbidden_today_get}" in line:',
                "diff --git a/issues/1-smartcloset-mvp/issue-1.md b/issues/1-smartcloset-mvp/issue-1.md",
                "+++ b/issues/1-smartcloset-mvp/issue-1.md",
                "@@ -1,0 +1,1 @@",
                "+- Redis 범위가 추가되었습니다.",
                "diff --git a/docs/AUTH.md b/docs/AUTH.md",
                "+++ b/docs/AUTH.md",
                "@@ -1,0 +1,1 @@",
                "+refresh token을 반환하지 않는다.",
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
                "+AWS 배포는 제공하지 않는다.",
                "+회원가입/로그인은 구현하지 않는다.",
                "+SmartCloset 1차 MVP의 추천은 AI/GPT 추천이 아니라 규칙 기반 추천이다.",
                '+rg -n "recommendations/today" .',
                "+Spring Security와 회원가입을 구현한다.",
                "+AWS 배포를 구현한다.",
                "+AI/GPT 추천을 구현한다.",
                "+refresh token을 구현한다.",
                "+소셜 로그인 기능을 구현한다.",
                "+이메일 인증을 구현한다.",
                "+비밀번호 재설정을 구현한다.",
                "+CD 자동화를 구현한다.",
                "+이미지 업로드를 구현한다.",
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
    assert any("AWS 배포" in finding for finding in findings)
    assert any("CD 자동화" in finding for finding in findings)
    assert any("AI/GPT" in finding for finding in findings)
    assert any("이미지 업로드" in finding for finding in findings)
    assert any("외부 주소/지도 API" in finding for finding in findings)
    assert not any("docs/AUTH.md" in finding for finding in findings)
    assert not any("외부 Weather API" in finding for finding in findings)
    assert not any("로그인/회원가입" in finding for finding in findings)
    assert not any("Spring Security" in finding for finding in findings)
    assert len(findings) == len(set(findings))


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
