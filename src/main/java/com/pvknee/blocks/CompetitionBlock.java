package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.data.Competition;
import com.pvknee.data.Competitions;
import com.pvknee.ui.Card;
import com.pvknee.ui.Divider;
import com.pvknee.ui.Fonts;
import com.pvknee.ui.IconCaption;
import com.pvknee.ui.Labels;
import com.pvknee.ui.Section;
import com.pvknee.ui.SectionHeader;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import com.pvknee.ui.TimeBar;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.border.EmptyBorder;

class CompetitionBlock implements BlockRenderer
{
	private static final int SIDE_ICON_SIZE = 32;
	private static final int HERO_ICON_SIZE = 48;
	private static final float HERO_NAME_SIZE = 20f;
	private static final int INNER_WIDTH = Labels.CONTENT_WIDTH - 2 * Spacing.PADDING;

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

		ThemeColours colours = context.getColours();
		String title = block.setting("title", null);
		if (block.setting("layout", "side").equalsIgnoreCase("hero"))
		{
			return Section.titled(title, hero(matching.get(0), upcoming, block, context), colours);
		}

		SectionHeader header = null;
		if (title != null)
		{
			header = new SectionHeader(title, colours);
			Competition first = matching.get(0);
			Instant time = Times.parse(upcoming ? first.getStartsAt() : first.getEndsAt());
			if (time != null)
			{
				header.withTimeAside(
					upcoming ? () -> Times.date(time, true) : () -> Times.left(time),
					() -> !upcoming && Times.endingSoon(time),
					colours);
				context.onUpdate(header::update);
			}
		}
		return new Section(header, side(matching, block, context));
	}

	/**
	 * One clickable column per competition, with thin dividers between them. The weekly competitions always share
	 * their times, so the time is the title's aside rather than part of the card.
	 */
	private static Card side(List<Competition> competitions, Block block, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		int count = competitions.size();
		int dividerWidth = 1 + 2 * Spacing.ITEM_GAP;
		int columnWidth = (INNER_WIDTH - dividerWidth * (count - 1)) / count - 2 * Spacing.ITEM_GAP;

		Card card = new Card(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weighty = 1;
		for (Competition competition : competitions)
		{
			if (constraints.gridx > 0)
			{
				constraints.weightx = 0;
				constraints.insets = new Insets(Spacing.ITEM_GAP, Spacing.ITEM_GAP, Spacing.ITEM_GAP, Spacing.ITEM_GAP);
				card.add(Divider.vertical(colours.getMuted()), constraints);
				constraints.gridx++;
			}

			Card column = new Card(new BorderLayout(), false);
			column.setBorder(new EmptyBorder(Spacing.ITEM_GAP, Spacing.ITEM_GAP, Spacing.ITEM_GAP, Spacing.ITEM_GAP));
			context.linkToPage(column, block, competition.getType());
			column.add(new IconCaption(
				kind(competition),
				context.metricIcon(competition.getMetric(), SIDE_ICON_SIZE),
				competition.getName(),
				colours.resolve(competition.getColour(), colours.getValue()),
				columnWidth,
				colours), BorderLayout.NORTH);

			constraints.weightx = 1;
			constraints.insets = new Insets(0, 0, 0, 0);
			card.add(column, constraints);
			constraints.gridx++;
		}
		return card;
	}

	/**
	 * One big competition: icon, name, then a day bar with the end date on the left and the time left on the right
	 * (or for an upcoming one, the start date and the time until it starts, without a bar).
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
			Fonts.boldName().deriveFont(HERO_NAME_SIZE),
			INNER_WIDTH,
			colours), BorderLayout.NORTH);

		Instant start = Times.parse(competition.getStartsAt());
		Instant end = Times.parse(competition.getEndsAt());
		if (upcoming && start != null)
		{
			TimeBar bar = new TimeBar(null, Times.date(start, true),
				() -> "Starts in " + Times.duration(Duration.between(Instant.now(), start)), null, colours);
			context.onUpdate(bar::update);
			card.add(bar, BorderLayout.SOUTH);
		}
		else if (!upcoming && start != null && end != null)
		{
			card.add(context.runningBar(start, end, Times.date(end, true), true), BorderLayout.SOUTH);
		}
		return card;
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
