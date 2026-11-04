# PvKnee plugin: Phase 5 tasks (review fixes)

Read `docs/SESSION.md`, then `docs/DESIGN.md`. This file is the task list for Phase 5 only.

**Status:** not started. The owner's Apps Script changes (task 0) are written and tested; deploy them first.

## Goal (the Phase 5 gate)

The plugin is ready for Plugin Hub review: it follows RuneLite's own patterns, contacts outside servers as little as
possible, cannot be pointed at other sites or made to render HTML by sheet text, survives bad data, and cleans up
fully when turned off. An independent code review (against RuneLite's docs, comparable Plugin Hub plugins and
published guidance) found the issues below; each was checked against the code before being listed here.

Network, as checked by the review: the plugin never sends a Wise Old Man group or competition update (the Apps
Script does that, about once a day and after a changeover), and normal use is well inside Wise Old Man's limits.
This phase removes the remaining direct Wise Old Man reads, so the only Wise Old Man call left is the opt-in
logout update.

## Endpoint changes (made by the owner's Apps Script, task 0)

- **Standings are in the endpoint.** Each entry of `competitions.current` now has:
  ```json
  "standings": [ { "name": "gimonnaise", "gained": 76521 }, { "name": "VVonky Camel", "gained": 45693 } ],
  "standingsAt": "2026-11-04T00:05:00Z"
  ```
  Everyone in the competition, in Wise Old Man's order (at most 500), as the sheet last copied them (every 5
  minutes). Empty for a few minutes after a changeover, until the new competition's first copy. `upcoming` entries
  have no standings.
- **Hidden pages are no longer sent.** A page whose `show` is `FALSE` is left out of `pages` entirely (for example
  `tab_bingo` while no bingo sheet is set). Rank-gated pages are still sent; they are a convenience, not access
  control, so nothing private may go on them.
- **`?nocache` and `?bingo=preview` need a key.** Without `&key=<PREVIEW_KEY>` (a Script Property, owner only) both
  are ignored and the cached response is served.
- **Errors are generic.** On failure the endpoint sends `{ "version": 1, "error": "The clan sheet could not be read.
  Try again later." }`; the details go to the script's log.
- **Bingo counts are whole numbers.** Tile numbers, `needed`, amounts and item IDs are rounded before sending.
  Points (team and member) may still have decimals.
- Page references (`{Sheet!A1}`, `cell=`, `range=`) can only read page tabs, data tabs and the public Settings cells;
  a table's `range=` returns at most 200 rows by 10 columns.

## Tasks

### 0. Owner: deploy the Apps Script (not a code task)

1. Paste the new `Code.gs`, save, run `moveSecrets()` once from the editor and approve. It moves the Wise Old Man
   verification code (V2), the Discord webhook (V4) and the API key (V5) into Script Properties, clears those cells,
   and logs the preview key. Keep the key private.
2. Manage deployments, edit, Version: New version (never "New deployment").
3. Check: the endpoint still loads in the dev client; `?bingo=preview` without the key shows the normal bingo;
   `competitions.current[0].standings` is filled.

### 1. Repository: licence and build output

- Add `LICENSE` with the BSD 2-Clause text, copyright line `Copyright (c) 2026, JackieHann` unless the owner gives
  another name. (The Plugin Hub requires a licence; this is the one blocker the review found.)
- Add `bin/` to `.gitignore`. The owner then runs `git rm -r --cached bin` (two editor build files are committed).
- Suggested commit: `chore: add licence and ignore editor build output`

### 2. Remove the world hop

`client.hopToWorld` is a restricted API: Plugin Hub maintainers say it makes a plugin "ineligible for automated
review, now and for any future updates" (comments on plugin-hub PRs #19066 and #16521). The sheet could also name any
world, including PvP or Deadman worlds.

- Delete `WorldHopper` and the header block's `hop` setting (and its click handling). The header pill (e.g.
  "World 509") stays, as information only, with no hand cursor or tooltip about hopping.
