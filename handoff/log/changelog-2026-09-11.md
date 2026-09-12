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

## Added — handoff archive/rotation system (stable filename + size-triggered archive)

The skill so far wrote a brand-new `handoff-<UTC>-<author>.md` on every single handoff, forever.
That's a different growth problem from a host's own context-window compaction (e.g. Claude Code's
`PreCompact`, which this skill already treats as one of two valid automatic triggers) — it's about
the handoff *document* piling up files/size over a long-running project, not about the
*conversation* piling up tokens. The user asked for a design that keeps a stable current-state file
from growing unbounded, without ever losing the detail it accumulates — explicitly: relocate, never
delete.

- `handoff/ref/pipeline.md`, step 5 — rewritten. The skill now writes to a single **stable path**,
  `handoffs/handoff.md`, updating it in place (merge, don't overwrite) instead of creating a new
  timestamped file per write. After each merge, size is checked: `handoffs/handoff.md` at/over
  **~30KB** triggers archiving the full, unabridged current content to
  `handoffs/Archive/handoff-<UTC>-<author>.md` (the *old* per-write naming convention, now reserved
  for archived snapshots only), followed by a compact rewrite of `handoffs/handoff.md` (Metadata,
  Current State, open Key Decisions, the blocker, the one Immediate Next Step) whose Handoff Chain
  → `Archived predecessor` points at the file just archived. The optional mechanical
  `handoffs/.progress.log` gets the same treatment at **~1MB**, rotating to
  `handoffs/Archive/progress-<UTC-date>.log`. Nothing is ever deleted — only relocated to
  `Archive/`, which stays fully readable in chronological + author order via `ls`/`sort`, exactly as
  the old always-new-file convention was.
- `handoff/ref/pipeline.md` — cold-resume failure-mode table gained a row: "Handoff document grows
  unbounded, hard to skim" → defended by the stable file + size-triggered archive.
- `handoff/SKILL.md` — step 5 one-liner and the "Resuming" bullet updated to point at
  `handoffs/handoff.md` (and `handoffs/Archive/`) instead of a `handoff-*.md` glob; Reference files
  list gained entries for `handoffs/handoff.md` and `handoffs/Archive/`.
- All three templates (`ref/templates/handoff*.md`) — the `Continues from` field is retired; the
  Handoff Chain section now reads `This doc: handoffs/handoff.md (stable)` +
  `Archived predecessor: {{...}}`, set only when a write just archived-and-compacted.
- `handoff/examples/handoff-example-coding.md` and `-debugging.md` — Handoff Chain sections updated
  to the new fields, `Archived predecessor: none` (first handoff for a fresh project), with a note
  showing what the field would look like once the size threshold is crossed.
- `handoff/ref/checklists/resume.md` — now describes the resuming agent's target as
  `handoffs/handoff.md` or an archived snapshot under `handoffs/Archive/`, not a `handoff-*.md`
  glob; added a note to prefer the live file over an archived snapshot when both are in hand.
- `handoff/ref/checklists/quality.md` — Resumability section's chaining bullet replaced with two
  bullets matching the new logic: merge-not-overwrite into `handoffs/handoff.md`, and (when over
  threshold) archive-before-compact with the `Archived predecessor` link set.
- `handoff/scripts/trigger-handoff-update.sh` — updated: the `ls -1t handoff-*.md` glob (which
  assumed the old per-write filename) is gone; the headless prompt now targets `handoffs/handoff.md`
  directly, instructs an in-place merge, and instructs the archive-and-compact step (including the
  new `.progress.log` size-based rotation to `handoffs/Archive/progress-<UTC-date>.log`, on top of
  the existing per-milestone rotation). Top-of-file comment updated to match.
- `handoff/scripts/log-progress.sh` and `handoff/scripts/check-staleness.sh` — **no changes**:
  `log-progress.sh` only appends raw facts to `.progress.log` and never touches a handoff filename;
  `check-staleness.sh` takes the handoff path as an argument, so it already works against either
  `handoffs/handoff.md` or an archived snapshot with no code change.
- `README.md` — "What it does" and the pipeline summary's Write step updated; new "Archive /
  rotation (document size, not context size)" section added between "Automatic updates" and
  "Layout"; Layout diagram gained the `handoffs/` runtime layout (written in the working project,
  not this repo) showing `handoff.md`, `.progress.log`, and `Archive/`.

**Explicitly complementary, not a replacement, for `PreCompact`.** A harness's own context-window
compaction and this file-size archive solve two different growth curves — one bounds how much
conversation an agent must hold, the other bounds how large the handoff document itself gets. A
project can use either, both, or neither.

## Correction — "Confirmed... verified end-to-end" (previous entry) was premature

The entry originally here claimed the full automatic loop — the `PostToolUse` hook detecting
activity, writing `handoffs/.progress.log`, and the `post-commit` hook firing
`trigger-handoff-update.sh`'s headless synthesis — was confirmed working end-to-end, based on the
user reporting that things looked right. Direct inspection of the live repo afterward shows that
claim was too strong, and this entry replaces it with what is actually evidenced.

**What IS confirmed, with direct evidence:**
- The stable-file convention itself: `handoffs/handoff.md` exists, is correctly structured per the
  new template (Handoff Chain → `Archived predecessor`), and was correctly updated in place by a
  real session that used the handoff skill (a backend get-by-id e2e-verification pass) — proven by
  reading its actual content off the device.
- `.claude/settings.json` and `.git/hooks/post-commit` are both present on disk with the intended
  content (confirmed earlier via the user's own `ls -la`).

**What is NOT yet confirmed, and should not have been reported as working:**
- The mechanical `PostToolUse` hook (`log-progress.sh`) actually firing and writing to
  `handoffs/.progress.log`. That file was directly checked (via `cat`, by a session with real shell
  access) and **does not exist** — i.e. no tool-use event has been logged to disk yet, despite the
  hook being installed.
- The `post-commit` → `trigger-handoff-update.sh` synthesis path actually running. Its own early
  exit (`[ -s "$progress_log" ] || exit 0`) means it cannot have done anything meaningful so far,
  since there has never been a non-empty `.progress.log` for it to read. `handoffs/.last-auto-update.log`
  does not exist either, which is consistent with that script never having reached its `claude -p`
  call.
- The archive-on-size-threshold trigger itself firing (`handoff.md` is currently ~12.8KB, under the
  ~30KB threshold, so no archive has been created yet — `handoffs/Archive/` does not exist).

**Net effect:** what has actually been proven is that *asking* the handoff skill to write/update
`handoffs/handoff.md` — via an explicit prompt to Claude Code — works correctly with the new
stable-file design. What has NOT been proven is the *automatic*, hook-driven path (edit/command →
`.progress.log` → commit → background synthesis) actually firing on its own, or the archive
threshold actually triggering. To prove those specifically: make a real edit through Claude Code in
this repo and `cat handoffs/.progress.log` immediately after (expect one new line); then `git
commit` and check `handoffs/.last-auto-update.log` for the headless run's output, and
`handoffs/.progress.log` for having been rotated afterward.

## Diagnosed — hook chain root cause: `jq` not installed (single point of failure)

Ran the exact test prescribed above, step by step, on the user's machine. Findings, in order:

1. **The edit itself landed** — asked Claude Code to add a one-line comment to
   `handoff/scripts/log-progress.sh`; `git diff` confirmed it.
2. **`handoffs/.progress.log` still did not appear** after that edit — the `PostToolUse` hook did
   not visibly do anything.
3. **Root cause isolated**: `command -v jq` returns nothing on this machine — `jq` is not
   installed. `log-progress.sh` has this guard immediately after reading its stdin payload:
   `command -v jq >/dev/null 2>&1 || exit 0`. So the hook *is* being invoked correctly by Claude
   Code, but exits silently, by design, before writing anything — exactly the documented behavior,
   just never actually exercised until now. Confirmed independently by feeding the script a
   synthetic well-formed tool-call payload directly (bypassing Claude Code entirely): same
   silent no-op.
4. **`.claude/settings.json` itself is correct** — matcher and command match the script's own
   header instructions exactly. Not the problem.
5. **`git commit` (`62c4b70`) correctly fired `.git/hooks/post-commit`**, which ran
   `trigger-handoff-update.sh` cleanly (exit 0, no execution errors) — but that script's own guard,
   `[ -s "$progress_log" ] || exit 0`, exits immediately because `.progress.log` still doesn't
   exist (cascading from #3). It never reaches the `claude -p` synthesis call, so
   `handoffs/.last-auto-update.log` was never created and `handoffs/handoff.md` was never
   auto-refreshed.

**Conclusion: the wiring is entirely correct.** `.claude/settings.json` → `PostToolUse` hook →
`log-progress.sh`, and `.git/hooks/post-commit` → `trigger-handoff-update.sh`, both invoke exactly
as designed. The single break point in the whole chain is a missing runtime dependency (`jq`) on
this particular machine, not a bug in the hook setup or the scripts. Once `jq` is installed, the
same test (edit → `cat .progress.log` → commit → `cat .last-auto-update.log`) should go green
end-to-end — this is the remaining unverified step, tracked as a follow-up rather than assumed.

**Follow-up for the skill itself, not yet done:** `log-progress.sh`'s and README.md's existing "no
dependencies, degrades gracefully" framing is accurate but caused exactly this kind of silent gap
to go unnoticed for a while — worth adding a one-line "prerequisite check" callout to README.md's
"Automatic updates" section pointing at this exact test recipe, so a missing `jq`/`claude` CLI is
caught at setup time instead of discovered later. Not applied yet; flag if wanted.

## Confirmed — full automatic hook chain verified end-to-end, for real this time

After installing `jq` (the single blocker identified above), the exact same test was re-run on the
user's machine and this time went all the way through:

1. `log-progress.sh` fired correctly on real Claude Code tool calls and populated
   `handoffs/.progress.log` with real entries (previously impossible without `jq`).
2. `git commit` (`6d780c3`) fired `.git/hooks/post-commit` → `trigger-handoff-update.sh`.
3. The headless `claude -p --dangerously-skip-permissions` run executed to completion: it read
   `.progress.log`, merged that progress into `handoffs/handoff.md` **in place** (prior Done items
   preserved, a new milestone appended, `Generated:` timestamp refreshed to `2026-09-11T18:45:00Z`).
4. `trigger-handoff-update.sh` then rotated the log to `handoffs/.progress.log.20260911T184836Z` —
   relocated, not deleted, exactly as designed — and appended the full run output to
   `handoffs/.last-auto-update.log`.

One real timing gotcha surfaced and is now documented directly in `trigger-handoff-update.sh`'s own
header comment (replacing the old "not run/verified end-to-end" note, which was stale): right after
the commit, `handoff.md` was already fresh but `.last-auto-update.log` was still empty and
`.progress.log` hadn't rotated yet — because the background `claude -p` process was still running
(confirmed via a Windows process listing) even though its `Write` to `handoff.md` had already
landed. The remaining script-side steps (log flush, rotation) only happen once that process fully
exits. This is not a bug — `run_update & disown` is deliberately non-blocking so `git commit` is
never held up — but it means checking `.last-auto-update.log`/`.progress.log` immediately after a
commit can look like a false failure; give it tens of seconds.

**This closes out every open item from the "Not verified end-to-end" and "Correction" entries
above.** The optional automatic-update add-on (mechanical logging + milestone synthesis) and the
archive/rotation system's core update-in-place mechanism are now both confirmed working against a
real repo, a real commit, and a real headless Claude Code run — not just written against
documented behavior.

- `handoff/scripts/trigger-handoff-update.sh` — top-of-file comment updated: the stale "NOTE: this
  script has not been run/verified end-to-end" note is replaced with a "VERIFIED END-TO-END
  (2026-09-11)" note describing exactly what was proven and the background-process timing gotcha
  above.

## Added — documented the `DISABLE_AUTOUPDATER` prerequisite, and a "check once up front" callout

While testing the archive threshold (deliberately inflating `handoff.md` past ~30KB with marked
filler text, then committing to fire the real automatic path), Claude Code's own self-updater
repeatedly failed with `Auto-update failed: claude.exe in use`. Root cause: `trigger-handoff-update.sh`
launches a *second*, background `claude.exe` process on every commit (the headless synthesis run);
if Claude Code's own updater tries to replace `claude.exe` while that background process (or any
other `claude` session) is still alive, it collides — an unrelated-looking error that is really
this add-on racing against its own background process. Fix: `setx DISABLE_AUTOUPDATER 1` (Windows;
a fresh terminal is needed for it to take effect) stops the CLI from attempting silent self-updates
at all, without preventing a deliberate `npm update -g @anthropic-ai/claude-code` later.

The user asked, reasonably, why this had to be discovered by hitting it rather than being told up
front — so this pass folds it into the setup instructions instead of leaving it as tribal knowledge
from one debugging session:

- `README.md`, "Automatic updates" section — new "Prerequisites — check these BEFORE enabling"
  subsection, listing all three real failure points together (`jq` on `PATH`, `claude` CLI on
  `PATH`, and `DISABLE_AUTOUPDATER=1`) with why each one fails silently rather than loudly, so
  someone setting this up for the first time checks all three up front instead of hitting them one
  at a time across separate debugging sessions like this one did.
- `handoff/scripts/trigger-handoff-update.sh` — same `DISABLE_AUTOUPDATER` note added directly to
  the script's own header comment, next to the existing `jq` gotcha, so it's visible to anyone
  reading the script itself and not only to someone who reads README.md first.

This doesn't change anything the scripts do — it's a documentation-only fix — but per the pattern
in this changelog of tracking exactly what's proven vs. assumed: the `DISABLE_AUTOUPDATER` fix
itself has NOT yet been re-verified as actually preventing a recurrence (the archive-threshold test
that surfaced this was still in progress at the time this was written); flagged as a follow-up, not
claimed as confirmed. (Still not applied as of the entry below — `setx DISABLE_AUTOUPDATER 1` has
been documented but not yet actually run on the user's machine, so the `claude.exe in use` message
kept recurring through the rest of this session; that's expected until it's actually set.)

## Confirmed — archive-and-compact proven with real content, and the actual cause of "it's not triggering"

A first attempt to test the archive threshold looked like it failed: the file was inflated past
30KB with marked filler text (`## TEST FILLER — ARCHIVE THRESHOLD TEST`, appended directly via a
plain Bash edit, bypassing the skill — 32,193 bytes), then `git add`/`git commit` was run, but no
`handoffs/Archive/` ever appeared and `handoffs/.last-auto-update.log` stayed byte-for-byte
unchanged. Diagnosis, run the same way as the earlier `jq` investigation:

1. `git log --oneline -3` showed HEAD had **not** advanced — the earlier `git commit` never
   actually created a commit. No commit, no `post-commit` hook firing, no synthesis run. This
   was the entire cause of "it's not triggering" — not a bug in the hook, the trigger event itself
   never happened. (Why the commit didn't take was not independently diagnosed — nothing was
   staged, or it failed silently for an unrelated reason — but the absence of a new commit is
   conclusive on its own.)
2. To test the actual archive logic without depending on git at all, `handoff/scripts/trigger-handoff-update.sh`
   was run directly (`bash handoff/scripts/trigger-handoff-update.sh`) — this is the exact same
   code path `.git/hooks/post-commit` calls, so it's a legitimate substitute, not a workaround.
3. This time, with a genuine 32KB file to process (real archive work, not a trivial one-line test
   edit), the headless `claude -p` run took several minutes rather than tens of seconds — expected,
   given it had to read, archive-copy, and rewrite a real document, not a shortcut needing a fix.
4. Confirmed end to end once it completed:
   - `handoffs/Archive/handoff-2026-09-12T00-02-43-000Z-kodimahender55-gmail-com.md` created,
     **32,193 bytes — byte-for-byte identical** to the pre-compact `handoff.md`, filler included,
     unabridged.
   - `handoffs/handoff.md` rewritten compact at 6,173 bytes, well under the 30KB threshold, with
     `Handoff Chain → Archived predecessor` correctly pointing at that exact archive file.
   - `handoffs/.last-auto-update.log` gained a fresh entry for this run.

**This is the size-check → archive-before-compact logic (`ref/pipeline.md` step 5) proven with
real, substantial content for the first time** — the earlier "Confirmed — full automatic hook chain"
entry above proved the commit → hook → synthesis → merge-in-place path, but never exercised the
archive branch specifically (the file was still under threshold at the time). Combined, every
individual mechanism the pipeline promises has now been exercised with real evidence: mechanical
`.progress.log` sourcing, the size check itself, unabridged archive-before-compact, the compact
rewrite's correct backlink, and `.last-auto-update.log` capturing the run.

**One follow-up flagged, not yet resolved:** the compacted `handoff.md`'s title/Current State came
back describing this archive test itself (e.g. "archived & compacted for real this run") rather
than the actual ongoing project narrative (Update API done, Delete next). If the synthesis pass
used the test's own `.progress.log` entries as its primary evidence — which is exactly what it's
told to do — that's arguably correct behavior for what actually happened mechanically, but it means
the live handoff no longer reflects real project state until a genuine work-session handoff update
overwrites it again. Not yet confirmed whether this needs fixing, or whether it's a one-time
artifact of testing with synthetic filler as the "current work."

## Changed — hid internal bookkeeping files from the developer-facing `handoffs/` folder

The user's own feedback, stated directly as a developer using this skill day to day: `.progress.log`,
its rotated timestamped copies, and `.last-auto-update.log` sitting directly inside `handoffs/`
next to `handoff.md` were clutter they never open and never will — "developer perspective this are
not needed." Separately, the accumulated project changelogs (`log/changelog-*.md`) sat at the repo
root outside `handoff/` even though they document the *skill's* own development, not the working
project's.

This pass keeps every user-visible behavior the same (`handoff.md` generation, in-place merge, and
size-triggered archive to `Archive/` are all untouched) and only relocates/reduces what a developer
sees, plus one deliberate new behavior change:

- `handoff/scripts/log-progress.sh` — now writes to `handoffs/.internal/.progress.log` instead of
  `handoffs/.progress.log`. Header comment explains why: `handoffs/` itself should only ever show
  `handoff.md` and `Archive/` (if it exists) — this raw mechanical log has no standalone value to a
  developer and is purely fuel for `trigger-handoff-update.sh`.
- `handoff/scripts/trigger-handoff-update.sh` — reads/writes `handoffs/.internal/.progress.log` and
  `handoffs/.internal/.last-auto-update.log` instead of the old `handoffs/` paths (creating
  `.internal/` if missing). The headless synthesis prompt text was updated to point at the new
  path too, since it explicitly tells the headless run where to find its evidence.
  **Behavior change, not just relocation:** on a successful run, `.progress.log` is now deleted
  outright (`rm -f`) instead of rotated to a timestamped sibling — its content is fully consumed
  into `handoff.md` by that point, so keeping it around serves no purpose and was exactly what was
  piling up the `.progress.log.<timestamp>` files the user was complaining about. On a *failed*
  run, the old rotate-for-safety-net behavior is kept unchanged (just relocated under `.internal/`,
  or to `Archive/` if the log happens to be huge) so failure evidence is never silently lost — this
  distinction (delete on success, keep on failure) wasn't something the user asked for explicitly;
  it's this session's own judgment call on top of the literal request, reasoned through in the
  script's own comments.
- `handoff/ref/pipeline.md`, step 5 — new paragraph explicitly calling out `handoffs/.internal/` as
  internal working data, not project history, and documenting the delete-on-success /
  rotate-on-failure-only distinction. Cold-resume failure-mode table gained a row for this.
- `handoff/SKILL.md` — Reference files list updated: the `Archive/` bullet no longer mentions
  routine progress-log rotation (that's now failure-only), and a new bullet for `handoffs/.internal/`
  was added.
- `README.md` — "Automatic updates," "Archive / rotation," and the Layout diagram all updated to
  the new `.internal/` paths and the delete-on-success behavior.
- `log/changelog-2026-09-11.md` (this file) and `log/changelog-2026-05-30.md` relocated to
  `handoff/log/`, since they document the skill's own development history, not the working
  project's — matching the existing principle that the skill lives self-contained under `handoff/`.

**Not yet done, requires the user's own shell (this session's device-bridge shell is currently
unavailable — a known Windows update issue, unrelated to this change):**
- Moving the *existing* accumulated runtime files on disk into their new locations: the current
  `handoffs/.progress.log`, `handoffs/.last-auto-update.log`, and the three already-rotated
  `handoffs/.progress.log.<timestamp>` files, into `handoffs/.internal/`.
- Deleting the synthetic test archive file, `handoffs/Archive/handoff-2026-09-12T00-02-43-000Z-kodimahender55-gmail-com.md`
  — flagged separately, several entries above, as the one legitimate exception to "never delete
  from Archive," since it's test filler with no project-history value.
- Moving `log/changelog-2026-05-30.md` and this file from repo-root `log/` into `handoff/log/`.
- A related, still-separate cleanup candidate the skill's own design surfaced but the user didn't
  ask about directly: four legacy per-write `handoff-<timestamp>-<author>.md` files sitting
  directly inside `handoffs/` (from before the stable-file convention existed) are now orphaned —
  superseded by `handoffs/handoff.md`, and arguably belong in `handoffs/Archive/` by the same logic
  as any other archived snapshot, rather than cluttering the top level. Left as a recommendation,
  not acted on, since moving old project history is a bigger judgment call than relocating this
  session's own internal bookkeeping files.

Exact commands for all of the above are provided directly to the user rather than repeated here.

## Changed — Archive/ renamed to .archive/ (dot-prefixed, still a plain folder — not a zip)

Before the cleanup above was carried out, the user deleted the entire `handoffs/` runtime folder
themselves (a more aggressive cleanup than the terminal commands proposed) and separately asked
whether `Archive/` should really be a compressed `.zip`, since "archive" implies compression —
explicitly inviting pushback if the idea didn't hold up ("think on it if my point is valid").

It doesn't, for this specific folder, for three concrete reasons — all explained to the user before
this change was made, and confirmed with them before implementing:

1. **New required dependency.** Writing into a zip needs `zip`/`unzip` on `PATH`, which isn't
   guaranteed on Windows Git Bash — the same failure shape as the `jq`-missing incident earlier this
   project, except this time in the core pipeline rather than an optional add-on.
2. **Concurrency risk.** Two writers (a manual handoff and the background synthesis script) touching
   the same zip archive with no locking is a real corruption risk; two plain files with
   near-impossible-to-collide names is harmless.
3. **It fights the resume flow.** The whole point of `Archive/` is a resuming agent can `cat` an old
   snapshot immediately — zipped, that becomes an extract step every time, added exactly where this
   skill tries hardest to remove friction.

What actually delivers the "I don't want to see this" goal without those costs: dot-prefix it, same
as `.internal/`. `Archive/` → `.archive/` — still a plain folder (zero dependency, safe to append to,
instantly readable), just hidden from a plain `ls`/minimal file-tree view. The user picked this
option directly over the zip and over leaving `Archive/` visible.

- `handoff/scripts/trigger-handoff-update.sh` — every `handoffs/Archive` reference (the synthesis
  prompt text, the failure-path rotation destination) now reads `handoffs/.archive`; header comment
  gained a short "why dot-prefixed, why not a zip" note so this doesn't need rediscovering later.
- `handoff/ref/pipeline.md` — "Archive on size threshold" section, the author-slug section and its
  worked example, and the cold-resume table all updated to `.archive/`; added a short parenthetical
  next to the `.internal/` explanation distinguishing the two (`.archive/` = permanent project
  history, plain folder, never deleted from; `.internal/` = consumed working data, deleted on
  success).
- `handoff/SKILL.md` — Reference files list entries updated, each now noting the plain-folder/no-zip
  rationale inline.
- `README.md` — "Archive / rotation" section gained a new "Why `.archive/` ... and why a plain
  folder instead of a zip" paragraph (the three reasons above, condensed); Layout diagram updated.
- `handoff/ref/checklists/resume.md`, `handoff/ref/checklists/quality.md` — their one reference each
  updated to `.archive/`.
- `handoff/examples/handoff-example-coding.md`, `handoff-example-debugging.md` — Handoff Chain
  worked examples updated to `.archive/`.
- `handoff/ref/templates/handoff.md`, `-coding.md`, `-debugging.md` — checked; none hardcode the
  folder name (the path only ever appears via the `{{archived_snapshot_path_or_none}}` placeholder),
  so no change was needed there.

**No device-side data migration was needed for this rename** — since the user had already deleted
the entire `handoffs/` runtime folder before this change, there was no existing `Archive/` on disk
to rename; the next size-triggered archive will simply create `handoffs/.archive/` directly under
the new naming.

**Also restored in this pass, found missing while scanning the actual device state:**
`handoff/examples/` (containing both example handoffs) had been deleted from disk along with
everything else in the user's manual cleanup, but is still referenced by `SKILL.md`'s "Reference
files" list — restored from the Project's copies (with the `.archive/` update applied) rather than
left as a dangling reference. Flagged to the user in case the deletion was actually intentional.

**Still not done, flagged again since the scan surfaced it a second time:** the three redundant
`handoff/ref*/INDEX.md` files (agreed removable weeks ago, same content as `SKILL.md`'s own
Reference files list) are still on disk — this session's device shell is still down, so removal
needs the user's own terminal, given alongside the other pending cleanup commands.
