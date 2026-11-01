package com.pvknee.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * A place as a word in a small rounded pill, e.g. "1st", in dark text on its place colour: gold, silver and bronze
 * for the top three, grey below.
 */
public class PlacePill extends JComponent
{
	static final int HEIGHT = 18;
	private static final int PADDING = 7;

	private final String text;
	private final Color colour;

	public PlacePill(int rank)
	{
		this.text = Labels.ordinal(rank);
		this.colour = RankBadge.placeColour(rank);
		setFont(FontManager.getRunescapeSmallFont());
		setPreferredSize(new Dimension(getFontMetrics(getFont()).stringWidth(text) + 2 * PADDING, HEIGHT));
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		int width = getPreferredSize().width;
		int x = (getWidth() - width) / 2;
		int y = (getHeight() - HEIGHT) / 2;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(colour);
		g2.fillRoundRect(x, y, width, HEIGHT, HEIGHT, HEIGHT);

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		g2.setFont(getFont());
		g2.setColor(ColorScheme.DARK_GRAY_COLOR);
		int textX = x + (width - g2.getFontMetrics().stringWidth(text)) / 2;
		int textY = y + (HEIGHT + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;
		g2.drawString(text, textX, textY);
		g2.dispose();
	}
}
