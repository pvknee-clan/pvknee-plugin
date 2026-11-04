# PvKnee plugin: checks before submitting to the Plugin Hub

Read `docs/SESSION.md` first. These are not part of any phase: do them only when the owner decides to submit.
Phase 1 is done and checked in the dev client.

- [x] Phase 5 (review fixes, `docs/PHASE5.md`) done and signed off by the owner.
- [x] Licence: `LICENSE` (BSD 2-Clause) added in Phase 5 task 1.
- [x] Images: only `https://raw.githubusercontent.com`, at most 256 KB and 512 px a side (Phase 5 task 7).
- [x] Debug section: the endpoint override is only shown and used in developer mode (Phase 5 task 12); checked in a
  normal client built with `gradlew shadowJar` and started with `java -ea -jar build\libs\pvknee-unspecified-all.jar`.
- [x] `DevLog` chat messages only appear in developer mode: none showed in that normal client.
- Decide whether `README.md` should name the clan's sheet ID and endpoint (`DESIGN.md` calls them clan assets to
  document; the README currently leaves them out because it is public).
- Owner: the clan Settings keys (Wise Old Man verification code, Discord webhook, API key) are in the Apps Script's
  Script Properties (`moveSecrets()`), not in the sheet; V2, V4 and V5 stay empty.
- Optional checks not yet seen in the client: a long event or competition name wrapping inside its card, and
  `competition` with `which=upcoming` (no page uses it yet). The 10-minute background refresh is confirmed from the
  client log (loads 10 minutes apart with no clicks).
- [x] In the pull request's plugin file, add a third-party warning line alongside `repository=` and `commit=` (the
  full file is under "The plugin-hub pull request" below):
  `warning=This plugin submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers`
  (the wording `AGENTS.md` uses; reviewers may adjust it).
  The plugin contacts Google (the clan sheet's endpoint) as its core feature, not an opt-in setting, and reviewers
  add this line for such plugins anyway. Wise Old Man is contacted only by the opt-in logout update.
- Owner note, not a code task: the endpoint address in `ClanDataClient` is the Apps Script deployment. Never make a
  "New deployment"; always use Manage deployments, edit, Version: New version, or every installed copy breaks
  until a plugin update is approved.
- In the pull request description, mention that the bingo page shows sample data from a test sheet so reviewers can
  see it working.
- [x] Project files match RuneLite's example-plugin template (`build.gradle`, `runelite-plugin.properties`,
  `settings.gradle`, `.gitignore`): checked against the template; the only differences are the renames and `bin/`.
- Before the pull request, once no more cloud sessions need the notes: take `docs/` (and `.claude/` if present) out
  of Git again, so reviewers only see the plugin. Add these two lines back to the end of `.gitignore`:
  ```
  docs/
  .claude/
  ```
  then run `git rm -r --cached docs`, commit (`chore: stop tracking project notes`) and push. The files stay on
  your computer; only Git stops tracking them. Use that commit's hash in `plugins/pvknee`.
- Last: open the pull request to `runelite/plugin-hub` with the `Generated-by: Claude Code` footer from `AGENTS.md`
  (keep it: the template asks for it, and reviewers read the code either way).

## The plugin-hub pull request

In a fork of `runelite/plugin-hub`, add one file, `plugins/pvknee`, with exactly these lines. Replace the commit with
the full hash of the commit you tested last (`git rev-parse HEAD`); it was
`0b3fe93363b31590e0b6965f737359e562334fec` when this was written.

```
repository=https://github.com/pvknee-clan/pvknee-plugin.git
commit=0b3fe93363b31590e0b6965f737359e562334fec
warning=This plugin submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers
```

Pull request title: `Add PvKnee`. Description (the `Generated-by` line goes last, on its own line, at the very end
of the description):

```
PvKnee is a sidebar panel for the PvKnee clan. Pages, events, challenges and standings come from the clan's Google
Sheet through one Apps Script endpoint, so members need no API key or setup.

- Network: the clan's Apps Script endpoint (Google) as the core feature, boss icons from raw.githubusercontent.com,
  and Wise Old Man only for the opt-in "Update Wise Old Man on logout" (off by default, with the 3rd-party warning).
  No player data is sent except the username in that opt-in update.
- The bingo page currently shows sample data from a test sheet, so it can be seen working.
- Team dots in chat and above heads mark clan members only, and are hidden in the Wilderness, on PvP worlds and in
  PvP areas.

Generated-by: Claude Code
```

## After approval on the Plugin Hub

- Before telling members: point the main sheet's `Settings!B8` (Active bingo sheet) at a real bingo sheet, or clear it.
  It is left on the sample bingo for review, so reviewers can see the bingo page working.
- Tell members to install PvKnee and uninstall Clan Events.
- Delete the old Clan Events tabs from the sheet (`home`, `hub`, `sotw`, `botw`, the `hof_*` tabs and their `*_layout`
  and `*_data` helpers), after checking nothing the plugin uses reads from them.
