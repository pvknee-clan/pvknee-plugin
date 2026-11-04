# PvKnee plugin: Phase 2 tasks (design)

**Status: complete (31 Oct 2026).** Tasks 0 to 15 are built and confirmed in the dev client; the UI rules are in `DESIGN.md`.

Read `docs/SESSION.md`, then `docs/DESIGN.md`. This file is the task list for Phase 2 only.

## Goal (the Phase 2 gate)

The four pages (Home, Skill of the Week, Boss of the Week, Events) look finished and cohesive, the UI kit is settled,
and a short "UI rules" section in `DESIGN.md` records the decisions, so later phases build on a fixed look instead of
re-deciding it.

## How this phase works

- Design decisions are made with the owner outside Claude Code, from screenshots and mock-ups. Tasks are added to
  this file as each decision is agreed. Do only what a task says; do not redesign beyond it.
- Make every visual change in the `ui` kit first (or in a shared helper), then use it in the blocks. A block should
  not gain its own one-off styling.
- After each task, run the dev client and tell the owner which pages to look at.

## Agreed decisions (from the first visual review)

The mock-ups are settled; the tasks below build these. HTML copies of the mock-ups are in `docs/mockups/` (for
layout, colours and sizes only: they use a web pixel font as a stand-in for the RuneScape fonts, and their icons are
not available offline).

- **No dashed separators.** Spacing separates sections; a section title or, at most, a faint thin line marks a real
  break. Keep the `separator` block type so existing sheet rows still work, but draw it as a thin line or a gap.
- **Titles.** A card holding a list (links, coming up, standings) has its title above the card. A card that is one
  thing (clan message, a hero competition) needs no separate title.
- **Top bar.** A square home button (house icon), then the page dropdown, then a square refresh button (refresh
  icon). Both buttons use the card style and hover. No text buttons.
- **Loading.** The refresh icon spins while loading, on first open and on refresh. No "Loading..." line that pushes
  content down. Errors show as a small notice card under the top bar.
- **Alignment.** One consistent rule across blocks, broken only when the content needs it.
- **Links.** A compact 2x2 grid (icon above or beside the label). Labels come from the sheet, so they can be
  shortened there.
- **Leaderboard.** One table instead of a podium card plus a table. The top `podium` rows get medal-coloured rank
  and name and slightly taller rows; all values are right-aligned in one style.
- **Empty standings invite people to take part,** e.g. "No kills yet. Kill Cerberus this week to get on the board", with
  a button to the competition. No "update on Wise Old Man" step: the plugin's logout update (Phase 3) covers that. Wording comes from a block setting (e.g. `empty=`) with a
  sensible default.
- **"You" slot.** Leave room above the leaderboard for the member's own place and gain, and a highlighted row. Built
  in Phase 3 with the `player` block (it needs the logged-in name).
- **Events page** is hidden in the sheet (`tab_events` show = FALSE) until there are events besides the weekly
  competitions.
- **Coming-up entries** say what they are (Skill of the Week, Boss of the Week or Event), with the time on its own
  line when entries start at different times.
- **Timings are per item.** A progress bar shows how far an item is through its own start-to-end span, never "the
  week". A block shows one shared time (and one bar) only when every item in it has the same time; otherwise each
  item shows its own. Items without a fixed end get no bar; upcoming items never get a bar. The competition card's
  label is "Now running", not "This week".
- **Running one-off events on Home** (e.g. bingo) come from an `events` block with `when=running` and a new filter
  setting (e.g. `kind=event`) that leaves out the weekly skill and boss competitions, so they are not shown twice.
  A block with nothing to show draws nothing, so the section disappears when no such event is running. The Events
  page stays hidden. Bingo may get its own `kind` later (Phase 4).

## Tasks

**Mock-ups:** only the files directly in `docs/mockups/` are current: `HomeV3`, `SotwV3`, `LongNames` (v3) and
`ChallengesLastDay`, `HappeningBingo`, `HeroBars` (v2; for hero names, `SotwV3` wins). The v1 files are in `docs/mockups/old/` as history: never match them, and where a task below names a v1 file
(e.g. `Main.dc.html`), the built result plus the UI rules in `DESIGN.md` now take precedence.


