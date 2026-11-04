# PvKnee plugin: Phase 1 tasks

Read `docs/SESSION.md`, then `docs/DESIGN.md`. This file is the task list for Phase 1 only.

## Goal (the Phase 1 gate)

The panel shows the four sheet pages (Home, Skill of the Week, Boss of the Week, Events) as properly designed native
pages, matching what Clan Events shows today: theme colours, skill and boss icons, live countdowns, live Wise Old Man
standings and link buttons. It is then ready for its first Plugin Hub submission.

## Fixed facts

| Item | Value |
| --- | --- |
| Clan endpoint | Already in `ClanDataClient` (see `docs/PHASE0.md` for the data contract) |
| WOM competition details | `GET https://api.wiseoldman.net/v2/competitions/{womId}` (no key; returns `participations` with `player.displayName` and `progress.gained`) |
| WOM competition page | `https://wiseoldman.net/competitions/{womId}` |
| Skill and boss icons | `iconBase + metric + ".png"`, e.g. `.../metrics/woodcutting.png` (`iconBase` comes from the endpoint) |
| Medal colours | Gold `#FFD700`, silver `#C0C0C0`, bronze `#CD7F32` (fixed, not themed) |
| Times | Show in UTC. Dates as `Fri 6 Nov, 17:00 UTC`; durations as `6d 4h`, `5h 12m`, `12m` |

## Block settings reference (as the sheet uses them today)

All settings arrive as strings. Missing or invalid values fall back to the defaults shown.

| Block | Settings | What to draw |
| --- | --- | --- |
| `heading` | `text`; `size` 1-3 (default 2); `colour` = header, accent, value, muted or a hex (default accent) | Centred bold title |
| `text` | `text`; `style` may contain `muted`, `italic`, `bold`, `signoff` (muted italic, small, centred); `align` left or center (default center) | Wrapped text |
| `separator` | none | The theme's `separator` string in the muted colour, centred |
| `message` | `title`; `text` | A slightly lighter box: title in accent bold, text in value colour. Draw nothing if `text` is empty |
| `links` | `title` | Optional heading, then one full-width button per entry in `links` (16 px icon if `icon` is set). Click opens the URL with `LinkBrowser` |
| `events` | `when` = running, upcoming or both (default upcoming); `limit` (default 4); `title`; `skill_page`, `boss_page` = page id running events open on click (optional) | Optional heading, then per event: 16 px icon if it has a `metric`, the name in bold (skill colour if `colour` is set, else value), then a muted line: `Skill` or `Boss` for competitions, then the time. Upcoming within 24 h: `in 5h 12m`; otherwise the start date. Running: `Ends in 6d 4h` |
| `competition` | `which` = current or upcoming (default current); `type` = skill, boss or all (default all); `layout` = side or hero (default side); `skill_page`, `boss_page` = page id to open on click (optional) | **side:** the matching competitions side by side, each with a muted `Skill`/`Boss` caption, 32 px icon and bold name; one shared `Ends in ...` line under them when they end together. **hero:** one competition, 48 px icon, larger bold name, then `Ends Fri 6 Nov, 17:00 UTC` |
| `leaderboard` | `competition` = current_skill, current_boss or a WOM id; `rows` (default 20); `podium` (default 3); `prefix` (e.g. `+`); `label` (column title, default `Gained`); `button` (button text) | Podium: the top `podium` places centred, `1st  Name` in medal colour, value underneath in muted. Then a table (`#`, `Player`, label) for the next places up to `rows`. Only players with gained > 0. Empty: `No gains yet`. Optional button under it opening the WOM competition page |
| anything else | | Skip silently (log at debug) |

## Tasks

0. **Fixes from the Phase 0 review.**
   - `runelite-plugin.properties`: change `build=standard.gradle` back to `build=standard`.
   - If the endpoint JSON contains an `error` field, treat it as a failed load (show the error state), not as empty data.
   - If `version` is higher than 1, still render, but show a small muted line: "A newer plugin version is available".
1. **Block system.** A small `BlockRenderer` interface and a registry mapping block type to renderer. Each renderer
   gets the block and a shared context (clan data, resolved theme colours, image loader, link opener, WOM client) and
   returns a Swing component. `PvKneePanel` builds a page by asking the registry for each block. Implement `heading`,
   `text`, `separator` and `message` here. One class per block type, kept short.
