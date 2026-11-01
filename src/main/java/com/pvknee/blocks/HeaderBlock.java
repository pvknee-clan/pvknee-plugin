package com.pvknee.blocks;

import com.pvknee.data.Bingo;
import com.pvknee.data.Block;
import com.pvknee.data.Competition;
import com.pvknee.player.PlayerState;
import com.pvknee.ui.PageHeader;
import com.pvknee.ui.Pill;
import com.pvknee.ui.ThemeColours;
import java.time.Instant;
import javax.swing.JComponent;

/**
 * The top of a page: its title, an optional subtitle or a competition's or the bingo's dates, and an optional pill.
 */
class HeaderBlock implements BlockRenderer
{
	@Override
	public JComponent render(Block block, BlockContext context)
	{
		Bingo bingo = isBingo(block) ? context.getBingoView().getBingo() : null;
		String title = block.setting("title", bingo == null ? null : bingo.getName());
		if (title == null)
		{
			return null;
		}

		ThemeColours colours = context.getColours();
		String pillText = block.setting("pill", null);
		Pill pill = pillText == null ? null
			: new Pill(pillText, colours.getValue(), context.mutedGlyph(block.setting("icon", null), Pill.ICON_SIZE));
		if (pill != null)
		{
			linkHop(pill, block, context);
		}
		return new PageHeader(title, HeadingBlock.size(block), subtitle(block, context), pill, colours);
	}

	/**
	 * Makes the pill hop to the world its "hop" setting names, unless the member is already on it.
	 */
	private static void linkHop(Pill pill, Block block, BlockContext context)
	{
		int world = block.intSetting("hop", 0);
		if (world <= 0)
		{
			return;
		}

		PlayerState player = context.getPlayer();
		if (player.isLoggedIn() && player.getWorld() == world)
		{
			pill.setToolTipText("You're on world " + world);
			return;
		}

		pill.setToolTipText("Hop to world " + world);
		pill.onClick(() -> context.getHopper().hop(world));
	}

	/**
	 * The dates "dates" names (current_skill, current_boss or bingo) if there are any, otherwise the "subtitle"
	 * setting.
	 */
	private static String subtitle(Block block, BlockContext context)
	{
		Instant start;
		Instant end;
		if (isBingo(block))
		{
			Bingo bingo = context.getBingoView().getBingo();
			start = bingo == null ? null : Times.parse(bingo.getStartsAt());
			end = bingo == null ? null : Times.parse(bingo.getEndsAt());
		}
		else
		{
			Competition competition = context.currentCompetition(block.setting("dates", null));
			start = competition == null ? null : Times.parse(competition.getStartsAt());
			end = competition == null ? null : Times.parse(competition.getEndsAt());
		}
		return start != null && end != null ? Times.range(start, end) : block.setting("subtitle", null);
	}

	private static boolean isBingo(Block block)
	{
		return "bingo".equalsIgnoreCase(block.setting("dates", null));
	}
}
