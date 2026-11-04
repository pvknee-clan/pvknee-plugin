# PvKnee

A sidebar panel for the PvKnee clan. Install it, click the icon, and see what the clan is doing: no API key, sheet ID or
setup needed.

**Install:** in RuneLite, open the Configuration panel (the wrench icon), click **Plugin Hub**, search for **PvKnee**
and click Install. The PvKnee icon then appears in the sidebar.

Anyone can install it and view the clan's pages, but it is made for PvKnee members: your place in the challenges,
rank-limited pages and bingo team icons only mean something if you are in the clan.

- **Pages from the clan sheet.** Home, Skill of the Week, Boss of the Week and any other page the clan's admins add,
  in the clan's theme colours.
- **Challenges.** This week's skill and boss with their icons and the time left. Click one to open its page.
- **Standings.** One standings table for each challenge, from Wise Old Man via the clan sheet, which copies them
  every 5 minutes. Players level on the same gain share a place.
- **Events.** What is happening now and what is coming up, in your local time (or UTC if you prefer).
- **Links.** One-click buttons to the clan's Discord and other pages. Only pages on Discord, Wise Old Man, the Old
  School wiki and runescape.com are opened, so a changed link in the clan sheet cannot send you anywhere else.
- **Your place.** On the challenge pages, a card with your own place and gain, and your row highlighted in the
  standings.
- **Admin pages.** Pages the clan limits to a rank, e.g. Administrator, only show for members of that rank or above.
- **Login message** (off by default). When you log in, a short message about this week's challenges. Only you see it.
- **Chat commands.** Type `!sotw` or `!botw` to see the challenge, its time left and your place and gain. Others see
  your command as usual; only you see the reply.
- **Clan bingo.** When the clan runs a bingo, a page with each team's board, coloured by progress, and the details of
  any tile you select: what it needs, who got what and when it was completed. Switch between teams' boards, see
  the standings and, at the end, the winner. View only: the data comes from the clan's bingo sheet, and submissions
  stay in Discord.
- **Bingo team icons.** While a bingo runs, a dot in each team's colour in front of the names of clan members on a
  team in clan and public chat (after the clan rank icon), and beside the names RuneLite's Player Indicators plugin
  shows for them above their heads. Only clan members are marked, and no dots show in the Wilderness, on PvP worlds or
  in PvP areas. Only you see them; your messages are sent as usual.
- **Bingo team overlay.** While a bingo runs and you are on a team, a small on-screen box with your team and the date
  and time in UTC, e.g. `Team Ember · Tue 3 Nov 2026, 21:35 UTC`, so screenshots for submissions show when they were
  taken. Move it like any RuneLite overlay.
- **More page blocks.** Tables, big numbers and countdowns the clan's admins can add to any page.

The panel keeps itself up to date while it is open. While it is closed, the plugin only loads the clan data once when
you log in, for the login message and the bingo team icons and overlay (not at all if those are turned off), and when
you use `!sotw` or `!botw` with data more than 10 minutes old.

## Settings

- **Display:** **Show times in UTC** (off): all times in UTC instead of your local time.
- **Chat:** **Login message** (off): this week's challenges in chat once each time you log in.
- **Wise Old Man:** **Update on logout** (off): updates your Wise Old Man profile when you log out after
  making progress, so your place in the challenges stays up to date.
- **Bingo** (all on): **Team icons in chat**, **Team icons above heads** and
  **Bingo team overlay**.

## Services the plugin contacts

| Service | Address | What for | When |
| --- | --- | --- | --- |
| The clan's Google Apps Script | `script.google.com` (redirects to `script.googleusercontent.com`) | The panel's pages, theme, competitions and their standings, events, links and bingo, built from the clan's Google Sheets | When the panel opens, on Refresh, every 10 minutes while it is open, once at login and for `!sotw` / `!botw`; at most once a minute after a good load, and less often after a failed one |
| Wise Old Man | `api.wiseoldman.net` | A profile update for your username | Only with **Update Wise Old Man on logout** on, when you log out after progress (see Settings) |
| GitHub | `raw.githubusercontent.com` | Boss icons from Wise Old Man's public repository | Once per icon while the plugin is on |

Skill and item icons come from RuneLite itself; all other icons are built into the plugin.
Images are only ever loaded from `https://raw.githubusercontent.com`, at most 256 KB and 512 pixels a side; the
clan sheet cannot point the plugin at any other site.

## What leaves your computer

- **The request for the clan data**, to Google. It carries no player data: every member gets the same response.
- **Your username**, to Wise Old Man, only if you turn on **Update Wise Old Man on logout**, and only when it sends.
- **Requests for boss icons**, to GitHub. They carry no player data.

Nothing else is sent anywhere. Your clan rank, world and name are read from the game client and stay on your
computer, and the standings come with the clan data, so the plugin never looks anyone up. As with any web request,
each service can see your IP address.

## Stored on your computer

- The last clan data that loaded successfully, in RuneLite's plugin data folder, so the panel can still show
  something, with a "Last updated" time, when the clan's endpoint cannot be reached.
- With **Update Wise Old Man on logout** on, when each name was last sent, in RuneLite's settings, so it is sent at
  most once every 10 minutes even across restarts.

## Licence

BSD 2-Clause; see `LICENSE`.
