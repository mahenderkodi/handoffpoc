#!/usr/bin/env bash
# Turns the mechanical log from log-progress.sh into a real handoff update, by
# invoking Claude Code non-interactively (headless, `claude -p`) to run the
# handoff skill's pipeline and MERGE into the latest existing handoff (per
# ref/pipeline.md step 5's chaining rule), using the progress log as step 1's
# evidence -- a headless run has no tool history of its own to derive from.
#
# OPTIONAL add-on, same as log-progress.sh -- not part of the core skill.
#
# Fires from:
#   - a git post-commit hook (install below) -- one real milestone per commit
#   - optionally, .claude/settings.json's PreCompact hook, pointed at this
#     same script, for the skill's other existing "automatic" trigger
#
# Install the git hook once, from the repo root:
#   cat > .git/hooks/post-commit <<'EOF'
#   #!/usr/bin/env bash
#   exec "$(git rev-parse --show-toplevel)/handoff/scripts/trigger-handoff-update.sh"
#   EOF
#   chmod +x .git/hooks/post-commit
#
# IMPORTANT -- read before enabling:
# This runs Claude Code with --dangerously-skip-permissions, because an
# unattended background run has no one present to approve individual tool
# calls (Read the progress log, Write the handoff file) -- without the flag
# it would just hang or fail. That means this script gives Claude Code
# unattended write access, scoped in practice to what the handoff skill does
# (read handoffs/.progress.log and prior handoffs, write a new handoff .md),
# but not sandboxed to that scope by anything in this script. If you'd
# rather not grant a blanket skip, replace the flag below with Claude Code's
# --allowedTools to scope it to just Read/Write on handoffs/*.md, or don't
# install the git hook and rely on manual/PreCompact triggering instead.
#
# Never blocks `git commit` -- runs Claude in the background and logs its
# own output to handoffs/.last-auto-update.log for debugging.
#
# NOTE: this script has not been run/verified end-to-end -- it was written
# against Claude Code's documented headless (`-p`) and hooks behavior, but
# not executed in this environment. Test it once manually before trusting
# it to run unattended: run `handoff/scripts/trigger-handoff-update.sh`
# yourself after adding a line to handoffs/.progress.log, and check
# handoffs/.last-auto-update.log.

set -uo pipefail

repo_root="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$repo_root" || exit 0

progress_log="$repo_root/handoffs/.progress.log"

# Nothing mechanical was recorded since the last update -> nothing to synthesize.
[ -s "$progress_log" ] || exit 0

command -v claude >/dev/null 2>&1 || exit 0   # Claude Code CLI not on PATH here -> skip silently

run_update() {
  local latest
  latest="$(ls -1t "$repo_root"/handoffs/handoff-*.md 2>/dev/null | head -n1)"

  claude -p "Use the handoff skill. A mechanical progress log has been recorded at
handoffs/.progress.log since the last handoff (tab-separated: UTC timestamp,
tool name, then a file path or shell command) -- read it and treat its
contents as this run's read/modified-file and command evidence for
pipeline.md step 1, since a headless invocation has no tool history of its
own to derive from. ${latest:+The most recent handoff is $latest -- merge into it per step 5's chaining rule rather than starting a fresh one.}
Write the updated handoff, then stop. Do not ask questions; there is no one
present to answer them." \
    --dangerously-skip-permissions \
    >> "$repo_root/handoffs/.last-auto-update.log" 2>&1

  # Rotate rather than delete, so a failed run's evidence isn't silently lost.
  if [ -s "$progress_log" ]; then
    mv "$progress_log" "$progress_log.$(date -u +%Y%m%dT%H%M%SZ)"
  fi
}

run_update &
disown
exit 0
