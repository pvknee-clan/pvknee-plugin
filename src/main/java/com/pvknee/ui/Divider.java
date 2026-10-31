package com.pvknee.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import javax.swing.JComponent;

/**
 * A faint one-pixel line across the full width, for a real break between sections.
 */
public class Divider extends JComponent
{
	private static final int STRENGTH = 64;

	private final Color colour;

	public Divider(Color colour)
	{
		this.colour = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), STRENGTH);
		setPreferredSize(new Dimension(0, 1));
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		g.setColor(colour);
		g.fillRect(0, 0, getWidth(), 1);
	}
}
