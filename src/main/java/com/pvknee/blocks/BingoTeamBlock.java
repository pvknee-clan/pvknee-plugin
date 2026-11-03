package com.pvknee.blocks;

import com.pvknee.bingo.TeamColours;
import com.pvknee.data.Bingo;
import com.pvknee.data.Block;
import com.pvknee.player.PlayerState;
import com.pvknee.ui.Card;
import com.pvknee.ui.Centred;
import com.pvknee.ui.Divider;
import com.pvknee.ui.Dots;
import com.pvknee.ui.EmptyState;
import com.pvknee.ui.Fonts;
import com.pvknee.ui.GlyphIcon;
import com.pvknee.ui.Glyphs;
import com.pvknee.ui.IconButton;
import com.pvknee.ui.Labels;
import com.pvknee.ui.PlacePill;
import com.pvknee.ui.ProgressBar;
import com.pvknee.ui.RankBadge;
import com.pvknee.ui.Section;
import com.pvknee.ui.SectionHeader;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

/**
 * The bingo team being viewed: a switcher through every team's board, its rank, tiles done and points, a bar of tiles
 * done (left out before the start), and its members' names in two columns. The title's aside is the time until the start, the
 * time left, or "Ended". Above it, while the bingo runs, a note for members who are logged out or not on a team, and
 * once it has ended, the result: "Tallying results" until the admins finish, then the winner.
 */
class BingoTeamBlock implements BlockRenderer
{
	private static final int INNER_WIDTH = Labels.CONTENT_WIDTH - 2 * Spacing.PADDING;
	private static final int ROW_GAP = 8;
	private static final int SWITCHER_GAP = 6;
	private static final int TROPHY_SIZE = 28;
	private static final float WINNER_SIZE = 17f;
	private static final int RESULT_GAP = 6;
	private static final int MEMBER_COLUMN_GAP = 10;
	/** Inside each member's name, so the member's own highlight reaches past the text. */
	private static final int MEMBER_PADDING_X = 4;
	private static final int MEMBER_PADDING_Y = 3;
	/** The strength of the accent tint behind the member's own name, out of 255. */
	private static final int OWN_TINT = 26;

	@Override
	public JComponent render(Block block, BlockContext context)
	{
		BingoView view = context.getBingoView();
		Bingo bingo = view.getBingo();
		if (bingo == null || view.getTeam() == null)
		{
			return null;
		}

		ThemeColours colours = context.getColours();
		Instant start = Times.parse(bingo.getStartsAt());
		Instant end = Times.parse(bingo.getEndsAt());
		SectionHeader header = new SectionHeader(title(view), colours)
			.withTimeAside(() -> time(start, end), () -> started(start) && end != null && Times.endingSoon(end), colours);
		context.onUpdate(header::update);
		view.onChange(() -> header.setTitle(title(view)));
		Section team = new Section(header, context.followBingo(() -> card(view, bingo, context.getPlayer(), colours)));

		JComponent above = above(view, bingo, end, context);
		if (above == null)
		{
			return team;
		}
		JPanel stack = new JPanel(new BorderLayout(0, Spacing.BLOCK_GAP));
		stack.setOpaque(false);
		stack.add(above, BorderLayout.NORTH);
		stack.add(team, BorderLayout.CENTER);
		return stack;
	}

	/**
	 * What goes above the team card: the result once the bingo has ended, otherwise a note when the member is logged
	 * out or not on a team. Null for nothing.
	 */
	private static JComponent above(BingoView view, Bingo bingo, Instant end, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		Bingo.State state = bingo.getState();
		if (state == Bingo.State.TALLYING || state == Bingo.State.FINAL)
		{
			SectionHeader header = new SectionHeader("Result", colours)
				.withAside(() -> end == null ? "" : "Ended " + Times.date(end, false), colours);
			return new Section(header, state == Bingo.State.TALLYING
				? new EmptyState(Glyphs.HOURGLASS, "Tallying results", "Points below may still change.", colours)
				: result(view, bingo, colours));
		}
		if (!context.getPlayer().isLoggedIn())
		{
			return new EmptyState(Glyphs.USER, "Log in to see your team", "The teams and their points still show below.", colours);
		}
		if (view.getOwnTeam() == null)
		{
			return new EmptyState(Glyphs.GROUP, "You're not on a team", "Ask an admin to add you. You can still follow the teams below.", colours);
		}
		return null;
	}

