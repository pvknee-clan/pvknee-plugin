package com.pvknee.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.FontManager;

/**
 * A podium place: "1st  Name" in the medal colour, the value underneath in muted.
 */
public class MedalRow extends JPanel
{
	private static final Color[] MEDALS = {
		new Color(0xFFD700),
		new Color(0xC0C0C0),
		new Color(0xCD7F32),
	};

	public MedalRow(int place, String name, String value, ThemeColours colours)
	{
		super(new BorderLayout());
		setOpaque(false);

		Color colour = place >= 1 && place <= MEDALS.length ? MEDALS[place - 1] : colours.getValue();
		add(line(ordinal(place) + "  " + name, colour, true), BorderLayout.NORTH);
		add(line(value, colours.getMuted(), false), BorderLayout.CENTER);
	}

	private static JLabel line(String text, Color colour, boolean name)
	{
		JLabel label = new JLabel(text);
		label.setForeground(colour);
		label.setFont(name ? Fonts.name() : FontManager.getRunescapeSmallFont());
		label.setHorizontalAlignment(SwingConstants.CENTER);
		return label;
	}

	private static String ordinal(int place)
	{
		int lastTwo = place % 100;
		if (lastTwo >= 11 && lastTwo <= 13)
		{
			return place + "th";
		}

		switch (place % 10)
		{
			case 1:
				return place + "st";
			case 2:
				return place + "nd";
			case 3:
				return place + "rd";
			default:
				return place + "th";
		}
	}
}
