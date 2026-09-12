---
name: handoff
description: Save task state in a structured Markdown handoff for a fresh-context agent, and resume cleanly from one. Use when asked to hand off, save state, prepare a session for continuation, or when given a handoff document or <handoff-context> block to resume from.
---

# handoff

Write the document your next agent opens before it works. A handoff compacts everything that
matters about a long-running task into one structured markdown file, so an agent with an empty
context window resumes exactly where you left off — without re-reading the conversation.

## When to use

**Manual** — the user asked: a handoff / state save / "next plan of action". Write the file.
**Automatic** — only when the calling host/harness explicitly signals a context handoff (e.g. an `auto`/`--auto` invocation flag, or a harness hook such as Claude Code's `PreCompact`). Write the file and append the auto-continue note (step 7). Never self-trigger from a heuristic — a file-edit count, a message count, or "this feels like a lot of work" is NOT authorization to interrupt in-progress work.
**Resuming** — if you are given a `<handoff-context>` block or asked to continue from `handoffs/handoff.md` (or an archived snapshot under `handoffs/.archive/`), first follow [ref/checklists/resume.md](ref/checklists/resume.md) (staleness check, honor DO NOT REDO, honor settled decisions) before touching any code.

## Pipeline (summary)

**gather → structure → write → verify → resume**. Full detail in
[ref/pipeline.md](ref/pipeline.md).

0. **Preflight.** Capture the known goal and next action even if little work has happened. If neither exists, say "Nothing to hand off yet"; never fabricate progress.
1. **Gather mechanically.** Derive read/modified sets from tool history; capture commands, test output, and error text verbatim; confirm git branch/HEAD/dirty and author (GitHub login via `gh api user`, falling back to git config) via read-only probes. Never trust recall.
2. **Preserve the open thread.** Capture any unanswered user question VERBATIM (the most-failed item) → Open Questions, and Current State if it blocks the next step.
3. **Structure.** Fill `ref/templates/handoff.md` — `-coding.md` for a feature build, `-debugging.md` for an investigation — in order: Current State first; Done labeled "DO NOT REDO" + quantified; ONE next step; decisions with rationale; concrete file:line / commands / real errors.
4. **Self-verify.** Run `ref/checklists/quality.md`; fix any secret, placeholder, vagueness, missing verify command, or dropped open question before writing.
5. **Write.** Update `handoffs/handoff.md` in place (create `handoffs/` if missing) — merge, don't overwrite: promote In Progress → Done, refresh the Immediate Next Step, drop stale Open Questions. If the merged draft would put the file at/over ~30KB, first archive the full current content, unabridged, to `handoffs/.archive/handoff-<UTC>-<author>.md` (slug from the authenticated GitHub login via `gh api user`, falling back to `git config user.name`/`user.email`), then write a compact `handoffs/handoff.md` whose Handoff Chain → `Archived predecessor` points at it. Never delete detail — only relocate it to `.archive/`.
6. **Emit + report.** Print the path + a one-line summary, then the doc wrapped in `<handoff-context>…</handoff-context>` (exact wrapper in [ref/pipeline.md](ref/pipeline.md)) as the paste-ready resume block.
7. **(Auto trigger only)** Add the auto-continue note: honor the user's LATEST intent; say so if nothing remains.

## Reference files

- [ref/pipeline.md](ref/pipeline.md) — the full step-by-step pipeline and cold-resume failure modes
- [ref/templates/handoff.md](ref/templates/handoff.md) — the document template (general)
- [ref/templates/handoff-coding.md](ref/templates/handoff-coding.md) — feature-build variant
- [ref/templates/handoff-debugging.md](ref/templates/handoff-debugging.md) — bug/debug variant
- [ref/checklists/quality.md](ref/checklists/quality.md) — pre-write self-verification gate
- [ref/checklists/resume.md](ref/checklists/resume.md) — what the RESUMING agent does first
- [examples/](examples/) — filled handoffs you can imitate
- [scripts/check-staleness.sh](scripts/check-staleness.sh) — validate a handoff before trusting it
- [scripts/log-progress.sh](scripts/log-progress.sh) / [scripts/trigger-handoff-update.sh](scripts/trigger-handoff-update.sh) — optional add-on for automatic updates (mechanical logging + milestone-triggered synthesis); not part of the core pipeline, see README.md's "Automatic updates" section before enabling
- `handoffs/handoff.md` — the one stable, always-current handoff file (not a skill file itself; created by the skill in your working project)
- `handoffs/.archive/` — full historical snapshots (`handoff-<UTC>-<author>.md`), written only when `handoffs/handoff.md` crosses its size threshold — see `ref/pipeline.md` step 5; also the failure-only fallback destination for an oversized `.internal/.progress.log`. Dot-prefixed like `.internal/` so a plain `ls handoffs/` shows just `handoff.md` — but it's a real directory, not a zip: never deleted from, always a plain `cat` away, no extra dependency.
- `handoffs/.internal/` — optional add-on bookkeeping only (`.progress.log`, `.last-auto-update.log`), deliberately hidden from normal view; not something you read or write directly as part of the core pipeline — see `ref/pipeline.md` step 5's "internal working data" note. Unlike `.archive/`, this one IS deleted from on a successful run.

## Hard rules

- Capture exact technical state, not abstractions — paths, symbols, commands, real errors.
- Never include secrets, tokens, or keys.
- Producing the artifact is the whole job: reply with it and where it lives, and route the conversation's own questions into Open Questions rather than answering them.
- The file is the source of truth; the `<handoff-context>` block is its transport.
