# The handoff pipeline (full detail)

This is the long form of the summary in `SKILL.md`. It mirrors oh-my-pi's handoff-generation
pipeline (`docs/handoff-generation-pipeline.md`, `compaction/prompts/handoff-document.md`) adapted
to a file-writing skill.

Mental model: **gather → structure → write → verify → resume**. You are writing for *another
instance of yourself* that has **no access to this conversation**. If a fact is not in the document,
it does not exist for the next agent.

---

## 0. Preflight guard

Capture the known goal and next action even in an early session. If neither exists, reply
`Nothing to hand off yet`. Never fabricate progress or use message/file counts to reject a useful handoff.

## 1. Gather state mechanically, not from memory

Recall is unreliable for exactly the things a resume depends on: paths, symbol names, counts, error
text. Derive them from evidence instead.

- **Read-set / modified-set.** From your own tool history, list every file you opened (read-set) and
  every file you wrote or edited (modified-set). A file that was only read does not belong in the
  modified-set. oh-my-pi emits these as `<read-files>` / `<modified-files>` tags derived from tool
  calls, not from the model's recollection — do the same.
- **Commands + results.** Capture commands run and their outcomes, especially the last test/build/
  lint run. Quote failures and error messages **verbatim**.
- **Live repo state.** Run read-only probes and record the output:
  ```bash
  git rev-parse --abbrev-ref HEAD       # branch
  git rev-parse --short HEAD            # HEAD commit
  git status --porcelain                # clean vs dirty, untracked
  gh api user --jq .login 2>/dev/null   # GitHub login of whoever is authenticated to push
  git config user.name                  # fallback: local git identity
  ```
  Author identity resolves to whoever is actually pushing, not a name typed into local config once
  and forgotten: prefer the GitHub login from `gh api user` (requires the `gh` CLI installed and
  authenticated) — this is dynamic by construction, since it reads the credentials active on
  whichever machine/session runs the skill, so a different developer pushing from their own login
  gets their own name automatically. If `gh` is missing or not authenticated, fall back to `git
  config user.name`, then the local part of `git config user.email`, then `unknown`. Never ask the
  user or guess an identity — this is a mechanical probe like the others.

## 2. Preserve the open thread

Find any unanswered question, request, or imperative still awaiting the user or caller — e.g.
"please run X and paste the output", an unresolved design choice, a pending approval. Capture it
**verbatim**. This is the single most resumption-critical item and the most common cold-resume
failure: the new agent silently drops the thread and does the wrong thing. It goes into **Open
Questions**, and into **Current State** as the blocker if it gates the next step.

## 3. Structure into the fixed template

Fill `ref/templates/handoff.md` section by section, in the fixed order, so a resuming agent always
finds each part in the same place. Section discipline:

- **Current State first.** One paragraph of what is true *right now* + the active blocker. This kills
  the "where was I" re-establishment cost.
- **Done = DO NOT REDO, quantified, with evidence.** "30/90 tests passing", "4 of 6 endpoints done",
  each line with a concrete path:line or command output. Duplicated work is the most expensive
  failure mode; label finished work loudly.
