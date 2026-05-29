#!/usr/bin/env python3
"""Run Harness steps through small PRs, read-only review, and safe merge."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ALLOWED_CODEX_EFFORTS = ("minimal", "low", "medium", "high", "xhigh")
DEFAULT_STEP_EFFORT = "medium"
DEFAULT_REVIEW_EFFORT = "high"
DEFAULT_FIX_EFFORT = "medium"
FALLBACK_REVIEW_CHECK_COMMAND = "python3 scripts/checks.py --stage manual"


def validate_codex_effort(effort: str, *, allow_xhigh: bool = False) -> str:
    if effort not in ALLOWED_CODEX_EFFORTS:
        allowed = ", ".join(ALLOWED_CODEX_EFFORTS)
        raise ValueError(f"codex effort must be one of: {allowed}")
    if effort == "xhigh" and not allow_xhigh:
        raise ValueError("xhigh effort requires --allow-xhigh")
    return effort


def codex_effort_config(effort: str) -> list[str]:
    return ["-c", f'model_reasoning_effort="{effort}"']


class AutopilotError(RuntimeError):
    """Raised when the autopilot loop cannot safely continue."""


@dataclass(frozen=True)
class ReviewResult:
    passed: bool
    findings: list[str]
    summary: str = ""
    checks_passed: bool = True
    diff_passed: bool = True
    forbidden_passed: bool = True
    codex_passed: bool = True
    commands: tuple[str, ...] = ()

    def to_markdown(self) -> str:
        conclusion = self.summary or (
            "블로커 없음. 이 step PR은 merge 가능합니다."
            if self.passed
            else "블로커가 있어 merge하지 않습니다."
        )
        rows = [
            ("로컬 검증", self.checks_passed, "step 인수 기준 명령"),
            ("diff 검사", self.diff_passed, "git diff --check"),
            ("금지 범위", self.forbidden_passed, "MVP 제외 범위와 금지 API 검색"),
            ("자체 리뷰", self.codex_passed, "Codex read-only review"),
        ]
        lines = [
            "## 자체 리뷰",
            "",
            "| 항목 | 결과 | 비고 |",
            "| --- | --- | --- |",
        ]
        for name, passed, note in rows:
            lines.append(f"| {name} | {'통과' if passed else '실패'} | {note} |")

        if self.commands:
            lines.extend(["", "## 확인한 명령", "", "```bash"])
            lines.extend(self.commands)
            lines.append("```")

        lines.extend(["", "## 발견사항"])
        if self.findings:
            lines.extend(f"- {finding}" for finding in _dedupe(self.findings))
        else:
            lines.append("- 없음")

        lines.extend(["", "## 리뷰 결론", conclusion])
        return "\n".join(lines)


@dataclass(frozen=True)
class IssueRecord:
    number: int
    title: str
    body: str
    local_path: Path
    github_url: str


def _dedupe(items: list[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for item in items:
        if item in seen:
            continue
        seen.add(item)
        result.append(item)
    return result


class AutopilotRunner:
    """Coordinates a Harness phase as a sequence of small step PRs."""

    TASK_REASON_ENDINGS = (
        ("한다", "하기"),
        ("만든다", "만들기"),
        ("다룬다", "다루기"),
        ("잠근다", "잠그기"),
        ("바꾼다", "바꾸기"),
        ("맞춘다", "맞추기"),
        ("둔다", "두기"),
    )
    SAFE_NEGATION_MARKERS = (
        "금지",
        "제외",
        "사용하지",
        "구현하지",
        "문서화하지",
        "표시하지",
        "추가하지",
        "제공하지",
        "호출하지",
        "반환하지",
        "변경하지",
        "저장하지",
        "노출하지",
        "넣지",
        "만들지",
        "허용하지",
        "포함하지",
        "포함되지",
        "남기지",
        "되살리지",
        "쓰이지 않았",
        "않았는지",
        "없어야",
        "없다",
        "없음",
        "필수처럼 보이지",
        "처럼 보이는",
        "범위가 아니다",
        "범위가 아니",
        "범위 밖",
        "후속 mvp",
        "can revisit",
        "1차 이후",
        "후보로 이동",
        "아니라",
        "없이",
        "없는",
        "필요 없다",
        "not ",
        "do not",
        "out of scope",
        "비범위",
        "제거",
    )
    SAFE_SECTION_MARKERS = (
        "제외",
        "제외 범위",
        "제외되는 범위",
        "금지사항",
        "비범위",
        "out of scope",
        "1차 이후",
        "후속",
        "p2",
    )
    SAFE_COMMAND_PREFIXES = (
        "rg ",
        "! rg ",
        "grep ",
        "! grep ",
        "git grep ",
        "! git grep ",
    )
    FORBIDDEN_SCAN_EXCLUDED_PATHS = (
        "scripts/autopilot.py",
        "scripts/test_autopilot.py",
    )
    FORBIDDEN_SCAN_EXCLUDED_PREFIXES = (
        "issues/",
    )
    MVP8_ACCOUNT_STABILITY_PHASE = "8-smartcloset-account-stability"
    MVP9_UI_UX_PHASE = "9-smartcloset-ui-ux-redesign"
    MVP8_ALLOWED_SCOPE_MESSAGES_BY_STEP = {
        "refresh token 범위가 추가되었습니다.": frozenset(range(8)),
        "이메일 인증 범위가 추가되었습니다.": frozenset((0, 2, 5, 7)),
        "비밀번호 재설정 범위가 추가되었습니다.": frozenset((0, 2, 5, 7)),
        "소셜 로그인 범위가 추가되었습니다.": frozenset((0, 3, 5, 7)),
    }
    ACCOUNT_STABILITY_SCOPE_MESSAGES = frozenset(
        (
            "refresh token 범위가 추가되었습니다.",
            "이메일 인증 범위가 추가되었습니다.",
            "비밀번호 재설정 범위가 추가되었습니다.",
            "소셜 로그인 범위가 추가되었습니다.",
        )
    )
    MVP9_ACCOUNT_MAINTENANCE_STEP_NAMES = frozenset(
        (
            "mvp9-docs-archive",
            "app-shell-auth-redesign",
            "account-settings",
            "docs-qa",
        )
    )
    MVP9_ACCOUNT_MAINTENANCE_MARKERS = (
        "유지",
        "기존",
        "상태",
        "표시",
        "provider",
        "제공자",
        "버튼",
        "ux",
        "계약",
        "흐름",
        "안내",
        "확인",
        "진입",
        "보여",
        "keep",
        "preserve",
        "status",
        "display",
    )
    MVP9_ACCOUNT_EXPANSION_MARKERS = (
        "구현",
        "추가",
        "도입",
        "신규",
        "새로",
        "발급",
        "저장",
        "저장한다",
        "저장하도록",
        "생성",
        "생성한다",
        "만든다",
        "implement",
        "add ",
        "introduce",
        "new ",
        "issue ",
        "store ",
        "create ",
    )
    HUNK_RE = re.compile(r"@@ -\d+(?:,\d+)? \+(\d+)(?:,\d+)? @@")
    STEP_OUTPUT_RE = re.compile(r"^phases/[^/]+/step\d+-output\.json$")

    def __init__(
        self,
        phase: str,
        *,
        base: str = "main",
        max_review_fixes: int = 2,
        unsafe: bool = False,
        step_effort: str = DEFAULT_STEP_EFFORT,
        review_effort: str = DEFAULT_REVIEW_EFFORT,
        fix_effort: str = DEFAULT_FIX_EFFORT,
        allow_xhigh: bool = False,
        root: Path = ROOT,
    ):
        self.phase = phase
        self.base = base
        self.max_review_fixes = max_review_fixes
        self.unsafe = unsafe
        self.step_effort = validate_codex_effort(step_effort, allow_xhigh=allow_xhigh)
        self.review_effort = validate_codex_effort(review_effort, allow_xhigh=allow_xhigh)
        self.fix_effort = validate_codex_effort(fix_effort, allow_xhigh=allow_xhigh)
        self.allow_xhigh = allow_xhigh
        self.root = Path(root)

    # --- command helpers ---

    def _run(
        self,
        cmd: list[str],
        *,
        check: bool = True,
        timeout: int | None = None,
    ) -> subprocess.CompletedProcess:
        result = subprocess.run(
            cmd,
            cwd=self.root,
            capture_output=True,
            text=True,
            timeout=timeout,
        )
        if check and result.returncode != 0:
            raise AutopilotError(self._command_failure(cmd, result))
        return result

    def _run_shell(
        self,
        command: str,
        *,
        check: bool = True,
        timeout: int | None = None,
    ) -> subprocess.CompletedProcess:
        result = subprocess.run(
            command,
            cwd=self.root,
            shell=True,
            capture_output=True,
            text=True,
            timeout=timeout,
        )
        if check and result.returncode != 0:
            raise AutopilotError(self._shell_command_failure(command, result))
        return result

    def _git(self, *args: str, check: bool = True) -> subprocess.CompletedProcess:
        return self._run(["git", *args], check=check)

    def _gh(self, *args: str, check: bool = True) -> subprocess.CompletedProcess:
        return self._run(["gh", *args], check=check)

    # --- public flow ---

    def run(self) -> str:
        self._ensure_preconditions()
        merged_prs: list[str] = []

        while True:
            step = self._next_pending_step()
            if step is None:
                self._run_final_gate()
                return "\n".join(merged_prs) if merged_prs else f"No pending steps for {self.phase}."

            branch = self._step_branch(step)
            self._run_step(branch, step)
            pr_url = self._create_pr(branch, step)
            self._review_and_fix_until_passed(pr_url, branch, step)

            self._mark_ready_and_merge(pr_url)
            merged_prs.append(pr_url)
            self._sync_base()

    def _review_and_fix_until_passed(self, pr_url: str, branch: str, step: dict) -> ReviewResult:
        issue: IssueRecord | None = None
        last_review: ReviewResult | None = None

        for attempt in range(self.max_review_fixes + 1):
            review = self._run_review_gate(step)
            last_review = review
            self._comment_review(pr_url, review)
            if review.passed:
                if issue is not None:
                    self._resolve_failure_issue(issue, review, step)
                return review

            if issue is None:
                issue = self._record_failure(pr_url, review, step)
            else:
                self._append_failure_attempt(issue, review, step, attempt)

            if attempt >= self.max_review_fixes:
                raise AutopilotError(
                    "자동 리뷰 수정 최대 횟수를 초과했습니다. PR과 Issue는 열린 상태로 유지합니다.\n"
                    f"PR: {pr_url}\n"
                    f"Issue: {issue.github_url or issue.local_path}\n"
                    f"마지막 결과: {last_review.summary}"
                )

            self._invoke_codex_fix(issue, branch, step, review, attempt + 1)
            self._commit_dirty_fix(step)
            self._push_branch(branch)

        raise AutopilotError(f"Step PR review gate failed: {pr_url}")

    # --- setup and step state ---

    def _ensure_preconditions(self):
        status = self._git("status", "--short", "--untracked-files=all").stdout.strip()
        if status:
            raise AutopilotError(
                "작업트리가 clean 상태가 아닙니다. 자동 PR 루프는 unrelated 변경 방지를 위해 중단합니다.\n"
                + status
            )

        self._gh("auth", "status")
        self._git("remote", "get-url", "origin")
        self._sync_base()
        self._ensure_base_checks_pass()

    def _sync_base(self):
        self._git("fetch", "origin", self.base)
        self._git("checkout", self.base)
        self._git("pull", "--ff-only", "origin", self.base)

    def _ensure_base_checks_pass(self):
        result = self._run_shell(FALLBACK_REVIEW_CHECK_COMMAND, check=False, timeout=1800)
        if result.returncode != 0:
            raise AutopilotError(
                f"base 브랜치 `{self.base}`의 manual 검증이 이미 실패해서 자동 PR 루프를 시작하지 않습니다.\n"
                + self._shell_command_failure(FALLBACK_REVIEW_CHECK_COMMAND, result)
            )

    def _phase_index_path(self) -> Path:
        return self.root / "phases" / self.phase / "index.json"

    def _load_phase_index(self) -> dict:
        return json.loads(self._phase_index_path().read_text(encoding="utf-8"))

    def _next_pending_step(self) -> dict | None:
        index = self._load_phase_index()
        return next((s for s in index.get("steps", []) if s.get("status") == "pending"), None)

    def _step_branch(self, step: dict) -> str:
        return f"codex/{self.phase}-step{step['step']}-{step['name']}"

    # --- step execution and PR ---

    def _run_step(self, branch: str, step: dict):
        cmd = [
            sys.executable,
            "scripts/execute.py",
            self.phase,
            "--branch",
            branch,
            "--push",
            "--step",
            str(step["step"]),
            "--codex-effort",
            self.step_effort,
        ]
        if self.step_effort == "xhigh":
            cmd.append("--allow-xhigh")
        if self.unsafe:
            cmd.append("--unsafe")
        self._run(cmd, timeout=1800)

    def _create_pr(self, branch: str, step: dict) -> str:
        title = f"feat: {self.phase} {step['step']}단계 {step['name']} 구현"
        body = self._pr_body(branch, step)
        result = self._gh(
            "pr",
            "create",
            "--base",
            self.base,
            "--head",
            branch,
            "--title",
            title,
            "--body",
            body,
            "--draft",
        )
        return self._extract_url(result.stdout) or branch

    def _pr_body(self, branch: str, step: dict) -> str:
        refreshed = self._step_from_index(step["step"]) or step
        summary = refreshed.get("summary") or "step 실행 결과를 phase index에 기록했습니다."
        task = self._step_task_summary(step)
        changed_files = self._changed_files()
        changed_section = "\n".join(f"- `{path}`" for path in changed_files[:12])
        if len(changed_files) > 12:
            changed_section += f"\n- 외 {len(changed_files) - 12}개 파일"
        if not changed_section:
            changed_section = "- 코드 변경 없음"

        commands = "\n".join(f"- `{command}`" for command in self._review_commands(step))
        return (
            "## 작업 내용\n"
            f"- `{self.phase}` Step {step['step']} `{step['name']}` 범위를 구현했습니다.\n"
            f"- 산출물: {summary}\n\n"
            "## 변경 이유\n"
            f"- {self._step_change_reason(step)}\n\n"
            "## 주요 변경 사항\n"
            f"- Step 작업: {task}\n"
            f"{changed_section}\n\n"
            "## 테스트 및 확인\n"
            f"{commands}\n\n"
            "- Codex read-only review\n\n"
            "## 참고 사항\n"
            f"- 브랜치: `{branch}`\n"
            "- Draft PR로 생성하며 자체 리뷰 gate 통과 시 ready 전환 후 squash merge합니다.\n"
        )

    def _step_change_reason(self, step: dict) -> str:
        task = self._step_task_summary(step)
        purpose = self._task_purpose_clause(task)
        if purpose:
            return f"{purpose} 위해 필요한 변경을 반영했습니다."
        return (
            f"`{self.phase}` Step {step['step']} `{step['name']}` 범위의 "
            "제품/기술 계약을 충족하기 위해 필요한 변경을 반영했습니다."
        )

    def _task_purpose_clause(self, task: str) -> str:
        normalized = " ".join(task.strip().strip("- ").rstrip(".").split())
        if not normalized:
            return ""
        first_sentence = normalized.split(". ")[0].rstrip(".")
        for ending, replacement in self.TASK_REASON_ENDINGS:
            if first_sentence.endswith(ending):
                return first_sentence[: -len(ending)] + replacement
        return f"{first_sentence} 범위를 완료하기"

    def _step_from_index(self, step_num: int) -> dict | None:
        index = self._load_phase_index()
        return next((s for s in index.get("steps", []) if s.get("step") == step_num), None)

    def _step_task_summary(self, step: dict) -> str:
        path = self.root / "phases" / self.phase / f"step{step['step']}.md"
        if not path.exists():
            return step["name"]
        in_task = False
        for raw in path.read_text(encoding="utf-8").splitlines():
            line = raw.strip()
            if line == "## 작업":
                in_task = True
                continue
            if in_task and line.startswith("## "):
                break
            if in_task and line:
                return line.lstrip("- ")
        return step["name"]

    def _changed_files(self) -> list[str]:
        result = self._git("diff", "--name-only", f"origin/{self.base}...HEAD", check=False)
        if result.returncode != 0:
            return []
        return [line for line in result.stdout.splitlines() if line.strip()]

    def _review_commands(self, step: dict | None = None) -> tuple[str, ...]:
        return (
            *self._review_check_commands(step),
            f"git diff --check origin/{self.base}...HEAD",
        )

    def _review_check_commands(self, step: dict | None = None) -> tuple[str, ...]:
        return tuple(
            command
            for command in self._step_acceptance_commands(step)
            if not self._is_diff_check_command(command)
        )

    def _step_acceptance_commands(self, step: dict | None = None) -> tuple[str, ...]:
        if step is None:
            return (FALLBACK_REVIEW_CHECK_COMMAND,)

        step_number = self._step_number_from(step)
        if step_number is None:
            return (FALLBACK_REVIEW_CHECK_COMMAND,)

        path = self.root / "phases" / self.phase / f"step{step_number}.md"
        if not path.exists():
            return (FALLBACK_REVIEW_CHECK_COMMAND,)

        commands: list[str] = []
        in_acceptance = False
        in_code_block = False
        for raw in path.read_text(encoding="utf-8").splitlines():
            line = raw.strip()
            if line.startswith("## "):
                if in_acceptance:
                    break
                in_acceptance = line == "## 인수 기준"
                continue
            if not in_acceptance:
                continue
            if line.startswith("```"):
                in_code_block = not in_code_block
                continue
            if in_code_block and line and not line.startswith("#"):
                commands.append(line)

        return tuple(commands) or (FALLBACK_REVIEW_CHECK_COMMAND,)

    @staticmethod
    def _is_diff_check_command(command: str) -> bool:
        normalized = " ".join(command.split())
        return normalized == "git diff --check" or normalized.startswith("git diff --check ")

    def _run_final_gate(self):
        checks_path = self.root / "scripts" / "checks.py"
        if not checks_path.exists():
            return
        self._run([sys.executable, "scripts/checks.py", "--stage", "final"], timeout=1800)

    def _mark_ready_and_merge(self, pr_url: str):
        self._gh("pr", "ready", pr_url)
        self._gh("pr", "merge", pr_url, "--squash", "--delete-branch")

    def _comment_review(self, pr_url: str, review: ReviewResult):
        self._gh("pr", "comment", pr_url, "--body", review.to_markdown(), check=False)

    def _comment_issue(self, issue: IssueRecord, body: str):
        target = issue.github_url
        if not target:
            return
        self._gh("issue", "comment", target, "--body", body, check=False)

    # --- review gate ---

    def _run_review_gate(self, step: dict) -> ReviewResult:
        findings: list[str] = []
        commands = self._review_commands(step)

        checks_passed = True
        for command in self._review_check_commands(step):
            checks_result = self._run_shell(command, check=False, timeout=1800)
            if checks_result.returncode != 0:
                checks_passed = False
                findings.append(self._shell_command_failure(command, checks_result))

        diff_cmd = ["git", "diff", "--check", f"origin/{self.base}...HEAD"]
        diff_result = self._run(diff_cmd, check=False)
        diff_passed = diff_result.returncode == 0
        if not diff_passed:
            findings.append(self._command_failure(diff_cmd, diff_result))

        forbidden_findings = self._scan_forbidden_diff(step)
        findings.extend(forbidden_findings)
        forbidden_passed = not forbidden_findings

        codex_review = self._run_codex_review(step)
        if not codex_review.passed:
            findings.extend(codex_review.findings or [codex_review.summary or "Codex 자체 리뷰 실패"])

        findings = _dedupe(findings)
        passed = checks_passed and diff_passed and forbidden_passed and codex_review.passed and not findings
        return ReviewResult(
            passed,
            findings,
            "블로커 없음. 이 step PR은 merge 가능합니다." if passed else "블로커가 있어 merge하지 않습니다.",
            checks_passed=checks_passed,
            diff_passed=diff_passed,
            forbidden_passed=forbidden_passed,
            codex_passed=codex_review.passed,
            commands=commands,
        )

    def _command_failure(self, cmd: list[str], result: subprocess.CompletedProcess) -> str:
        output = "\n".join(part for part in (result.stdout.strip(), result.stderr.strip()) if part)
        return f"`{' '.join(cmd)}` 실패: {self._compact_output(output) or f'exit {result.returncode}'}"

    def _shell_command_failure(self, command: str, result: subprocess.CompletedProcess) -> str:
        output = "\n".join(part for part in (result.stdout.strip(), result.stderr.strip()) if part)
        return f"`{command}` 실패: {self._compact_output(output) or f'exit {result.returncode}'}"

    @staticmethod
    def _compact_output(output: str, *, max_chars: int = 1200) -> str:
        if len(output) <= max_chars:
            return output
        return output[:max_chars].rstrip() + "\n... output truncated ..."

    def _scan_forbidden_diff(self, step: dict | None = None) -> list[str]:
        result = self._git("diff", "--unified=0", f"origin/{self.base}...HEAD", check=False)
        if result.returncode != 0:
            return [self._command_failure(["git", "diff", "--unified=0", f"origin/{self.base}...HEAD"], result)]

        findings: list[str] = []
        finding_keys: set[tuple[str, str]] = set()
        safe_section = False
        current_file = ""
        new_line = 0

        for raw in result.stdout.splitlines():
            if raw.startswith("diff --git"):
                current_file = self._diff_new_path(raw)
                safe_section = False
                new_line = 0
                continue
            if raw.startswith("@@"):
                match = self.HUNK_RE.search(raw)
                new_line = int(match.group(1)) if match else 0
                safe_section = False
                continue
            if self._skip_forbidden_scan_file(current_file):
                continue
            if raw.startswith(" ") and new_line:
                new_line += 1
                continue
            if raw.startswith("-") and not raw.startswith("---"):
                continue
            if not raw.startswith("+") or raw.startswith("+++"):
                continue

            line_no = new_line
            if new_line:
                new_line += 1
            line = raw[1:].strip()
            lowered = line.lower()

            if line.startswith("#"):
                safe_section = any(marker in lowered for marker in self.SAFE_SECTION_MARKERS)
                continue
            if line.endswith(":") and any(marker in lowered for marker in self.SAFE_SECTION_MARKERS):
                safe_section = True
                continue
            if (
                safe_section
                or any(marker in lowered for marker in self.SAFE_NEGATION_MARKERS)
                or any(lowered.startswith(prefix) for prefix in self.SAFE_COMMAND_PREFIXES)
                or self._line_in_safe_section(current_file, line_no)
            ):
                continue

            for message in self._forbidden_messages(line):
                if self._is_allowed_scope_message(message, line, step):
                    continue
                key = (current_file, message)
                if key in finding_keys:
                    continue
                finding_keys.add(key)
                findings.append(self._format_finding(current_file, line_no, message))

        return findings

    def _forbidden_messages(self, line: str) -> list[str]:
        messages: list[str] = []
        lowered = line.lower()
        forbidden_today_get = "GET " + "/api/recommendations/today"
        if forbidden_today_get in line:
            messages.append("금지 API `" + forbidden_today_get + "`가 추가되었습니다.")
        if "refresh token" in lowered or "refreshtoken" in lowered or "리프레시 토큰" in line:
            messages.append("refresh token 범위가 추가되었습니다.")
        if "social login" in lowered or "소셜 로그인" in line:
            messages.append("소셜 로그인 범위가 추가되었습니다.")
        if "email verification" in lowered or "이메일 인증" in line:
            messages.append("이메일 인증 범위가 추가되었습니다.")
        if "password reset" in lowered or "비밀번호 재설정" in line:
            messages.append("비밀번호 재설정 범위가 추가되었습니다.")
        if (
            "외부 Weather API" in line
            and any(word in line for word in ("필수", "구현", "호출", "연동"))
            and "getVilageFcst" not in line
            and "기상청" not in line
        ):
            messages.append("외부 Weather API가 MVP 필수/구현 대상으로 추가되었습니다.")
        if "AWS" in line and any(word in line for word in ("필수", "구현", "배포")):
            messages.append("AWS 배포가 MVP 필수/구현 대상으로 추가되었습니다.")
        if (
            any(term in line for term in ("CD 자동화", "CD 배포"))
            or "cd automation" in lowered
            or "cd deployment" in lowered
        ) and any(word in line for word in ("필수", "구현", "배포", "자동화")):
            messages.append("CD 자동화 범위가 추가되었습니다.")
        if any(term in line for term in ("AI/GPT", "GPT 추천", "AI 추천")):
            messages.append("AI/GPT 추천 범위가 추가되었습니다.")
        if "AI 자동 태깅" in line or "ai automatic tagging" in lowered:
            messages.append("AI 자동 태깅 범위가 추가되었습니다.")
        if "Redis" in line:
            messages.append("Redis 범위가 추가되었습니다.")
        if "다중 이미지" in line or "multiple image" in lowered or "multiple images" in lowered:
            messages.append("다중 이미지 범위가 추가되었습니다.")
        if any(term in line for term in ("이미지 편집", "이미지 크롭", "이미지 리사이즈", "이미지 압축")):
            messages.append("이미지 편집/크롭/리사이즈/압축 범위가 추가되었습니다.")
        if any(term in lowered for term in ("image editing", "image cropping", "image resizing", "image compression")):
            messages.append("이미지 편집/크롭/리사이즈/압축 범위가 추가되었습니다.")
        if "S3" in line or "CDN" in line or "external image hosting" in lowered or "외부 image hosting" in line:
            messages.append("S3/CDN/external image hosting 범위가 추가되었습니다.")
        if (
            "이미지 기반 추천 점수" in line
            or "이미지 기반 추천 이유" in line
            or "image-based recommendation scoring" in lowered
            or "image-based recommendation reason" in lowered
        ):
            messages.append("이미지 기반 추천 점수/이유 범위가 추가되었습니다.")
        if any(term in lowered for term in ("external address", "external map", "map api", "address api")):
            messages.append("외부 주소/지도 API 범위가 추가되었습니다.")
        if "외부 주소" in line or "외부 지도" in line or "지도 API" in line or "주소 API" in line:
            messages.append("외부 주소/지도 API 범위가 추가되었습니다.")
        return messages

    def _is_allowed_scope_message(self, message: str, line: str, step: dict | None) -> bool:
        if self.phase == self.MVP9_UI_UX_PHASE:
            return self._is_allowed_mvp9_account_maintenance_message(message, line, step)

        if self.phase != self.MVP8_ACCOUNT_STABILITY_PHASE:
            return False

        allowed_steps = self.MVP8_ALLOWED_SCOPE_MESSAGES_BY_STEP.get(message)
        if allowed_steps is None:
            return False

        step_number = self._step_number_from(step)
        if step_number is not None and step_number not in allowed_steps:
            return False

        if message == "소셜 로그인 범위가 추가되었습니다.":
            lowered = line.lower()
            return "google" in lowered or "oauth" in lowered

        return True

    def _is_allowed_mvp9_account_maintenance_message(
        self, message: str, line: str, step: dict | None
    ) -> bool:
        if message not in self.ACCOUNT_STABILITY_SCOPE_MESSAGES:
            return False

        if not self._is_mvp9_account_maintenance_step(step):
            return False

        lowered = line.lower()
        if any(marker in lowered for marker in self.MVP9_ACCOUNT_EXPANSION_MARKERS):
            return False

        return any(marker in lowered for marker in self.MVP9_ACCOUNT_MAINTENANCE_MARKERS)

    def _is_mvp9_account_maintenance_step(self, step: dict | None) -> bool:
        if not isinstance(step, dict):
            return False
        name = step.get("name")
        return isinstance(name, str) and name in self.MVP9_ACCOUNT_MAINTENANCE_STEP_NAMES

    @staticmethod
    def _step_number_from(step: dict | None) -> int | None:
        if not isinstance(step, dict):
            return None
        value = step.get("step")
        return value if isinstance(value, int) else None

    @staticmethod
    def _format_finding(path: str, line_no: int, message: str) -> str:
        if path and line_no:
            return f"{path}:{line_no} - {message}"
        if path:
            return f"{path} - {message}"
        return message

    def _diff_new_path(self, diff_header: str) -> str:
        parts = diff_header.split()
        if len(parts) >= 4 and parts[3].startswith("b/"):
            return parts[3][2:]
        return ""

    def _skip_forbidden_scan_file(self, path: str) -> bool:
        return (
            path in self.FORBIDDEN_SCAN_EXCLUDED_PATHS
            or any(path.startswith(prefix) for prefix in self.FORBIDDEN_SCAN_EXCLUDED_PREFIXES)
            or self.STEP_OUTPUT_RE.match(path) is not None
        )

    def _line_in_safe_section(self, path: str, line_no: int) -> bool:
        if not path or line_no <= 0:
            return False
        file_path = self.root / path
        if not file_path.exists() or not file_path.is_file():
            return False
        try:
            lines = file_path.read_text(encoding="utf-8").splitlines()
        except OSError:
            return False
        start = min(line_no - 1, len(lines) - 1)
        for raw in reversed(lines[: start + 1]):
            line = raw.strip()
            if not line:
                continue
            lowered = line.lower()
            if line.startswith("#"):
                return any(marker in lowered for marker in self.SAFE_SECTION_MARKERS)
            if line.endswith(":") and any(marker in lowered for marker in self.SAFE_SECTION_MARKERS):
                return True
        return False

    def _run_codex_review(self, step: dict) -> ReviewResult:
        prompt = self._codex_review_prompt(step)
        cmd = self._codex_exec_cmd(prompt, self.review_effort)
        result = self._run(cmd, check=False, timeout=1800)
        if result.returncode != 0:
            return ReviewResult(
                False,
                [self._command_failure(self._codex_exec_cmd("<review-prompt>", self.review_effort), result)],
                "자체 리뷰 실행 실패",
                codex_passed=False,
            )
        parsed = self._parse_review_result(result.stdout)
        if parsed is None:
            return ReviewResult(
                False,
                ["자체 리뷰 실행 오류: JSON 결과를 파싱하지 못했습니다."],
                "자체 리뷰 실행 오류",
                codex_passed=False,
            )
        return parsed

    def _codex_review_prompt(self, step: dict) -> str:
        step_num = step.get("step", "?")
        step_name = step.get("name", "unknown")
        phase_readme = f"phases/{self.phase}/README.md"
        step_file = f"phases/{self.phase}/step{step_num}.md"
        return (
            "Read-only review only. Do not modify files. "
            f"Review the current branch diff against origin/{self.base} for SmartCloset MVP rules. "
            f"Current Harness step is Step {step_num} `{step_name}`. "
            "Ignore generated review-failure records under issues/**; they are audit logs, not implementation changes. "
            f"Check {phase_readme} and {step_file} first, then AGENTS.md, docs/PRD.md, docs/API.md, "
            "docs/RECOMMENDATION_RULES.md, docs/ARCHITECTURE.md, docs/adr/. "
            "For intermediate step PRs, the current step file is the step-local review contract. "
            "Missing functionality assigned to future steps is not a blocker. "
            "Implementing future-step scope inside the current step is a blocker. "
            "Do not require frontend auth/session, preferences API, recommendation history, preferenceScore, "
            "or final all-/api security boundary unless the current step file explicitly requires them. "
            "Focus on blockers: bugs, missing tests, MVP scope violations, API contract violations, build/test risk. "
            "Return only JSON with keys: pass (boolean), summary (string), findings (array of strings)."
        )

    def _parse_review_result(self, stdout: str) -> ReviewResult | None:
        candidates = list(reversed([line.strip() for line in stdout.splitlines() if line.strip()]))
        if stdout.strip():
            candidates.append(stdout.strip())

        for candidate in candidates:
            parsed = self._try_parse_review_candidate(candidate)
            if parsed is not None:
                return parsed
        return None

    def _try_parse_review_candidate(self, candidate: str) -> ReviewResult | None:
        try:
            data = json.loads(candidate)
        except json.JSONDecodeError:
            match = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", candidate, re.DOTALL)
            if not match:
                match = re.search(r"(\{.*\})", candidate, re.DOTALL)
            if not match:
                return None
            try:
                data = json.loads(match.group(1))
            except json.JSONDecodeError:
                return None

        return self._try_parse_review_payload(data)

    def _try_parse_review_payload(self, payload: object) -> ReviewResult | None:
        if isinstance(payload, str):
            return self._try_parse_review_candidate(payload)
        if isinstance(payload, list):
            for item in reversed(payload):
                nested = self._try_parse_review_payload(item)
                if nested is not None:
                    return nested
            return None
        if not isinstance(payload, dict):
            return None

        passed = payload.get("pass", payload.get("passed"))
        if isinstance(passed, str):
            passed = passed.lower() in {"true", "pass", "passed", "ok"}
        if isinstance(passed, bool):
            findings = payload.get("findings", [])
            if isinstance(findings, str):
                findings = [findings]
            if not isinstance(findings, list):
                findings = [str(findings)]
            summary = str(payload.get("summary", ""))
            return ReviewResult(passed, [str(item) for item in findings], summary, codex_passed=passed)

        for key in ("result", "message", "content", "final", "text", "value", "output", "item"):
            value = payload.get(key)
            if isinstance(value, (str, dict, list)):
                nested = self._try_parse_review_payload(value)
                if nested is not None:
                    return nested
        for value in payload.values():
            if isinstance(value, (str, dict, list)):
                nested = self._try_parse_review_payload(value)
                if nested is not None:
                    return nested
        return None

    # --- issue recording ---

    def _record_failure(self, pr_url: str, review: ReviewResult, step: dict) -> IssueRecord:
        number = self._next_issue_number()
        title = f"{self.phase} step {step['step']} 자동 리뷰 실패 {number}"
        body = self._issue_body(pr_url, review, step)
        issue_dir = self.root / "issues" / self.phase
        issue_dir.mkdir(parents=True, exist_ok=True)
        local_path = issue_dir / f"issue-{number}.md"
        local_path.write_text(f"# Issue {number}: {title}\n\n{body}", encoding="utf-8")

        gh_result = self._gh("issue", "create", "--title", title, "--body", body, check=False)
        github_url = self._extract_url(gh_result.stdout) if gh_result.returncode == 0 else ""
        self._commit_issue_record(local_path, step)
        return IssueRecord(number, title, body, local_path, github_url)

    def _append_failure_attempt(self, issue: IssueRecord, review: ReviewResult, step: dict, attempt: int):
        body = (
            f"## 재시도 {attempt} 리뷰 실패\n\n"
            f"{review.to_markdown()}\n"
        )
        existing = issue.local_path.read_text(encoding="utf-8")
        issue.local_path.write_text(f"{existing.rstrip()}\n\n---\n\n{body}", encoding="utf-8")
        self._comment_issue(issue, body)
        self._commit_issue_record(issue.local_path, step)

    def _resolve_failure_issue(self, issue: IssueRecord, review: ReviewResult, step: dict):
        body = (
            "## 자동 수정 완료\n\n"
            "같은 PR 브랜치에서 자동 수정 후 리뷰 gate를 통과했습니다.\n\n"
            f"{review.to_markdown()}\n"
        )
        existing = issue.local_path.read_text(encoding="utf-8")
        issue.local_path.write_text(f"{existing.rstrip()}\n\n---\n\n{body}", encoding="utf-8")
        if issue.github_url:
            self._gh("issue", "close", issue.github_url, "--comment", body, check=False)
        self._commit_issue_record(issue.local_path, step, message_suffix="자동 리뷰 해결 기록")

    def _commit_issue_record(self, local_path: Path, step: dict, *, message_suffix: str = "자동 리뷰 실패 기록"):
        try:
            rel_path = local_path.relative_to(self.root)
        except ValueError:
            rel_path = local_path

        if self._git("add", "--", str(rel_path), check=False).returncode != 0:
            return
        if self._git("diff", "--cached", "--quiet", check=False).returncode == 0:
            return

        message = f"chore: {self.phase} {step['step']}단계 {message_suffix}"
        if self._git("commit", "-m", message, check=False).returncode == 0:
            self._git("push", check=False)

    def _invoke_codex_fix(self, issue: IssueRecord, branch: str, step: dict,
                          review: ReviewResult, attempt: int):
        prompt = (
            "당신은 SmartCloset step PR 자동 리뷰 수정 담당자입니다. "
            f"현재 브랜치 `{branch}`에서 같은 PR의 리뷰 실패만 수정하세요.\n\n"
            "## 작업 범위\n"
            f"- Phase: {self.phase}\n"
            f"- Step: {step['step']} `{step['name']}`\n"
            f"- Fix attempt: {attempt}/{self.max_review_fixes}\n"
            "- 새 브랜치나 새 PR을 만들지 마세요.\n"
            "- 기존 변경을 되돌리지 말고, 리뷰 finding을 해결하는 데 필요한 최소 변경만 하세요.\n"
            "- 현재 step 파일에 없는 미래 step 기능을 구현해서 리뷰를 통과시키지 마세요.\n"
            "- 미래 step 미구현 finding은 현재 step 범위 밖이면 구현으로 해결하지 마세요.\n"
            "- 커밋과 push는 autopilot runner가 처리하므로 직접 커밋하지 마세요.\n\n"
            "## Issue\n"
            f"{issue.body}\n\n"
            "## 현재 리뷰 결과\n"
            f"{review.to_markdown()}\n\n"
            "수정 후 가능한 검증을 실행하고, 수정한 파일은 working tree에 남겨두세요."
        )
        self._run(self._codex_exec_cmd(prompt, self.fix_effort), timeout=1800)

    def _codex_exec_cmd(self, prompt: str, effort: str) -> list[str]:
        return ["codex", "exec", "--json", *codex_effort_config(effort), prompt]

    def _commit_dirty_fix(self, step: dict):
        status = self._git("status", "--short", "--untracked-files=all").stdout.strip()
        if not status:
            return
        self._git("add", "-A")
        if self._git("diff", "--cached", "--quiet", check=False).returncode == 0:
            return
        msg = f"fix: {self.phase} {step['step']}단계 리뷰 이슈 수정"
        self._git("commit", "-m", msg)

    def _push_branch(self, branch: str):
        self._git("push", "-u", "origin", branch)

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

    def _issue_body(self, pr_url: str, review: ReviewResult, step: dict) -> str:
        commands = "\n".join(self._review_commands(step))
        return (
            "## 발생 위치\n"
            f"- Phase: {self.phase}\n"
            f"- Step: {step['step']} `{step['name']}`\n"
            f"- PR: {pr_url}\n\n"
            "## 재현 명령\n"
            "```bash\n"
            f"{commands}\n"
            "```\n\n"
            "## 핵심 에러\n"
            f"{review.to_markdown()}\n\n"
            "## 수정 방향\n"
            "- 같은 PR 브랜치에서 발견사항을 수정하고 같은 gate를 다시 통과시킨다.\n\n"
            "## 완료 기준\n"
            "- 로컬 검증, 금지 범위 검색, Codex 자체 리뷰를 모두 통과한다.\n"
        )

    # --- parsing ---

    @staticmethod
    def _extract_url(output: str) -> str:
        match = re.search(r"https://\S+", output)
        return match.group(0).rstrip(")") if match else ""


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Run a Harness phase as reviewed step PRs.")
    parser.add_argument("phase", help="Phase directory name, e.g. 1-smartcloset-mvp")
    parser.add_argument("--base", default="main", help="Base branch for PRs and merges")
    parser.add_argument(
        "--max-review-fixes",
        type=int,
        default=2,
        help="Maximum automatic fix attempts inside the same step PR",
    )
    parser.add_argument("--unsafe", action="store_true", help="Pass --unsafe to scripts/execute.py")
    parser.add_argument(
        "--step-effort",
        choices=ALLOWED_CODEX_EFFORTS,
        default=DEFAULT_STEP_EFFORT,
        help="Reasoning effort for step implementation calls",
    )
    parser.add_argument(
        "--review-effort",
        choices=ALLOWED_CODEX_EFFORTS,
        default=DEFAULT_REVIEW_EFFORT,
        help="Reasoning effort for PR self-review calls",
    )
    parser.add_argument(
        "--fix-effort",
        choices=ALLOWED_CODEX_EFFORTS,
        default=DEFAULT_FIX_EFFORT,
        help="Reasoning effort for automatic review-fix calls",
    )
    parser.add_argument("--allow-xhigh", action="store_true", help="Allow xhigh reasoning effort")
    args = parser.parse_args(argv)
    for name, effort in (
        ("--step-effort", args.step_effort),
        ("--review-effort", args.review_effort),
        ("--fix-effort", args.fix_effort),
    ):
        try:
            validate_codex_effort(effort, allow_xhigh=args.allow_xhigh)
        except ValueError as exc:
            parser.error(f"{name}: {exc}")

    try:
        pr_urls = AutopilotRunner(
            args.phase,
            base=args.base,
            max_review_fixes=args.max_review_fixes,
            unsafe=args.unsafe,
            step_effort=args.step_effort,
            review_effort=args.review_effort,
            fix_effort=args.fix_effort,
            allow_xhigh=args.allow_xhigh,
        ).run()
    except AutopilotError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 1

    print(f"Autopilot completed: {pr_urls}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
