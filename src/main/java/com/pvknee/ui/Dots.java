package com.pvknee.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;

/**
 * A centred row of small dots, one per choice, with the current one in a highlight colour, e.g. which team is shown.
 */
public class Dots extends JComponent
{
	private static final int SIZE = 6;
	private static final int GAP = 4;
	private static final Color OFF = new Color(74, 74, 74);

	private final int count;
	private final int current;
	private final Color on;

	public Dots(int count, int current, Color on)
	{
		this.count = count;
		this.current = current;
		this.on = on;
		setPreferredSize(new Dimension(count * SIZE + Math.max(0, count - 1) * GAP, SIZE));
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		int x = (getWidth() - getPreferredSize().width) / 2;
		int y = (getHeight() - SIZE) / 2;
		for (int i = 0; i < count; i++)
		{
			g2.setColor(i == current ? on : OFF);
			g2.fillOval(x + i * (SIZE + GAP), y, SIZE, SIZE);
		}
		g2.dispose();
	}
}