0. **Carry-over fixes from the Phase 1 review.**
   - **Open pages at the top.** When the selected page changes (dropdown or a page link), scroll the content back to
     the top. The minute-by-minute update and the background refresh keep the current scroll position.
   - **Lighter live updates.** Today the whole page is rebuilt every minute. Instead, update only the text that
     changes (countdowns such as `Ends in 6d 4h`) in place, e.g. through a small kit piece whose text comes from a
     supplier that the panel refreshes each minute for the current page. Standings must still refresh when their
     5-minute cache expires, and a new endpoint load still rebuilds the page.

1. **Top bar.** Replace the dropdown row with: a square home button (house icon), the page dropdown, and a square
   refresh button (refresh icon). Add an `IconButton` kit piece (square, card style and hover, icon only, tooltip,
   accessible name) and use it for both. Small 16 px icons can be bundled PNG resources or drawn with Java2D; keep
   them crisp at 1x. The home button opens the first page and is outlined in the theme accent while that page is
   shown. While loading (first open and refresh) the refresh icon spins and the button is disabled; remove the
   "Loading..." line. Failures show in a new `Notice` kit piece (a small card under the top bar), which also carries
   the "Last updated" and "newer version" lines. Mock-up: `docs/mockups/Main.dc.html` (top of page).
2. **Spacing, separators and section titles.** One spacing rhythm in `Spacing` (between blocks, title to card,
   inside cards). The `separator` block draws a faint thin line in the muted colour at low strength, never the dashed
   text. `SectionHeader` becomes left-aligned, with an optional muted aside on the right (used for shared times and
   column labels). Inside one-thing cards (the clan message), the title is a small accent label in capitals. Check
   the RuneScape bold font's "W" (it renders as "W ebb" and "VVonky" today); if it cannot be fixed, use the regular
   RuneScape font for names. Add a `pill` option to the `text` block's `style` (small rounded outline, used for
   "Home world 509").
3. **Competition card.** Side layout: a small "NOW RUNNING" label (or "STARTING", for `which=upcoming`) with the
   shared time as its aside and one `ProgressBar` under the columns, when all competitions share their times;
   otherwise each column shows its own time and bar. Columns stay clickable with the hover from `Card`. Hero layout:
   icon, name, then a bar with "5d 22h left" on the left and the end date on the right. Bars and times update with
   the live updates from task 0. Mock-ups: `Main.dc.html`, `Sotw.dc.html`, `HomeBingo.dc.html`.
4. **Events block.** All entries in one card with thin dividers, title above (left-aligned). Each entry: icon, name,
   then a muted caption saying what it is (`Skill of the Week`, `Boss of the Week`, or `Event`). When every entry
   shares a start time (upcoming) or end time (running), show it once as the title's aside; otherwise each entry
   shows its own on an extra line. Running entries with a fixed end get their own `ProgressBar`. New setting `kind`
   (e.g. `kind=event`, or a comma list) filters by the event's `kind`; default is all. A block with nothing to show
   draws nothing, title included. Mock-up: `HomeBingo.dc.html` (Happening Now, Coming Up).
5. **Links block.** A 2-column grid of compact buttons, icon above the label. Mock-up: `Main.dc.html` (Links).
6. **Leaderboard as one table.** Replace the podium card plus table with one table in one card: medal-coloured
   number badges and names for the top `podium` rows (slightly taller), plain rank numbers below, every value
   right-aligned in the value colour. The `label` setting becomes the title's aside (e.g. `XP gained`, `Kills`).
   Empty state: a card with a muted trophy icon, a bold line and a muted line, then the button. New setting `empty`
   for the muted line, defaulting to "Kill {name} this week to get on the board." for bosses and "Train {name} this
   week to get on the board." for skills, where `{name}` is the competition's name; the bold line is "No kills yet"
   or "No gains yet". Mock-ups: `Sotw.dc.html` (ignore the dashed "You" slot: Phase 3), `Botw.dc.html`.

