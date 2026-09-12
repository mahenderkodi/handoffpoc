# handoff

**The document your next agent opens before it works.**

Live page → **https://cskwork.github.io/agent-handoff/**

A [Claude Code](https://docs.claude.com/en/docs/claude-code) skill for long-running tasks. When a
session runs long, `handoff` compacts everything that matters into one structured markdown file —
objective, current state, what's done (with evidence), what's left, key decisions and why, files
touched, gotchas, the single exact next action, and how to verify — so a **fresh-context agent
resumes with zero ambiguity**.

Pipeline and document structure are inspired by [oh-my-pi](https://github.com/can1357/oh-my-pi)'s
`/handoff`. See [Credit](#credit).

## What it does

- **Captures exact state, not vibes** — real file paths, commands, test results, error text; the
  read/modified file sets are derived mechanically from tool history, never from recall.
- **Defends against cold-resume failures** — an explicit DO-NOT-REDO done list, a single exact next
  step, and any pending user question preserved verbatim.
- **Paste-ready and self-maintaining** — updates the single stable `handoffs/handoff.md` in place
  and emits a `<handoff-context>` block to paste into a fresh chat; when the file grows past a size
  threshold it archives the full prior version to `handoffs/.archive/` and keeps going compact.

## How it works (pipeline)

`gather → structure → write → verify → resume`

1. **Gather** — scan tool history, derive read/modified sets, run read-only `git`/test probes.
2. **Structure** — fill the fixed template; Current State first, Done labeled DO NOT REDO.
3. **Write** — update `handoffs/handoff.md` in place, merging rather than overwriting; archive the
   full prior version to `handoffs/.archive/` first if the file has grown past ~30KB.
4. **Verify** — run the quality checklist (no secrets, no placeholders, copy-paste verify commands,
   open question preserved).
5. **Resume** — print the path + the `<handoff-context>` resume block.

Full detail: [`handoff/ref/pipeline.md`](handoff/ref/pipeline.md).

## Install

The skill lives in the `handoff/` folder so the skill directory name matches the skill name.

```bash
git clone https://github.com/cskwork/agent-handoff.git
ln -s "$PWD/agent-handoff/handoff" ~/.claude/skills/handoff   # Claude Code
ln -s "$PWD/agent-handoff/handoff" ~/.codex/skills/handoff    # Codex
```

Invoke it explicitly, or let your harness trigger it automatically — e.g. Claude Code's
`PreCompact` hook, right before context gets compacted. It never self-triggers from a heuristic
like an edit count:

> create a handoff · 핸드오프 만들어 · summarize where we left off · next plan of action

Output lands in `handoffs/` in your working project. Other agents (Codex, Gemini) can use it too —
point them at `handoff/SKILL.md`.

## Automatic updates (optional)

The core skill above is always manual-or-explicit-trigger — that's deliberate (see `SKILL.md`'s
"When to use"), because regenerating a full handoff on every edit would be both noisy and
expensive. If you want handoffs to stay current with less remembering-to-ask, two optional scripts
add a two-tier automation on top, without changing that default:

1. **Mechanical logging, always on, zero cost.** `handoff/scripts/log-progress.sh`, wired as a
   Claude Code `PostToolUse` hook, appends one line per file edit or command run to
   `handoffs/.internal/.progress.log` — no AI call, just a fact log. Requires `jq`.
2. **Real synthesis, only at milestones.** `handoff/scripts/trigger-handoff-update.sh`, wired as a
   git `post-commit` hook (a commit is a real milestone), reads that log and invokes Claude Code
   headlessly to merge it into the latest handoff, then deletes the log now that its content has
   been consumed. Requires the `claude` CLI on `PATH`.

Both files live under `handoffs/.internal/`, not directly inside `handoffs/` — deliberately kept
out of sight, since neither has any standalone value to a developer browsing the project (see
"Archive / rotation" below for exactly why and when).

### Prerequisites — check these BEFORE enabling, not after something silently doesn't work

Both scripts fail **silently** when a dependency is missing (by design — a hook must never break
the tool call or commit it's attached to), which makes a missing dependency easy to mistake for a
setup mistake. Check all three once, up front, so the flow stays smooth instead of stalling on a
rediscovery later:

- **`jq` on `PATH`.** Without it, `log-progress.sh` no-ops on every tool call and
  `handoffs/.internal/.progress.log` never gets created — nothing else in the chain can run. Verify
  with `jq --version`; install with `winget install jqlang.jq -e`, `choco install jq -y`, or by
  dropping the binary onto `PATH` directly (e.g. into Git Bash's own `/usr/bin/`).
- **`claude` CLI on `PATH`.** Without it, `trigger-handoff-update.sh` no-ops after `git commit` and
  `handoffs/.internal/.last-auto-update.log` never gets created. Verify with `claude --version`.
- **Claude Code's own self-updater disabled, on Windows in particular.** This add-on launches a
  *second*, background `claude.exe` process on every commit (the headless synthesis run). If
  Claude Code's own auto-updater tries to replace `claude.exe` while that background process (or
  any other `claude` session) is still running, the update fails with `claude.exe in use` — an
  unrelated-looking error that is really just this add-on's own background process holding a file
  lock. Set once, in a fresh terminal afterward:
  ```bash
  setx DISABLE_AUTOUPDATER 1
  ```
  This doesn't stop you from updating deliberately later (`npm update -g @anthropic-ai/claude-code`
  whenever you want) — it just stops the CLI from silently racing itself against this add-on's own
  background process.

Setup — in the **working project** (not this skill's own repo), add to `.claude/settings.json`:

```json
{
  "hooks": {
    "PostToolUse": [
      { "matcher": "Edit|Write|MultiEdit|Bash",
        "hooks": [ { "type": "command", "command": "handoff/scripts/log-progress.sh" } ] }
    ]
  }
}
```

and, from the project's repo root, install the git hook once:

```bash
cat > .git/hooks/post-commit <<'EOF'
#!/usr/bin/env bash
exec "$(git rev-parse --show-toplevel)/handoff/scripts/trigger-handoff-update.sh"
EOF
chmod +x .git/hooks/post-commit
```

**Read the safety note at the top of `trigger-handoff-update.sh` before enabling it.** It runs
Claude Code with `--dangerously-skip-permissions` — required for an unattended run to do anything
at all, since there's no one present to approve tool calls, but it is a real, blanket grant, not
sandboxed by the script to only the handoff skill's own reads/writes. If that's not a trade you
want, use `--allowedTools` scoped to `handoffs/*.md` instead, or skip the git hook and keep
triggering updates manually or via `PreCompact` only.

Both scripts are optional add-ons layered on top of the skill, not part of its core pipeline —
skip this section entirely and the skill works exactly as described above, dependency-free.

## Archive / rotation (document size, not context size)

`handoffs/handoff.md` is a single stable file, updated in place on every write instead of
growing a new timestamped file per write. That keeps "where's the current handoff" unambiguous,
but a live document that only ever grows eventually stops being skimmable. So the skill checks
size on every write (`ref/pipeline.md` step 5):

- `handoffs/handoff.md` at/over **~30KB** → the full current content is archived, unabridged, to
  `handoffs/.archive/handoff-<UTC>-<author>.md` (the old per-write naming, now reserved for
  archived snapshots), then `handoffs/handoff.md` is rewritten compact — Current State, still-open
  decisions, the active blocker, the one Immediate Next Step — with `Handoff Chain → Archived
  predecessor` pointing at the file just archived.

That's project history, and it's never deleted — only relocated to `.archive/`, which stays in
chronological + author order and is exactly as readable as the old always-append convention was.
This is unrelated to, and complementary with, a host's own context-window compaction (e.g. Claude
Code's `PreCompact`): that manages the size of the *conversation*; this manages the size of the
*handoff file itself*.

**Why `.archive/` (dot-prefixed) instead of `Archive/`, and why a plain folder instead of a zip.**
Dot-prefixing keeps a plain `ls handoffs/` showing just `handoff.md` — the same reasoning as
`.internal/` below. It stays a plain directory rather than a zip on purpose: a zip would need
`zip`/`unzip` on `PATH` (a new required dependency this skill otherwise avoids everywhere), risks
corruption if two processes write into it around the same time (no locking exists here), and turns
every resume from an old snapshot into an extract-then-read step instead of a plain `cat`. None of
that is worth it for what's really just retention, not compression.

**`handoffs/.internal/.progress.log` (the automatic-update add-on's own mechanical log) is treated
differently, on purpose,** because it isn't project history — it's consumed input, fully absorbed
into `handoff.md` the moment a synthesis run succeeds. So on a successful run it's simply deleted,
not rotated: keeping timestamped copies of a file nobody reads was exactly the clutter this
section exists to avoid. The one exception is a *failed* run: then the log is kept — rotated to a
timestamped sibling under `.internal/`, or to `handoffs/.archive/` if it happens to have grown past
**~1MB** — as a safety net, so a real failure's evidence isn't silently thrown away along with the
routine case.

## Layout

```
handoff/                      the skill (folder name = skill name)
  SKILL.md                    lean entry (frontmatter + pipeline summary)
  ref/pipeline.md             full pipeline + cold-resume failure-mode table
  ref/templates/handoff*.md   general / coding / debugging templates
  ref/checklists/*.md         authoring gate + receiving-agent checklist
  scripts/check-staleness.sh  validate a handoff before trusting it
  scripts/log-progress.sh          optional: PostToolUse mechanical logger
  scripts/trigger-handoff-update.sh  optional: milestone-triggered auto-update
  examples/                   filled handoffs (coding + debugging)
  log/                        this skill's own development changelog
docs/index.html               GitHub Pages landing page
README.md · LICENSE           repo meta

# written by the skill in your WORKING project, not in this repo:
handoffs/handoff.md            the one stable, always-current handoff — the
                                only thing a plain `ls handoffs/` shows
handoffs/.archive/             full historical snapshots of handoff.md,
                                written only past the size threshold (never
                                deleted from — a plain folder, not a zip; see
                                "Archive / rotation" above for why) — dot-
                                prefixed like .internal/ so it stays out of a
                                plain listing too
handoffs/.internal/            optional add-on bookkeeping only, hidden from
                                normal view (.progress.log,
                                .last-auto-update.log) — see "Automatic
                                updates" and "Archive / rotation" above
```

## Credit

Pipeline and document structure inspired by [oh-my-pi](https://github.com/can1357/oh-my-pi)'s
`/handoff` (Goal / Constraints / Progress / Key Decisions / Critical Context / Next Steps), its
`createHandoffFileName` ISO timestamp naming, its mechanically-derived `<read-files>`/
`<modified-files>` tags, and its `<handoff-context>` re-injection wrapper. This skill adapts that
design to a file-writing skill: because a skill cannot fork a session, it **always writes the
markdown file** as the durable cross-agent transport.

## License

MIT — see [LICENSE](LICENSE).
