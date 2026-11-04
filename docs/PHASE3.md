# PvKnee plugin: Phase 3 tasks (in-game features)

Read `docs/SESSION.md`, then `docs/DESIGN.md` (including the UI rules). This file is the task list for Phase 3 only.

**Status: built (1 Nov 2026).** Tasks 0 to 12 are built and confirmed in the dev client. The `table` block also needs
the endpoint to send `rows` (Apps Script updated and deployed 1 Nov, not yet verified: before the first real
`table` row, check the endpoint with `?nocache=1` shows `"rows"` for it).

Decisions: alerts may cause one quiet endpoint load at login (the README says so); `!sotw` / `!botw` are typed in
normal chat like RuneLite's own `!kc` (others see the command, only the reply is private).

## Goal (the Phase 3 gate)

The plugin does things a sheet cannot: it knows who you are in game, shows your own place in the challenges, hops to
the clan world, greets you on login, answers `!sotw` / `!botw`, alerts you about challenges, can update Wise Old Man
when you log out (opt-in), and shows rank-gated pages. Every new piece follows the UI rules and reuses the kit.

## Ground rules for this phase

- **Client thread.** Anything that reads the game client (`Client`, players, clan, world) runs on the client thread
  (`ClientThread`), and the result is handed to Swing with `SwingUtilities.invokeLater`. Never touch Swing from the
  client thread or the client from Swing.
- **No automation.** Every game action comes from one deliberate click or command by the member. Nothing is typed,
  sent or clicked for them.
- **Third-party opt-in.** Anything that sends player data (for example the logged-in name) to a third-party server is
  a setting that is off by default and carries the warning text from `AGENTS.md`.
- **Personal settings** live in `PvKneeConfig` (not in the Debug section); clan-wide wording lives in the sheet.
- **See what it is doing.** Every in-game feature reports its steps through one small helper (e.g. `DevLog`): always
  `log.debug`, and, only when RuneLite runs in developer mode (`@Named("developerMode")`, as `gradlew.bat run` does),
  also a console chat message prefixed `[PvKnee]`, e.g. `[PvKnee] hop: world 509 requested`, `[PvKnee] player:
  Iron Aeria, 2nd of 14`. Members never see these. Each task's summary says which messages to look for.
- **Sheet and endpoint changes** are made by the owner. Where a task needs one, it is listed under "Needs first".

## Tasks

0. **Top bar refactor and dev log.** Move the top bar (home button, page picker, refresh button and its spinner) out
   of `PvKneePanel` into its own kit class, with no visible change. `PvKneePanel` keeps page building, notices and
   timers. Add the `DevLog` helper from the ground rules and use it for endpoint loads and refreshes, so it can be
   seen working before the in-game features arrive.
1. **Player info and the `player` block.** A small shared service (e.g. `PlayerInfo`) that tracks, on the client
   thread, whether you are logged in, your display name, current world and clan rank, updating on the relevant
   events (game state, clan channel). It notifies the panel on change so the current page redraws. Blocks read it
   through `BlockContext`. Then the `player` block (the sheet already has `player | competition=current_skill` and
   `current_boss` rows above the leaderboards):
   - On the board: a card in the `HeroBars`/old `Sotw.dc.html` style: "You", your medal badge or rank number, name,
     value on the right, and a muted line such as "5,555 behind 1st" (or "Leading" when 1st).
   - Logged in, not on the board: a quiet line "You're not on the board yet".
   - Logged out: draw nothing.
   - Your row in the leaderboard table gets a subtle highlight.
   - Names match case-insensitively, treating spaces, underscores and hyphens as the same (Wise Old Man and the
     client differ). Uses the standings already loaded; no extra request.
2. **Hop from the world pill.** The `header` block's pill gets an optional `hop` setting (e.g.
   `hop={Settings!B4}`). Clicking the pill hops to that world the way RuneLite's World Hopper plugin does (look the
   world up through RuneLite's `WorldService`, build the client world, hop on the client thread; on the login screen
   change the login world instead). Tooltip "Hop to world 509". On that world already: not clickable, tooltip "You're
   on world 509". The game's own hop rules apply; the plugin does not retry or work around them.
