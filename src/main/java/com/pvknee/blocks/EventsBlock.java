package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.data.ClanEvent;
import com.pvknee.ui.EntryRow;
import com.pvknee.ui.GlyphIcon;
import com.pvknee.ui.Glyphs;
import com.pvknee.ui.Labels;
import com.pvknee.ui.ListCard;
import com.pvknee.ui.Section;
import com.pvknee.ui.SectionHeader;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.JLabel;
import net.runelite.client.ui.FontManager;

/**
 * Running and upcoming events in one list card. A time every entry shares shows once beside the title; otherwise
 * each entry shows its own. Running entries, and a bingo even before it starts, open the page the block names for
 * their kind (e.g. bingo_page). A block with nothing to show draws nothing.
 */
class EventsBlock implements BlockRenderer
{
	private static final int ROW_WIDTH = Labels.CONTENT_WIDTH - 2 * Spacing.PADDING;
	/** Line icons look the same size as pictures when drawn slightly smaller. */
	private static final int GLYPH_SIZE = 18;

	@Override
	public JComponent render(Block block, BlockContext context)
	{
		String when = block.setting("when", "upcoming").toLowerCase();
		boolean both = when.equals("both");
		boolean showRunning = both || when.equals("running");
		boolean showUpcoming = both || !showRunning;
		Set<String> kinds = kinds(block.setting("kind", "all"));

		Instant now = Instant.now();
		List<TimedEvent> running = new ArrayList<>();
		List<TimedEvent> upcoming = new ArrayList<>();
		List<ClanEvent> events = context.getData().getEvents();
		for (ClanEvent event : events == null ? List.<ClanEvent>of() : events)
		{
			TimedEvent timed = new TimedEvent(event, now);
			if (timed.start == null || event.getName() == null || (kinds != null && !kinds.contains(kind(event))))
			{
				continue;
			}
			if (showRunning && timed.running)
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
		if (shown.isEmpty())
		{
			return null;
		}

		ThemeColours colours = context.getColours();
		String title = block.setting("title", null);
		TimedEvent first = shown.get(0);
		boolean shared = title != null && first.timeText() != null && shown.stream()
			.map(e -> Arrays.asList(e.running, e.running ? e.end : e.start, e.event.isHasTime()))
			.distinct()
			.count() == 1;

		ListCard card = new ListCard(colours);
		for (TimedEvent event : shown)
		{
			card.addEntry(entry(event, !shared, block, context));
		}

		SectionHeader header = null;
		if (title != null)
		{
			header = new SectionHeader(title, colours);
			if (shared)
			{
				header.withTimeAside(first::timeText, first::endingSoon, colours);
				context.onUpdate(header::update);
			}
		}
		return new Section(header, card);
	}

	private static EntryRow entry(TimedEvent timed, boolean showTime, Block block, BlockContext context)
	{
		ClanEvent event = timed.event;
		ThemeColours colours = context.getColours();
		boolean bingo = kind(event).equals("bingo");
		JComponent icon = event.getMetric() == null || event.getMetric().isEmpty()
			? new JLabel(new GlyphIcon(bingo ? Glyphs.GRID : Glyphs.CALENDAR, colours.getMuted(), GLYPH_SIZE))
			: context.metricIcon(event.getMetric(), EntryRow.ICON_SIZE);

		EntryRow row = new EntryRow(icon, event.getName(), colours.resolve(event.getColour(), colours.getValue()), ROW_WIDTH);
		row.addCaption(caption(event), colours);
		if (showTime && !timed.running)
		{
			row.addLine(context.liveLabel(timed::timeText, colours.getMuted(), FontManager.getRunescapeSmallFont(), row.getTextWidth(), false));
		}

		// The bingo page has its own state for before the start, so an upcoming bingo opens it too.
		if (timed.running || bingo)
		{
			context.linkToPage(row, block, kind(event));
		}
		if (timed.running)
		{
			if (timed.end != null)
			{
				row.setBar(context.runningBar(timed.start, timed.end, null, showTime));
			}
		}
		return row;
	}

	/**
	 * The kinds to show from a comma list, e.g. "event" or "skill, boss"; null for all.
	 */
	private static Set<String> kinds(String setting)
	{
		Set<String> kinds = Arrays.stream(setting.split(","))
			.map(k -> k.trim().toLowerCase(Locale.ENGLISH))
			.filter(k -> !k.isEmpty())
			.collect(Collectors.toSet());
		return kinds.isEmpty() || kinds.contains("all") ? null : kinds;
	}

	/**
	 * The event's kind in lower case; events without one count as "event".
	 */
	private static String kind(ClanEvent event)
	{
		String kind = Objects.toString(event.getKind(), "").trim().toLowerCase(Locale.ENGLISH);
		return kind.isEmpty() ? "event" : kind;
	}

	private static String caption(ClanEvent event)
	{
		switch (kind(event))
		{
			case "skill":
				return "Skill of the Week";
			case "boss":
				return "Boss of the Week";
			case "bingo":
				return "Bingo";
			default:
				return "Event";
		}
	}

	private static class TimedEvent
	{
		final ClanEvent event;
		final Instant start;
		final Instant end;
		final boolean running;

		TimedEvent(ClanEvent event, Instant now)
		{
			this.event = event;
			this.start = Times.parse(event.getStartsAt());
			this.end = Times.parse(event.getEndsAt());
			this.running = start != null && !now.isBefore(start) && (end == null || now.isBefore(end));
		}

		/**
		 * e.g. "6d 4h left", "Ends in 5h 12m", "Tomorrow, 17:00" or "Fri 6 Nov". Null for a running event without an
		 * end.
		 */
		String timeText()
		{
			if (running)
			{
				return end == null ? null : Times.left(end);
			}
			return Times.date(start, event.isHasTime());
		}

		boolean endingSoon()
		{
			return running && end != null && Times.endingSoon(end);
		}
	}
}
