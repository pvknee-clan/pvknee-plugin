package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.data.Competition;
import com.pvknee.data.Competitions;
import com.pvknee.ui.Card;
import com.pvknee.ui.IconCaption;
import com.pvknee.ui.Labels;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.FontManager;

class CompetitionBlock implements BlockRenderer
{
	private static final int SIDE_ICON_SIZE = 32;
	private static final int HERO_ICON_SIZE = 48;
	private static final float HERO_NAME_SIZE = 20f;
	private static final int INNER_WIDTH = Labels.CONTENT_WIDTH - 2 * Spacing.PADDING;
	private static final Duration SOON = Duration.ofHours(24);

	@Override
	public JComponent render(Block block, BlockContext context)
	{
		boolean upcoming = block.setting("which", "current").equalsIgnoreCase("upcoming");
		String type = block.setting("type", "all").toLowerCase();

		Competitions competitions = context.getData().getCompetitions();
		List<Competition> source = competitions == null ? null : upcoming ? competitions.getUpcoming() : competitions.getCurrent();
		if (source == null)
		{
			return null;
		}

		List<Competition> matching = source.stream()
			.filter(c -> c.getName() != null)
			.filter(c -> type.equals("all") || type.equalsIgnoreCase(c.getType()))
			.collect(Collectors.toList());
		if (matching.isEmpty())
		{
			return null;
		}

		return block.setting("layout", "side").equalsIgnoreCase("hero")
			? hero(matching.get(0), upcoming, context)
			: side(matching, upcoming, context);
	}

	private static Card side(List<Competition> competitions, boolean upcoming, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		int count = competitions.size();
		int columnWidth = (INNER_WIDTH - Spacing.PADDING * (count - 1)) / count;
		boolean sharedTime = competitions.stream()
			.map(c -> upcoming ? c.getStartsAt() : c.getEndsAt())
			.distinct()
			.count() == 1;

		JPanel columns = new JPanel(new GridLayout(1, count, Spacing.PADDING, 0));
		columns.setOpaque(false);
		for (Competition competition : competitions)
		{
			JPanel column = new JPanel(new BorderLayout(0, Spacing.ITEM_GAP));
			column.setOpaque(false);
			column.add(new IconCaption(
				kind(competition),
				context.metricIcon(competition.getMetric(), SIDE_ICON_SIZE),
				competition.getName(),
				colours.resolve(competition.getColour(), colours.getValue()),
				columnWidth,
				colours), BorderLayout.NORTH);
			if (!sharedTime)
			{
				addTime(column, relativeTime(competition, upcoming), columnWidth, colours);
			}
			columns.add(column);
		}

		Card card = new Card(new BorderLayout(0, Spacing.ITEM_GAP));
		card.add(columns, BorderLayout.CENTER);
		if (sharedTime)
		{
			addTime(card, relativeTime(competitions.get(0), upcoming), INNER_WIDTH, colours);
		}
		return card;
	}

	private static Card hero(Competition competition, boolean upcoming, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		Card card = new Card(new BorderLayout(0, Spacing.ITEM_GAP));
		card.add(new IconCaption(
			null,
			context.metricIcon(competition.getMetric(), HERO_ICON_SIZE),
			competition.getName(),
			colours.resolve(competition.getColour(), colours.getValue()),
			FontManager.getRunescapeBoldFont().deriveFont(HERO_NAME_SIZE),
			INNER_WIDTH,
			colours), BorderLayout.NORTH);

		Instant time = Times.parse(upcoming ? competition.getStartsAt() : competition.getEndsAt());
		if (time != null)
		{
			addTime(card, (upcoming ? "Starts " : "Ends ") + Times.date(time, true), INNER_WIDTH, colours);
		}
		return card;
	}

	/**
	 * e.g. "Ends in 6d 4h", "Starts in 5h 12m" or "Starts Fri 6 Nov, 17:00 UTC". Null if the time is unknown.
	 */
	private static String relativeTime(Competition competition, boolean upcoming)
	{
		Instant now = Instant.now();
		if (!upcoming)
		{
			Instant end = Times.parse(competition.getEndsAt());
			return end == null ? null : "Ends in " + Times.duration(Duration.between(now, end));
		}

		Instant start = Times.parse(competition.getStartsAt());
		if (start == null)
		{
			return null;
		}
		Duration until = Duration.between(now, start);
		return until.compareTo(SOON) < 0 ? "Starts in " + Times.duration(until) : "Starts " + Times.date(start, true);
	}

	private static void addTime(JPanel panel, String text, int width, ThemeColours colours)
	{
		if (text != null)
		{
			JLabel label = Labels.label(text, colours.getMuted(), FontManager.getRunescapeSmallFont(), width, true);
			panel.add(label, BorderLayout.SOUTH);
		}
	}

	private static String kind(Competition competition)
	{
		String type = Objects.toString(competition.getType(), "");
		if (type.equalsIgnoreCase("skill"))
		{
			return "Skill";
		}
		return type.equalsIgnoreCase("boss") ? "Boss" : null;
	}
}
