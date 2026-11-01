package com.pvknee.blocks;

import com.pvknee.data.Bingo;
import com.pvknee.data.Block;
import com.pvknee.ui.Labels;
import com.pvknee.ui.RankBadge;
import com.pvknee.ui.RankTable;
import com.pvknee.ui.Section;
import com.pvknee.ui.SectionHeader;
import com.pvknee.ui.ThemeColours;
import javax.swing.JComponent;

/**
 * Every bingo team's standing: medal badges for the top places, the team's name, tiles done and points, with the
 * member's own team highlighted. Clicking a team shows its board.
 */
class BingoTeamsBlock implements BlockRenderer
{
	@Override
	public JComponent render(Block block, BlockContext context)
	{
		BingoView view = context.getBingoView();
		Bingo bingo = view.getBingo();
		if (bingo == null || bingo.getTeams().isEmpty())
		{
			return null;
		}

		ThemeColours colours = context.getColours();
		RankTable table = new RankTable(colours);
		for (Bingo.Team team : bingo.getTeams())
		{
			int rank = team.getRank();
			table.addRow(RankTable.Row.builder()
				.rank(rank)
				.medal(rank >= 1 && rank <= RankBadge.medalPlaces())
				.name(team.getName())
				.caption(team.getTilesDone() + (team.getTilesDone() == 1 ? " tile" : " tiles"))
				.value(Labels.decimal(team.getPoints()))
				.highlight(view.isOwnTeam(team))
				.onClick(() -> view.selectTeam(team))
				.build());
		}

		String title = block.setting("title", null);
		SectionHeader header = title == null ? null : new SectionHeader(title, colours).withAside(() -> "Points", colours);
		return new Section(header, table);
	}
}
