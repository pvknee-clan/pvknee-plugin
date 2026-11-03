# PvKnee

A sidebar panel for the PvKnee clan. Install it, click the icon, and see what the clan is doing: no API key, sheet ID or
setup needed.

- **Pages from the clan sheet.** Home, Skill of the Week, Boss of the Week and any other page the clan's admins add,
  in the clan's theme colours.
- **Challenges.** This week's skill and boss with their icons and the time left. Click one to open its page.
- **Live standings.** One standings table for each challenge, straight from Wise Old Man.
- **Events.** What is happening now and what is coming up, in your local time (or UTC if you prefer).
- **Links.** One-click buttons to the clan's Discord and other pages.
- **Your place.** On the challenge pages, a card with your own place and gain, and your row highlighted in the
  standings.
- **Hop to the clan world.** Click the world on Home to hop there (or, on the login screen, to pick it as your login
  world). The game's usual hop rules apply.
- **Admin pages.** Pages the clan limits to a rank, e.g. Administrator, only show for members of that rank or above.
- **Chat messages.** When you log in, a short message about this week's challenges, and a message when a challenge
  starts, has 1 hour left and ends. Only you see them.
- **Chat commands.** Type `!sotw` or `!botw` to see the challenge, its time left and your place and gain. Others see
  your command as usual; only you see the reply.
- **Clan bingo.** When the clan runs a bingo, a page with each team's board, coloured by progress, and the details of
  any tile you select: what it needs, who got what and when it was completed. Switch between teams' boards, see
  the standings and, at the end, the winner. View only: the data comes from the clan's bingo sheet, and submissions
  stay in Discord.
- **Bingo team icons.** While a bingo runs, a dot in each team's colour in front of its players' names in clan and
  public chat and in the Clan tab's member list (in clan chat and the member list, in place of the rank icon), and
  beside the names RuneLite's Player Indicators plugin shows for them above their heads (not in the Wilderness or on
  PvP worlds). Only you see them; your messages are sent as usual.
- **Bingo team overlay.** While a bingo runs and you are on a team, a small on-screen box with your team and the date
  and time in UTC, e.g. `Team Ember · Tue 3 Nov 2026, 21:35 UTC`, so screenshots for submissions show when they were
  taken. Move it like any RuneLite overlay.
- **More page blocks.** Tables, big numbers and countdowns the clan's admins can add to any page.

The panel keeps itself up to date while it is open. While it is closed, the plugin only loads the clan data once when
you log in, for the chat messages and team icons.

## Settings

- **Show times in UTC** (off by default): show all times in UTC, marked "UTC", instead of your local time.
- **Update Wise Old Man on logout** (off by default): when you log out, ask Wise Old Man to update your profile, so the
  challenge standings are more up to date. At most once every 10 minutes per name. This sends your username to Wise
  Old Man.
- **Challenge messages** (all on by default):
  - **Login message**: a message about this week's challenges once each time you log in.
  - **Challenge start**, **1 hour left**, **Challenge end**: a message at each of those moments while you are logged
    in.
- **Bingo** (all on by default):
  - **Team icons in chat**: team dots in clan and public chat and the Clan tab's member list.
  - **Team icons above heads**: team dots beside the players' names from Player Indicators. Needs RuneLite's Player
    Indicators plugin turned on and showing names for that player's group (yourself, clan members, others and so
    on); where it shows no name, there is no dot.
  - **Bingo team overlay**: the on-screen box with your team and the time in UTC.

## Services the plugin contacts

| Service | Address | What for | When |
| --- | --- | --- | --- |
| The clan's Google Apps Script | `script.google.com` (redirects to `script.googleusercontent.com`) | The panel's pages, theme, competitions, events, links and bingo, built from the clan's Google Sheets | When the panel is opened, on Refresh, and every 10 minutes while it is open; once when you log in (unless loaded in the last 10 minutes), for the chat messages; when you use `!sotw` or `!botw` and the data is more than 10 minutes old |
| Wise Old Man | `api.wiseoldman.net` | Competition standings (public data) | When a leaderboard or your place is shown, or you use `!sotw` or `!botw`, at most once every 5 minutes per competition |
| Wise Old Man | `api.wiseoldman.net` | A profile update for your username | Only with **Update Wise Old Man on logout** on: when you log out, at most once every 10 minutes per name |
| GitHub | `raw.githubusercontent.com` | Boss icons from Wise Old Man's public repository | Once per icon per session |

Skill and item icons come from RuneLite itself; all other icons are built into the plugin.

The plugin does not send your username or any other player data to these services, except your username to Wise Old
Man when you turn on **Update Wise Old Man on logout**. Your clan rank, world and name are read from the game client
and stay on your computer. As with any web request, each service can see your IP address.

## Stored on your computer

The last clan data that loaded successfully is saved in RuneLite's plugin data folder, so the panel can still show
something, with a "Last updated" time, when the clan's endpoint cannot be reached.
