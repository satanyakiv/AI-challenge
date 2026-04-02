# Plan: Set Up OpenAI Codex Plugin for Claude Code

## Context

OpenAI released `codex-plugin-cc` (March 30, 2026) — an official plugin that lets Claude Code users run Codex reviews and delegate tasks. The user wants it installed and wired into the existing `/review` workflow.

**Current state:**
- Codex CLI v0.118.0 installed at `/opt/homebrew/bin/codex`
- Auth: **Logged in using ChatGPT** (no API key needed)
- Node.js v20.20.0 (meets 18.18+ requirement)
- Project already trusted in `~/.codex/config.toml`
- No `codex` plugin in Claude Code yet
- Existing `/review` command in Ukrainian with 4-step manual checklist

**No blockers.** All prerequisites are met.

---

## Steps

### Phase A: Plugin Installation (user runs interactively)

Claude will guide the user to run these 4 commands in sequence:

1. `/plugin marketplace add openai/codex-plugin-cc`
2. `/plugin install codex@openai-codex`
3. `/reload-plugins`
4. `/codex:setup` — verify Codex CLI + auth are detected

### Phase B: File Changes (Claude does)

**B1. Update `.claude/commands/review.md`**
- Add new **Step 1 — Codex automated review** before existing manual steps
- Keep the existing architecture + test checklists intact (Steps 2-5)
- Add `--base main` for branch review, `--background` for large diffs
- Add "Джерело" (Source) column to report table (Codex vs Manual)
- Graceful fallback: if Codex unavailable, skip Step 1
- Keep language in Ukrainian

**B2. Add `.codex/` exclusions to `.gitignore`**
- Append Codex cache/session/sqlite patterns
- Keep `.codex/config.toml` committable

**B3. Optionally create `.codex/config.toml`** (project-level)
- Minimal: sandbox read permissions for review mode
- Skip if user prefers to rely on user-level config only

### Phase C: Smoke Test

1. `codex review --commit HEAD` via Bash (read-only CLI test)
2. User runs `/codex:review --background` in Claude Code
3. User runs `/codex:status` → `/codex:result`
4. User runs `/review` to exercise the updated command

---

## Critical Files

| File | Action |
|------|--------|
| `.claude/commands/review.md` | Rewrite: add Codex step, keep manual checklist |
| `.gitignore` | Append `.codex/` cache exclusions |
| `~/.claude/settings.json` | Modified by `/plugin` commands (user action) |

## Verification

1. `/codex:setup` reports green (CLI found, auth valid)
2. `/codex:review --background` starts without error
3. `/codex:status` shows the running/completed job
4. `/codex:result` returns review output
5. `/review` runs both Codex + manual checklist
