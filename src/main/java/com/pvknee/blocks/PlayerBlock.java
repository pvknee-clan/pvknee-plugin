package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.player.PlayerState;
import com.pvknee.ui.Labels;
import com.pvknee.ui.RankBadge;
import com.pvknee.ui.StandingCard;
import com.pvknee.ui.ThemeColours;
import com.pvknee.wom.Standing;
import java.awt.BorderLayout;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JComponent;
import javax.swing.JPanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.FontManager;

/**
 * The member's own place in a competition, from the standings the leaderboard loads: a card when they are on the
 * board, a quiet line when others are and they are not, and nothing while logged out or the board is empty.
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

		CompetitionTarget target = CompetitionTarget.from(block.setting("competition", "current_skill"), context);
		if (target == null)
		{
			log.debug("No competition for player block {}", block.getSettings());
			return null;
		}

		// Empty until the standings arrive, and after an error.
		JPanel place = new JPanel(new BorderLayout());
		place.setOpaque(false);

		Consumer<List<Standing>> onStandings = new Consumer<>()
		{
			private List<Standing> shown;

			@Override
			public void accept(List<Standing> result)
			{
				if (result != shown)
				{
					shown = result;
					place.removeAll();
					List<Standing> gainers = Standing.gainers(result);
					// An empty board is left to the leaderboard's empty state, so the message isn't shown twice.
					if (!gainers.isEmpty())
					{
						place.add(placeView(gainers, player, target, block, context));
					}
					place.revalidate();
					place.repaint();
				}
			}
		};
		Runnable load = () -> context.getWom().standings(target.getWomId(), onStandings, error -> {});
		load.run();
		context.onUpdate(load);
		return place;
	}

	private static JComponent placeView(List<Standing> gainers, PlayerState player, CompetitionTarget target, Block block, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		int index = 0;
		while (index < gainers.size() && !player.isNamed(gainers.get(index).getName()))
		{
			index++;
		}

		if (index == gainers.size())
		{
			context.getDevLog().log("player: {}, not on the board of {}", player.getName(), gainers.size());
			return Labels.label("You're not on the board yet", colours.getMuted(), FontManager.getRunescapeSmallFont(), Labels.CONTENT_WIDTH, true);
		}

		int rank = index + 1;
		context.getDevLog().log("player: {}, {} of {}", player.getName(), Labels.ordinal(rank), gainers.size());
		Standing mine = gainers.get(index);
		long behind = gainers.get(0).getGained() - mine.getGained();
		String line = rank == 1 ? "Leading" : behind == 0 ? "Level with 1st" : Labels.number(behind) + " behind 1st";
		boolean medal = rank <= Math.min(RankBadge.medalPlaces(), Math.max(0, block.intSetting("podium", 3)));
		return new StandingCard("You", rank, medal, mine.getName(), line, Labels.score(mine.getGained(), target.getType()), colours);
	}
}
