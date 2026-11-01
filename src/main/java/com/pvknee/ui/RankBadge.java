package com.pvknee.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * A place in the standings: a round medal-coloured badge for the top places, otherwise a muted number, both in a
 * slot of the same width.
 */
public final class RankBadge
{
	private static final Color[] MEDALS = {
		new Color(0xFFD700),
		new Color(0xC0C0C0),
		new Color(0xCD7F32),
	};
	private static final int SLOT = 18;

	private RankBadge()
	{
	}

	/**
	 * The number of places that can get a medal.
	 */
	public static int medalPlaces()
	{
		return MEDALS.length;
	}

	/**
	 * The medal colour of one of the top {@link #medalPlaces()} places.
	 */
	public static Color medalColour(int rank)
	{
		return MEDALS[rank - 1];
	}

	/**
	 * A medal badge, which must be for one of the top {@link #medalPlaces()} places, or a plain muted number.
	 */
	public static JComponent of(int rank, boolean medal, ThemeColours colours)
	{
		if (medal)
		{
			return new Badge(rank, medalColour(rank));
		}

		JLabel label = new JLabel(String.valueOf(rank), SwingConstants.CENTER);
		label.setForeground(colours.getMuted());
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setPreferredSize(new Dimension(SLOT, label.getPreferredSize().height));
		return label;
	}

	/**
	 * A round medal-coloured badge with the place number in dark text.
	 */
	private static class Badge extends JComponent
	{
		private final String number;
		private final Color colour;

		Badge(int rank, Color colour)
		{
			this.number = String.valueOf(rank);
			this.colour = colour;
			setFont(FontManager.getRunescapeSmallFont());
			setPreferredSize(new Dimension(SLOT, SLOT));
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			int y = (getHeight() - SLOT) / 2;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(colour);
			g2.fillOval(0, y, SLOT, SLOT);

			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g2.setFont(getFont());
			g2.setColor(ColorScheme.DARK_GRAY_COLOR);
			int textX = (SLOT - g2.getFontMetrics().stringWidth(number)) / 2;
			int textY = y + (SLOT + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;
			g2.drawString(number, textX, textY);
			g2.dispose();
		}
	}
}