- **One Immediate Next Step.** Not a list — the single exact first action ("run `npm test
  src/auth.test.ts`; it fails at line 42 on a null token — fix `getToken()` in `src/auth.ts:18`").
- **Decisions WITH rationale.** The *why*, not just the *what*, so the next agent does not relitigate
  or silently reverse a settled choice.
- **Concrete over abstract.** Real file:line references, real symbol/type names, exact commands, real
  error text. "Capture exact technical state, not abstractions."

## 4. Self-verify before writing

Run `ref/checklists/quality.md`. Reject and fix the draft if it:
- contains secrets / tokens / keys;
- still has unfilled placeholders (`{{...}}`, `[TODO]`);
- uses vague abstractions instead of concrete paths/commands;
- lacks copy-pasteable verification commands;
- omits a preserved open question that actually exists.

## 5. Write the file

Save to the single **stable path** `handoffs/handoff.md` **in the working project** (not in this
skill's repo) — update it in place. Do not create a new timestamped file on every write; the old
per-write `handoff-<UTC-timestamp>-<author>.md` naming is now reserved for **archived snapshots
only** (see "Archive on size threshold" below). Create the `handoffs/` directory if missing.

**Merge, don't overwrite.** If `handoffs/handoff.md` already exists, this write is an update to it:
keep all still-relevant prior info, promote In Progress → Done, refresh the Immediate Next Step, and
drop or update stale Open Questions (oh-my-pi's `compaction-update-summary` pattern) — the same
discipline as before, just applied to one persistent file instead of a fresh one each time.

**Archive on size threshold — never delete project history, only relocate.** After drafting the
merged content, check its size before writing it:
- `handoffs/handoff.md` over **~30KB** → too large to stay skimmable as a single current-state
  document.

If the handoff draft is over threshold: first copy the FULL merged draft, unabridged, to
`handoffs/.archive/handoff-<UTC-timestamp>-<author>.md` (create `handoffs/.archive/` if missing),
using the same timestamp+author-slug convention described below. Then write a **compact** version
to `handoffs/handoff.md` containing only: Metadata, Current State, still-open Key Decisions, the
active blocker, the single Immediate Next Step, and any still-unanswered Open Question — plus a
`Handoff Chain` → `Archived predecessor:` line pointing at the file you just archived. Everything
else that no longer fits is not lost, only relocated: the full detail is still readable in
`handoffs/.archive/`, in chronological + author order via `ls`/`sort`, exactly as the old per-write
convention was.

This whole mechanism is about **document size**, a completely different problem from **context-
window size**: it keeps `handoffs/handoff.md` itself small enough to read in one pass, regardless
of how a host's own context-compaction (e.g. Claude Code's `PreCompact`) behaves. Use both — they
solve different things.

**`handoffs/.internal/` is a separate concern: internal working data, not project history.** If
you're using the optional automatic-update add-on (`scripts/log-progress.sh` +
`scripts/trigger-handoff-update.sh`), its mechanical progress log lives at
`handoffs/.internal/.progress.log` — deliberately out of `handoffs/` itself, so that folder only
ever shows what a developer actually wants to see (`handoff.md`; `.archive/` stays out of a plain
`ls` too, dot-prefixed like `.internal/`, though unlike `.internal/` it holds real project history
and is never deleted from). Unlike the "never
delete, only relocate" rule above for handoff content, `.progress.log` is deliberately deleted
once a synthesis run successfully merges it into `handoff.md` — it's consumed input with no
standalone value once consumed, not a record worth keeping. `trigger-handoff-update.sh` handles
this itself (delete on a successful run; rotate to a timestamped sibling under `.internal/`, or to
`handoffs/.archive/` if it happens to have grown past ~1MB, only on a *failed* run, so failure
evidence isn't silently lost). This skill's own manual pipeline never touches `.internal/`
directly — it only ever reads `handoffs/.internal/.progress.log` as step 1 evidence when invoked
headlessly by that script.

**Author slug.** Take the identity resolved in step 1 — `gh api user --jq .login` when the `gh`
CLI is installed and authenticated (the GitHub account actually pushing right now), else `git
config user.name`, else the local part of `git config user.email`, else `unknown` — lowercase it,
replace every run of characters outside `[a-z0-9]` with a single `-`, and trim leading/trailing
`-`. Do not truncate; keep the whole slug so two people with similar names stay distinguishable.
Resolving from the authenticated pusher rather than a fixed local display name is what makes this
dynamic across developers: run the skill under a different person's `gh` login (or, absent `gh`,
their own `git config user.name`) and their identity appears automatically — nothing here is
hardcoded to one person or one machine. This slug convention now applies only to files written
under `handoffs/.archive/` (the live file has no timestamp in its name):

```
gh api user --jq .login  ->  "mahenderkodi"  ->  mahenderkodi
handoffs/.archive/handoff-2026-05-30T12-00-00-000Z-mahenderkodi.md
```

Timestamp stays first so `ls`/`sort` on `handoffs/.archive/` still lists snapshots in
chronological order; the author slug is a suffix, filterable with e.g. `ls handoffs/.archive/ |
grep mahenderkodi`.

## 6. Emit the resume primitive + report

Print:
1. the absolute path to `handoffs/handoff.md` (and, if an archive-and-compact just happened, the
   archived snapshot's path too), and
2. a one-line summary of the state, then
3. the ready-to-paste resume block (oh-my-pi's `createHandoffContext` wrapper, adapted):

```
<handoff-context>
…the full handoff document…
</handoff-context>

The above is a handoff document from a previous session. Before continuing, check staleness
(compare the Metadata git branch/HEAD to the live repo, or run `scripts/check-staleness.sh
<path>`) and follow `ref/checklists/resume.md`. Then continue the work seamlessly.
```

Do **not** continue the task or answer the conversation's questions. Output only the artifact and
where it lives.

## 7. Auto-continue note (automatic trigger only)

"Automatic trigger" means the calling host/harness explicitly signalled a context handoff — an
`auto` invocation flag, or a harness hook like Claude Code's `PreCompact` — never a self-imposed
heuristic such as an edit count (see `SKILL.md`'s "When to use"). On a genuine automatic trigger,
append a short note instructing the resuming agent to **honor the user's latest intent over older
recorded plans**, and to say so briefly if nothing remains rather than inventing busywork. On a
manual trigger, omit this — the user is present and steering.

---

## Cold-resume failure modes (what this pipeline defends against)

| Failure | Cause | Defense in this pipeline |
|---|---|---|
| Redoes finished work | "Done" not labeled / not quantified | Done section, DO NOT REDO, with evidence (step 3) |
| Starts in the wrong place | Vague or multi-item next steps | One Immediate Next Step (step 3) |
| Drops the user's pending request | Open thread not captured | Preserve open thread verbatim (step 2) |
| Reverses a settled decision | Rationale missing | Key Decisions with the *why* (step 3) |
| Edits the wrong files | Blast radius unknown | Mechanical read/modified sets (step 1) |
| Trusts a stale doc | No environment anchor | Metadata git branch/HEAD + `check-staleness.sh` |
| Hallucinated paths/counts | Written from memory | Gather mechanically, never from recall (step 1) |
| Leaks credentials | Pasted raw command output | Quality gate rejects secrets (step 4) |
| Unclear who authored a handoff | No identity captured | Author from the authenticated GitHub login (`gh`), falling back to `git config`, in filename + Metadata (steps 1, 5) |
| Handoff document grows unbounded, hard to skim | Every write kept appending/creating full files forever | Stable `handoffs/handoff.md` + size-triggered archive-and-compact, never a delete (step 5) |
| Internal bookkeeping files clutter the developer's view | Mechanical log + auto-update log written directly into `handoffs/` alongside the file a developer actually reads | `handoffs/.internal/` holds `.progress.log` / `.last-auto-update.log` out of sight; consumed progress logs are deleted on success rather than piling up (step 5) |
