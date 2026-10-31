package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.data.Competition;
import com.pvknee.data.Competitions;
import com.pvknee.ui.Card;
import com.pvknee.ui.Labels;
import com.pvknee.ui.MedalRow;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import com.pvknee.wom.Standing;
import com.pvknee.wom.WomClient;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;

@Slf4j
class LeaderboardBlock implements BlockRenderer
{
	private static final int RANK_WIDTH = 24;

	@Override
	public JComponent render(Block block, BlockContext context)
	{
		Long competitionId = competitionId(block.setting("competition", "current_skill"), context);
		if (competitionId == null)
		{
			log.debug("No competition for leaderboard {}", block.getSettings());
			return null;
		}

		ThemeColours colours = context.getColours();
		JPanel standings = column();
		standings.add(muted("Loading standings...", colours));

		JPanel panel = column();
		panel.add(standings);
		String button = block.setting("button", null);
		if (button != null)
		{
			panel.add(button(button, WomClient.competitionPage(competitionId), colours));
		}

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
					show(standings, standingsView(result, block, colours));
				}
			}
		};
		Runnable load = () -> context.getWom().standings(competitionId, onStandings,
			error -> show(standings, muted("Couldn't load standings", colours)));
		load.run();
		context.onUpdate(load);
		return panel;
	}

	/**
	 * current_skill and current_boss pick from the current competitions; anything else is a WOM id.
	 */
	private static Long competitionId(String setting, BlockContext context)
	{
		String type = setting.equalsIgnoreCase("current_skill") ? "skill"
			: setting.equalsIgnoreCase("current_boss") ? "boss"
			: null;
		if (type == null)
		{
			try
			{
				return Long.parseLong(setting);
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
			.map(Competition::getWomId)
			.findFirst()
			.orElse(null);
	}

	private static JComponent standingsView(List<Standing> standings, Block block, ThemeColours colours)
	{
		List<Standing> gainers = standings.stream()
			.filter(s -> s.getGained() > 0)
			.limit(Math.max(0, block.intSetting("rows", 20)))
			.collect(Collectors.toList());
		if (gainers.isEmpty())
		{
			return muted("No gains yet", colours);
		}

		String prefix = block.setting("prefix", "");
		int podium = Math.min(gainers.size(), Math.max(0, block.intSetting("podium", 3)));
		JPanel view = column();

		if (podium > 0)
		{
			Card podiumCard = new Card(new DynamicGridLayout(0, 1, 0, Spacing.ITEM_GAP));
			for (int i = 0; i < podium; i++)
			{
				Standing standing = gainers.get(i);
				podiumCard.add(new MedalRow(i + 1, standing.getName(), prefix + format(standing.getGained()), colours));
			}
			view.add(podiumCard);
		}

		if (gainers.size() > podium)
		{
			Card table = new Card(new DynamicGridLayout(0, 1, 0, 2));
			Font headerFont = FontManager.getRunescapeSmallFont();
			table.add(row("#", "Player", block.setting("label", "Gained"), colours.getMuted(), colours.getMuted(), headerFont));
			for (int i = podium; i < gainers.size(); i++)
			{
				Standing standing = gainers.get(i);
				table.add(row(String.valueOf(i + 1), standing.getName(), prefix + format(standing.getGained()),
					colours.getMuted(), colours.getValue(), FontManager.getRunescapeFont()));
			}
			view.add(table);
		}
		return view;
	}

	private static JPanel row(String rank, String name, String value, Color rankColour, Color colour, Font font)
	{
		JPanel row = new JPanel(new BorderLayout(Spacing.ITEM_GAP, 0));
		row.setOpaque(false);

		JLabel rankLabel = text(rank, rankColour, font, SwingConstants.LEFT);
		rankLabel.setPreferredSize(new Dimension(RANK_WIDTH, rankLabel.getPreferredSize().height));
		row.add(rankLabel, BorderLayout.WEST);
		row.add(text(name, colour, font, SwingConstants.LEFT), BorderLayout.CENTER);
		row.add(text(value, colour, font, SwingConstants.RIGHT), BorderLayout.EAST);
		return row;
	}

	private static Card button(String text, String url, ThemeColours colours)
	{
		Card card = new Card(new FlowLayout(FlowLayout.CENTER, 0, 0));
		card.setToolTipText(url);
		card.onClick(() -> LinkBrowser.browse(url));
		card.add(text(text, colours.getValue(), FontManager.getRunescapeFont(), SwingConstants.CENTER));
		return card;
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

	private static JLabel text(String text, Color colour, Font font, int alignment)
	{
		JLabel label = new JLabel(text);
		label.setForeground(colour);
		label.setFont(font);
		label.setHorizontalAlignment(alignment);
		return label;
	}

	private static String format(long value)
	{
		return String.format(Locale.ENGLISH, "%,d", value);
	}
}
