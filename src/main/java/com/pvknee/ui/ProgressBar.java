package com.pvknee.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/**
 * A thin rounded bar filled from the left, e.g. a team's tiles done.
 */
public class ProgressBar extends JComponent
{
	private static final int HEIGHT = 5;

	private final double fraction;
	private final Color colour;

	/**
	 * @param fraction how full the bar is, between 0 and 1
	 */
	public ProgressBar(double fraction, Color colour)
	{
		this.fraction = Math.max(0, Math.min(1, fraction));
		this.colour = colour;
		setPreferredSize(new Dimension(0, HEIGHT));
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int y = (getHeight() - HEIGHT) / 2;
		g2.setColor(Card.BORDER);
		g2.fillRoundRect(0, y, getWidth(), HEIGHT, HEIGHT, HEIGHT);

		// Anything started shows at least a dot, so a little progress isn't lost.
		int filled = fraction == 0 ? 0 : Math.max(HEIGHT, (int) Math.round(getWidth() * fraction));
		if (filled > 0)
		{
			g2.setColor(colour);
			g2.fillRoundRect(0, y, filled, HEIGHT, HEIGHT, HEIGHT);
		}
		g2.dispose();
	}
}
