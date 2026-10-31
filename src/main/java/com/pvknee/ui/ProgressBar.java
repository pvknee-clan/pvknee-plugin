package com.pvknee.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/**
 * A thin rounded bar filled to a fraction between 0 and 1.
 */
public class ProgressBar extends JComponent
{
	private static final int HEIGHT = 4;
	private final Color colour;
	private double fraction;

	public ProgressBar(Color colour, double fraction)
	{
		this.colour = colour;
		setFraction(fraction);
		setPreferredSize(new Dimension(0, HEIGHT));
	}

	public void setFraction(double fraction)
	{
		this.fraction = Math.max(0, Math.min(1, fraction));
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(Card.BORDER);
		g2.fillRoundRect(0, 0, getWidth(), HEIGHT, HEIGHT, HEIGHT);

		int filled = (int) Math.round(getWidth() * fraction);
		if (filled > 0)
		{
			g2.setColor(colour);
			g2.fillRoundRect(0, 0, filled, HEIGHT, HEIGHT, HEIGHT);
		}
		g2.dispose();
	}
}
