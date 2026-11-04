# Session rules (read first, every session)

This repository is the PvKnee RuneLite plugin. Project notes live only in `docs/`, which is never committed.

## Reading order

1. This file.
2. `docs/DESIGN.md`: what the plugin is and how it works (pages, blocks, endpoint, phases).
3. The current phase file, e.g. `docs/PHASE0.md`: the task list to work through.

## Repository rules

- `CLAUDE.md` and `AGENTS.md` come from RuneLite's template. Follow them, but never edit them or add project notes to them.
- The `docs/` and `.claude/` folders are excluded from Git and must never be committed or referenced in committed files.
- Do not commit, pull or push. The owner reviews every step and handles Git.
- Never add `Co-Authored-By`, "Generated with" or any other attribution to commits or code. The only exception is the
  `Generated-by: Claude Code` footer the owner adds to the plugin-hub pull request description (see `AGENTS.md`).
- Keep the code small, readable and cohesive: no leftover template code, no unused classes, no comments about how the code was produced.
- No API keys, webhook URLs or passwords anywhere in the code.

## Reuse first

- Build every block from the `ui` kit and the shared helpers (theme colours, labels, icons, time formatting, image
  loader, WOM client). Blocks should mostly arrange kit pieces, not style things themselves.
- If a look or pattern is needed in two places, add it to the kit or a helper instead of copying it.
- Prefer extending a kit piece (a new option or variant) over one-off styling inside a block.
- Before writing something new, check whether a helper already does it.

## How to work

- Do one task at a time from the current phase file, in order.
- After each task, run the plugin in the dev client (`gradlew.bat run`) and check it works.
- Then stop, summarise what changed in a few lines, and suggest a commit message.
- Anything done beyond the task as written (agreed with the owner along the way) goes in the phase file's "Changes
  made beyond the task list" section, so the docs always match the code.
- Phase files may point at mock-ups in `docs/mockups/`. Only the files directly in that folder are current; never
  match anything in `docs/mockups/old/`.

## Commit messages

One line: `type: very brief summary`, where type is one of:

| Type | Use for |
| --- | --- |
| `feat` | New behaviour (a block, the panel, a setting) |
| `fix` | Something broken now works |
| `chore` | Housekeeping: renames, build files, config |
| `debug` | Temporary logging or diagnostics |
| `security` | Anything that protects users or data |

Example: `feat: add page dropdown`
