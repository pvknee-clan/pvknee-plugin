package com.pvknee.blocks;

import com.pvknee.data.Bingo;
import com.pvknee.data.Block;
import com.pvknee.player.PlayerState;
import com.pvknee.ui.Labels;
import com.pvknee.ui.RankTable;
import com.pvknee.ui.Section;
import com.pvknee.ui.SectionHeader;
import com.pvknee.ui.ThemeColours;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.swing.JComponent;

/**
 * The viewed bingo team's members: name, tiles they helped complete and points, best first, with the member's own
 * row highlighted. The points column is left out with points=false, or when the endpoint stops sending points.
 */
class BingoMembersBlock implements BlockRenderer
{
	@Override
	public JComponent render(Block block, BlockContext context)
	{
		BingoView view = context.getBingoView();
		if (view.getBingo() == null || view.getTeam() == null)
		{
			return null;
		}

		boolean points = block.booleanSetting("points", true) && view.getBingo().getTeams().stream()
			.flatMap(team -> team.getMembers().stream())
			.anyMatch(member -> member.getPoints() != null);
		ThemeColours colours = context.getColours();
		String title = block.setting("title", null);
		SectionHeader header = title == null ? null : new SectionHeader(title, colours);
		if (header != null && points)
		{
			header.withAside(() -> "Points", colours);
		}
		return new Section(header, context.followBingo(() -> table(view.getTeam(), points, context.getPlayer(), colours)));
	}

	private static JComponent table(Bingo.Team team, boolean points, PlayerState player, ThemeColours colours)
	{
		List<Bingo.Member> members = team.getMembers().stream()
			.filter(member -> member.getName() != null)
			.sorted(Comparator.comparing((Bingo.Member member) -> Objects.requireNonNullElse(member.getPoints(), 0.0)).reversed())
			.collect(Collectors.toList());
		if (members.isEmpty())
		{
			return null;
		}

		RankTable table = new RankTable(colours);
		for (Bingo.Member member : members)
		{
			boolean you = player.isNamed(member.getName());
			table.addRow(RankTable.Row.builder()
				.name(member.getName())
				.suffix(you ? "(you)" : null)
				.caption(member.getTiles() + (member.getTiles() == 1 ? " tile" : " tiles"))
				.value(points && member.getPoints() != null ? Labels.decimal(member.getPoints()) : null)
				.highlight(you)
				.build());
		}
		return table;
	}
}
