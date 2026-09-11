# Changelog — 2026-09-11

Closes the gaps found during a structural review of the `handoff` skill: an inconsistent
auto-trigger claim across three files, and a missing instruction for the *resuming* side of a
handoff (only the writing side was ever specified).

## Fixed — auto-trigger claim, now consistent everywhere

`SKILL.md` already said a file-edit count is not a reason to auto-trigger, but `README.md` and
`docs/index.html` both still said the skill "auto-triggers near ~80% context... or after 5+
edits." Three files disagreeing about what starts automatic mode is a real inconsistency, not a
wording nit — a resuming or automated agent could act on the wrong one.

- `handoff/SKILL.md` — "When to use" now states the actual signal explicitly: an `auto`/`--auto`
  invocation flag, or a harness hook (e.g. Claude Code's `PreCompact`) — never a self-imposed
  heuristic.
- `handoff/ref/pipeline.md` — step 7 reworded to match; "automatic trigger" is now defined the
  same way in both files.
- `README.md` — Install section reworded to match; dropped the "~80% context / 5+ edits" claim.
- `docs/index.html` — both the hero pill and the Install note reworded (EN + KO) to match.

## Fixed — no instruction existed for the resuming side of a handoff

Every existing instruction in this skill covered *writing* a handoff. Nothing told a fresh agent
receiving one to check staleness or read `ref/checklists/resume.md` before acting on it — the
`<handoff-context>` trailer sentence just said "continue the work seamlessly" with no preceding
check. In practice this meant `scripts/check-staleness.sh` and `resume.md` could sit unused
indefinitely: nothing in the pasted resume block ever pointed a fresh agent at them.

- `handoff/ref/pipeline.md`, step 6 — the `<handoff-context>` trailer sentence now reads: "Before
  continuing, check staleness (compare the Metadata git branch/HEAD to the live repo, or run
  `scripts/check-staleness.sh <path>`) and follow `ref/checklists/resume.md`. Then continue the
  work seamlessly." This is the reliable fix, because it travels with the pasted document itself —
  it doesn't depend on the skill being invoked at all.
- `handoff/SKILL.md` — frontmatter `description` now also covers resuming ("...and resume cleanly
  from one... or when given a handoff document or `<handoff-context>` block to resume from"), and
  "When to use" gained a third bullet, **Resuming**, pointing explicitly at `ref/checklists/resume.md`.
  This is the secondary, belt-and-suspenders fix — it only helps when the skill's own trigger
  matching fires.

Note: the `<handoff-context>` trailer sentence was previously a verbatim, unmodified copy of
oh-my-pi's wrapper text (`agent-session.ts:571`, per `changelog-2026-05-30.md`). This is now a
deliberate departure from that upstream text — recorded here rather than silently drifting from
"verbatim" without explanation.

## Added — author identity in the handoff filename

Filenames only ever encoded *when* a handoff was written, not *who* wrote it — with multiple
people (or agents acting for different people) generating handoffs in the same project, there was
no way to tell at a glance who authored which one without opening each file.

- `handoff/ref/pipeline.md`, step 1 — added `git config user.name` to the existing read-only
  "Live repo state" probes (falls back to the local part of `git config user.email`, then
  `unknown`). This is a local-only read — no new dependency, no network call, consistent with
  "gather mechanically... never trust recall." Deliberately **not** the true GitHub username: that
  would require the `gh` CLI (installed + authenticated) and a network call, which this skill's
  design has avoided everywhere else.
- `handoff/ref/pipeline.md`, step 5 — filename convention is now
  `handoff-<UTC-timestamp>-<author-slug>.md` (e.g. `handoff-2026-05-30T12-00-00-000Z-ganesh-kumar-s.md`).
  The timestamp stays *first* so `ls`/`sort` on `handoffs/` still lists files chronologically; the
  author slug is a suffix, filterable with `ls handoffs/ | grep <slug>`. Putting the author first
  was considered and rejected — it would cluster listings by person instead of by time, losing an
  existing useful property for no real gain.
- `handoff/SKILL.md`, step 5 — updated to match.
- All three templates (`ref/templates/handoff*.md`) — Metadata section gained an `Author:` line
  next to the existing `Agent / model:` line, so identity is recorded inside the document too, not
  only in the filename.
- `handoff/examples/handoff-example-coding.md` and `-debugging.md` — updated Metadata and Handoff
  Chain filenames to demonstrate the new convention (fictional slugs `jordan-lee`, `alex-chen`).

If you want the *actual* GitHub username instead of the local git identity, that's a separate,
larger change (adds a `gh` CLI + network dependency) — not done here; flag it if you want it.

## Pending — not done in this pass, needs a manual step

`ref/INDEX.md`, `ref/templates/INDEX.md`, and `ref/checklists/INDEX.md` were confirmed redundant
with `SKILL.md`'s own "Reference files" list (same file list, no unique content) and agreed for
removal, but the three docs in this Project all share the literal name `INDEX.md`, so they cannot
be safely targeted for deletion by path through the available tooling without risking deleting the
wrong one. These need to be removed manually from this Project's doc list — delete all three
`INDEX.md` entries. Nothing references them; `SKILL.md` never linked to them in the first place,
which is why they were safe to remove.

The same collision affects `README.md` (the repo-root file and `handoff/examples/README.md` share
that name here too) — see the separate note left in this pass about the root `README.md` update.

## Changed — author identity now resolves the actual GitHub pusher, not just a local name

The `git config user.name` value picked up above is just a local display name — it has no
necessary relationship to which GitHub account is actually authenticated to push. This surfaced
concretely today: this repo's `origin` was repointed from the skill's upstream
(`cskwork/agent-handoff.git`, 403 for this user) to `mahenderkodi/handoffpoc.git`, but
`git config user.name` on this machine is `Ganesh Kumar S` — so the handoff saved just before this
change (`claude/handoff-2026-09-11T01-38-27-000Z-ganesh-kumar-s.md`) carried a filename that didn't
match the GitHub account doing the pushing. The user asked explicitly for the filename to reflect
*whoever is currently pushing*, and to do so **dynamically** — so that a different developer
pushing from their own machine/session gets their own name automatically, with nothing hardcoded.

This reverses the "not done here" note left earlier today and adds the `gh` CLI as an optional
(not required) dependency:

- `handoff/ref/pipeline.md`, step 1 — the "Live repo state" probes now try
  `gh api user --jq .login 2>/dev/null` first (the GitHub login of whoever is currently
  authenticated to push — correct by construction across machines and developers, since it reads
  live credentials rather than a name typed into config once), falling back to `git config
  user.name`, then the local part of `git config user.email`, then `unknown`.
- `handoff/ref/pipeline.md`, step 5 — author-slug derivation and the worked example updated to
  match (`gh api user --jq .login -> "mahenderkodi" -> mahenderkodi`); rationale paragraph added
  explaining why resolving from the authenticated pusher (not a fixed local display name) is what
  makes the convention dynamic across developers sharing a repo.
- Cold-resume failure-mode table — "Unclear who authored a handoff" row updated to mention the
  `gh`-first resolution order.
- `handoff/SKILL.md` — steps 1 and 5 updated to match.
- All three templates (`ref/templates/handoff*.md`) and both examples — `Author:` line parenthetical
  updated from `(from \`git config user.name\`/\`user.email\`)` to `(from \`gh api user\`, falling
  back to \`git config user.name\`/\`user.email\`)`.
- Degrades gracefully: if `gh` isn't installed or isn't authenticated, the probe fails silently
  (`2>/dev/null`) and the existing `git config` fallback chain applies unchanged — this is still a
  zero-*required*-dependency skill, `gh` only improves accuracy when present.

## Added — optional automatic-update tooling (mechanical log + milestone synthesis)

Every handoff so far in this repo has been written by explicitly asking for one. The user asked
whether the file could instead stay current as work happens, without remembering to ask each time.

The core skill's manual/explicit-trigger design (`SKILL.md`'s "When to use") is deliberate and
unchanged by this — regenerating a full handoff on every single edit would be noisy and expensive,
and nothing here makes that the default. Instead, two new **optional** scripts add automation on
top, off by default, requiring their own opt-in setup per project:

- `handoff/scripts/log-progress.sh` — a Claude Code `PostToolUse` hook. Appends one line per
  file edit or command run to `handoffs/.progress.log` — raw mechanical facts only (timestamp,
  tool name, file path/command), no AI call, no interpretation. Requires `jq`; no-ops silently if
  missing rather than failing the tool call it's attached to.
- `handoff/scripts/trigger-handoff-update.sh` — fired by a git `post-commit` hook (a commit is a
  real milestone). Reads the progress log and invokes `claude -p` headlessly to merge it into the
  latest handoff per the existing chaining rule (step 5), then rotates the log so facts aren't
  double-counted. Requires the `claude` CLI on `PATH`; no-ops silently if missing.

**Important trade-off, documented at the top of `trigger-handoff-update.sh` and in README.md's new
"Automatic updates" section:** the headless run uses `--dangerously-skip-permissions`, because an
unattended background process has no one present to approve individual tool calls. This is a real,
blanket permission grant — the script does not sandbox it to only the handoff skill's own
reads/writes. Anyone enabling the git hook should read that note first; `--allowedTools` scoped to
`handoffs/*.md`, or skipping the git hook and relying on manual/`PreCompact` triggering only, are
both suggested as lower-risk alternatives in the same note.

- `README.md` — new "Automatic updates (optional)" section: the two-tier design, setup JSON/shell
  snippets, and the permissions trade-off, placed between Install and Layout.
- `handoff/SKILL.md` — Reference files list gained a pointer to both scripts, explicitly marked
  optional/not-core.

**Not verified end-to-end.** Both scripts were written against Claude Code's documented hooks and
headless (`-p`) behavior, but this environment has no way to actually run Claude Code or fire a git
hook to confirm they work as written — no shell execution is available on the user's device this
session (see the `device_bash` gotcha in the handoff docs), and this cloud sandbox doesn't have
Claude Code installed. Before relying on this unattended, run
`handoff/scripts/trigger-handoff-update.sh` manually once (after putting a line in
`handoffs/.progress.log`) and check `handoffs/.last-auto-update.log`.
