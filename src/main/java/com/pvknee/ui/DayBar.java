package com.pvknee.ui;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import javax.swing.JComponent;

/**
 * A bar of segments, e.g. one per day, that drains from the left as time passes: elapsed segments are empty, the
 * current one is filled on its right-hand side, the rest are full. Accent colour, or the ending-soon colour.
 */
public class DayBar extends JComponent
{
	private static final int HEIGHT = 6;
	private static final int GAP = 2;
	private static final int ARC = 4;

	private final int segments;
	private final DoubleSupplier fraction;
	private final BooleanSupplier endingSoon;
	private final ThemeColours colours;
	private double elapsed;
	private boolean soon;

	/**
	 * @param fraction how far through its span the item is, between 0 and 1
	 */
	public DayBar(int segments, DoubleSupplier fraction, BooleanSupplier endingSoon, ThemeColours colours)
	{
		this.segments = Math.max(1, segments);
		this.fraction = fraction;
		this.endingSoon = endingSoon;
		this.colours = colours;
		setPreferredSize(new Dimension(0, HEIGHT));
		update();
	}

	/**
	 * Asks how far through its span the item is again, e.g. every minute.
	 */
	public void update()
	{
		elapsed = Math.max(0, Math.min(1, fraction.getAsDouble()));
		soon = endingSoon.getAsBoolean();
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		double width = (getWidth() - GAP * (segments - 1)) / (double) segments;
		for (int i = 0; i < segments; i++)
		{
			int x = (int) Math.round(i * (width + GAP));
			int w = (int) Math.round(i * (width + GAP) + width) - x;
			g2.setColor(Card.BORDER);
			g2.fillRoundRect(x, 0, w, HEIGHT, ARC, ARC);

			double left = Math.max(0, Math.min(1, (i + 1) - elapsed * segments));
			int filled = (int) Math.round(w * left);
			if (filled > 0)
			{
				g2.setColor(soon ? ThemeColours.ENDING_SOON : colours.getAccent());
				g2.fillRoundRect(x + w - filled, 0, filled, HEIGHT, ARC, ARC);
			}
		}
		g2.dispose();
	}
}
