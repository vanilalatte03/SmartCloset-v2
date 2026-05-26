import json
import sys
from pathlib import Path
from unittest.mock import patch

sys.path.insert(0, str(Path(__file__).parent))
import checks


def test_commands_from_docs_reads_active_table(tmp_path):
    docs = tmp_path / "docs"
    docs.mkdir()
    (docs / "COMMANDS.md").write_text(
        """
## 활성 명령

| 이름 | 명령 | 필수 | 설명 |
| --- | --- | --- | --- |
| lint | `ruff check .` | no | lint |
| test | `python -m pytest` | yes | tests |
| build |  | yes | empty |
| frontend-build | `cd frontend && npm run build` | yes | frontend |
| harness-test | `python3 -m pytest scripts/test_checks.py scripts/test_execute.py scripts/test_autopilot.py scripts/test_guard.py` | yes | harness |
| docs-check | `python3 scripts/checks.py --docs-check` | yes | docs |
""".strip()
    )

    result = checks.commands_from_docs(tmp_path)

    assert result["lint"][0].command == "ruff check ."
    assert result["test"][0].command == "python -m pytest"
    assert result["frontend-build"][0].command == "cd frontend && npm run build"
    assert (
        result["harness-test"][0].command
        == "python3 -m pytest scripts/test_checks.py scripts/test_execute.py scripts/test_autopilot.py scripts/test_guard.py"
    )
    assert result["docs-check"][0].command == "python3 scripts/checks.py --docs-check"
    assert "build" not in result


def test_profile_commands_take_precedence(tmp_path):
    codex = tmp_path / ".codex"
    codex.mkdir()
    (codex / "project-profile.json").write_text(
        json.dumps({"commands": {"test": ["custom test"]}})
    )
    docs = tmp_path / "docs"
    docs.mkdir()
    (docs / "COMMANDS.md").write_text(
        """
## 활성 명령
| 이름 | 명령 | 필수 | 설명 |
| --- | --- | --- | --- |
| test | `docs test` | yes | tests |
""".strip()
    )

    selected = checks.collect_checks(tmp_path)

    assert [command.command for command in selected] == ["custom test"]


def test_manual_stage_excludes_final_only_docs_check(tmp_path):
    docs = tmp_path / "docs"
    docs.mkdir()
    (docs / "COMMANDS.md").write_text(
        """
## 활성 명령
| 이름 | 명령 | 필수 | 설명 |
| --- | --- | --- | --- |
| test | `python -m pytest` | yes | tests |
| harness-test | `python3 -m pytest scripts/test_checks.py` | yes | harness |
| docs-check | `python3 scripts/checks.py --docs-check` | yes | docs |
""".strip()
    )

    selected = checks.collect_checks(tmp_path, "manual")

    assert [command.name for command in selected] == ["test"]


def test_final_stage_includes_docs_check(tmp_path):
    docs = tmp_path / "docs"
    docs.mkdir()
    (docs / "COMMANDS.md").write_text(
        """
## 활성 명령
| 이름 | 명령 | 필수 | 설명 |
| --- | --- | --- | --- |
| test | `python -m pytest` | yes | tests |
| harness-test | `python3 -m pytest scripts/test_checks.py` | yes | harness |
| docs-check | `python3 scripts/checks.py --docs-check` | yes | docs |
""".strip()
    )

    selected = checks.collect_checks(tmp_path, "final")

    assert [command.name for command in selected] == ["test", "harness-test", "docs-check"]


def test_detect_node_uses_lockfile_package_manager(tmp_path):
    (tmp_path / "package.json").write_text(json.dumps({"scripts": {"test": "vitest", "build": "vite build"}}))
    (tmp_path / "pnpm-lock.yaml").write_text("")

    selected = checks.detect_commands(tmp_path)

    assert selected["test"][0].command == "pnpm test"
    assert selected["build"][0].command == "pnpm build"


def test_detect_spring_prefers_gradle(tmp_path):
    (tmp_path / "gradlew").write_text("")
    (tmp_path / "pom.xml").write_text("<project />")

    selected = checks.detect_commands(tmp_path)

    assert selected["test"][0].command == "./gradlew test"
    assert selected["build"][0].command == "./gradlew build"


def test_detect_python_uses_uv_when_available(tmp_path):
    (tmp_path / "pyproject.toml").write_text("[project]\nname = 'demo'\n")
    with patch("shutil.which", return_value="/opt/bin/uv"):
        selected = checks.detect_commands(tmp_path)

    assert selected["test"][0].command == "uv run pytest"
    assert selected["lint"][0].command == "uv run ruff check ."


def test_placeholder_commands_are_ignored():
    assert not checks.is_real_command("<docs/COMMANDS.md의 test 명령>")
    assert not checks.is_real_command("")
    assert checks.is_real_command("python -m pytest")


