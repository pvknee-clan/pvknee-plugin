package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.ui.Card;
import com.pvknee.ui.Fonts;
import com.pvknee.ui.Labels;
import com.pvknee.ui.Section;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import com.pvknee.ui.TimeBar;
import java.awt.BorderLayout;
import java.time.Duration;
import java.time.Instant;
import javax.swing.JComponent;
import lombok.extern.slf4j.Slf4j;

/**
 * A label and a live countdown to a date, e.g. "Bingo starts in" with the date on the left and "3d 4h" on the right,
 * in the ending-soon colour in the last 24 hours. Nothing once the date has passed.
 */
@Slf4j
class CountdownBlock implements BlockRenderer
{
	private static final int INNER_WIDTH = Labels.CONTENT_WIDTH - 2 * Spacing.PADDING;

	@Override
	public JComponent render(Block block, BlockContext context)
	{
		String label = block.setting("label", null);
		Instant until = Times.parse(block.setting("until", null));
		if (label == null || until == null)
		{
			log.debug("Countdown needs a label and an ISO time: {}", block.getSettings());
			return null;
		}
		if (!Instant.now().isBefore(until))
		{
			return null;
		}

		ThemeColours colours = context.getColours();
		TimeBar time = new TimeBar(null, Times.date(until, true),
			() -> Instant.now().isBefore(until) ? Times.duration(Duration.between(Instant.now(), until)) : "Now",
			() -> Times.endingSoon(until),
			colours);
		context.onUpdate(time::update);

		Card card = new Card(new BorderLayout(0, Spacing.TITLE_GAP));
		card.add(Labels.label(label, colours.getValue(), Fonts.name(), INNER_WIDTH, false), BorderLayout.NORTH);
		card.add(time, BorderLayout.SOUTH);
		return Section.titled(block.setting("title", null), card, colours);
	}
}
