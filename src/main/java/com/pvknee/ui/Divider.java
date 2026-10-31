package com.pvknee.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;

/**
 * A faint one-pixel line across the full width, for a real break between sections, or down the full height to
 * separate columns.
 */
public class Divider extends JComponent
{
	private static final int STRENGTH = 64;

	private final Color colour;

	public Divider(Color colour)
	{
		this(colour, false);
	}

	private Divider(Color colour, boolean vertical)
	{
		this.colour = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), STRENGTH);
		setPreferredSize(vertical ? new Dimension(1, 0) : new Dimension(0, 1));
	}

	/**
	 * A line down the full height, e.g. between columns in a card.
	 */
	public static Divider vertical(Color colour)
	{
		return new Divider(colour, true);
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		g.setColor(colour);
		g.fillRect(0, 0, getWidth(), getHeight());
	}
}