7. **Competition card bar placement.** Every timed item reads the same way: its bar at the bottom with its time
   directly under it (`TimeBar`), as the hero card and running event rows already do. Side layout: the "NOW
   RUNNING" (or "STARTING") label alone at the top, then the columns, then one `TimeBar` across the card ("5d 22h
   left" on the left, the end date on the right) when all competitions share their times; otherwise each column gets
   its own `TimeBar` under it. This replaces the post-phase note below.
8. **Element and asset check against the mock-ups.** Go through every element on Home, Skill of the Week and Boss of
   the Week against `docs/mockups/` and fix what does not match. Known gaps:
   - **Page dropdown.** It still looks like a default Swing combo box beside the card-style icon buttons. Make it a
     card-style picker the same height as `IconButton` (30 px): the page title on the left, a muted chevron on the
     right, card background, border and hover; clicking opens the list of visible pages, drawn to match (dark
     background, value-coloured text, hover tint, the current page marked). Add it to the kit (e.g. `PagePicker`).
   - **Icons: use the mock-ups' icons everywhere.** Every icon in the panel is a built-in `Glyphs` shape copied from
     the inline SVGs in `docs/mockups/` (same paths, 24-unit grid, round caps and joins, stroked not filled), so the
     set is consistent and nothing is downloaded except skill/boss pictures. The set, and where each is used:
     | Glyph | Mock-up source | Used for |
     | --- | --- | --- |
     | `home` | top bar house | Home button |
     | `refresh` | top bar arrow | Refresh button (spins while loading) |
     | `chevron` | dropdown chevron | Page picker |
     | `globe` | "Home world 509" pill | `text` blocks with `style=pill` (new `icon=` setting on the text block, e.g. `icon=globe`) |
     | `chat` | Discord link | Link icons |
     | `group` | Overview link | Link icons |
     | `chart` | Competitions link | Link icons |
     | `trophy` | Achievements link, empty standings | Link icons, `EmptyState` |
     | `external` | "Full standings" / "Open on Wise Old Man" | Every button that opens Wise Old Man or another site: the leaderboard's `button` on Skill and Boss of the Week and the empty-state button (small, muted, after the text, via `LinkButton`) |
     | `calendar` | `[Clan event]` row in `HomeBingo.dc.html` | One-off events with no picture |
     | `grid` | `[Bingo name]` row in `HomeBingo.dc.html` | Kept for bingo (Phase 4); add the shape now |
     Replace any existing glyph that differs from its mock-up version. Link icons: the sheet's link icon column takes
     a glyph name, optionally with a colour (`chat:#8C9EFF`); otherwise the glyph uses the value colour. An unknown
     name is ignored (log at debug, draw the label alone). Image addresses are no longer loaded for links.
   - Anything else that differs from the mock-ups (colours, borders, sizes, alignment): list it in the summary and fix
     it if it is a kit change; ask first if it needs a new decision.

9. **Bar captions show the time left, on the right.** Under every bar the caption is just the time left,
   right-aligned (e.g. `5d 22h left`): the shared bar on the competition card, per-column bars, and running event
   rows (their "Ends in ..." caption becomes "... left" under the bar). No end date on these: the next competition's
   start, which is the same moment, already shows in Coming Up. Exception: the hero layout (Skill and Boss of the
   Week pages, which have no Coming Up) keeps the end date, on the left, with the time left on the right. Give
   `TimeBar` what it needs for a lone right-aligned caption (today a lone caption is centred).

10. **One header style.** Every titled block shows its title as a bold accent section title above its card, like
    "Coming Up" and "Links". The `message` block's `title` (now `Announcement` in the sheet) moves from the small
    capitals label inside the card to a section title above it. The `competition` block gets a `title` setting
    (the sheet now has `title=Weekly Challenges`) drawn the same way; without it there is no title, and the
    hard-coded "NOW RUNNING" / "STARTING" label is removed. Remove `SectionHeader.cardLabel` if nothing else uses it.