- README: drop the hop. The owner removes `hop={Settings!B4}` from `tab_home` row 5.
- Suggested commit: `feat: remove one-click world hop`

### 3. Standings from the endpoint, not Wise Old Man

- `Competition` gets `standings` (list of name and gained) and `standingsAt`.
- `LeaderboardBlock`, `PlayerBlock` and `!sotw` / `!botw` read them from the loaded data. Remove the Wise Old Man
  reading code (`WomClient.standings`, its cache and in-flight map, and the blocks' one-minute live update); the
  panel's normal refresh (10 minutes, or the Refresh button) brings new standings. Keep `WomClient` only for the
  logout update (task 4); if that leaves it tiny, fold it into `LogoutUpdate`.
- **Ties share a place** (as bingo does): equal `gained` gives the same place and the same medal, and the next place
  skips (1st, 1st, 3rd). The "You" card's "Level with 1st" and its place pill must agree.
- Empty standings (just after a changeover): the leaderboard's and the "You" card's existing empty states.
- If a block shows when the standings were refreshed, use `standingsAt`.
- README: the services table no longer lists Wise Old Man for standings; the plugin only contacts it for the
  opt-in logout update.
- Suggested commit: `feat: read challenge standings from the clan sheet`

### 4. Logout update: only when there is something to update

Keep it opt-in and off by default. Send `POST /players/{name}` on logout only when all of these hold:

- **Progress this session:** total XP changed by more than 10,000 since login (RuneLite's XP Updater uses the same
  threshold), **or** a kill or completion count message was seen this session. These are game messages such as
  "Your Vorkath kill count is: 123." and "Your completed Chambers of Xeric count is: 45." (see how RuneLite's
  `ChatCommandsPlugin` matches them); any one counts.
- **Not already sent by XP Updater:** skip if RuneLite's XP Updater plugin is on with its Wise Old Man option ticked
  (read its config with `ConfigManager`, as the Player Indicators check does), so nobody sends two.
- **A normal world:** skip on seasonal, Deadman, tournament, beta, fresh-start and no-save worlds (`WorldType`).
- At most one per 10 minutes per name, remembered across restarts (in the plugin's config, not a file).
- Send an identifying User-Agent, e.g. `PvKnee-RuneLite-Plugin/<version>`, as Wise Old Man asks.
- Setting description and README: say when it sends, and that XP Updater is an alternative.
- Suggested commit: `feat: only send the logout update after progress`

### 5. Network hygiene

- **Refresh button:** at most one load per 60 seconds (the endpoint's cache time). A click inside that time shows
  the current data and says when it was loaded.
- **Back-off:** after a failed endpoint load, wait 1, then 5, then 15 minutes before the next automatic try (reset on
  success). A failed `!sotw` / `!botw` must not reload each time.
- **Login load:** skip it when every feature that needs it is off (login message, challenge alerts, team icons,
  team overlay); load on first use instead (panel open, chat command).
- **Hang fix:** in `ClanDataClient`, when the first load fails and there is no saved copy, `loadSaved` returns
  without calling back, so `waiting` is never cleared and every later login message and chat command is queued
  forever. Always call `finishRecent` (with null on failure) and handle null in the callers.
- Suggested commit: `fix: limit refreshes and recover from failed loads`

### 6. Safe display of sheet text

A Swing label or tooltip whose text starts with `<html>` renders HTML, including `<img src=http://...>` which loads
from any site outside OkHttp. `Labels` escapes, but these use raw text:
`PagePicker` (page titles, tooltip), `DataTable` (cells, tooltips), `Pill`, `LinkButton` (text, URL tooltip),
`LinksBlock` (URL tooltip), `Labels` (long-text tooltip), `RankTable` (names, `prefix`), `BingoTeamBlock`,
`BingoTileDetail` (contributors, notes, part names), `TileGrid` (tile tooltip).

- Make the kit safe by default: every kit label sets `putClientProperty("html.disable", Boolean.TRUE)` or escapes
  its text, and tooltips with outside text are escaped. One helper, used everywhere; no raw `new JLabel(text)` with
  sheet or Wise Old Man text.
- `TeamOverlay`: the team name is drawn as plain text (no `<col>` tags take effect) and clamped to a sensible width.
- Suggested commit: `security: never render sheet text as HTML`

### 7. Images: allowed host and size limits

(Replaces the image item in `docs/SUBMISSION.md`.)

- Only load `https://raw.githubusercontent.com/...`, matching the host exactly. Use the endpoint's `iconBase` only if
  it starts with the allowed prefix; otherwise use the built-in default. Other addresses: skip, log at debug, leave
  the empty space.
- Cap the download at 256 KB, and read the image's size before decoding (`ImageIO` reader); refuse anything over
  512 px a side.
- Remember failed addresses for the session, so a page rebuild (e.g. after a world hop) does not fetch them again.
- README: state the rule.
- Suggested commit: `security: limit where and how big images can be`

### 8. Survive bad data

- **One bad field must not break everything:** a parse error in `bingo` (or any one section) leaves that section
  null and the rest of the endpoint working, instead of rejecting the whole response.
- **One bad block must not break the page:** wrap each block's render in try/catch (log at debug, skip the block).
- **Loading flag:** in `PvKneePanel.show`, clear `loading` before anything that can throw, so a bad response cannot
  stop all later refreshes. Treat null `pages` as empty.
- **Links to missing pages** (e.g. Home's bingo link while `tab_bingo` is hidden and therefore not sent): do nothing
  and show no hand cursor.
- **Links:** only `http` / `https` URLs are shown; skip others when building the page (today `LinkBrowser` throws on
  the Swing thread).
- **Limits:** `Labels.clamped` measures at most the first 2,000 characters; `DayBar` falls back to one plain bar if a
  competition is longer than 60 days.
- Suggested commit: `fix: keep the panel working with bad data`

### 9. Time fixes

- **Date-only events** (`hasTime: false`, sent as UTC midnight): show the date as the sheet has it (format in UTC) and
  treat the event as running for that whole calendar day in the player's own time. Today, west of the UK "Sat 7 Nov"
  shows as "Fri 6 Nov" and appears under Happening Now from Friday evening.
- **`!botw` time left** uses the boss competition's end (today `Messages` takes the skill's end when both exist).
- Suggested commit: `fix: date-only events and boss time left`

### 10. Team dots: the RuneLite way, clan members only, no PvP

- **Chat (public and clan):** build the dot the way RuneLite's `ChatChannelPlugin` adds icons: in the
  `chatMessageBuilding` `ScriptCallbackEvent`, put the dot image tag in front of the name on the string stack. The
  clan rank icon stays. Remove the widget hiding and moving after script 84, the 4-children assumption and the
  `MessageNode` name changes; nothing then needs undoing on shutdown beyond a chat refresh.
- **Clan tab member list:** if it can be done without hiding or moving the game's widgets (e.g. through the list's
  own build script, `ScriptID.CLAN_SIDEPANEL_DRAW` rather than the hard-coded 4399), add the dot in front of the
  name; otherwise drop it from the member list. No hard-coded script IDs.
- The owner then judges in the dev client whether rank icon plus dot is too busy. The current version (dot in place
  of the rank icon) stays in Git history as the fallback; record the outcome in this file.
- **Clan members only:** only mark names that are in the clan's member list (`client.getClanSettings()`), so a
  bingo sheet cannot be used to tag outsiders (Jagex's rules forbid PvP opponent identification).
- **No PvP:** overhead dots are also hidden when `VarbitID.PVP_AREA_CLIENT` is 1 (PvP areas outside the Wilderness,
  where Player Indicators' PvP mode shows names). Chat dots follow the same rule as overhead dots (Wilderness, PvP
  worlds, PvP areas).
- Suggested commit: `feat: add team dots the way RuneLite core adds chat icons`

### 11. Per-tick work and cleanup on shutdown

- `TeamHeadOverlay`: work out whether Player Indicators is on, and the hidden reason, on `PluginChanged`,
  `ConfigChanged` and data loads, not every game tick. Build debug strings (e.g. the sorted name list) only in
  developer mode.
- `TeamRoster`: update when data loads instead of polling.
- `shutDown`: clear the image cache, team dots, roster and the loaded data, so nothing stays in memory after the
  plugin is turned off. Bound the image cache (e.g. 200 entries).
- Suggested commit: `perf: less work per tick and full cleanup on shutdown`

### 12. Debug setting and tidy-up

- **Endpoint override:** only shown and used in developer mode (`@Named("developerMode")`); in a normal client the
  built-in address is always used. Remove the Debug section's "remove before publishing" comment, since the section
  is now dev-only. (Replaces the Debug item in `docs/SUBMISSION.md`.) The owner previews a draft bingo in the dev
  client with the override set to the endpoint plus `?bingo=preview&key=<preview key>`.
- **Dead code:** remove unused fields (`ClanData.clan`, `generatedAt`, `ClanEvent.fullName`, `Page.icon`) and anything
  left unused after tasks 2 to 4.
- **Duplication:** one label factory in the kit instead of the copies in `StandingCard`, `RankTable`, `RankBadge`,
  `TimeBar`, `BingoTileDetail` and `BingoTeamBlock`; one shared standings helper for `LeaderboardBlock` and
  `PlayerBlock`.
- **Names:** use RuneLite's `Text.standardize` / `Text.toJagexName` instead of the custom `nameKey` normalising.
- Suggested commit: `refactor: tidy after the review`

### 13. Docs

- README: hop removed; standings come from the clan sheet; when the logout update sends; image rule; what leaves the
  player's computer (the endpoint request to Google; the opt-in logout update to Wise Old Man with the player's name).
- `docs/SUBMISSION.md`: tick off the licence, image and Debug items done here.
- Record anything done beyond these tasks in "Changes made beyond the task list" below.

## Checked by the review and fine (no task)

Injected `OkHttpClient` and `Gson`, network on OkHttp threads, Swing on the Swing thread; no reflection, JNI,
processes or file APIs; saved copy written through RuneLite's plugin data folder; chat messages built with
`ChatMessageBuilder` (sheet values escaped, no chat tag injection); chat commands answer only your own messages and
never send or type chat; overlays, nav button, listeners, chat commands and timers removed on shutdown; logging at
debug only, dev chat output gated on developer mode; Player Indicators' config read through `ConfigManager` (reading
is allowed; only changing another plugin's config is not); `build.gradle` matches the template.

## Out of scope for Phase 5

New features; Plugin Hub submission itself (`docs/SUBMISSION.md`).

## Changes made beyond the task list

- Task 3: a leaderboard or player block's `competition=<WOM id>` now only matches a current competition (only those
  carry standings in the endpoint); any other id draws nothing. A tied lead reads "Level with 1st" rather than
  "Leading".
- Task 4: only the "too little progress, skipped" path was tested in-game. The send after progress, the 10-minute
  gap across restarts, the XP Updater check and the special-world check are untested; worst case, no update is sent.
- Task 5: a Refresh press within a minute of a good load does nothing visible (only a dev log line), instead of saying
  when the data was loaded; the owner found the notice cluttered the panel.
- Task 5: the endpoint request waits up to 30 seconds for a response instead of OkHttp's default 10. In the dev client
  the first (uncached) load timed out after exactly 10 seconds while the script was still building the response;
  the next load, served from the script's cache, took about a second.
- Task 6: the safe label factory (`Labels.plain`) replaced the private label helpers in `StandingCard`, `RankTable`,
  `RankBadge`, `TimeBar`, `BingoTileDetail` and `BingoTeamBlock`, so that part of task 12's duplication item is done.