def write_docs_check_fixture(root: Path):
    phase_dir = root / "phases" / "5-smartcloset-clothing-images"
    phase_dir.mkdir(parents=True)
    (root / "phases" / "index.json").write_text(
        json.dumps(
            {
                "phases": [
                    {"dir": "4-smartcloset-usable-ux", "status": "completed"},
                    {"dir": "5-smartcloset-clothing-images", "status": "pending"},
                ]
            }
        )
    )
    (phase_dir / "docs-checks.json").write_text(
        json.dumps(
            {
                "paths": [
                    "README.md",
                    "docs",
                    "AGENTS.md",
                    ".agents/skills/smartcloset-backend/SKILL.md",
                    "phases/5-smartcloset-clothing-images",
                    "frontend/src",
                ],
                "skipDirs": [".git", "__pycache__"],
                "skipSuffixes": [".json"],
                "required": [
                    {
                        "name": "MVP5 image upload contract markers",
                        "pattern": "MVP5|이미지 업로드|/api/clothes/.*/image|CLOTHING_IMAGE_STORAGE_DIR",
                    }
                ],
                "forbidden": [
                    {
                        "name": "forbidden today recommendation GET API",
                        "pattern": "GET /api/recommendations/(today)",
                    }
                ],
            }
        )
    )
    docs = root / "docs"
    docs.mkdir()
    (root / "README.md").write_text("MVP5 이미지 업로드 /api/clothes/1/image CLOTHING_IMAGE_STORAGE_DIR")
    (docs / "API.md").write_text("POST /api/recommendations")
    (root / "AGENTS.md").write_text("project rules")
    skill_dir = root / ".agents" / "skills" / "smartcloset-backend"
    skill_dir.mkdir(parents=True)
    (skill_dir / "SKILL.md").write_text("smartcloset skill")
    (phase_dir / "README.md").write_text("MVP5 clothing image upload")
    (phase_dir / "step0-output.json").write_text("GET /api/recommendations/today")
    frontend_dir = root / "frontend" / "src"
    frontend_dir.mkdir(parents=True)
    (frontend_dir / "App.tsx").write_text("const endpoint = '/api/recommendations';")


def test_docs_check_passes_for_current_contract_markers(tmp_path):
    write_docs_check_fixture(tmp_path)

    assert checks.run_docs_checks(tmp_path) == 0


def test_docs_check_flags_forbidden_today_endpoint(tmp_path, capsys):
    write_docs_check_fixture(tmp_path)
    (tmp_path / "docs" / "API.md").write_text("GET /api/recommendations/today")

    assert checks.run_docs_checks(tmp_path) == 1
    captured = capsys.readouterr()
    assert "forbidden today recommendation GET API" in captured.err
    assert "docs/API.md:1" in captured.err


def test_docs_check_supports_custom_config_path(tmp_path, capsys):
    config = tmp_path / "custom-docs-checks.json"
    config.write_text(
        json.dumps(
            {
                "paths": ["docs"],
                "required": [{"name": "custom marker", "pattern": "CUSTOM_MARKER"}],
                "forbidden": [],
            }
        )
    )
    docs = tmp_path / "docs"
    docs.mkdir()
    (docs / "README.md").write_text("plain docs")

    assert checks.run_docs_checks(tmp_path, str(config)) == 1
    captured = capsys.readouterr()
    assert "custom marker" in captured.err


def test_docs_check_required_rule_paths_are_scoped(tmp_path, capsys):
    config = tmp_path / "docs-checks.json"
    config.write_text(
        json.dumps(
            {
                "paths": ["docs"],
                "required": [
                    {
                        "name": "API-only marker",
                        "paths": ["docs/API.md"],
                        "pattern": "API_ONLY_MARKER",
                    }
                ],
                "forbidden": [],
            }
        )
    )
    docs = tmp_path / "docs"
    docs.mkdir()
    (docs / "API.md").write_text("plain api")
    (docs / "FRONTEND.md").write_text("API_ONLY_MARKER")

    assert checks.run_docs_checks(tmp_path, str(config)) == 1
    captured = capsys.readouterr()
    assert "API-only marker" in captured.err

    (docs / "API.md").write_text("API_ONLY_MARKER")
    assert checks.run_docs_checks(tmp_path, str(config)) == 0


def test_docs_check_forbidden_rule_paths_are_scoped(tmp_path, capsys):
    config = tmp_path / "docs-checks.json"
    config.write_text(
        json.dumps(
            {
                "paths": ["docs"],
                "required": [],
                "forbidden": [
                    {
                        "name": "API-only forbidden marker",
                        "paths": ["docs/API.md"],
                        "pattern": "FORBIDDEN_MARKER",
                    }
                ],
            }
        )
    )
    docs = tmp_path / "docs"
    docs.mkdir()
    (docs / "API.md").write_text("plain api")
    (docs / "FRONTEND.md").write_text("FORBIDDEN_MARKER")

    assert checks.run_docs_checks(tmp_path, str(config)) == 0

    (docs / "API.md").write_text("FORBIDDEN_MARKER")
    assert checks.run_docs_checks(tmp_path, str(config)) == 1
    captured = capsys.readouterr()
    assert "docs/API.md:1" in captured.err


def test_docs_check_discovers_latest_completed_phase_config(tmp_path):
    phases = tmp_path / "phases"
    phase4 = phases / "4-smartcloset-usable-ux"
    phase5 = phases / "5-smartcloset-clothing-images"
    phase4.mkdir(parents=True)
    phase5.mkdir()
    (phases / "index.json").write_text(
        json.dumps(
            {
                "phases": [
                    {"dir": "4-smartcloset-usable-ux", "status": "completed"},
                    {"dir": "5-smartcloset-clothing-images", "status": "completed"},
                ]
            }
        )
    )
    (phase4 / "docs-checks.json").write_text(json.dumps({"paths": ["docs"], "required": [], "forbidden": []}))
    (phase5 / "docs-checks.json").write_text(json.dumps({"paths": ["README.md"], "required": [], "forbidden": []}))

    assert checks.discover_docs_check_config(tmp_path) == phase5 / "docs-checks.json"
