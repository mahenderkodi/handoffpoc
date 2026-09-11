#!/usr/bin/env bash
# handoff-hook-test (jq verified)
# PostToolUse hook: mechanically logs what changed, for the handoff skill's
# periodic-synthesis auto-update mode. Never blocks or fails the tool call
# it's watching -- always exits 0, writes nothing to stdout.
#
# This is OPTIONAL tooling on top of the handoff skill, not part of the
# skill's core pipeline -- the skill itself stays dependency-free. Only
# needed if you want handoffs/.progress.log to feed automatic updates
# (see trigger-handoff-update.sh and README.md's "Automatic updates" section).
#
# Requires: jq. If jq isn't installed, this silently no-ops (never fails
# the tool call it's attached to).
#
# Install, in the WORKING PROJECT's .claude/settings.json (not this skill's
# own settings -- this hook runs once per project that opts in):
#   "hooks": {
#     "PostToolUse": [
#       { "matcher": "Edit|Write|MultiEdit|Bash",
#         "hooks": [ { "type": "command", "command": "handoff/scripts/log-progress.sh" } ] }
#     ]
#   }
#
# Adjust the command path if the skill is installed somewhere other than
# ./handoff relative to the project root Claude Code runs hooks from.

set -uo pipefail

input="$(cat)"
command -v jq >/dev/null 2>&1 || exit 0

tool_name="$(printf '%s' "$input" | jq -r '.tool_name // empty' 2>/dev/null)"
[ -z "$tool_name" ] && exit 0

detail=""
case "$tool_name" in
  Edit|Write|MultiEdit)
    detail="$(printf '%s' "$input" | jq -r '.tool_input.file_path // empty' 2>/dev/null)"
    ;;
  Bash)
    detail="$(printf '%s' "$input" | jq -r '.tool_input.command // empty' 2>/dev/null | head -c 200)"
    ;;
  *)
    exit 0   # only log tool calls that represent real work on the project
    ;;
esac
[ -z "$detail" ] && exit 0

repo_root="$(git rev-parse --show-toplevel 2>/dev/null || pwd)"
log_dir="$repo_root/handoffs"
mkdir -p "$log_dir" 2>/dev/null

ts="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
printf '%s\t%s\t%s\n' "$ts" "$tool_name" "$detail" >> "$log_dir/.progress.log" 2>/dev/null

exit 0
