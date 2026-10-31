package com.pvknee.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Shape;
import javax.swing.Icon;

/**
 * A {@link Glyphs} line icon as a Swing icon, e.g. for a list entry that has no picture of its own.
 */
public class GlyphIcon implements Icon
{
	private final Shape glyph;
	private final Color colour;
	private final int size;

	public GlyphIcon(Shape glyph, Color colour, int size)
	{
		this.glyph = glyph;
		this.colour = colour;
		this.size = size;
	}

	@Override
	public void paintIcon(Component c, Graphics g, int x, int y)
	{
		Glyphs.draw(g, glyph, colour, size, x + size / 2.0, y + size / 2.0, 0);
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