3. **Rank-gated pages.** A page's `rank` header row (e.g. `Administrator`, or a rank number) hides the page unless
   your clan rank is at least that rank, read from the client's clan settings (rank titles as the clan names them).
   Logged out or not in the clan: hidden. Replaces task 9 of Phase 1's "hide pages whose rank is set". The page
   picker and page links update when your rank becomes known.
4. **Login message.** Once per login, a chat message (a game message, only you see it) built from the endpoint's
   `messages.login` template. Personal setting `Login message` (on by default). No template: no message.
   - Done by the owner: the `data_messages` tab (column A key, column B wording) and the endpoint's `messages`
     object, e.g. `"messages": {"login": "...", "cmd_sotw": "...", "cmd_botw": "..."}`.
   - **Placeholders** (replace each wherever it appears; an unknown `{...}` is left as typed):
     | Placeholder | Filled with | Example |
     | --- | --- | --- |
     | `{skill}` | Current Skill of the Week name | `Woodcutting` |
     | `{boss}` | Current Boss of the Week name | `Cerberus` |
     | `{timeLeft}` | Time left of the current challenges, same format as the Challenges aside | `5d 20h` |
     | `{place}` | Your place in that challenge, or `not on the board yet` | `2nd` |
     | `{you}` | Your place and score, or `no xp yet` / `no kills yet` (task 12) | `2nd, 1,942 xp` |
     | `{challenges}` | The challenges an alert is about, joined with `, ` (task 12) | `Woodcutting, Cerberus` |
     | `{gain}` | Your gain as `1,942 xp` for skills or `12 kills` for bosses (no `+`, see task 10), or `no gains yet` | `1,942 xp` |
     `{place}` and `{gain}` are for the commands (task 5); in the login message they may be left unfilled. The current wording is listed in task 12.
5. **`!sotw` and `!botw`.** When you type `!sotw` or `!botw` in chat, reply with a local game message: the challenge,
   time left and your place and gain (from the standings, fetched if not cached). Use RuneLite's chat command
   support the way its own chat commands do. The reply is only shown to you; nothing extra is sent to the game.
   Wording from the endpoint's `messages.cmd_sotw` / `messages.cmd_botw`, using the placeholders listed in task 4,
   with the current templates as built-in defaults when a key is missing.
6. **Challenge alerts.** RuneLite notifications (through `Notifier`) when a challenge starts, has 1 hour left, and
   ends. Personal settings to turn each on or off (on by default). Works while the panel is closed: use the last
   loaded data (or the saved copy) and one quiet endpoint load when you log in. Never notify twice for the same
   moment; nothing for times already past when the client starts.
7. **Update Wise Old Man on logout (opt-in).** Personal setting `Update Wise Old Man on logout`, off by default, with
   the third-party warning from `AGENTS.md`. When on, after you log out, request a Wise Old Man update for the name
   you were logged in as (`POST https://api.wiseoldman.net/v2/players/{name}`), at most once per 10 minutes per
   name. Failures are logged at debug only. When the setting is off and the standings are empty, the empty state
   may add "Turn on 'Update Wise Old Man on logout' so your gains count."
8. **New blocks: `table`, `stat`, `countdown`.**
   - `table`: a sheet range as a table in a card (`range`, optional `header`, `columns`, `widths`), using the
     `RankTable`/list look. Needs first: the endpoint returns the range's values for the block.
   - `stat`: one big number with a label (`label`, `value`, which may use sheet placeholders).
   - `countdown`: a label and a live countdown to a date (`label`, `until`), using `Times` and `LiveLabel`, with the
     ending-soon colour in the last 24 hours.
9. **README and services.** Update `README.md`: the new features and settings, and the services table (the logout
   update sends your username to Wise Old Man, only when turned on; one quiet endpoint load at login for alerts).

