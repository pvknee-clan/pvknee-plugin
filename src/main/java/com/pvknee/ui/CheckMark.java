package com.pvknee.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;

/**
 * A round mark for something done or not: a green disc with a dark tick, or an empty outlined circle.
 */
public class CheckMark implements Icon
{
	private static final Color TICK = new Color(27, 27, 27);
	private static final Color EMPTY = new Color(90, 90, 90);

	private final boolean done;
	private final int size;

	public CheckMark(boolean done, int size)
	{
		this.done = done;
		this.size = size;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if (done)
		{
			g2.setColor(ThemeColours.DONE);
			g2.fillOval(x, y, size, size);
			Glyphs.draw(g2, Glyphs.TICK, TICK, size - 3, x + size / 2.0, y + size / 2.0, 0, 4f);
		}
		else
		{
			g2.setColor(EMPTY);
			g2.drawOval(x, y, size - 1, size - 1);
		}
		g2.dispose();
	}

	@Override
	public int getIconWidth()
	{
		return size;
	}

	@Override
	public int getIconHeight()
	{
		return size;
	}
}
