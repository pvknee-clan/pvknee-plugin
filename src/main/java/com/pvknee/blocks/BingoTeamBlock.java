package com.pvknee.blocks;

import com.pvknee.data.Bingo;
import com.pvknee.data.Block;
import com.pvknee.ui.Card;
import com.pvknee.ui.Centred;
import com.pvknee.ui.Dots;
import com.pvknee.ui.Fonts;
import com.pvknee.ui.Glyphs;
import com.pvknee.ui.IconButton;
import com.pvknee.ui.Labels;
import com.pvknee.ui.ProgressBar;
import com.pvknee.ui.RankBadge;
import com.pvknee.ui.Section;
import com.pvknee.ui.SectionHeader;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import java.awt.BorderLayout;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

/**
 * The bingo team being viewed: a switcher through every team's board, its rank, tiles done and points, and a bar of
 * tiles done. The title's aside is the time until the start, the time left, or "Ended".
 */
class BingoTeamBlock implements BlockRenderer
{
	private static final int INNER_WIDTH = Labels.CONTENT_WIDTH - 2 * Spacing.PADDING;
	private static final int ROW_GAP = 8;
	private static final int SWITCHER_GAP = 6;

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
		return new Section(header, context.followBingo(() -> card(view, bingo, colours)));
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

	private static Card card(BingoView view, Bingo bingo, ThemeColours colours)
	{
		Bingo.Team team = view.getTeam();
		int total = bingo.getBoardSize() * bingo.getBoardSize();

		Card card = new Card(new DynamicGridLayout(0, 1, 0, ROW_GAP));
		card.add(switcher(view, bingo.getTeams(), team, colours));
		card.add(standing(team, total, colours));
		card.add(new ProgressBar(total == 0 ? 0 : (double) team.getTilesDone() / total, ThemeColours.DONE));
		return card;
	}

	/**
	 * The team's name with a dot per team under it, between arrows that step through the teams and wrap around.
	 * Just the name when there is only one team.
	 */
	private static JComponent switcher(BingoView view, List<Bingo.Team> teams, Bingo.Team team, ThemeColours colours)
	{
		int count = teams.size();
		int index = teams.indexOf(team);
		int nameWidth = count > 1 ? INNER_WIDTH - 2 * (IconButton.SMALL_SIZE + SWITCHER_GAP) : INNER_WIDTH;

		JPanel middle = new JPanel(new BorderLayout(0, Spacing.ITEM_GAP));
		middle.setOpaque(false);
		middle.add(Labels.clamped(Objects.toString(team.getName(), ""), colours.resolve(team.getColour(), colours.getValue()),
			Fonts.boldName(), nameWidth, true, Labels.NAME_LINES), BorderLayout.CENTER);

		JPanel row = new JPanel(new BorderLayout(SWITCHER_GAP, 0));
		row.setOpaque(false);
		row.add(middle, BorderLayout.CENTER);
		if (count > 1)
		{
			middle.add(new Dots(count, index, colours.getAccent()), BorderLayout.SOUTH);
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
		row.add(RankBadge.of(rank, rank >= 1 && rank <= RankBadge.medalPlaces(), colours), BorderLayout.WEST);

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
