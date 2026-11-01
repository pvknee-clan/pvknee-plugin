package com.pvknee.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

/**
 * One player's place in a card, e.g. the member's own: a muted caption such as "You", their {@link RankBadge}, their
 * name with a muted line under it, and their value on the right.
 */
public class StandingCard extends Card
{
	public StandingCard(String caption, int rank, boolean medal, String name, String line, String value, ThemeColours colours)
	{
		super(new BorderLayout(2 * Spacing.ITEM_GAP, 0));

		JPanel place = new JPanel(new BorderLayout(2 * Spacing.ITEM_GAP, 0));
		place.setOpaque(false);
		place.add(text(caption, colours.getMuted(), FontManager.getRunescapeSmallFont()), BorderLayout.WEST);
		place.add(RankBadge.of(rank, medal, colours), BorderLayout.EAST);

		JPanel who = new JPanel(new DynamicGridLayout(0, 1, 0, 0));
		who.setOpaque(false);
		who.add(text(name, RankTable.NAME, Fonts.boldName()));
		who.add(text(line, colours.getMuted(), FontManager.getRunescapeSmallFont()));

		add(place, BorderLayout.WEST);
		add(who, BorderLayout.CENTER);
		add(text(value, colours.getValue(), Fonts.boldName()), BorderLayout.EAST);
	}

	private static JLabel text(String text, Color colour, Font font)
	{
		JLabel label = new JLabel(text);
		label.setForeground(colour);
		label.setFont(font);
		return label;
	}
}
