package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.data.Competitions;
import com.pvknee.ui.EmptyState;
import com.pvknee.ui.Glyphs;
import com.pvknee.ui.Labels;
import com.pvknee.ui.LinkButton;
import com.pvknee.ui.RankTable;
import com.pvknee.ui.Section;
import com.pvknee.ui.SectionHeader;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import com.pvknee.wom.Standing;
import com.pvknee.wom.WomClient;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

/**
 * A competition's standings as one ranked table, or an invitation to take part while nobody has gained yet.
 */
@Slf4j
class LeaderboardBlock implements BlockRenderer
{
	@Override
	public JComponent render(Block block, BlockContext context)
	{
		Target target = target(block.setting("competition", "current_skill"), context);
		if (target == null)
		{
			log.debug("No competition for leaderboard {}", block.getSettings());
			return null;
		}

		ThemeColours colours = context.getColours();
		String label = block.setting("label", target.isBoss() ? "Kills" : target.isSkill() ? "XP gained" : "Gained");
		SectionHeader header = new SectionHeader(block.setting("title", "Standings"), colours).withAside(() -> label, colours);

		JPanel standings = column();
		standings.add(muted("Loading standings...", colours));

		// Asked again every minute; WOM answers from its cache, so the table is only redrawn when new standings arrive.
		Consumer<List<Standing>> onStandings = new Consumer<>()
		{
			private List<Standing> shown;

			@Override
			public void accept(List<Standing> result)
			{
				if (result != shown)
				{
					shown = result;
					show(standings, standingsView(result, target, block, colours));
				}
			}
		};
		Runnable load = () -> context.getWom().standings(target.getWomId(), onStandings,
			error -> show(standings, muted("Couldn't load standings", colours)));
		load.run();
		context.onUpdate(load);
		return new Section(header, standings);
	}

	/**
	 * current_skill and current_boss pick from the current competitions; anything else is a WOM id.
	 */
	private static Target target(String setting, BlockContext context)
	{
		String type = setting.equalsIgnoreCase("current_skill") ? "skill"
			: setting.equalsIgnoreCase("current_boss") ? "boss"
			: null;
		if (type == null)
		{
			try
			{
				return new Target(Long.parseLong(setting), null, null);
			}
			catch (NumberFormatException e)
			{
				return null;
			}
		}

		Competitions competitions = context.getData().getCompetitions();
		if (competitions == null || competitions.getCurrent() == null)
		{
			return null;
		}
		return competitions.getCurrent().stream()
			.filter(c -> type.equalsIgnoreCase(c.getType()) && c.getWomId() > 0)
			.findFirst()
			.map(c -> new Target(c.getWomId(), type, c.getName()))
			.orElse(null);
	}

	private static JComponent standingsView(List<Standing> standings, Target target, Block block, ThemeColours colours)
	{
		String url = WomClient.competitionPage(target.getWomId());
		List<Standing> gainers = standings.stream()
			.filter(s -> s.getGained() > 0)
			.limit(Math.max(0, block.intSetting("rows", 20)))
			.collect(Collectors.toList());
		if (gainers.isEmpty())
		{
			LinkButton button = new LinkButton(block.setting("button", "Open on Wise Old Man"), url, colours);
			return new EmptyState(Glyphs.TROPHY, target.isBoss() ? "No kills yet" : "No gains yet", emptyText(target, block), button, colours);
		}

		String prefix = block.setting("prefix", "");
		int medals = Math.min(RankTable.medalPlaces(), Math.max(0, block.intSetting("podium", 3)));
		RankTable table = new RankTable(colours);
		for (int i = 0; i < gainers.size(); i++)
		{
			Standing standing = gainers.get(i);
			table.addRow(i + 1, standing.getName(), prefix + format(standing.getGained()), i < medals);
		}

		JPanel view = column();
		view.add(table);
		String button = block.setting("button", null);
		if (button != null)
		{
			view.add(new LinkButton(button, url, colours));
		}
		return view;
	}

	/**
	 * The empty setting, or a default inviting people to take part, with {name} replaced by the competition's name.
	 */
	private static String emptyText(Target target, Block block)
	{
		String fallback = target.isBoss() ? "Kill {name} this week to get on the board."
			: target.isSkill() ? "Train {name} this week to get on the board."
			: "Take part this week to get on the board.";
		String name = target.getName() == null ? "the competition" : target.getName();
		return block.setting("empty", fallback).replace("{name}", name);
	}

	private static void show(JPanel container, JComponent content)
	{
		container.removeAll();
		container.add(content);
		container.revalidate();
		container.repaint();
	}

	private static JPanel column()
	{
		JPanel panel = new JPanel(new DynamicGridLayout(0, 1, 0, Spacing.ITEM_GAP));
		panel.setOpaque(false);
		return panel;
	}

	private static JLabel muted(String text, ThemeColours colours)
	{
		return Labels.label(text, colours.getMuted(), FontManager.getRunescapeSmallFont(), Labels.CONTENT_WIDTH, true);
	}

	private static String format(long value)
	{
		return String.format(Locale.ENGLISH, "%,d", value);
	}

	/**
	 * The WOM competition to show, with its type and name when it is one of the current competitions.
	 */
	@Value
	private static class Target
	{
		long womId;
		String type;
		String name;

		boolean isSkill()
		{
			return "skill".equals(type);
		}

		boolean isBoss()
		{
			return "boss".equals(type);
		}
	}
}
