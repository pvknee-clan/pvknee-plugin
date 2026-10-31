package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.data.ClanData;
import com.pvknee.data.Competition;
import com.pvknee.data.Competitions;
import com.pvknee.ui.Card;
import com.pvknee.ui.DayBar;
import com.pvknee.ui.GlyphIcon;
import com.pvknee.ui.Glyphs;
import com.pvknee.ui.LiveLabel;
import com.pvknee.ui.ThemeColours;
import com.pvknee.ui.TimeBar;
import com.pvknee.wom.WomClient;
import java.awt.Color;
import java.awt.Font;
import java.awt.Shape;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JLabel;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

/**
 * Shared state every block renderer can read.
 */
@Slf4j
@Value
public class BlockContext
{
	ClanData data;
	ThemeColours colours;
	Icons icons;
	PageNavigator pages;
	WomClient wom;
	/** Run by the panel every minute while the page is shown. */
	List<Runnable> liveUpdates;

	/**
	 * The icon for a Wise Old Man metric, e.g. woodcutting or cerberus.
	 */
	public JLabel metricIcon(String metric, int size)
	{
		return icons.metric(metric, data.getIconBase(), size);
	}

	/**
	 * The current competition a setting names: current_skill or current_boss. Null for anything else, or if there is
	 * no such competition.
	 */
	public Competition currentCompetition(String setting)
	{
		String type = "current_skill".equalsIgnoreCase(setting) ? "skill"
			: "current_boss".equalsIgnoreCase(setting) ? "boss"
			: null;
		Competitions competitions = data.getCompetitions();
		if (type == null || competitions == null || competitions.getCurrent() == null)
		{
			return null;
		}
		return competitions.getCurrent().stream()
			.filter(c -> type.equalsIgnoreCase(c.getType()))
			.findFirst()
			.orElse(null);
	}

	/**
	 * A muted glyph by its sheet name, e.g. "globe". Null if there is none or the name is unknown.
	 */
	public GlyphIcon mutedGlyph(String name, int size)
	{
		if (name == null)
		{
			return null;
		}

		Shape glyph = Glyphs.named(name);
		if (glyph == null)
		{
			log.debug("Unknown icon {}", name);
			return null;
		}
		return new GlyphIcon(glyph, colours.getMuted(), size);
	}

	/**
	 * Makes the card open the page the block sets for this type (skill_page or boss_page), if it can be opened.
	 */
	public void linkToPage(Card card, Block block, String type)
	{
		String pageId = type == null ? null : block.setting(type.toLowerCase() + "_page", null);
		if (pageId != null && pages.canOpen(pageId))
		{
			card.onClick(() -> pages.open(pageId));
		}
	}

	/**
	 * A label whose text is asked for again every minute, for countdowns.
	 */
	public LiveLabel liveLabel(Supplier<String> text, Color colour, Font font, int width, boolean centred)
	{
		LiveLabel label = new LiveLabel(text, colour, font, width, centred);
		onUpdate(label::update);
		return label;
	}

	/**
	 * A day bar for a running item's span, with the time left on the right (or no caption) and an optional fixed
	 * caption on the left, kept up to date every minute.
	 */
	public TimeBar runningBar(Instant start, Instant end, String left, boolean timeLeft)
	{
		DayBar bar = new DayBar(Times.segments(start, end), () -> Times.progress(start, end), () -> Times.endingSoon(end), colours);
		TimeBar timeBar = new TimeBar(bar, left, timeLeft ? () -> Times.left(end) : null, () -> Times.endingSoon(end), colours);
		onUpdate(timeBar::update);
		return timeBar;
	}

	/**
	 * Runs the update every minute while the page is shown, e.g. to move a progress bar.
	 */
	public void onUpdate(Runnable update)
	{
		liveUpdates.add(update);
	}
}
