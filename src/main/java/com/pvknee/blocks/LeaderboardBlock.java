package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.data.Competition;
import com.pvknee.data.Placing;
import com.pvknee.ui.EmptyState;
import com.pvknee.ui.Glyphs;
import com.pvknee.ui.Labels;
import com.pvknee.ui.LinkButton;
import com.pvknee.ui.RankBadge;
import com.pvknee.ui.RankTable;
import com.pvknee.ui.Section;
import com.pvknee.ui.SectionHeader;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.JPanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.DynamicGridLayout;

/**
 * A competition's standings as one ranked table with an optional button under it that opens the full standings on
 * Wise Old Man, or an invitation to take part while nobody has gained yet.
 */
@Slf4j
class LeaderboardBlock implements BlockRenderer
{
	@Override
	public JComponent render(Block block, BlockContext context)
	{
		Competition competition = context.competition(block.setting("competition", "current_skill"));
		if (competition == null)
		{
			log.debug("No competition for leaderboard {}", block.getSettings());
			return null;
		}

		ThemeColours colours = context.getColours();
		String label = block.setting("label", isBoss(competition) ? "Kills" : isSkill(competition) ? "XP gained" : "Gained");
		SectionHeader header = new SectionHeader(block.setting("title", "Standings"), colours).withAside(() -> label, colours);
		return new Section(header, standingsView(competition, block, context));
	}

	private static JComponent standingsView(Competition competition, Block block, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		List<Placing> board = Placing.board(competition).stream()
			.limit(Math.max(0, block.intSetting("rows", 20)))
			.collect(Collectors.toList());
		if (board.isEmpty())
		{
			return new EmptyState(Glyphs.TROPHY, isBoss(competition) ? "No kills yet" : "No gains yet", emptyText(competition, block), colours);
		}

		String prefix = block.setting("prefix", "");
		int medals = Math.min(RankBadge.medalPlaces(), Math.max(0, block.intSetting("podium", 3)));
		RankTable table = new RankTable(colours);
		for (Placing placing : board)
		{
			table.addRow(placing.getPlace(), placing.getName(), prefix + Labels.number(placing.getGained()),
				placing.getPlace() <= medals, context.getPlayer().isNamed(placing.getName()));
		}

		String button = block.setting("button", null);
		if (button == null || competition.getWomId() <= 0)
		{
			return table;
		}

		JPanel view = new JPanel(new DynamicGridLayout(0, 1, 0, Spacing.BLOCK_GAP));
		view.setOpaque(false);
		view.add(table);
		view.add(new LinkButton(button, competition.womPage(), colours));
		return view;
	}

	/**
	 * The empty setting, or a default inviting people to take part, with {name} replaced by the competition's name.
	 */
	private static String emptyText(Competition competition, Block block)
	{
		String fallback = isBoss(competition) ? "Kill {name} this week to get on the board."
			: isSkill(competition) ? "Train {name} this week to get on the board."
			: "Take part this week to get on the board.";
		String name = competition.getName() == null ? "the competition" : competition.getName();
		return block.setting("empty", fallback).replace("{name}", name);
	}

	private static boolean isSkill(Competition competition)
	{
		return "skill".equalsIgnoreCase(competition.getType());
	}

	private static boolean isBoss(Competition competition)
	{
		return "boss".equalsIgnoreCase(competition.getType());
	}
}
