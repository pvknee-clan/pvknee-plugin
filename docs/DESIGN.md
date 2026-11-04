# PvKnee Clan Plugin: Design

Oct 31, 2026 · @Jake

## Summary

A RuneLite plugin called PvKnee that members install from the Plugin Hub and use straight away, with no API key, sheet ID or setup. The clan's existing Google Sheet stays the control centre: admins edit pages, messages, themes, events and the competition rotation there, and the plugin renders them as native panels.

**Goals**

- **Zero setup for members.** Install, click the icon, done. Personal preferences only, on by default except features RuneLite requires to be opt-in.
- **The sheet decides content, the plugin decides behaviour.** Most changes stay sheet edits that need no Plugin Hub review.
- **More than a sheet can do.** Live Wise Old Man data, updating WOM on logout, login messages, chat commands, notifications and bingo team icons.
- **Abstraction first.** Pages are built from generic blocks, so new pages rarely need new code.

**Non-goals**

- A general plugin for every clan. It is built for PvKnee; other clans are not supported.
- A clan server or database. The only services are Google (sheet and Apps Script), Wise Old Man and optionally Discord.
- Replacing the Apps Script. It keeps running the rotation, archiving and scheduling.
- Anything that automates gameplay or gives an in-game advantage.

## Lessons from other clan plugins

Every clan plugin reviewed solves the same problem: a member opens a sidebar and sees what the clan is doing. They differ in where the data lives and how much is hard-coded. Flux is directly descended from Clan Events: it carries Maz's copyright and reuses his dropdown code. Terpinheimer in turn borrows Flux's UI class.

