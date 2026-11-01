package com.pvknee.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;
import javax.swing.JComponent;

/**
 * A centred row of small dots, one per choice, each in its own colour, e.g. a team's place. The current one is a
 * little larger with a ring in its colour; the others are dimmed.
 */
public class Dots extends JComponent
{
	private static final int SIZE = 6;
	private static final int CURRENT_SIZE = 8;
	private static final int RING = 12;
	private static final int GAP = 4;
	private static final int DIMMED = 110;

	private final List<Color> colours;
	private final int current;

	public Dots(List<Color> colours, int current)
	{
		this.colours = colours;
		this.current = current;
		int count = colours.size();
		int width = count * SIZE + Math.max(0, count - 1) * GAP + (current >= 0 && current < count ? RING - SIZE : 0);
		setPreferredSize(new Dimension(width, RING));
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		double x = (getWidth() - getPreferredSize().width) / 2.0;
		double centreY = getHeight() / 2.0;
		for (int i = 0; i < colours.size(); i++)
		{
			Color colour = colours.get(i);
			int slot = i == current ? RING : SIZE;
			double centreX = x + slot / 2.0;
			if (i == current)
			{
				g2.setColor(colour);
				fillCircle(g2, centreX, centreY, CURRENT_SIZE);
				g2.drawOval((int) Math.round(centreX - RING / 2.0), (int) Math.round(centreY - RING / 2.0), RING - 1, RING - 1);
			}
			else
			{
				g2.setColor(new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), DIMMED));
				fillCircle(g2, centreX, centreY, SIZE);
			}
			x += slot + GAP;
		}
		g2.dispose();
	}

	private static void fillCircle(Graphics2D g2, double centreX, double centreY, int size)
	{
		g2.fillOval((int) Math.round(centreX - size / 2.0), (int) Math.round(centreY - size / 2.0), size, size);
	}
}