10. **"You" card value.** Show the member's own value as `{number} xp` for a skill (e.g. `1,942 xp`) and
    `{number} kills` for a boss (e.g. `12 kills`), with no `+` prefix: it is the member's own score, not a gain in a
    list. The same format fills `{gain}` in the `!sotw` / `!botw` replies (task 5), replacing `+1,942`. The `+` is
    only used in the standings table, for skill gains (its `prefix` setting).
11. **No duplicate empty message.** When the standings are empty, the `player` block draws nothing; the leaderboard's
    empty state ("No kills yet ...") is the only message. When others are on the board and the member is not, the
    "You're not on the board yet" line still shows.

12. **Shorter chat messages, highlighted values, alerts from the sheet.**
    - **Highlight colour.** Build every chat line with RuneLite's `ChatMessageBuilder`: template text in
      `ChatColorType.NORMAL`, each placeholder's value in `ChatColorType.HIGHLIGHT`, so RuneLite's own chat colour
      settings (and the opaque or transparent chatbox) decide the colours. No fixed or theme colours in chat.
    - **New placeholder `{you}`:** your place and score, e.g. `2nd, 1,942 xp` or `3rd, 12 kills` (task 10's format),
      or `no xp yet` / `no kills yet` when you have no gains. Uses the full standings, so a place below the table's
      `rows` cut-off still shows (e.g. `23rd, 3 kills`). `{place}` and `{gain}` stay available.
    - **Alerts from the sheet.** The start, 1 hour left and end messages come from `messages.alert_start`,
      `messages.alert_hour` and `messages.alert_end`, with a `{challenges}` placeholder: the names joined with `, `
      (e.g. `Woodcutting, Cerberus`), each name highlighted.
    - **Current wording** (also the built-in defaults when a key is missing):
      | Key | Wording |
      | --- | --- |
      | `login` | `PvKnee \| Skill: {skill} \| Boss: {boss} \| {timeLeft} left` |
      | `cmd_sotw` | `{skill} \| {timeLeft} left \| You: {you}` |
      | `cmd_botw` | `{boss} \| {timeLeft} left \| You: {you}` |
      | `alert_start` | `PvKnee \| Started: {challenges}` |
      | `alert_hour` | `PvKnee \| 1 hour left: {challenges}` |
      | `alert_end` | `PvKnee \| Ended: {challenges}` |
    - Add `{you}` and `{challenges}` to the placeholder table in task 4.

## Later (not Phase 3)

- Theme override in personal settings; theme rotation.
- The sheet time placeholder (`{time:Fri 17:00}`), see `docs/PHASE2.md`.
- Hall of Fame page.
- After Phase 4: style the chat messages (login message, `!sotw` / `!botw` replies) so they look good, e.g. colours
  and layout, instead of the plain game-message text.

## Out of scope for Phase 3

Bingo (its own phase, Phase 4, to be designed first), Plugin Hub submission (`docs/SUBMISSION.md`).

## Changes made beyond the task list

(Add anything done beyond a task here, as `docs/SESSION.md` asks.)

- Task 6: challenge alerts are game chat messages (only you see them), not RuneLite notifications through
  `Notifier`, which felt like too much for the plugin. Moments while you are logged out are skipped.
- Tasks 4 and 6: the login message and the alerts are one feature, "Challenge messages": one settings section with
  four on/off switches (`Login message`, `Challenge start`, `1 hour left`, `Challenge end`, all on by default) and one
  class (`chat/ChallengeMessages`). The login endpoint load happens even with the login message off, so the alerts
  have fresh times. A single switch for all four may come later.
- Task 7: no hint in the empty standings about turning on 'Update Wise Old Man on logout'; it was unnecessary,
  since the sheet's script updates everyone now and then anyway.
- Task 12: every chat message starts with an empty `|` prefix, because the game hides everything up to the first `|`
  of a game message (colour tags included); the templates then show exactly as written.
- Task 12: values are highlighted with RuneLite's Chat Color "game message highlight" setting itself (opaque or
  transparent to match the chatbox when the message is sent; default red), not `ChatColorType.HIGHLIGHT`. RuneLite
  swaps its default highlight for the game's own game message colour when one is set, so values looked like the
  wording.