| Plugin | Where data comes from | Notable design | What we take or avoid |
| --- | --- | --- | --- |
| [Clan Events](https://github.com/cmsu224/clan-events) (Maz, \~89k installs) | Google Sheets API; each user enters a sheet ID and API key | A fixed list of 7 pages in code; each tab's rows are rendered as HTML | Avoid the fixed page list and the per-user key. Keep the refresh button and its clear error state. |
| [Flux](https://github.com/abristow3/Flux-Runelite-Plugin) | Sheets API with the key and sheet ID written into the code; WOM API | A CONFIG tab copied into hidden plugin settings; WOM competitions found by title; `!SOTW` / `!BOTM` commands; login message; admin card shown only to Administrator rank and above | Take the config-tab pattern, the commands, the login message and the rank-gated admin view. Avoid publishing a key in public code and the hard-coded SOTW, BOTM and Hunt cards. |
| [Terpinheimer](https://github.com/FadedOSRS/terpinheimerccplugin) | Own website plus WOM | Remote config for announcements and rank permissions; updates WOM on logout; Discord webhooks; attendance tracking | Take the logout WOM update and rank-title permissions. Avoid committing secrets: a working Discord webhook URL is visible in its public source. |
| [Final Boss](https://github.com/orgonag/fbclan-plugin) | Own Supabase database plus WOM | Membership checked against the clan's WOM group; one shared colour theme class; LFG, drop log and PB boards | Take the WOM-group membership check and a single theme class. Avoid running our own database. |
| [Ironforged Clan Events](https://github.com/y-u-m-e/Event-List) | Clan's own API | Team badges above players' heads and next to names in chat, from a roster | Take the team-icon approach for bingo (11 by 11 px chat icons plus an overhead overlay). |
| [Anvil](https://github.com/AhmedFathy2001/anvil-plugin) | Anvil's website | One bootstrap call returns everything; last good response cached on disk; conditional requests; bingo tiles credited automatically; tamper-evident screenshot overlay | Take the single bootstrap call, the disk cache and conditional requests. Bingo auto-credit is a later-phase idea. |

**What this means for us**

- **Pages from the sheet, not from code.** Clan Events and Flux both hard-code their pages, so every new page needs a release. Our `tab_` discovery avoids that.
- **No key in the code.** Flux and Terpinheimer show how easily secrets leak into public repositories. Our Apps Script endpoint needs no key.
- **One call, cached.** Like Anvil, fetch everything in one request and keep the last good copy, so the panel still shows something when Google or WOM is slow.
- **Rank-aware extras are normal.** Flux and Terpinheimer both gate admin features by in-game clan rank, which the RuneLite API exposes directly.

## Architecture

The sheet and Apps Script stay as they are; the plugin is a new front end that reads one endpoint and talks to Wise Old Man directly.

[Diagram: architecture · sheet, script, plugin, WOM, Discord]

Admins only ever touch the sheet. The script turns it into JSON and keeps running the rotation; each member's plugin fetches that JSON plus live standings from WOM, so there is no clan server and nothing for members to configure.

## Pages

Any sheet tab whose name starts with `tab_` becomes a page in the plugin's dropdown, in the same left-to-right order as the tabs. Only `tab_home` is required; it always opens first.

**Discovery rules**

- `tab_*` is a page. Everything else (Settings, `data_*`, `sotw_data`, `events_data`) is never shown.
- **Order** follows tab position, so dragging a tab reorders the dropdown.
- **Missing `tab_home`:** the first `tab_` page opens instead, and admins see a warning line.
- **Renaming a tab** changes its id, so links that point at it by name must be updated too.

**Page header (rows 1 to 4 of every page tab)**

| Row | Key | Example | Default when blank |
| --- | --- | --- | --- |
| 1 | title | Bingo: Team Board | Tab name tidied: `tab_my_bingo` becomes "My bingo" |
| 2 | icon | WOM metric name or image link | No icon |
| 3 | show | TRUE, FALSE or a formula | TRUE |
| 4 | rank | Minimum clan rank, e.g. Administrator | Everyone |

Row 5 onwards holds the page's blocks, one per row (see Block catalogue).

- **Drafts:** set `show` to FALSE to build a page without members seeing it. A formula works too, so a page can appear only during an event.
- **Admin pages:** `rank` hides a page from members below that clan rank, read from the game client. This is how Flux and Terpinheimer gate their admin views.

**Built-in pages**

Some pages are too interactive for blocks, so they are written in code but still read their content from the sheet.

| Page | Lives in code because | Reads from the sheet |
| --- | --- | --- |
| Bingo | Phase 4: now a block page (`tab_bingo`) rather than built-in | A separate bingo sheet per event (Settings, Tiles, Teams, Log), linked from the main sheet's `Active bingo sheet` cell |
| Hall of Fame | Sorting and filtering past results | `comp_history` |

- A Settings row per built-in page (`Bingo page: on/off`) shows or hides it.
- Built-in pages are placed among sheet pages by a `position` setting, so Bingo can sit right after Home during bingo season.

## Block catalogue

A page is a list of blocks, one per row from row 5: column A is the block type and columns B onwards are `key=value` settings. Every block also accepts `show` (TRUE, FALSE or a formula) and `rank`, the same as a page.

| Block | What it draws | Main settings | Data source |
| --- | --- | --- | --- |
| header | The page header: centred title, optional subtitle or a competition's dates, optional pill that can hop to a world | `title`, `size`, `subtitle`, `dates` (current_skill, current_boss), `pill`, `icon`, `hop` | Inline or sheet placeholders |
| heading | A section title in the accent colour | `text`, `size` (1 to 3) | Inline |
| text | One or more lines of plain or lightly formatted text | `text` (supports **bold**, *italic*, line breaks), `colour`, `align` | Inline or a cell |
| message | A highlighted announcement box | `text` or `cell`, `title` | A cell, e.g. Settings!B7 |
| separator | The theme's divider line | `style` (dashes, line, space) | None |
| competition | A card per competition: icon, name, countdown, top 3 | `which` (current, upcoming, both), `type` (skill, boss, all), `top` (0 to 3), `layout` (side, hero), `skill_page` / `boss_page` (optional page id, e.g. `tab_sotw`: clicking that competition opens the page; leave off upcoming blocks) | WOM, plus the rotation in Settings |
| leaderboard | A ranked table of one competition | `competition` (current skill, current boss, or a WOM id), `rows` (default 20), `value` (gained, total), `prefix` | WOM |
| events | Running or upcoming events with times and icons | `when` (running, upcoming, both), `limit`, `show_time` | Events list in Settings |
| table | Any range from the sheet as a table | `range` (e.g. `data_raids!A1:D20`), `columns`, `widths`, `header` | Sheet range |
| links | Buttons, each with an optional icon | `range` (default Settings links), `style` (full, compact, grid) | Settings links |
| image | A picture or banner | `src` (link or WOM metric), `width`, `height` | Link |
| stat | One big value with a muted label above it, in a card | `label`, `value` (may use sheet placeholders), `title` | Inline or sheet placeholders |
| countdown | A label, the date and a live countdown; orange in the last 24 hours; nothing once passed | `label`, `until` (an ISO time, e.g. 2026-11-14T17:00:00Z), `title` | Inline or a cell |
| player | Your own place and gain in a competition (a card when on the board, a quiet line when not, nothing when logged out); your row is highlighted in the standings | `competition` | WOM, using your logged-in name |

**Rules that keep it flexible**

- **Unknown blocks are skipped.** An older plugin version ignores block types it does not recognise, so admins can prepare pages before an update ships.
- **Missing settings fall back to defaults.** A row with only `competition` in column A still draws a sensible card.
- **Colours always come from the theme** unless a block sets `colour`. Skill names keep their own colours from the Icon colours table.
- **Data sources are shared.** WOM, sheet ranges and the events list are fetched once per refresh, and every block reads from the same cache.

**Example rows**

| A | B | C | D |
| --- | --- | --- | --- |
| message | cell=Settings!B7 | title=Clan Message |  |
| competition | which=current | layout=side | top=0 |
| events | when=upcoming | limit=4 |  |
| links |  |  |  |

## Settings split

Personal preferences live in RuneLite's normal plugin settings and are saved per player; clan-wide settings live in the sheet and only admins change them.

| Setting | Where | Default | Why there |
| --- | --- | --- | --- |
| Login message | Plugin settings | On | A personal annoyance level |
| Competition notifications (start, ending soon, final results) | Plugin settings | On | Personal |
| Update Wise Old Man on logout | Plugin settings | Off (opt-in, with RuneLite's third-party warning) | RuneLite requires features that contact a third-party server to be off by default |
| Team icons in chat and above heads | Plugin settings | On | Personal |
| Sidebar position | Plugin settings | Middle | Personal |
| Theme override | Plugin settings | Use clan theme | Lets someone pick a theme they can read more easily |
| Theme, clan message, links, events, rotation | Sheet (Settings tab) | Set by admins | The same for everyone |
| Which pages show and who sees them | Sheet (page headers) | Set by admins | The same for everyone |
| Chat command replies and login wording | Sheet (`data_messages`) | Set by admins | Wording changes without a release |
| Data source override | Hidden plugin setting | Blank | Point at a test sheet without releasing an update |

**Themes**

- The sheet's Themes table and the Theme dropdown stay as they are. The plugin reads the four colours (header, accent, value, muted) from the endpoint.
- The plugin maps them onto every component: headings use accent, names use value, captions use muted, page titles use header.
- Medal colours (gold, silver, bronze) and skill colours stay fixed across themes, as on the current pages.
- "Rotate hourly" from the earlier idea becomes a Theme option the endpoint resolves, so every member sees the same theme at the same time.

## Endpoint format

The existing Apps Script is published as a web app that anyone can call without signing in. One request returns the whole panel as JSON, so a member's refresh is a single call.

```json
{
  "version": 1,
  "generatedAt": "2026-10-31T12:00:00Z",
  "clan": { "name": "PvKnee", "tagline": "...", "world": 509, "signOff": "Iron Aeria" },
  "theme": { "name": "Ember", "header": "#FF7043", "accent": "#FFCA28", "value": "#FFE082", "muted": "#A1887F", "separator": "- - - -" },
  "pages": [
    { "id": "tab_home", "title": "Home", "icon": null, "show": true, "rank": null,
      "blocks": [ { "type": "message", "settings": { "title": "Clan Message", "text": "..." } } ] }
  ],
  "builtIn": { "bingo": { "show": false, "position": 2 } },
  "competitions": {
    "current":  [ { "type": "skill", "name": "Woodcutting", "metric": "woodcutting", "womId": 163518, "startsAt": "...", "endsAt": "...", "colour": "#6AA84F" } ],
    "upcoming": [ { "type": "skill", "name": "Fletching", "metric": "fletching", "womId": 163520, "startsAt": "..." } ]
  },
  "events": [ { "name": "Bingo", "start": "...", "end": "...", "kind": "event" } ],
  "links": [ { "label": "Discord", "url": "...", "icon": "..." } ],
  "messages": { "login": "PvKnee: SOTW {skill}, {timeLeft} left", "cmd_sotw": "..." }
}
```

**Rules**

- **Versioned.** `version` only goes up when a change would break older plugins. Plugins ignore fields they do not know.
- **Standings are not in it.** Leaderboards are fetched by each member straight from WOM using `womId`, so the endpoint stays small and every member uses their own WOM allowance.
- **Server-side cache.** The script keeps the built JSON for 60 seconds, so 70 members refreshing at once cost one sheet read.
- **Client-side cache.** The plugin keeps the last good copy on disk, like Anvil, and shows it with a "last updated" note if a refresh fails.
- **Refresh timing.** On panel open, on the Refresh button, and every 10 minutes while the panel is visible. No polling while the panel is closed.
- **Fallback.** If the web app is unreachable, the plugin shows its saved copy of the last good load with a "Last updated" note. A direct read through Google's export link was considered and dropped as unnecessary.

## UI rules

Settled in Phase 2. Every later block and page follows these, so the panel keeps one look without re-deciding it.

**The kit.** Blocks arrange kit pieces; they never style themselves. Anything needed twice becomes a kit piece or an option on one.

| Piece | Use |
| --- | --- |
| `Card` | Rounded panel with a subtle border; `onClick` adds the hand cursor and hover; a borderless variant for clickable areas inside a card |
| `PageHeader` | The top of every page: centred title, optional subtitle (text or a competition's dates) and pill, grouped tightly |
| `Section`, `SectionHeader` | A left-aligned title above its content, with an optional muted aside on the right; also the centred page title |
| `ListCard`, `EntryRow` | A list in one card with thin dividers; each entry has a fixed icon slot, a name, muted lines and an optional bar |
| `TimeBar`, `DayBar`, `LiveLabel` | A draining day-segmented bar with its time directly under it; text that updates in place each minute |
| `RankTable`, `PlacePill` | Standings: place pills (1st, 2nd, 3rd in medal colours) and names for the top places, plain ordinals below, right-aligned values |
| `EmptyState` | Stands in for an empty list (muted icon, bold line, muted line), with a quiet form for loading and errors |
| `IconButton`, `LinkButton`, `Pill` | Square icon buttons, text buttons that open a page, a small rounded label |
| `Notice` | Status lines under the top bar: errors, when the data is from, a newer version |
| `Divider`, `Spacing`, `Fonts`, `Glyphs` | A faint line for a real break; the spacing rhythm; fonts by role; built-in line icons |

**Spacing.** 14 px between blocks, 6 px from a section title to its card, 4 px between items, 10 px padding inside cards, 8 px corner rounding. No dashed separators: spacing and section titles separate sections, and a faint line marks a real break only.

**Colour.** Header: the page title. Accent: section titles, card labels and the active control. Value: names, values and button text. Muted: captions, times and asides. Skill names keep their own colours; medal colours (gold, silver, bronze) and the ending-soon orange never change with the theme.

**Type.** RuneScape fonts throughout. Names and body text share the regular font and size; the small font is for captions, times and asides only. Bold names (e.g. hero names) use the regular font bolded by Java, because the RuneScape bold font draws "W" like "VV".

**Layout.** Every page starts with a page header: a centred title, an optional subtitle (e.g. the competition's dates) and an optional pill, grouped tightly. One section-title style: every titled block has a bold section title above its card; titles come from the sheet. Section titles are left-aligned; hero content (the clan name, a featured competition) is centred. Alignment changes only when the content needs it.

**Timing.** Every timed item carries its own times, and its bar sits at the bottom with its time directly under it: the time left, on the right. Only the hero layout also shows the end date, on the left. Bars are split into days and drain as time passes. A section's shared time sits beside its title (the Challenges card has no bar). In an item's last 24 hours its time turns orange and reads "Ends in ...". Upcoming items and items without a fixed end get no bar. Dates near now read "Today" or "Tomorrow". Times are in the member's local time unless they choose UTC in settings.

**States.** While loading, the refresh icon spins; content never shifts for a loading line. Problems and stale data appear in the notice card. An empty list shows an empty state with an action, never a bare line. A block with nothing to show draws nothing, title included.

**Chat.** Chat lines are short, separated with " | ", and worded from the sheet's `data_messages`. Filled-in values use RuneLite's own game-message highlight colour, so members' chat colour settings and the chatbox style decide the look. Only the member sees them.

**Interaction.** Anything clickable is a card with the hand cursor and hover, and acts on release. The home button is outlined in the accent while Home is shown. Opening a page starts at the top; live updates keep the scroll position.

## Example page layouts

These mock-ups show where the design is heading: four sheet pages that admins control entirely, then three built-in pages and the personal settings panel. Names come from the clan's group; all numbers are made up.

[Diagram: mock-ups · Home, Skill of the Week, Boss of the Week, Events]

The sheet rows behind these four pages (rows 1 to 4 are the page header, blocks start at row 5):

| Page tab | Block rows (column A, then settings) |
| --- | --- |
| `tab_home` | header · message `cell=Settings!B7` · competition `which=current layout=side top=0` · events `when=upcoming limit=4 title=Coming Up` · links · text `cell=Settings!B6 style=signoff` |
| `tab_sotw` | competition `type=skill which=current layout=hero` · player `competition=current_skill` · leaderboard `competition=current_skill rows=9 podium=3 prefix=+` · links `range=Settings!D6:F6` |
| `tab_botw` | competition `type=boss which=current layout=hero` · player `competition=current_boss` · leaderboard `competition=current_boss rows=7 podium=3` · heading `text=Last week` · text `cell=data_history!B2` |
| `tab_events` | events `when=running` · countdown `label=Bingo starts in until=data_events!B3` · links `range=data_links!A2:C2` · events `when=upcoming limit=5` |

[Diagram: mock-ups · Hall of Fame, Clan Info, Bingo, plugin settings]

| Page | Built from | Notes |
| --- | --- | --- |
| Hall of Fame | Built-in; reads `comp_history` | Filters by type (skill, boss) and period. "Biggest week" needs gained XP stored per competition, which the archive already writes. |
| `tab_rules` (Clan Info) | Sheet blocks: heading · text · table `range=data_ranks!A2:C10` · stat `label=Members value=wom:members` · links | Shows how a purely informational page needs no code. |
| Bingo | Block page `tab_bingo`, fed by the active bingo sheet (Phase 4) | Team switcher, board with tile detail, teammates and standings; hidden when no bingo sheet is set. Mock-ups: `BingoBoard`, `BingoStates`. |
| Plugin settings | RuneLite's own settings panel | Personal switches only; everything clan-wide stays in the sheet. |

## In-game features

These are what a sheet alone cannot do, and the main reason to build the plugin. Each can be switched off in the plugin's settings, and all wording comes from the sheet.

| Feature | What the member sees | How it works | Precedent |
| --- | --- | --- | --- |
| WOM update on logout | Nothing; their WOM profile is current | Posts to WOM's player update endpoint for the logged-in name when they log out | Terpinheimer, XP Updater |
| Login message | "PvKnee: SOTW Woodcutting, 6d left. BOTW Cerberus." in chat once per login | Template from `data_messages`, filled from the endpoint | Flux |
| Chat commands | Typing `!sotw` shows the skill, time left and your rank | Reads current standings from WOM; replies only to you, never in public chat | Flux (`!SOTW`, `!BOTM`) |
| Challenge messages | A chat message only you see when a challenge starts, has 1 hour left, or ends (each can be turned off) | Competition times from the last load, plus one quiet load at login | Built (Phase 3) |
| Team icons | A dot in the team colour in place of the clan rank icon in clan chat, before names in public chat, and beside names above heads (not in the Wilderness or on PvP worlds), only while a bingo runs and only for players on its board | Roster from the bingo sheet's Teams tab; RuneLite's chat icon support plus an overhead overlay; local display only | Ironforged Clan Events |
| Team overlay | A movable on-screen box with your team name and the UTC date and time, for screenshot proof | Shown only while you are on a team in a running bingo; setting on by default | Common in bingo plugins |
| World hop | Clicking "World 509" on Home hops there (or sets the login world when logged out) | RuneLite's world list and hop call, on one deliberate click; game hop rules still apply | RuneLite's World Hopper |
| Membership check | Admin pages and extras only for clan members | In-game clan rank from RuneLite, with the WOM group as a fallback | Final Boss, Flux |

**Deliberately left out**

- Sending player data to any service other than WOM, Google and Discord.
- Live player locations or anything resembling a clan map, to avoid a Plugin Hub warning.
- Automatic posts to public chat. Command replies show only to the person who typed them.

## Plugin Hub and maintenance

Sheet edits never need RuneLite's approval; every change to the plugin's code does. Content therefore belongs in the sheet, and code changes are batched into planned releases.

**Review**

- Each release is a pull request to RuneLite's plugin-hub repository pointing at a commit in our repo. A reviewer checks it before members receive it.
- Single-clan plugins are accepted: Flux, Terpinheimer, Final Boss and Ironforged are all on the Hub.
- RuneLite's rules: any setting for a feature that contacts a third-party server must be off by default and carry the standard IP-address warning. Because the panel itself reads our endpoint and WOM, expect the listing to carry that warning too; many accepted plugins do. We only talk to Google, WOM and (optionally) Discord.
- Nothing that automates gameplay or gives an in-game advantage.

**Code hygiene**

- No keys, webhook URLs or passwords in the code. Terpinheimer's public repo exposes a working Discord webhook; Flux's exposes a Sheets API key.
- BSD-2 licence, as Plugin Hub expects. If we reuse Clan Events code, keep Maz's copyright notice, as Flux does.
- Small, readable classes per block type, so another member can add a block without understanding the whole plugin. Reuse first: blocks are assembled from a shared UI kit (card, section header, medal row, progress bar, icon caption) and shared helpers; anything needed twice goes into the kit rather than being copied.
  - Leave the template's CLAUDE.md and AGENTS.md (RuneLite's rules for coding assistants) unchanged; project notes live only in a local docs/ folder.
  - docs/ and .claude/ are excluded from Git and never committed.
  - Commits use the owner's Git identity only, with no co-author or attribution lines.

**Ownership**

- Jake is the sole maintainer: the repository is on his GitHub and the Apps Script runs under his Google account. Without him there are no updates or fixes, which is accepted.
- A trusted admin has edit access to the sheet, so content (announcement, events, themes, pages) can be changed without any technical knowledge.
- The sheet ID and endpoint are clan assets: document them in the repo's README, and treat the sheet as permanent.

## Phase plan and open questions

Phase 1 matches what Clan Events shows today. Phase 2 settles the design before more features are built on the UI kit. Plugin Hub submission happens when the owner decides; every later phase waits for the one before it to be done.

[Diagram: phase plan · 5 phases, 5 gates. Phase 0 Foundation (done) → Phase 1 Parity with Clan Events (done) → Phase 2 Design (done) → Phase 3 In-game features (done: your place, login and challenge messages, !sotw/!botw, opt-in WOM update on logout, rank-gated pages, header/table/stat/countdown/player blocks; world hop later removed) → Phase 4 Bingo (done: team boards, tile details with parts, members, place pills, all states, team icons and overlay) → Phase 5 Review fixes (independent code review: licence, no world hop, standings from the sheet, safe text and images, team dots the core way) → Later (Hall of Fame, admin tools, theme rotation). Gates: endpoint draws Home; Phase 1 stable in the dev client; pages and UI kit agreed with UI rules written; on the Plugin Hub and installed by most active members; one full bingo run without manual scoring.]

**Open questions**

- [ ] Plugin name and sidebar icon: "PvKnee" with the clan logo?
- [x] Ownership: settled. Jake is the sole maintainer; a trusted admin edits the sheet.
- [x] Clan Events: settled. Its sheet tabs stay until the plugin is approved on the Plugin Hub, then they are deleted.
- [ ] Bingo scoring: fully automatic, or automatic with admin approval for drops?
- [ ] Should the Apps Script get a Wise Old Man API key now, given the rate-limit error on 30 October?
- [ ] Which existing pages move first: Home, Skill of the Week and Boss of the Week only, or the placeholder pages too?

**Sources**

- [Clan Events source](https://github.com/cmsu224/clan-events)
- [Flux source](https://github.com/abristow3/Flux-Runelite-Plugin)
- [Terpinheimer source](https://github.com/FadedOSRS/terpinheimerccplugin)
- [Final Boss source](https://github.com/orgonag/fbclan-plugin)
- [Ironforged Clan Events source](https://github.com/y-u-m-e/Event-List)
- [Anvil source](https://github.com/AhmedFathy2001/anvil-plugin)
- [Wise Old Man API rate limits](https://docs.wiseoldman.net/api)
