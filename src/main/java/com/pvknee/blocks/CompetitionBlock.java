package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.data.Competition;
import com.pvknee.data.Competitions;
import com.pvknee.ui.Card;
import com.pvknee.ui.Fonts;
import com.pvknee.ui.IconCaption;
import com.pvknee.ui.Labels;
import com.pvknee.ui.SectionHeader;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import com.pvknee.ui.TimeBar;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

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
			? hero(matching.get(0), upcoming, block, context)
			: side(matching, upcoming, block, context);
	}

	/**
	 * A "NOW RUNNING" (or "STARTING") label over one column per competition. When they all share their times, one
	 * time bar runs across the bottom of the card; otherwise each column has its own at its bottom.
	 */
	private static Card side(List<Competition> competitions, boolean upcoming, Block block, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		int count = competitions.size();
		int columnWidth = (INNER_WIDTH - Spacing.TITLE_GAP * (count - 1)) / count - 2 * Spacing.ITEM_GAP;
		Competition first = competitions.get(0);
		boolean shared = competitions.stream()
			.map(c -> Arrays.asList(c.getStartsAt(), upcoming ? null : c.getEndsAt()))
			.distinct()
			.count() == 1;

		JPanel columns = new JPanel(new GridLayout(1, count, Spacing.TITLE_GAP, 0));
		columns.setOpaque(false);
		for (Competition competition : competitions)
		{
			Card column = new Card(new BorderLayout(0, Spacing.ITEM_GAP), false);
			column.setBorder(new EmptyBorder(Spacing.ITEM_GAP, Spacing.ITEM_GAP, Spacing.ITEM_GAP, Spacing.ITEM_GAP));
			context.linkToPage(column, block, competition.getType());
			column.add(new IconCaption(
				kind(competition),
				context.metricIcon(competition.getMetric(), SIDE_ICON_SIZE),
				competition.getName(),
				colours.resolve(competition.getColour(), colours.getValue()),
				columnWidth,
				colours), BorderLayout.NORTH);
			if (!shared)
			{
				addTimeBar(column, fraction(competition, upcoming), null, time(competition, upcoming), context);
			}
			columns.add(column);
		}

		Card card = new Card(new BorderLayout(0, Spacing.TITLE_GAP));
		card.add(SectionHeader.cardLabel(upcoming ? "Starting" : "Now running", colours, INNER_WIDTH), BorderLayout.NORTH);
		card.add(columns, BorderLayout.CENTER);
		if (shared)
		{
			addTimeBar(card, fraction(first, upcoming), null, time(first, upcoming), context);
		}
		return card;
	}

	/**
	 * One big competition: icon, name, then a bar with the end date on the left and the time left on the right (or
	 * for an upcoming one, the start date and the time until it starts, without a bar).
	 */
	private static Card hero(Competition competition, boolean upcoming, Block block, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		Card card = new Card(new BorderLayout(0, Spacing.TITLE_GAP));
		context.linkToPage(card, block, competition.getType());
		card.add(new IconCaption(
			null,
			context.metricIcon(competition.getMetric(), HERO_ICON_SIZE),
			competition.getName(),
			colours.resolve(competition.getColour(), colours.getValue()),
			Fonts.name().deriveFont(HERO_NAME_SIZE),
			INNER_WIDTH,
			colours), BorderLayout.NORTH);
		Instant time = Times.parse(upcoming ? competition.getStartsAt() : competition.getEndsAt());
		if (time != null)
		{
			Supplier<String> right = upcoming
				? () -> "Starts in " + Times.duration(Duration.between(Instant.now(), time))
				: () -> timeText(competition, false);
			addTimeBar(card, fraction(competition, upcoming), Times.date(time, true), right, context);
		}
		return card;
	}

	/**
	 * Adds a bar and captions at the bottom of the panel, kept up to date every minute. Adds nothing if all parts
	 * are missing.
	 */
	private static void addTimeBar(JPanel panel, DoubleSupplier fraction, String left, Supplier<String> right, BlockContext context)
	{
		if (fraction == null && left == null && right == null)
		{
			return;
		}

		TimeBar bar = new TimeBar(fraction, left, right, context.getColours());
		context.onUpdate(bar::update);
		panel.add(bar, BorderLayout.SOUTH);
	}

	/**
	 * How far a running competition is through its own start-to-end span. Null for upcoming competitions and when
	 * either time is unknown, as those get no bar.
	 */
	private static DoubleSupplier fraction(Competition competition, boolean upcoming)
	{
		Instant start = Times.parse(competition.getStartsAt());
		Instant end = Times.parse(competition.getEndsAt());
		return upcoming || start == null || end == null ? null : () -> Times.progress(start, end);
	}

	/**
	 * The competition's time as a live caption, or null if the time is unknown.
	 */
	private static Supplier<String> time(Competition competition, boolean upcoming)
	{
		return timeText(competition, upcoming) == null ? null : () -> timeText(competition, upcoming);
	}

	/**
	 * e.g. "5d 22h left", "in 5h 12m" or "Fri 6 Nov, 17:00 UTC". Null if the time is unknown.
	 */
	private static String timeText(Competition competition, boolean upcoming)
	{
		Instant now = Instant.now();
		if (!upcoming)
		{
			Instant end = Times.parse(competition.getEndsAt());
			return end == null ? null : Times.duration(Duration.between(now, end)) + " left";
		}

		Instant start = Times.parse(competition.getStartsAt());
		if (start == null)
		{
			return null;
		}
		Duration until = Duration.between(now, start);
		return until.compareTo(SOON) < 0 ? "in " + Times.duration(until) : Times.date(start, true);
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
