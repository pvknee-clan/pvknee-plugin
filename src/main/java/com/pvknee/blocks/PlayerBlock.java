package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.data.Competition;
import com.pvknee.data.Placing;
import com.pvknee.player.PlayerState;
import com.pvknee.ui.Labels;
import com.pvknee.ui.RankBadge;
import com.pvknee.ui.StandingCard;
import com.pvknee.ui.ThemeColours;
import java.util.List;
import javax.swing.JComponent;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.FontManager;

/**
 * The member's own place in a competition: a card when they are on the board, a quiet line when others are and they
 * are not, and nothing while logged out or the board is empty.
 */
@Slf4j
class PlayerBlock implements BlockRenderer
{
	@Override
	public JComponent render(Block block, BlockContext context)
	{
		PlayerState player = context.getPlayer();
		if (!player.isLoggedIn() || player.getName() == null)
		{
			return null;
		}

		Competition competition = context.competition(block.setting("competition", "current_skill"));
		if (competition == null)
		{
			log.debug("No competition for player block {}", block.getSettings());
			return null;
		}

		// An empty board is left to the leaderboard's empty state, so the message isn't shown twice.
		List<Placing> board = Placing.board(competition);
		return board.isEmpty() ? null : placeView(board, player, competition, block, context);
	}

	private static JComponent placeView(List<Placing> board, PlayerState player, Competition competition, Block block, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		Placing mine = Placing.find(board, player::isNamed);
		if (mine == null)
		{
			context.getDevLog().log("player: {}, not on the board of {}", player.getName(), board.size());
			return Labels.label("You're not on the board yet", colours.getMuted(), FontManager.getRunescapeSmallFont(), Labels.CONTENT_WIDTH, true);
		}

		int place = mine.getPlace();
		context.getDevLog().log("player: {}, {} of {}", player.getName(), Labels.ordinal(place), board.size());
		long behind = board.get(0).getGained() - mine.getGained();
		String line = behind > 0 ? Labels.number(behind) + " behind 1st" : mine.isShared(board) ? "Level with 1st" : "Leading";
		boolean medal = place <= Math.min(RankBadge.medalPlaces(), Math.max(0, block.intSetting("podium", 3)));
		return new StandingCard("You", place, medal, mine.getName(), line, Labels.score(mine.getGained(), competition.getType()), colours);
	}
}
