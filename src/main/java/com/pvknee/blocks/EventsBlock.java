package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.data.ClanEvent;
import com.pvknee.ui.Card;
import com.pvknee.ui.Labels;
import com.pvknee.ui.SectionHeader;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import java.awt.BorderLayout;
import java.awt.Color;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

class EventsBlock implements BlockRenderer
{
	private static final int ICON_SIZE = 16;
	private static final Duration SOON = Duration.ofHours(24);
	private static final int TEXT_WIDTH = Labels.CONTENT_WIDTH - 3 * Spacing.PADDING - ICON_SIZE;

	@Override
	public JComponent render(Block block, BlockContext context)
	{
		String when = block.setting("when", "upcoming").toLowerCase();
		boolean both = when.equals("both");
		boolean showRunning = both || when.equals("running");
		boolean showUpcoming = both || !showRunning;

		Instant now = Instant.now();
		List<TimedEvent> running = new ArrayList<>();
		List<TimedEvent> upcoming = new ArrayList<>();
		List<ClanEvent> events = context.getData().getEvents();
		for (ClanEvent event : events == null ? List.<ClanEvent>of() : events)
		{
			TimedEvent timed = new TimedEvent(event);
			if (timed.start == null || event.getName() == null)
			{
				continue;
			}
			if (showRunning && timed.isRunning(now))
			{
				running.add(timed);
			}
			else if (showUpcoming && now.isBefore(timed.start))
			{
				upcoming.add(timed);
			}
		}
		running.sort(Comparator.comparing(e -> e.end == null ? Instant.MAX : e.end));
		upcoming.sort(Comparator.comparing(e -> e.start));

		List<TimedEvent> shown = new ArrayList<>(running);
		shown.addAll(upcoming);
		shown = shown.subList(0, Math.min(shown.size(), Math.max(0, block.intSetting("limit", 4))));

		String title = block.setting("title", null);
		if (shown.isEmpty() && title == null)
		{
			return null;
		}

		ThemeColours colours = context.getColours();
		JPanel panel = new JPanel(new DynamicGridLayout(0, 1, 0, Spacing.ITEM_GAP));
		panel.setOpaque(false);
		if (title != null)
		{
			panel.add(new SectionHeader(title, colours));
		}

		if (shown.isEmpty())
		{
			String empty = showRunning && !showUpcoming ? "Nothing happening right now" : "Nothing coming up";
			panel.add(Labels.label(empty, colours.getMuted(), FontManager.getRunescapeSmallFont(), Labels.CONTENT_WIDTH, true));
		}
		for (TimedEvent event : shown)
		{
			panel.add(eventCard(event, now, context));
		}
		return panel;
	}

	private static Card eventCard(TimedEvent timed, Instant now, BlockContext context)
	{
		ClanEvent event = timed.event;
		ThemeColours colours = context.getColours();
		Card card = new Card(new BorderLayout(Spacing.PADDING, 0));

		if (event.getMetric() != null && !event.getMetric().isEmpty())
		{
			card.add(context.metricIcon(event.getMetric(), ICON_SIZE), BorderLayout.WEST);
		}

		Color nameColour = colours.resolve(event.getColour(), colours.getValue());
		JPanel text = new JPanel(new BorderLayout());
		text.setOpaque(false);
		text.add(Labels.label(event.getName(), nameColour, FontManager.getRunescapeBoldFont(), TEXT_WIDTH, false), BorderLayout.NORTH);
		text.add(Labels.label(details(timed, now), colours.getMuted(), FontManager.getRunescapeSmallFont(), TEXT_WIDTH, false), BorderLayout.CENTER);
		card.add(text, BorderLayout.CENTER);
		return card;
	}

	/**
	 * e.g. "Skill · Ends in 6d 4h" or "Boss · in 5h 12m".
	 */
	private static String details(TimedEvent timed, Instant now)
	{
		String time;
		if (timed.isRunning(now))
		{
			time = timed.end == null ? "Happening now" : "Ends in " + Times.duration(Duration.between(now, timed.end));
		}
		else
		{
			Duration until = Duration.between(now, timed.start);
			boolean hasTime = timed.event.isHasTime();
			time = hasTime && until.compareTo(SOON) < 0 ? "in " + Times.duration(until) : Times.date(timed.start, hasTime);
		}

		String kind = timed.event.getKind();
		if ("skill".equalsIgnoreCase(kind))
		{
			return "Skill · " + time;
		}
		if ("boss".equalsIgnoreCase(kind))
		{
			return "Boss · " + time;
		}
		return time;
	}

	private static class TimedEvent
	{
		final ClanEvent event;
		final Instant start;
		final Instant end;

		TimedEvent(ClanEvent event)
		{
			this.event = event;
			this.start = Times.parse(event.getStartsAt());
			this.end = Times.parse(event.getEndsAt());
		}

		boolean isRunning(Instant now)
		{
			return !now.isBefore(start) && (end == null || now.isBefore(end));
		}
	}
}