	/**
	 * The final result: a trophy, "[Team] won" with its points and tiles, then the member's own team's place.
	 */
	private static Card result(BingoView view, Bingo bingo, ThemeColours colours)
	{
		List<Bingo.Team> winners = bingo.getTeams().stream()
			.filter(team -> team.getRank() == 1)
			.collect(Collectors.toList());
		if (winners.isEmpty())
		{
			winners = bingo.getTeams().subList(0, 1);
		}
		Color gold = RankBadge.medalColour(1);
		int total = bingo.getBoardSize() * bingo.getBoardSize();

		Card card = new Card(new DynamicGridLayout(0, 1, 0, RESULT_GAP));
		card.add(new JLabel(new GlyphIcon(Glyphs.TROPHY, gold, TROPHY_SIZE)));
		String names = winners.stream().map(team -> Objects.toString(team.getName(), "")).collect(Collectors.joining(" & "));
		card.add(Labels.label(names + " won", gold, Fonts.boldName().deriveFont(WINNER_SIZE), INNER_WIDTH, true));
		Bingo.Team winner = winners.get(0);
		card.add(Labels.label(Labels.decimal(winner.getPoints()) + " pts · " + winner.getTilesDone() + " of " + total + " tiles",
			colours.getMuted(), FontManager.getRunescapeSmallFont(), INNER_WIDTH, true));

		Bingo.Team own = view.getOwnTeam();
		if (own != null)
		{
			card.add(new Divider(colours.getMuted()));
			JPanel row = new JPanel(new BorderLayout(ROW_GAP, 0));
			row.setOpaque(false);
			JLabel yours = new JLabel("Your team, " + own.getName());
			yours.setForeground(ThemeColours.TEXT);
			yours.setFont(FontManager.getRunescapeSmallFont());
			row.add(yours, BorderLayout.CENTER);
			JLabel place = new JLabel(Labels.ordinal(own.getRank()) + " · " + Labels.decimal(own.getPoints()) + " pts");
			place.setForeground(colours.getValue());
			place.setFont(FontManager.getRunescapeSmallFont());
			row.add(place, BorderLayout.EAST);
			card.add(row);
		}
		return card;
	}

	private static String title(BingoView view)
	{
		return view.isOwnTeam(view.getTeam()) ? "Your Team" : "Team";
	}

	/**
	 * e.g. "Starts in 2d 4h", "12d 20h left" or "Ended".
	 */
	private static String time(Instant start, Instant end)
	{
		Instant now = Instant.now();
		if (!started(start))
		{
			return "Starts in " + Times.duration(Duration.between(now, start));
		}
		if (end == null)
		{
			return "";
		}
		return now.isBefore(end) ? Times.left(end) : "Ended";
	}

	private static boolean started(Instant start)
	{
		return start == null || !Instant.now().isBefore(start);
	}

	private static Card card(BingoView view, Bingo bingo, PlayerState player, ThemeColours colours)
	{
		Bingo.Team team = view.getTeam();
		int total = bingo.getBoardSize() * bingo.getBoardSize();

		Card card = new Card(new DynamicGridLayout(0, 1, 0, ROW_GAP));
		card.add(switcher(view, bingo.getTeams(), team, colours));
		if (bingo.getState() != Bingo.State.UPCOMING)
		{
			card.add(standing(team, total, colours));
			card.add(new ProgressBar(total == 0 ? 0 : (double) team.getTilesDone() / total, ThemeColours.DONE));
		}
		if (!team.getMembers().isEmpty())
		{
			card.add(new Divider(colours.getMuted()));
			card.add(members(team, player, colours));
		}
		return card;
	}