2. **Image loader.** Loads an image URL once with the injected `OkHttpClient` (`enqueue`), caches it in memory, resizes
   it to the requested size, and updates the label on the Swing thread when it arrives. Show an empty space of the
   right size while loading. Never block the Swing or client thread.
3. **`links` block.**
3a. **UI kit** (added mid-phase; do this right after task 3 is committed). Create a small `ui` package of reusable,
    themed pieces that every block builds from, so the panel looks cohesive and custom rather than default Swing:
    - `Card`: a panel with rounded corners, a slightly lighter background than the panel and a subtle border.
    - `SectionHeader`: the centred accent title some blocks draw (replaces `Labels.title`).
    - `MedalRow`: a podium line (`1st  Name` in medal colour, value underneath in muted).
    - `ProgressBar`: a thin rounded bar in a theme colour (for countdowns now, bingo later).
    - `IconCaption`: an icon with a caption above and a bold name below (for the competition block).
    - `Spacing`: shared gap and padding constants.
    Use RuneLite's `FontManager` RuneScape fonts for headings and names. Build on the existing `Labels` and
    `ThemeColours` helpers instead of duplicating them. Then switch the five existing blocks (heading, text, separator,
    message, links) to the kit. Keep each class short.
3b. **Icons from the game client.** Skill icons come from RuneLite's built-in skill icon source (map the WOM metric
    name, e.g. `woodcutting`, to the client's skill), so they need no download. Boss icons keep coming from
    `iconBase + metric + ".png"` through the existing image loader. Put this behind one small icon helper that blocks
    call with a metric name and a size. (Later phases will use item icons by item ID for bingo tiles, which the client
    also provides.)
4. **Time formatting helper** (dates and durations as above), then the **`events` block**, built from the UI kit. Recompute running or
   upcoming from `startsAt` and `endsAt`, not from `status`.
5. **`competition` block** (side and hero layouts), built from the UI kit (`IconCaption`, `Card`).
5a. **Competition page links** (added mid-phase). Optional `skill_page` and `boss_page` settings on the `competition`
    block, e.g. `skill_page=tab_sotw boss_page=tab_botw`. A competition whose type has a page set becomes clickable
    (hand cursor, hover highlight) and opens that page in the dropdown. Without the settings, or if the page does not
    exist or is hidden, nothing is clickable. Give `Card` the click and hover behaviour so links and competitions share it;
    it acts when the left button is released over the card, so a small mouse movement does not cancel the click. The
    `events` block takes the same two settings, but only running events are clickable (an upcoming skill would open a
    page still showing the current one).
    Leave the settings off `which=upcoming` blocks (those pages show the current competition). Visibility is one check
    in the panel, shared by the dropdown and page links, so task 9 only needs to change that check.
6. **WOM client and `leaderboard` block**, built from the UI kit (`MedalRow` for the podium). Cache each competition's standings for 5 minutes. Resolve `current_skill`
   and `current_boss` from `competitions.current`. Show "Loading standings..." while fetching and a muted error line on
   failure.
7. **Live updates.** While the panel is visible: update countdown text every 60 seconds, refresh the endpoint every
   10 minutes, and refetch standings when their cache expires. Stop all timers when the panel is hidden or the plugin
   shuts down.
8. **Saved copy.** After each good load, save the endpoint JSON to disk (through RuneLite's `Filepath` utility, as
   `AGENTS.md` requires, with `internalName` set on the plugin). On start-up or after a failed refresh, show the saved
   copy with a small muted "Last updated hh:mm" line.
9. **Page visibility.** Hide pages whose `rank` is set (rank-gated pages arrive in Phase 3). Keep respecting `show`.
10. **Ready for the Plugin Hub.** The owner will supply a proper sidebar icon and a repository-root `icon.png` for the
    Hub listing; wire in the sidebar icon when provided. Update `README.md` with a short description and the services
    the plugin contacts (the clan's Google Apps Script endpoint, Wise Old Man, and GitHub for skill icons). Remove any
    leftover debug code (the Debug settings section stays until submission; see `docs/SUBMISSION.md`).
11. Moved: the checks that were here now live in `docs/SUBMISSION.md`, to do only when the owner decides to submit.

## Out of scope for Phase 1

`player`, `table`, `stat`, `countdown` and `image` blocks, login messages, chat commands, notifications, WOM update on
logout, rank checks, bingo and Hall of Fame. These are Phase 3 and later.
