package com.pvknee.blocks;

import com.pvknee.data.Block;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JComponent;
import lombok.extern.slf4j.Slf4j;

/**
 * Maps block types to their renderers. Unknown types are skipped.
 */
@Slf4j
@Singleton
public class BlockRegistry
{
	private final Map<String, BlockRenderer> renderers = new HashMap<>();

	@Inject
	BlockRegistry()
	{
		renderers.put("header", new HeaderBlock());
		renderers.put("heading", new HeadingBlock());
		renderers.put("text", new TextBlock());
		renderers.put("separator", new SeparatorBlock());
		renderers.put("message", new MessageBlock());
		renderers.put("links", new LinksBlock());
		renderers.put("events", new EventsBlock());
		renderers.put("competition", new CompetitionBlock());
		renderers.put("leaderboard", new LeaderboardBlock());
		renderers.put("player", new PlayerBlock());
		renderers.put("table", new TableBlock());
		renderers.put("stat", new StatBlock());
		renderers.put("countdown", new CountdownBlock());
		renderers.put("bingo_team", new BingoTeamBlock());
		renderers.put("bingo_board", new BingoBoardBlock());
		renderers.put("bingo_members", new BingoMembersBlock());
		renderers.put("bingo_teams", new BingoTeamsBlock());
	}

	public JComponent render(Block block, BlockContext context)
	{
		BlockRenderer renderer = renderers.get(block.getType());
		if (renderer == null)
		{
			log.debug("Skipping unknown block type {}", block.getType());
			return null;
		}
		try
		{
			return renderer.render(block, context);
		}
		catch (RuntimeException e)
		{
			// One bad block must not break the page: skip it and draw the rest.
			log.debug("Skipping {} block that failed to draw {}", block.getType(), block.getSettings(), e);
			return null;
		}
	}
}
