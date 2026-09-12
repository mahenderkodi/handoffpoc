#!/usr/bin/env bash
# Turns the mechanical log from log-progress.sh into a real handoff update, by
# invoking Claude Code non-interactively (headless, `claude -p`) to run the
# handoff skill's pipeline and update the stable handoffs/handoff.md IN PLACE
# (per ref/pipeline.md step 5), archiving-and-compacting it first if it has
# grown past that step's size threshold, using the progress log as step 1's
# evidence -- a headless run has no tool history of its own to derive from.
#
# OPTIONAL add-on, same as log-progress.sh -- not part of the core skill.
#
# Fires from:
#   - a git post-commit hook (install below) -- one real milestone per commit
#   - optionally, .claude/settings.json's PreCompact hook, pointed at this
#     same script, for the skill's other existing "automatic" trigger
#
# handoffs/handoff.md is the one stable, always-current file -- this script
# updates it in place and never creates a new handoff-<timestamp>.md itself.
# That naming is now reserved for handoffs/.archive/ snapshots only, written
# when handoff.md crosses the size threshold documented in ref/pipeline.md
# step 5 -- that content is never deleted, only relocated into .archive/ once
# the live file would otherwise grow unbounded. (Dot-prefixed like .internal/
# so a plain `ls handoffs/` still shows just handoff.md -- unlike .internal/,
# this folder holds real project history: still a plain directory, never a
# zip, so it stays dependency-free, safe under concurrent writes, and
# readable with a plain `cat` -- just out of the way of a casual listing.)
# The one exception is
# handoffs/.internal/.progress.log itself: once a run successfully merges it
# into handoff.md, the log is deleted (not archived) -- see the delete-vs-
# rotate note further down, where the actual logic lives.
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
# (read handoffs/.internal/.progress.log and prior handoffs, write a new
# handoff .md), but not sandboxed to that scope by anything in this script.
# If you'd
# rather not grant a blanket skip, replace the flag below with Claude Code's
# --allowedTools to scope it to just Read/Write on handoffs/*.md, or don't
# install the git hook and rely on manual/PreCompact triggering instead.
#
# Never blocks `git commit` -- runs Claude in the background and logs its
# own output to handoffs/.internal/.last-auto-update.log for debugging.
#
# Everything this add-on writes for its own bookkeeping (.progress.log,
# .last-auto-update.log) lives under handoffs/.internal/, deliberately kept
# out of handoffs/ itself. handoffs/ is what a developer actually looks at
# day to day -- ideally just handoff.md, with .internal/ and .archive/ both
# dot-prefixed out of a plain listing -- and neither of these internal
# bookkeeping files has any standalone value there. On a run that completes
# successfully, .progress.log is deleted outright rather than rotated to a
# timestamped sibling, since its content has already been merged into
# handoff.md and keeping it around only piles up files nobody reads. A FAILED
# run still rotates it (into .internal/, or to .archive/ if it's huge)
# instead of deleting it, so that evidence isn't silently lost if something
# goes wrong.
#
# VERIFIED END-TO-END (2026-09-11): a real commit on the user's machine fired
# this script via .git/hooks/post-commit, which ran the headless `claude -p`
# call above to completion -- it read the progress log, merged that
# progress into handoffs/handoff.md in place (Done items preserved, new
# milestone appended, Generated: timestamp refreshed), then this script
# cleared the progress log and logged the full run to
# handoffs/.internal/.last-auto-update.log. One real gotcha hit during that
# test: the headless claude -p process can still be running (check with a
# process listing, e.g. Get-CimInstance Win32_Process on Windows) even after
# handoff.md already shows the fresh write -- the log/cleanup steps below it
# only complete once that process actually exits, so an empty
# .last-auto-update.log or a still-present .progress.log right after a
# commit does not mean it failed; give it time (tens of seconds) before
# concluding that. (Note: at verification time this script still rotated
# .progress.log to a timestamped sibling on every run, developer-visible
# clutter it produced was the reason the delete-on-success behavior below
# was added afterward -- the merge/archive logic itself is unchanged.)
# The one-time missing-dependency gap this surfaced (not a bug in this
# script): log-progress.sh silently no-ops if `jq` isn't installed, which
# means .progress.log never gets populated and this script's own `[ -s
# "$progress_log" ] || exit 0` guard then exits before ever reaching the
# `claude -p` call -- install `jq` first if the chain seems inert.
#
# PREREQUISITE, Windows especially -- set this ONCE before relying on the git
# hook, not after hitting the symptom: this script launches a *second*,
# background `claude.exe` process on every commit. If Claude Code's own
# self-updater tries to replace claude.exe while that background process (or
# any other `claude` session) is still running, it fails with an
# unrelated-looking "claude.exe in use" error -- that's this add-on's own
# background process holding the file lock, not a real problem with your
# install. Fix once, in a fresh terminal afterward:
#   setx DISABLE_AUTOUPDATER 1
# You can still update deliberately later (npm update -g @anthropic-ai/claude-code);
# this only stops the CLI from silently racing itself against this script.

