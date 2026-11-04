# PvKnee plugin: Phase 4 tasks (bingo)

Read `docs/SESSION.md`, then `docs/DESIGN.md` (including the UI rules). This file is the task list for Phase 4 only.

**Status:** complete. Tasks 0 to 14 done and checked in the dev client (task 5's Teammates block is built but hidden;
see the end of this file). Sheet, endpoint and page rows are in place. Review fixes follow in `docs/PHASE5.md`.

## Goal (the Phase 4 gate)

A clan bingo page: each team's board (5x5 or 6x6) with an icon per tile, coloured by status, a detail card for the
selected tile, the team's members and points, and all teams' standings. Members can switch between teams' boards
(view only). Everything comes from a separate bingo Google Sheet that admins fill in, so one-off events are reusable:
copy the template, fill it in, point the main sheet at it. Submissions and approval stay manual in Discord; the
plugin only displays. The page can be hidden if bingo is not ready, so Plugin Hub submission does not wait on it.

Mock-ups: `docs/mockups/BingoBoard.dc.html` (interactive: team switcher and tile selection) and
`docs/mockups/BingoStates.dc.html` (before start, not on a team, logged out, tallying, final result).

## How bingo data flows

1. **Players** post proof in the clan's Discord submission channel (no change for people without the plugin).
2. **Admins** approve in Discord and add one row per approved submission to the bingo sheet's `Log` tab (by hand, or
   through an optional admin-only Google Form that writes to it). They never edit the board itself.
3. **The Apps Script** (owner's part) reads the active bingo sheet and works out every team's tiles, progress,
   completion dates, points and standings, and sends them as a `bingo` object in the endpoint.
4. **The plugin** shows it. Nothing is written back.

## The bingo sheet (a separate Google Sheet per event, made from a template)

| Tab | Columns | Notes |
| --- | --- | --- |
| `Settings` | Key / Value rows: `Name`, `Start`, `End` (UTC date and time), `Results final` (tick box), `Board size` (5 or 6), `Submission channel` (e.g. `#bingo-submissions`) | Before the end time: running. After it, until `Results final` is ticked: tallying. Then: final. |
| `Tiles` | `#`, `Name`, `Description`, `Item ID`, `Points`, `Needed` (default 1), `Unit` (e.g. `pieces`, `kills`, `xp`) | Item IDs from the OSRS Wiki (e.g. `11832` Bandos chestplate). Tiles fill the board left to right, top to bottom. |
| `Parts` | `Tile #`, `Part`, `Needed` (default 1), `Item ID` (optional) | Only for tiles made of several different things (e.g. a godsword: three shards and any hilt). A tile with parts is done when every part is in; its `Needed` and `Unit` are ignored. |
| `Teams` | `Team`, `Player`, `Colour` (optional hex, read from the team's first row that has one) | One row per player. No colour: the theme's value colour. |
| `Log` | `Date`, `Team`, `Tile #`, `Player`, `Amount`, `Approved by`, `Note` (e.g. `Bandos tassets`), `Part` (only for tiles with parts) | Only approved submissions. Mistakes are fixed by editing or deleting a row. |

The main sheet's Settings gets one cell, `Active bingo sheet` (the bingo sheet's link). Empty: no bingo; the
endpoint's `bingo` is `null` and the `tab_bingo` page hides itself (its `show` cell is a formula on that cell).

## Running an event (the owner's checklist)

1. **Draft.** Copy `PvKnee Bingo Template`, rename it (e.g. `PvKnee Bingo - Nov 2026`), clear the sample rows and
   fill in Settings and Tiles. Iterate on tiles with the clan as long as needed.
2. **Preview while drafting.** Put the copy's link in the main sheet's `Settings!B9` (Draft bingo sheet). In the dev
   client, set the endpoint override to the endpoint address plus `?bingo=preview&key=<preview key>` (the key is the
   `PREVIEW_KEY` Script Property, logged by `moveSecrets()`; without it the parameter is ignored): the bingo page then
   shows that sheet with its tiles, whatever its dates. Members see nothing of it. `previewBingo()` in the script
   editor logs the same data.
3. **Teams drafted (a few days before).** Fill in Teams and put the copy's link in `Settings!B8` (Active bingo
   sheet). Members now see the bingo page with their team and "Starts in"; the tiles stay hidden (the endpoint leaves
   them out until the start), so tiles can still change. Teams can change at any time.
