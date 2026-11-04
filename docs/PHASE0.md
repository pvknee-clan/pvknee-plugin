# PvKnee plugin: Phase 0 tasks

Read `docs/SESSION.md`, then `docs/DESIGN.md`. This file is the task list for Phase 0 only.
Work through the tasks in order, one at a time, and run the plugin in the dev client after each.

Follow `docs/SESSION.md` for the repository rules, working style and commit format.

## Goal (the Phase 0 gate)

The dev client shows a PvKnee icon in the sidebar. Opening it loads the clan endpoint, fills a dropdown with the
pages, and shows each page's blocks as simple text. Nothing fancy yet: this proves the data pipe end to end.

## Fixed facts

| Item | Value |
| --- | --- |
| Endpoint (GET, no auth, returns JSON) | `https://script.google.com/macros/s/AKfycbyuOXHeAB-dlWG4kSg1G2T4mXPPy13vyphLPGRH3iZRUiEKKmvToJmHm6M33vb_OSUe3Q/exec` |
| Redirects | The `/exec` URL answers with a 302 to `script.googleusercontent.com`. OkHttp follows it by default; keep that on. |
| Fresh copy (testing only) | Append `?nocache=1` to skip the 60-second server cache |
| Plugin name | PvKnee |
| Config group | `pvknee-clan` |
| Licence | BSD-2 (keep the template's licence) |

## Endpoint data contract (version 1)

Top level: `version`, `generatedAt`, `iconBase`, `clan`, `theme`, `pages`, `builtIn`, `competitions`, `events`,
`links`, `messages`. Ignore any field you do not recognise; never fail on unknown fields or unknown block types.

```json
{
  "version": 1,
  "generatedAt": "2026-10-31T13:06:10.017Z",
  "iconBase": "https://raw.githubusercontent.com/wise-old-man/wise-old-man/master/app/public/img/metrics/",
  "clan": { "name": "PvKnee", "tagline": "...", "world": "509", "clanChat": "PvKnee", "signOff": "Iron Aeria", "message": "..." },
  "theme": { "name": "Ember", "header": "#FF7043", "accent": "#FFCA28", "value": "#FFE082", "muted": "#A1887F", "separator": "- - - -" },
  "pages": [
    { "id": "tab_home", "title": "Home", "icon": null, "show": true, "rank": null,
      "blocks": [
        { "type": "heading", "settings": { "text": "PvKnee", "size": "3", "colour": "header" } },
        { "type": "text", "settings": { "text": "Home world 509" } },
        { "type": "separator", "settings": {} },
        { "type": "message", "settings": { "title": "Clan Message", "text": "..." } },
        { "type": "competition", "settings": { "which": "current", "layout": "side", "top": "0" } },
        { "type": "events", "settings": { "when": "upcoming", "limit": "4", "title": "Coming Up" } },
        { "type": "links", "settings": { "title": "Links" } }
      ] }
  ],
  "builtIn": {},
  "competitions": {
    "current":  [ { "type": "skill", "name": "Woodcutting", "metric": "woodcutting", "womId": 163518,
                    "startsAt": "2026-10-30T17:00:00.000Z", "endsAt": "2026-11-06T17:00:00.000Z", "colour": "#6AA84F" } ],
    "upcoming": [ { "type": "boss", "name": "Chambers of Xeric (CM)", "metric": "chambers_of_xeric_challenge_mode",
                    "womId": 163650, "startsAt": "...", "endsAt": "...", "colour": null } ]
  },
  "events": [ { "name": "Woodcutting", "fullName": "Skill of the Week: Woodcutting", "kind": "skill",
                "metric": "woodcutting", "colour": "#6AA84F", "startsAt": "...", "endsAt": "...",
                "hasTime": true, "status": "running" } ],
  "links": [ { "label": "Discord", "url": "https://discord.com/channels/...", "icon": "https://...png" } ],
  "messages": {}
}
```

Notes:
- All block settings values are strings. Parse numbers and booleans yourself, with defaults.
- `pages` are already in display order. Only show pages where `show` is true.
- Times are ISO-8601 UTC. Recompute running or upcoming from the times rather than trusting `status`.

## Tasks

1. **Rename the template.** Package `com.pvknee` (or similar), classes `PvKneePlugin` and `PvKneeConfig`, config
   group `pvknee-clan`, and update `build.gradle`, `settings.gradle` and `runelite-plugin.properties`
   (displayName PvKnee, a short description, tags such as clan, sotw, botw, events, wom). Follow `AGENTS.md`.
2. **Sidebar panel.** Add a `NavigationButton` with a placeholder 16x16 icon and a `PluginPanel`. Remove the
   template's greeter behaviour.
3. **Data models and client.** Plain model classes for the contract above (panel, theme, page, block with a
   `Map<String,String>` of settings, competition, event, link). Fetch with the injected `OkHttpClient` using
   `enqueue()`, parse with the injected `Gson`, and update Swing on the event dispatch thread.
4. **Panel layout.** A top row with a page dropdown and a Refresh button. Below it, a scrollable content area that
   lists the selected page's blocks as simple text, e.g. `heading: PvKnee`, `competition (which=current)`.
   Apply the theme colours to these text lines if easy; proper block renderers come in Phase 1.
5. **Loading and errors.** Show "Loading..." while fetching. On failure show "ERROR: Couldn't load the clan panel.
   Press Refresh to try again." in red (no separate Retry button; Refresh does the same), and log the error with `log.debug`. Keep the previous data if a refresh fails.
6. **When to fetch.** On first opening the panel and on the Refresh button. No background polling in Phase 0.
7. **Hidden test setting.** A hidden config item `endpointOverride` (blank by default) that replaces the built-in
   endpoint URL, for pointing at a test sheet without releasing an update.

## Out of scope for Phase 0

Wise Old Man calls, icons, real block renderers, disk cache, login messages, chat commands, notifications and
bingo. These are Phase 1 and later in `docs/DESIGN.md`.

## Rules to keep in mind

- No API keys, webhook URLs or passwords anywhere in the code. The endpoint needs none.
- Any future setting for a feature that contacts a third-party server must be off by default and carry RuneLite's
  standard warning (see `AGENTS.md`).
- Never block the client thread with network calls.