set -uo pipefail

repo_root="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
cd "$repo_root" || exit 0

internal_dir="$repo_root/handoffs/.internal"
progress_log="$internal_dir/.progress.log"
update_log="$internal_dir/.last-auto-update.log"
mkdir -p "$internal_dir" 2>/dev/null

# Nothing mechanical was recorded since the last update -> nothing to synthesize.
[ -s "$progress_log" ] || exit 0

command -v claude >/dev/null 2>&1 || exit 0   # Claude Code CLI not on PATH here -> skip silently

run_update() {
  local handoff_file="$repo_root/handoffs/handoff.md"

  claude -p "Use the handoff skill. A mechanical progress log has been recorded at
handoffs/.internal/.progress.log since the last update (tab-separated: UTC
timestamp, tool name, then a file path or shell command) -- read it and treat
its contents as this run's read/modified-file and command evidence for
pipeline.md step 1, since a headless invocation has no tool history of its
own to derive from. ${handoff_file:+If handoffs/handoff.md already exists, update it IN PLACE per pipeline.md step 5 -- merge this progress in (promote In Progress to Done, refresh the Immediate Next Step), do not start a fresh timestamped file.}
If the merged content would put handoffs/handoff.md at or over the size
threshold in pipeline.md step 5 (~30KB), first archive the full current
handoffs/handoff.md, unabridged, to handoffs/.archive/handoff-<UTC>-<author>.md,
then write a compact handoffs/handoff.md whose Handoff Chain -> Archived
predecessor points at that archived file -- never delete detail, only
relocate it. Write the updated handoff, then stop. Do not ask questions;
there is no one present to answer them." \
    --dangerously-skip-permissions \
    >> "$update_log" 2>&1
  local claude_exit=$?

  if [ "$claude_exit" -eq 0 ]; then
    # Success: the progress log's content is now merged into handoff.md, so
    # it has no further value -- delete it rather than piling up rotated
    # copies nobody looks at. This is the one deliberate exception to the
    # skill's general "never delete, only relocate" rule, because this file
    # is internal working data (consumed input), not project history.
    rm -f "$progress_log"
  elif [ -s "$progress_log" ]; then
    # Failure: keep the evidence as a safety net instead of losing it, same
    # as before -- just relocated under .internal/ instead of handoffs/.
    if [ "$(wc -c < "$progress_log" 2>/dev/null || echo 0)" -ge 1048576 ]; then
      mkdir -p "$repo_root/handoffs/.archive"
      mv "$progress_log" "$repo_root/handoffs/.archive/progress-$(date -u +%Y-%m-%d).log"
    else
      mv "$progress_log" "$progress_log.$(date -u +%Y%m%dT%H%M%SZ)"
    fi
  fi
}

run_update &
disown
exit 0