11. **Challenges, day bars and local times.** Mock-ups: `HomeV2.dc.html`, `ChallengesLastDay.dc.html`,
    `HappeningBingo.dc.html`, `HeroBars.dc.html` in `docs/mockups/`.
    - **Challenges card** (competition block, side layout). It only ever holds the weekly skill and boss, which
      always share their times, so drop the per-column timing paths. The block's `title` (now `Challenges` in the
      sheet) gets the time left as its aside (`5d 20h left`), updated live. No bar in this card. A thin vertical
      divider separates the columns, like the list dividers.
    - **Ending soon.** In an item's final 24 hours its time turns a fixed "ending soon" colour (`#FF9800`, fixed like
      the medal colours, not themed), bold, and reads `Ends in 5h 12m`. This applies to the Challenges aside, bar
      captions and hero cards.
    - **Day bar.** A new kit piece (or a `ProgressBar` option) used by running event rows and the hero layout: one
      segment per day (per week when the span is over 14 days), 2 px gaps, about 6 px tall, draining from the left:
      elapsed segments are empty, the current one is partly filled on its right-hand side, the rest are full. Accent
      colour, or the ending-soon colour in the last 24 hours. Caption: the time left on the right; the hero layout
      also shows the end date on the left.
    - **Dates and time zone.** Within today or tomorrow, dates read `Today, 17:00` / `Tomorrow, 17:00`; otherwise
      `Fri 6 Nov, 17:00`. This replaces the Phase 1 "in 5h 12m" form for upcoming items. Times use the member's
      local time zone by default. New personal setting `Show times in UTC` (off by default, not in the Debug
      section): when on, times are UTC and carry a `UTC` suffix; local times have no suffix. "Today" and
      "Tomorrow" follow the chosen zone. Keep all of this in `Times`.

12. **Quiet standings loading.** Replace the "Loading standings..." text line with the `EmptyState` look in a
    loading form: the muted trophy and a muted "Loading standings..." line, no bold line, same card size, so the
    page does not jump when the table arrives. Errors keep a muted line in the same card.