	/**
	 * A muted "Members" label with the count, then the names in two columns in sheet order, the member's own name
	 * with a soft accent tint. A name too wide for its column is cut short and shows in full on hover.
	 */
	private static JComponent members(Bingo.Team team, PlayerState player, ThemeColours colours)
	{
		JPanel heading = new JPanel(new BorderLayout());
		heading.setOpaque(false);
		heading.setBorder(new EmptyBorder(0, MEMBER_PADDING_X, 0, MEMBER_PADDING_X));
		heading.add(muted("Members", colours), BorderLayout.WEST);
		heading.add(muted(String.valueOf(team.getMembers().size()), colours), BorderLayout.EAST);

		int nameWidth = (INNER_WIDTH - MEMBER_COLUMN_GAP) / 2 - 2 * MEMBER_PADDING_X;
		Color tint = colours.getAccent();
		JPanel names = new JPanel(new GridLayout(0, 2, MEMBER_COLUMN_GAP, 1));
		names.setOpaque(false);
		for (Bingo.Member member : team.getMembers())
		{
			String name = Objects.toString(member.getName(), "");
			boolean own = player.isNamed(name);
			Card cell = new Card(new BorderLayout(), false);
			cell.setBorder(new EmptyBorder(MEMBER_PADDING_Y, MEMBER_PADDING_X, MEMBER_PADDING_Y, MEMBER_PADDING_X));
			cell.setArc(Spacing.CONTROL_ARC);
			cell.setBackground(own ? new Color(tint.getRed(), tint.getGreen(), tint.getBlue(), OWN_TINT) : new Color(0, 0, 0, 0));

			JLabel label = new JLabel(name);
			label.setForeground(own ? colours.getValue() : ThemeColours.TEXT);
			label.setFont(Fonts.name());
			if (label.getPreferredSize().width > nameWidth)
			{
				label.setToolTipText(name);
			}
			cell.add(label, BorderLayout.CENTER);
			names.add(cell);
		}

		JPanel section = new JPanel(new BorderLayout(0, 2));
		section.setOpaque(false);
		section.add(heading, BorderLayout.NORTH);
		section.add(names, BorderLayout.CENTER);
		return section;
	}

	private static JLabel muted(String text, ThemeColours colours)
	{
		JLabel label = new JLabel(text);
		label.setForeground(colours.getMuted());
		label.setFont(FontManager.getRunescapeSmallFont());
		return label;
	}

	/**
	 * The team's name with a dot per team under it, between arrows that step through the teams in place order (1st
	 * first) and wrap around, so flicking through it is the standings. The dots are in the same order, coloured by
	 * place. Just the name when there is only one team.
	 */
	private static JComponent switcher(BingoView view, List<Bingo.Team> all, Bingo.Team team, ThemeColours colours)
	{
		List<Bingo.Team> teams = all.stream()
			.sorted(Comparator.comparingInt(t -> t.getRank() >= 1 ? t.getRank() : Integer.MAX_VALUE))
			.collect(Collectors.toList());
		int count = teams.size();
		int index = teams.indexOf(team);
		int nameWidth = count > 1 ? INNER_WIDTH - 2 * (IconButton.SMALL_SIZE + SWITCHER_GAP) : INNER_WIDTH;

		JPanel middle = new JPanel(new BorderLayout(0, Spacing.ITEM_GAP));
		middle.setOpaque(false);
		middle.add(Labels.clamped(Objects.toString(team.getName(), ""), TeamColours.of(all, team),
			Fonts.boldName(), nameWidth, true, Labels.NAME_LINES), BorderLayout.CENTER);

		JPanel row = new JPanel(new BorderLayout(SWITCHER_GAP, 0));
		row.setOpaque(false);
		row.add(middle, BorderLayout.CENTER);
		if (count > 1)
		{
			List<Color> places = teams.stream().map(t -> RankBadge.placeColour(t.getRank())).collect(Collectors.toList());
			middle.add(new Dots(places, index), BorderLayout.SOUTH);
			row.add(new Centred(new IconButton(Glyphs.CHEVRON_LEFT, "Previous team",
				() -> view.selectTeam(teams.get((index + count - 1) % count))).small()), BorderLayout.WEST);
			row.add(new Centred(new IconButton(Glyphs.CHEVRON_RIGHT, "Next team",
				() -> view.selectTeam(teams.get((index + 1) % count))).small()), BorderLayout.EAST);
		}
		return row;
	}

	/**
	 * The rank badge, e.g. "9 of 36 tiles", and the points.
	 */
	private static JComponent standing(Bingo.Team team, int total, ThemeColours colours)
	{
		int rank = team.getRank();
		JPanel row = new JPanel(new BorderLayout(ROW_GAP, 0));
		row.setOpaque(false);
		if (rank >= 1)
		{
			row.add(new Centred(new PlacePill(rank)), BorderLayout.WEST);
		}

		JLabel tiles = new JLabel(team.getTilesDone() + " of " + total + " tiles");
		tiles.setForeground(colours.getMuted());
		tiles.setFont(FontManager.getRunescapeSmallFont());
		row.add(tiles, BorderLayout.CENTER);

		JLabel points = new JLabel(Labels.decimal(team.getPoints()) + " pts");
		points.setForeground(colours.getValue());
		points.setFont(Fonts.boldName());
		row.add(points, BorderLayout.EAST);
		return row;
	}
}
