# PvKnee

A sidebar panel for the PvKnee clan. Install it, click the icon, and see what the clan is doing: no API key, sheet ID or
setup needed.

- **Pages from the clan sheet.** Home, Skill of the Week, Boss of the Week and any other page the clan's admins add,
  in the clan's theme colours.
- **Challenges.** This week's skill and boss with their icons and the time left. Click one to open its page.
- **Live standings.** One standings table for each challenge, straight from Wise Old Man.
- **Events.** What is happening now and what is coming up, in your local time (or UTC if you prefer).
- **Links.** One-click buttons to the clan's Discord and other pages.

The panel keeps itself up to date while it is open, and only while it is open.

## Settings

- **Show times in UTC** (off by default): show all times in UTC, marked "UTC", instead of your local time.

## Services the plugin contacts

| Service | Address | What for | When |
| --- | --- | --- | --- |
| The clan's Google Apps Script | `script.google.com` (redirects to `script.googleusercontent.com`) | The panel's pages, theme, competitions, events and links, built from the clan's Google Sheet | When the panel is opened, on Refresh, and every 10 minutes while it is open |
| Wise Old Man | `api.wiseoldman.net` | Competition standings (public data) | When a leaderboard is shown, at most once every 5 minutes per competition |
| GitHub | `raw.githubusercontent.com` | Boss icons from Wise Old Man's public repository | Once per icon per session |

Skill icons come from RuneLite itself; all other icons are built into the plugin.

The plugin does not send your username or any other player data to these services. As with any web request, each
service can see your IP address.

## Stored on your computer

The last clan data that loaded successfully is saved in RuneLite's plugin data folder, so the panel can still show
something, with a "Last updated" time, when the clan's endpoint cannot be reached.