4. **Start.** The board appears automatically at the start time. From now on tiles may still be reworded or
   re-pointed, but never renumbered: the Log refers to tiles by `#`.
5. **During.** Admins add Log rows for approved submissions. Moving a player between teams keeps their old rows with
   the team named on each row.
6. **After the end.** "Tallying results" shows until `Results final` is ticked; then the winner shows.
   Home's Happening Now (while running) and Coming Up (before the start) show the bingo automatically: the Apps
   Script adds it to the events list (kind `bingo`) while it is upcoming or running, unless an event with the same
   name is already listed. Clicking it opens the bingo page.
7. **Done.** Clear `Settings!B8` when the clan no longer needs the page; the sheet stays as a record.

## Endpoint contract (`bingo`, sent by the owner's Apps Script)

```json
"bingo": {
  "name": "Clan Bingo",
  "startsAt": "2026-10-31T17:00:00Z", "endsAt": "2026-11-14T17:00:00Z",
  "state": "running",
  "boardSize": 6,
  "submit": "#bingo-submissions",
  "tiles": [
    { "num": 5, "name": "Two Bandos pieces", "description": "Any two of: chestplate, tassets, boots.",
      "itemId": 11832, "points": 2, "needed": 2, "unit": "pieces", "parts": [] },
    { "num": 20, "name": "Complete a godsword", "description": "One of each shard and any hilt.",
      "itemId": 11802, "points": 3, "needed": 4, "unit": "parts",
      "parts": [ { "name": "Shard 1", "needed": 1, "itemId": 11818 }, { "name": "Any hilt", "needed": 1, "itemId": 11810 } ] }
  ],
  "teams": [
    { "name": "Team Ember", "colour": null, "rank": 2, "points": 18, "tilesDone": 9,
      "members": [ { "name": "Iron Aeria", "points": 7.5, "tiles": 3 } ],
      "progress": [
        { "num": 5, "status": "progress", "amount": 1, "completedAt": null, "completedBy": null,
          "contributors": [ { "name": "W ebb", "amount": 1, "note": "Bandos tassets" } ] },
        { "num": 20, "status": "progress", "amount": 2, "completedAt": null, "completedBy": null,
          "contributors": [ ... ],
          "parts": [ { "name": "Shard 1", "needed": 1, "amount": 1, "done": true,
                       "contributors": [ { "name": "Iron Aeria", "amount": 1, "note": "" } ] } ] }
      ] }
  ]
}
```

- `state`: `upcoming`, `running`, `tallying` or `final`. While `upcoming`, `tiles` is empty and every team's
  `progress` is empty (the board is not revealed early); `boardSize`, teams and members are sent. With
  `?bingo=preview&key=<preview key>` the draft sheet (`Settings!B9`) is used and its tiles are always sent.
- `progress` lists only tiles with at least one log row; any other tile is not started.
- A tile is `done` when the summed amounts reach `needed`; `completedAt` / `completedBy` are the date and player of
  the row that got it there. Log rows after that are ignored. `amount` is capped at `needed`.
- **Tiles with parts:** `needed` is the number of parts and `unit` is `parts`; a team's `amount` is the number of
  parts complete, and `progress.parts` gives each part's amount and contributors (in the tile's part order). Log rows
  for such a tile count only when their `Part` matches a part name.
- **Member points, split by contribution:** for each done tile, each part counts equally, and within a part each
  contributor gets their share of its logged amount: `points / parts x their amount / part's total` (a tile without
  parts is one part). One decimal. A member's `tiles` is the number of done tiles they contributed to. This
  may be removed later, so the plugin must cope with it being hidden (see task 5).
- Teams are sorted by points (ties share a rank). Unknown fields are ignored, as everywhere.

## Tasks

0. **Bingo data and page state.** Model classes for the contract. A small shared view state for the bingo page (the
   selected team and the selected tile), reachable through `BlockContext`, so all bingo blocks on the page follow the
   same selection. The selected team defaults to the member's own team (from `PlayerInfo`, with the same loose name
   matching as the `player` block), or the top team when they have none. `DevLog` the state, own team and selection.
1. **Item icons.** Extend the icon helper with item icons by item ID, drawn by the game client (RuneLite's
   `ItemManager`), at the requested size, loaded without blocking Swing.
