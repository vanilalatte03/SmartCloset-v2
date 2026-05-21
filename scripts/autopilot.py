#!/usr/bin/env python3
"""Run a Harness phase through PR creation, review, issue, fix, and merge."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent


class AutopilotError(RuntimeError):
    """Raised when the autopilot loop cannot safely continue."""


@dataclass(frozen=True)
class ReviewResult:
    passed: bool
    findings: list[str]
    summary: str = ""

    def to_markdown(self) -> str:
        lines = [self.summary or ("통과" if self.passed else "실패")]
        if self.findings:
            lines.append("")
            lines.extend(f"- {finding}" for finding in self.findings)
        return "\n".join(lines)


@dataclass(frozen=True)
class IssueRecord:
    number: int
    title: str
    body: str
    local_path: Path
    github_url: str


class AutopilotRunner:
    """Coordinates a phase branch from execution to reviewed merge."""

    SAFE_NEGATION_MARKERS = (
        "금지",
        "제외",
        "사용하지",
        "구현하지",
        "필수처럼 보이지",
        "not ",
        "do not",
        "out of scope",
        "비범위",
        "제거",
    )

    def __init__(self, phase: str, *, base: str = "main",
                 max_review_fixes: int = 2, unsafe: bool = False,
                 root: Path = ROOT):
        self.phase = phase
        self.base = base
        self.max_review_fixes = max_review_fixes
        self.unsafe = unsafe
        self.root = Path(root)

    # --- command helpers ---

    def _run(self, cmd: list[str], *, check: bool = True,
             timeout: int | None = None) -> subprocess.CompletedProcess:
        result = subprocess.run(
            cmd,
            cwd=self.root,
            capture_output=True,
            text=True,
            timeout=timeout,
        )
        if check and result.returncode != 0:
            joined = " ".join(cmd)
            output = "\n".join(part for part in (result.stdout.strip(), result.stderr.strip()) if part)
            raise AutopilotError(f"명령 실패: {joined}\n{output}".strip())
        return result

    def _git(self, *args: str, check: bool = True) -> subprocess.CompletedProcess:
        return self._run(["git", *args], check=check)

    def _gh(self, *args: str, check: bool = True) -> subprocess.CompletedProcess:
        return self._run(["gh", *args], check=check)

    # --- public flow ---

    def run(self) -> str:
        self._ensure_preconditions()
        branch = f"codex/{self.phase}"
        self._run_phase(branch)
        return self._review_pr_loop(branch, attempt=0)

    # --- setup ---

    def _ensure_preconditions(self):
        status = self._git("status", "--short", "--untracked-files=all").stdout.strip()
        if status:
            raise AutopilotError(
                "작업트리가 clean 상태가 아닙니다. 자동 PR 루프는 unrelated 변경 방지를 위해 중단합니다.\n"
                + status
            )

        self._gh("auth", "status")
        self._git("remote", "get-url", "origin")
        self._git("fetch", "origin", self.base)
        self._git("checkout", self.base)
        self._git("pull", "--ff-only", "origin", self.base)

    def _run_phase(self, branch: str):
        cmd = [sys.executable, "scripts/execute.py", self.phase, "--branch", branch, "--push"]
        if self.unsafe:
            cmd.append("--unsafe")
        self._run(cmd, timeout=1800)

    # --- PR loop ---

    def _review_pr_loop(self, branch: str, *, attempt: int) -> str:
        pr_url = self._create_pr(branch, attempt=attempt)
        review = self._run_review_gate()
        if review.passed:
            self._mark_ready_and_merge(pr_url)
            return pr_url

        issue = self._record_failure(pr_url, review)
        self._comment_and_close_pr(pr_url, review)
        if attempt >= self.max_review_fixes:
            raise AutopilotError(
                "자동 리뷰 수정 최대 횟수를 초과했습니다.\n"
                f"마지막 PR: {pr_url}\n"
                f"마지막 Issue: {issue.github_url or issue.local_path}"
            )

        fix_branch = f"codex/{self.phase}-fix-{issue.number}-{attempt + 1}"
        self._prepare_fix_branch(fix_branch, branch)
        self._invoke_codex_fix(issue, fix_branch)
        self._commit_dirty_fix(issue)
        self._push_branch(fix_branch)
        return self._review_pr_loop(fix_branch, attempt=attempt + 1)

    def _create_pr(self, branch: str, *, attempt: int) -> str:
        title = f"[codex] {self.phase} 자동 구현"
        if attempt:
            title = f"[codex] {self.phase} 자동 리뷰 수정 {attempt}"
        body = self._pr_body(branch, attempt)
        result = self._gh(
            "pr", "create",
            "--base", self.base,
            "--head", branch,
            "--title", title,
            "--body", body,
            "--draft",
        )
        return self._extract_url(result.stdout) or branch

    def _pr_body(self, branch: str, attempt: int) -> str:
        mode = "초기 phase 실행" if attempt == 0 else f"자동 리뷰 수정 {attempt}차"
        return (
            "## 작업 내용\n"
            f"- `{self.phase}` Harness phase를 `{branch}` 브랜치에서 실행했습니다.\n"
            f"- PR 운영 모드: {mode}\n\n"
            "## 변경 이유\n"
            "- SmartCloset 구현을 Codex 자동 PR 루프로 진행하기 위해 생성했습니다.\n\n"
            "## 테스트 및 확인\n"
            "- Autopilot gate에서 로컬 검증, 금지 범위 검색, Codex 자체 리뷰를 실행합니다.\n\n"
            "## 참고 사항\n"
            "- 이 PR은 자동 gate 통과 후 ready 전환 및 squash merge 대상입니다.\n"
        )

    def _mark_ready_and_merge(self, pr_url: str):
        self._gh("pr", "ready", pr_url)
        self._gh("pr", "merge", pr_url, "--squash", "--delete-branch")

    def _comment_and_close_pr(self, pr_url: str, review: ReviewResult):
        body = "자동 리뷰 gate가 실패했습니다.\n\n" + review.to_markdown()
        self._gh("pr", "comment", pr_url, "--body", body, check=False)
        self._gh("pr", "close", pr_url, "--comment", "자동 리뷰 실패로 닫습니다.", check=False)

    # --- review gate ---

    def _run_review_gate(self) -> ReviewResult:
        findings: list[str] = []

        for cmd in (
            [sys.executable, "scripts/checks.py", "--stage", "manual"],
            ["git", "diff", "--check", f"origin/{self.base}...HEAD"],
        ):
            result = self._run(cmd, check=False)
            if result.returncode != 0:
                findings.append(self._command_failure(cmd, result))

        findings.extend(self._scan_forbidden_diff())
        codex_review = self._run_codex_review()
        if not codex_review.passed:
            findings.extend(codex_review.findings or [codex_review.summary or "Codex 자체 리뷰 실패"])

        if findings:
            return ReviewResult(False, findings, "Autopilot review gate failed.")
        return ReviewResult(True, [], "Autopilot review gate passed.")

    def _command_failure(self, cmd: list[str], result: subprocess.CompletedProcess) -> str:
        output = "\n".join(part for part in (result.stdout.strip(), result.stderr.strip()) if part)
        return f"`{' '.join(cmd)}` 실패: {output or f'exit {result.returncode}'}"

    def _scan_forbidden_diff(self) -> list[str]:
        result = self._git("diff", "--unified=0", f"origin/{self.base}...HEAD", check=False)
        if result.returncode != 0:
            return [self._command_failure(["git", "diff", "--unified=0", f"origin/{self.base}...HEAD"], result)]

        findings: list[str] = []
        for raw in result.stdout.splitlines():
            if not raw.startswith("+") or raw.startswith("+++"):
                continue
            line = raw[1:].strip()
            lowered = line.lower()
            if any(marker in lowered for marker in self.SAFE_NEGATION_MARKERS):
                continue
            if "GET /api/recommendations/today" in line:
                findings.append("금지 API `GET /api/recommendations/today`가 추가되었습니다.")
            if "외부 Weather API" in line and any(word in line for word in ("필수", "구현", "호출", "연동")):
                findings.append("외부 Weather API가 MVP 필수/구현 대상으로 추가되었습니다.")
            if "AWS" in line and any(word in line for word in ("필수", "구현", "배포")):
                findings.append("AWS 배포가 MVP 필수/구현 대상으로 추가되었습니다.")
            if any(term in line for term in ("Spring Security", "로그인", "회원가입")):
                findings.append("로그인/회원가입/Spring Security 범위가 추가되었습니다.")
            if any(term in line for term in ("AI/GPT", "GPT 추천", "AI 추천")):
                findings.append("AI/GPT 추천 범위가 추가되었습니다.")
            if "Redis" in line:
                findings.append("Redis 범위가 추가되었습니다.")
        return findings

    def _run_codex_review(self) -> ReviewResult:
        prompt = self._codex_review_prompt()
        result = self._run(["codex", "exec", "--json", prompt], check=False, timeout=1800)
        if result.returncode != 0:
            return ReviewResult(False, [self._command_failure(["codex", "exec", "--json", "<review-prompt>"], result)])
        return self._parse_review_result(result.stdout)

    def _codex_review_prompt(self) -> str:
        return (
            "Read-only review only. Do not modify files. "
            "Review the current branch diff against origin/{base} for SmartCloset MVP rules. "
            "Check AGENTS.md, docs/PRD.md, docs/API.md, docs/RECOMMENDATION_RULES.md, docs/ARCHITECTURE.md, docs/adr/. "
            "Return only JSON with keys: pass (boolean), summary (string), findings (array of strings)."
        ).format(base=self.base)

    def _parse_review_result(self, stdout: str) -> ReviewResult:
        candidates = list(reversed([line.strip() for line in stdout.splitlines() if line.strip()]))
        if stdout.strip():
            candidates.append(stdout.strip())

        for candidate in candidates:
            parsed = self._try_parse_review_candidate(candidate)
            if parsed is not None:
                return parsed
        return ReviewResult(False, ["Codex review output에서 JSON 결과를 찾지 못했습니다."])

    def _try_parse_review_candidate(self, candidate: str) -> ReviewResult | None:
        try:
            data = json.loads(candidate)
        except json.JSONDecodeError:
            match = re.search(r"\{.*\}", candidate, re.DOTALL)
            if not match:
                return None
            try:
                data = json.loads(match.group(0))
            except json.JSONDecodeError:
                return None

        if isinstance(data, dict):
            for key in ("result", "message", "content", "final"):
                value = data.get(key)
                if isinstance(value, str):
                    nested = self._try_parse_review_candidate(value)
                    if nested is not None:
                        return nested

            passed = data.get("pass", data.get("passed"))
            if isinstance(passed, str):
                passed = passed.lower() in {"true", "pass", "passed", "ok"}
            if isinstance(passed, bool):
                findings = data.get("findings", [])
                if isinstance(findings, str):
                    findings = [findings]
                if not isinstance(findings, list):
                    findings = [str(findings)]
                summary = str(data.get("summary", ""))
                return ReviewResult(passed, [str(item) for item in findings], summary)
        return None

    # --- issue and fix ---

    def _record_failure(self, pr_url: str, review: ReviewResult) -> IssueRecord:
        number = self._next_issue_number()
        title = f"{self.phase} 자동 리뷰 실패 {number}"
        body = self._issue_body(pr_url, review)
        issue_dir = self.root / "issues" / self.phase
        issue_dir.mkdir(parents=True, exist_ok=True)
        local_path = issue_dir / f"issue-{number}.md"
        local_path.write_text(f"# Issue {number}: {title}\n\n{body}", encoding="utf-8")

        gh_result = self._gh("issue", "create", "--title", title, "--body", body)
        github_url = self._extract_url(gh_result.stdout)
        return IssueRecord(number, title, body, local_path, github_url)

    def _next_issue_number(self) -> int:
        issue_dir = self.root / "issues" / self.phase
        if not issue_dir.is_dir():
            return 1
        numbers: list[int] = []
        for path in issue_dir.glob("issue-*.md"):
            match = re.match(r"issue-(\d+)\.md", path.name)
            if match:
                numbers.append(int(match.group(1)))
        return max(numbers, default=0) + 1

    def _issue_body(self, pr_url: str, review: ReviewResult) -> str:
        return (
            "## 발생 위치\n"
            f"- Phase: {self.phase}\n"
            f"- PR: {pr_url}\n\n"
            "## 재현 명령\n"
            "```bash\n"
            "python3 scripts/checks.py --stage manual\n"
            f"git diff --check origin/{self.base}...HEAD\n"
            "```\n\n"
            "## 핵심 에러\n"
            f"{review.to_markdown()}\n\n"
            "## 수정 방향\n"
            "- review findings를 반영한 fix branch를 만들고 같은 gate를 다시 통과시킨다.\n\n"
            "## 완료 기준\n"
            "- 로컬 검증, 금지 범위 검색, Codex 자체 리뷰를 모두 통과한다.\n"
        )

    def _prepare_fix_branch(self, fix_branch: str, source_branch: str):
        self._git("checkout", "-B", fix_branch, source_branch)

    def _invoke_codex_fix(self, issue: IssueRecord, fix_branch: str):
        prompt = (
            f"당신은 SmartCloset 자동 리뷰 fix 담당자입니다. 현재 브랜치 `{fix_branch}`에서 "
            "아래 이슈만 수정하세요. 기존 변경을 되돌리지 말고, AGENTS.md와 Codex 앱 커밋 지침을 따르세요.\n\n"
            f"{issue.body}\n\n"
            "수정 후 가능한 검증을 실행하고 필요한 변경을 커밋하세요."
        )
        self._run(["codex", "exec", "--json", prompt], timeout=1800)

    def _commit_dirty_fix(self, issue: IssueRecord):
        status = self._git("status", "--short", "--untracked-files=all").stdout.strip()
        if not status:
            return
        self._git("add", "-A")
        self._git("commit", "-m", f"fix: {self.phase} 자동 리뷰 이슈 수정")

    def _push_branch(self, branch: str):
        self._git("push", "-u", "origin", branch)

    # --- parsing ---

    @staticmethod
    def _extract_url(output: str) -> str:
        match = re.search(r"https://\S+", output)
        return match.group(0).rstrip(")") if match else ""


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Run a Harness phase through the automated PR loop.")
    parser.add_argument("phase", help="Phase directory name, e.g. 1-smartcloset-mvp")
    parser.add_argument("--base", default="main", help="Base branch for PRs and merges")
    parser.add_argument("--max-review-fixes", type=int, default=2, help="Maximum automatic review-fix attempts")
    parser.add_argument("--unsafe", action="store_true", help="Pass --unsafe to scripts/execute.py")
    args = parser.parse_args(argv)

    try:
        pr_url = AutopilotRunner(
            args.phase,
            base=args.base,
            max_review_fixes=args.max_review_fixes,
            unsafe=args.unsafe,
        ).run()
    except AutopilotError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1

    print(f"Autopilot completed: {pr_url}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