13. **Page header block.** A new `PageHeader` kit piece and `header` block: a centred title in the header colour, an
    optional muted subtitle and an optional `Pill`, spaced tightly as one unit (about 3 px title to subtitle, 5 px to
    the pill) instead of the block gap. Settings: `title`, `size` (as `heading`), `subtitle`, `dates`
    (`current_skill` or `current_boss`: the subtitle becomes that competition's date range, e.g. `Fri 30 Oct – Fri 6
    Nov`, in the member's chosen time zone), `pill` and `icon` (a glyph name for the pill). Values can use sheet
    placeholders, which the endpoint resolves, e.g. `title={Settings!B2}`, `pill=World {Settings!B4}`. Mock-ups:
    `HomeV3` (top), `SotwV3` (top). After this task the owner replaces Home's heading and two text rows with one
    `header` row and adds a `header` row to the top of the weekly pages.
14. **Text sizes, pill and bold hero names.** Body text (the `message` block and plain `text`) uses the same size as
    names (the regular RuneScape font), not the small font. The `Pill` uses the regular font with a slightly larger
    glyph. Hero names on the weekly pages are bold, using the regular font bolded by Java (like `Fonts.smallBold`),
    so "W" draws correctly. The leaderboard's button gets the block gap above it, not the item gap. Mock-ups:
    `HomeV3`, `SotwV3`.
15. **Long names.** Names wrap to at most two lines, then end with "...": centred in the Challenges columns, left-
    aligned in list rows (Coming Up, Happening Now). Check with long names such as "Theatre of Blood: Hard Mode" and
    "Tombs of Amascut: Expert Mode" by substituting them temporarily in the dev client, then revert the test code.
    Mock-up: `LongNames`.

After each task the owner updates the sheet to match (for example removing separator rows, shortening link labels,
adding the Happening Now events row). Do not edit the sheet.

The UI rules section in `DESIGN.md` is written by the owner from the finished result.

## Later (not Phase 2)

- Parked: a sheet placeholder for times, e.g. `{time:Fri 17:00}`, shown in each member's chosen time zone, so any
  sheet text can name a time without confusion.
- First task of Phase 3: move the top bar (home button, page picker, refresh and its spinner) out of `PvKneePanel`
  into its own kit class, before more is added to the panel.
- Phase 3, first in-game feature (after the top-bar refactor): **the `player` block** ("You" slot above the
  standings, as in the old `Sotw.dc.html`): "You · 2nd · +1,942" with "5,555 behind 1st", your row highlighted in
  the table; "You're not on the board yet" with no gains; nothing when logged out. Matches the logged-in name
  against the standings already loaded, so no extra request. The sheet already has `player` rows on both weekly
  pages.
- Phase 3, next: **hop from the world pill.** The `header` block's
  pill gets an optional `hop` setting (e.g. `hop={Settings!B4}`). Clicking the pill hops to that world the same way
  RuneLite's World Hopper does (world looked up through RuneLite's world list, hop on the client thread); when
  logged out it sets the login world instead. Tooltip "Hop to world 509"; when already on that world the pill is
  not clickable and says so. The game's own hop rules (e.g. combat) still apply. Flag it in the Plugin Hub
  submission notes, as the first feature that acts on the client.
- Icons in the page dropdown, using the page's `icon` header row.
- A page title bar, if pages still need one once the top bar is in.

## Out of scope for Phase 2

In-game features, rank checks and the `table`, `stat`, `countdown` and `player` blocks (Phase 3), bingo (Phase 4),
and Plugin Hub submission (`docs/SUBMISSION.md`).

## Changes made beyond the task list

Agreed with the owner while doing tasks 7 and 8.

- **Corner rounding.** Cards were rounded to 4 px because `Spacing.ARC` was used as a diameter. `ARC` is now 16
  (an 8 px radius, as in the mock-ups) and the new `CONTROL_ARC` is 12 (6 px) for `IconButton` and `PagePicker`.
  `Card.setArc` picks between them.
- **Refresh while loading.** The spinning refresh icon turns the accent colour, as in `Botw.dc.html`
  (`IconButton.setSpinning` now takes the colour, null to stop).
- **Leaderboard button.** No "Open on Wise Old Man" button anywhere. The `button` setting (e.g. `Full standings`)
  shows under the table only when there are entries; empty standings show just the `EmptyState` card, which no
  longer takes an action.
- **Glyphs.** Shapes are the mock-ups' SVG path strings, read by a small parser in `Glyphs`; `Glyphs.named()` looks
  them up by sheet name. `HOUSE` is now `HOME`. `Icons.url` was removed, as link images are no longer downloaded.
- **Not done:** the mock-up's 15 px refresh icon (16 px here) was left as is.

Made while doing task 11 (not in the task text; the owner checked the result in the client).

- **`DayBar` replaces `ProgressBar`.** Nothing used the plain bar any more, so it was removed; `TimeBar` now wraps a
  `DayBar`. `BlockContext.runningBar` builds the day bar and captions for a running item's span.
- **Upcoming Challenges card** (`which=upcoming`): the title's aside is the shared start date (e.g. `Tomorrow,
  17:00`), never ending-soon styled. A side card without a `title` shows no time.
- **Upcoming hero card** keeps "Starts in 1d 2h" on the right, with the start date on the left and no bar.
- **Ending-soon kit pieces.** `ThemeColours.ENDING_SOON`, `Fonts.smallBold()` (the small font, bolded by Java),
  `Labels.styleTime`, and `SectionHeader.withTimeAside` for live times. Events whose shared aside is a running end
  time also turn orange.
- **`Divider.vertical`** for the line between Challenges columns.
- **UTC setting.** Config key `showTimesInUtc`; changing it redraws the current page in place.

Made while doing tasks 13 to 15.

- **Page header subtitle** stays plain, not italic as in `HomeV3` (owner's choice).
- **Two-line names everywhere.** `Labels.clamped` is used by `IconCaption` as a whole, so hero names and link labels
  also stop at two lines, not only the Challenges columns and list rows. A shortened name shows the full name as a
  tooltip.

### Sheet changes (made by Claude with the owner's permission)

- `Settings` links, column F: header renamed to "Icon (glyph, e.g. chat:#8C9EFF)"; icons set to `chat:#8C9EFF`
  (Discord), `group` (Overview), `chart:#4FC3F7` (Competitions) and `trophy:#FFCA28` (Achievements).
- `tab_home` row 7 (the "Home world" pill): added `icon=globe` in D7.