2. **`bingo_team` block.** Mock-up: top of `BingoBoard`. Title "Your Team" (or "Team" when viewing another team) with
   the time as its aside ("12d 20h left", "Starts in 2d 4h", "Ended"). Card: a team switcher (two small chevron
   `IconButton`s around the team name, and a dot per team with the current one in the accent colour; it wraps around),
   then the rank badge, "9 of 36 tiles", points, and a thin green bar of tiles done. The team name uses the team's
   colour when set. Switching team updates every bingo block on the page.
3. **Tile grid kit piece and `bingo_board` block.** Mock-up: Board in `BingoBoard`. A square grid of `boardSize` x
   `boardSize` tiles that fills the card width, each with its item icon:
   - done: green tint and border with a small tick in the corner;
   - in progress: orange tint and border with a thin progress line along the bottom;
   - not started: plain, icon slightly dimmed;
   - selected: accent outline; clicking selects, clicking again clears.
   Status colours are fixed (green `#4CAF50`, orange `#FF9800`), like the medal colours. Hover text: the tile name and,
   when `needed` > 1, the fraction (e.g. "Two Bandos pieces · 1 / 2 pieces", "Complete a godsword · 2 / 4 parts"). A legend under the grid. Title "Board"
   with aside "Select a tile".
4. **Tile detail card** (part of `bingo_board`, directly under the grid). Mock-up: `BingoBoard`. Nothing selected: a
   dashed hint card. Selected: icon, name, "Tile 5", points pill (`+2`), description, then by status:
   - done: "Completed" in green, points awarded, completion date and who completed it;
   - in progress: "In progress" in orange, the fraction with the unit (`1 / 2 pieces`; xp as `640k / 1M xp`), a bar,
     and each contributor with their amount or note. A tile with parts shows a checklist instead of the contributor
     list: per part, its small item icon, a green tick when done (or `1 / 2` when it needs more) and who got it;
   - not started: "Not started" and "Got it? Post a screenshot in {submit} on Discord. An admin marks it here."
5. **`bingo_members` block** (Teammates). Mock-up: `BingoBoard`. The selected team's members: name, tiles, points
   (one decimal, `.0` dropped), the member highlighted with "(you)". Setting `points=false` hides the points column.
6. **`bingo_teams` block** (Teams). Mock-up: `BingoBoard`. Standings in the `RankTable` look: medal badges, team
   name, tiles, points; the member's team highlighted. Clicking a team selects it in the switcher.
7. **States.** Mock-up: `BingoStates`. Before start: the team card with "Starts in", a placeholder board ("The board is
   revealed when the bingo starts"). Logged in but not on a team: an `EmptyState` ("You're not on a team", group
   glyph), board and teammates follow the top team. Logged out: an `EmptyState` ("Log in to see your team"). Tallying:
   an `EmptyState` with an hourglass glyph ("Tallying results"; "Points below may still change"); board and standings
   stay. Final: a result card (trophy, "[Team] won", points and tiles, then "Your team, [Team]: 2nd · 18 pts"). Add
   the `hourglass` and `user` glyphs from the mock-up SVGs.
8. **README.** Add bingo to the features list (view only; data from the clan's bingo sheet).

Done by the owner (1 Nov): the bingo sheet template (`PvKnee Bingo Template`, with sample data: 25 tiles including a godsword tile with parts,
three teams, a few log rows; Iron Aeria is on Team Ember), the `Active bingo sheet` cell (main sheet `Settings!B8`), the `tab_bingo`
page (`header | dates=bingo`, `bingo_team`, `bingo_board`, `bingo_teams |
title=Standings`; its `show` cell is `=Settings!B8<>""`) and the Apps Script `bingo` object. The `header` block needs
`dates=bingo`: the bingo's date range as the subtitle and, when there is no `title`, the bingo's `name` as the title.

9. **Members in the team card.** Mock-up: `BingoBoard` (Your Team card). Under the tiles bar, a thin divider, a small
   muted "Members" label with the count on the right, then the selected team's member names in two columns (names
   only: no points or numbers), in sheet order. The member's own name has a soft accent highlight. A name too wide for
   its column ends with "..." and shows in full on hover (RuneScape names are at most 12 characters, so this is rare).
   Follows the team switcher. The separate `bingo_members` block stays parked. The last section's title is now
   `Standings` (sheet: `bingo_teams | title=Standings`).

