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
oh-my-pi's wrapper text (`agent-session.ts:571`, per `log/changelog-2026-05-30.md`). This is now a
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

`handoff/ref/INDEX.md`, `handoff/ref/templates/INDEX.md`, and `handoff/ref/checklists/INDEX.md`
were confirmed redundant with `SKILL.md`'s own "Reference files" list (same file list, no unique
content) and agreed for removal, but could not be deleted through the available tooling in this
pass. Delete these three files manually:

```
handoff/ref/INDEX.md
handoff/ref/templates/INDEX.md
handoff/ref/checklists/INDEX.md
```

Nothing else references them — `SKILL.md` never linked to them in the first place, which is why
they were safe to remove.
