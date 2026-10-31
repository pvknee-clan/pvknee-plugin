package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.data.Competition;
import com.pvknee.ui.PageHeader;
import com.pvknee.ui.Pill;
import com.pvknee.ui.ThemeColours;
import java.time.Instant;
import javax.swing.JComponent;

/**
 * The top of a page: its title, an optional subtitle or a competition's dates, and an optional pill.
 */
class HeaderBlock implements BlockRenderer
{
	@Override
	public JComponent render(Block block, BlockContext context)
	{
		String title = block.setting("title", null);
		if (title == null)
		{
			return null;
		}

		ThemeColours colours = context.getColours();
		String pillText = block.setting("pill", null);
		Pill pill = pillText == null ? null
			: new Pill(pillText, colours.getValue(), context.mutedGlyph(block.setting("icon", null), Pill.ICON_SIZE));
		return new PageHeader(title, HeadingBlock.size(block), subtitle(block, context), pill, colours);
	}

	/**
	 * The dates of the competition named by "dates" (current_skill or current_boss) if there is one, otherwise the
	 * "subtitle" setting.
	 */
	private static String subtitle(Block block, BlockContext context)
	{
		Competition competition = context.currentCompetition(block.setting("dates", null));
		Instant start = competition == null ? null : Times.parse(competition.getStartsAt());
		Instant end = competition == null ? null : Times.parse(competition.getEndsAt());
		return start != null && end != null ? Times.range(start, end) : block.setting("subtitle", null);
	}
}