10. **Place pill, no standings section.** The team card's rank badge becomes a medal-coloured pill with the place as
    a word: `1st`, `2nd`, `3rd` (gold, silver, bronze), then `4th` and below in a neutral grey, with dark text. The
    team switcher steps through teams in rank order (1st first), still opening on the member's own team, so flicking
    through it is the standings. The switcher's dots are in the same rank order and coloured by place (gold, silver,
    bronze, then grey); the current team's dot is slightly larger with a ring in its colour, the others dimmed. The `bingo_teams` row was removed from `tab_bingo` (1 Nov); the block stays in the
    code, so a bigger event can add `bingo_teams | title=Standings` back without an update. No "behind 1st" line.

11. **Place pills everywhere.** Make the task 10 pill a kit piece (e.g. `PlacePill`: the place as a word, `1st`,
    `2nd`, `3rd` in gold, silver and bronze with dark text, `4th` and below in grey) and use it wherever a rank badge
    is drawn today: the standings tables on Skill and Boss of the Week (`RankTable`) and the "You" card (`player`
    block). In the tables, medal rows get the pill and lower rows show the plain muted ordinal (`4th`, `12th`) in a
    rank column wide enough for `20th`, so names stay aligned. Remove the old round number badge if nothing else uses
    it.

12. **Bingo on Home links to the bingo page.** The Apps Script now adds the active bingo to the events list with
    `kind` `bingo` (while upcoming or running). In the `events` block: the caption for kind `bingo` is `Bingo` (not
    `Event`), with the `grid` glyph as its icon; and a bingo entry is clickable both while running and while upcoming
    (the bingo page has its own upcoming state), using the existing `{kind}_page` link setting, i.e. `bingo_page`.
    Other upcoming entries stay unclickable. Sheet (done): Home's Happening Now row is `events | when=running |
    kind=event,bingo | title=Happening Now | bingo_page=tab_bingo`, and Coming Up has `bingo_page=tab_bingo`.

13. **Team colours and team icons.** Purely visual and local: they only change what the member sees, never anything sent
    to the game or to other players, and messages the member sends are untouched.
    - **Team colours.** A team's colour is its sheet `Colour`, else a fixed default palette by team order in the sheet
      (red, blue, green, yellow, purple, orange, then repeating). Use it everywhere a team colour shows (team card name,
      icons, overlay). One shared helper.
    - **When.** Only while the bingo `state` is `running`, and only for players whose name is on a team in the live
      bingo (loose name matching as elsewhere: case, spaces, underscores and hyphens). Anyone else is untouched.
    - **Chat.** In clan chat, the team dot replaces the clan rank icon in front of the sender's name; in public chat the
      dot is added in front of the name. Use RuneLite's chat icon support (register the dot images once, insert them
      into the displayed name), the way rank and icon plugins do. Setting `Team icons in chat` (on by default).
    - **Above heads.** A small dot in the team colour drawn just left of the player's name position above their head
      (where name plugins such as Player Indicators show rank icons), as the plugin's own overlay. Hidden in the
      Wilderness and on PvP worlds. Look players up in a prepared name-to-team map, not by scanning the scene for
      every name each frame. Setting `Team icons above heads` (on by default).
    - The dots are small filled circles in the team colour with a thin dark outline, sized to match chat rank icons.
14. **Team overlay for screenshot proof.** A small RuneLite overlay (movable like any overlay) that shows the
    member's team name in the team colour, then the current date and time in UTC to the minute, e.g. `Team Ember ·
    Tue 3 Nov 2026, 21:35 UTC`, always UTC regardless of the time setting, so admins can check when a screenshot was
    taken. Shown only while logged in, on a team, and the bingo is `running`. Setting `Bingo team overlay` (on by
    default). The time updates every minute.

Settings for tasks 13 and 14 go in one "Bingo" settings section. Update the README's features and settings.

## Later (not Phase 4)

- Drop detection that suggests submissions; Discord posts (through the Apps Script, never a webhook in code).
- Possibly removing member points, if they make people feel bad.

## Out of scope for Phase 4

Writing anything back to the sheet from the plugin; automatic approval; Plugin Hub submission (`docs/SUBMISSION.md`).

## Changes made beyond the task list

(Add anything done beyond a task here, as `docs/SESSION.md` asks.)

- **`header` block, `dates=bingo`** (done between tasks 2 and 3): the subtitle is the bingo's date range, and with no
  `title` setting the title is the bingo's `name`. With no bingo, the header falls back to `title` and `subtitle` as
  before (and shows nothing without a `title`).
- **Tile detail card (task 4), agreed changes:** the no-selection hint reads "Select a tile to view it"; the
  not-started line ends at "on Discord." (no "An admin marks it here."); status words use the RuneScape bold font
  (Java-bolded small text was fuzzy); the points pill is bold; on a done tile the name, pill and "+X pts" are green.
- **Board (task 3), agreed changes:** item icons are trimmed of empty space before scaling and fill 75% of a tile;
  tiles grow to fill the card's width; the grid is updated in place on selection and team changes (no flicker).
- **Teammates (task 5) parked:** the `bingo_members` block is built and works, but it is hidden for now: its row
  was removed from the `tab_bingo` page (1 Nov; to bring it back, add `bingo_members | title=Teammates` after
  `bingo_board`), since splitting points (in-progress tiles, shared parts) has too many edge cases
  that members may find annoying. Add the row back to show it again; no plugin change needed. `RankTable` keeps the
  row options added for it (caption, suffix, unranked and clickable rows), which `bingo_teams` uses.
- **Standings (task 6) parked too (task 10):** the `bingo_teams` block is built but its row was removed from
  `tab_bingo` (1 Nov), as the switcher's place pills and coloured dots already show the order. To bring it back, add
  `bingo_teams | title=Standings` after `bingo_board`; no plugin change needed.
- **Teams (task 6), agreed change:** the team being viewed is outlined in the accent colour (a `RankTable` row
  option), on top of the own team's highlight.
- **Members in the team card (task 9):** before the start, the members list (with its count) replaces task 7's
  "N members" line, since it would only repeat the count.
- **Switcher order (task 10):** the switcher and its dots sort by each team's `rank` from the endpoint (1st first),
  not by `points` directly; teams on the same rank keep the endpoint's order, and a team without a rank goes last.
  This relies on the Apps Script ranking by points, as the endpoint contract says. If the two could ever differ,
  switch the sort to points (highest first).
- **Team colours (task 13), default palette order:** the endpoint sends teams sorted by points, not in sheet order,
  so a team without a sheet colour takes its palette colour by its place among the team names in alphabetical
  order. This keeps a team's colour fixed as points change. To follow sheet order instead, the Apps Script would need
  to send each team's sheet position. The team card's name now always has a team colour (before, a team without
  one used the theme's value colour).
- **Team icons (task 13), how clan chat works:** the game draws the clan rank icon as its own 13 px graphic beside
  the name, not inside the name. Each chat line uses four fixed chatbox children (name, message, "[clan]" prefix,
  rank icon). After every run of the chatbox build procedure (script 84, which every rebuild runs), the plugin hides
  the rank icon on lines from players on a team and moves the name, with the dot in front, into its place. Public
  chat adds the dot to the name itself.
- **Team icons in the clan member list (task 13), agreed addition:** the Clan tab's member list also shows the dot
  in place of the rank icon, under the `Team icons in chat` setting. After each run of the list's draw procedure
  (script 4399), the plugin hides the rank icon of each player on a team and widens the name to the left by the
  dot's width, with the dot in front, so names stay aligned. A nudge of the dot to the right was tried and undone,
  since it moved those names right too. Not done for a guest clan's list.
- **Team icons above heads (task 13), agreed nudge:** the dot is drawn 2 px above the middle of the name.
- **Team icons above heads (task 13), agreed change:** the dot shows only where RuneLite's Player Indicators plugin
  draws that player's name: the plugin is on, its name position is not Disabled, and the player's group (you, party,
  friends, friends chat, team, clan, others) is turned on, following that plugin's own rules. The dot sits left of
  the name, or left of Player Indicators' rank icon when it shows one, at any of its name positions (above head,
  model centre, model right). Mirrors Player Indicators' rules, so a change to them in RuneLite may need an update.
  Player Indicators is built into RuneLite, so everyone has it; with it turned off (or its names Disabled) there are
  no overhead dots, and chat and the member list are unaffected. The setting's description and the README say so.
- **Team icons (task 13), when:** besides `state` `running`, the icons also stop once `endsAt` has passed, so a
  saved copy from before the end cannot keep them showing.
- **Team overlay (task 14):** it uses the same roster as the team icons, so it also hides once `endsAt` has passed.
  It starts above the chatbox on the left (RuneLite's `BOTTOM_LEFT` anchor; `ABOVE_CHATBOX_RIGHT` also works). The always-UTC time format is a shared `Times` helper (`utcStamp`).
