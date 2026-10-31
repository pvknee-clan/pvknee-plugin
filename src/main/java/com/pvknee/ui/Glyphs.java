package com.pvknee.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

/**
 * Line icons drawn on a 24 by 24 grid, for {@link IconButton} and {@link GlyphIcon}.
 */
public final class Glyphs
{
	/** Grid size the shapes are drawn on. */
	private static final int GRID = 24;
	private static final float STROKE = 2.2f;

	public static final Shape HOUSE = house();
	public static final Shape REFRESH = refresh();
	public static final Shape CALENDAR = calendar();
	public static final Shape TROPHY = trophy();

	private Glyphs()
	{
	}

	/**
	 * Draws a glyph at the given pixel size, centred on a point and turned by an angle in radians.
	 */
	static void draw(Graphics g, Shape glyph, Color colour, int size, double centreX, double centreY, double angle)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.translate(centreX, centreY);
		g2.rotate(angle);
		g2.scale((double) size / GRID, (double) size / GRID);
		g2.translate(-GRID / 2.0, -GRID / 2.0);
		g2.setColor(colour);
		g2.setStroke(new BasicStroke(STROKE, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2.draw(glyph);
		g2.dispose();
	}

	private static Shape house()
	{
		Path2D path = new Path2D.Float();
		// Roof
		path.moveTo(3, 11);
		path.lineTo(12, 4);
		path.lineTo(21, 11);
		// Walls
		path.moveTo(5, 10);
		path.lineTo(5, 20);
		path.lineTo(19, 20);
		path.lineTo(19, 10);
		// Door
		path.moveTo(10, 20);
		path.lineTo(10, 14);
		path.lineTo(14, 14);
		path.lineTo(14, 20);
		return path;
	}

	private static Shape calendar()
	{
		Path2D path = new Path2D.Float();
		// Page
		path.append(new RoundRectangle2D.Float(4, 5, 16, 15, 4, 4), false);
		// Header line
		path.moveTo(4, 10);
		path.lineTo(20, 10);
		// Rings
		path.moveTo(9, 3);
		path.lineTo(9, 7);
		path.moveTo(15, 3);
		path.lineTo(15, 7);
		return path;
	}

	private static Shape trophy()
	{
		Path2D path = new Path2D.Float();
		// Cup
		path.moveTo(7, 4);
		path.lineTo(17, 4);
		path.lineTo(17, 9);
		path.append(new Arc2D.Float(7, 4, 10, 10, 0, -180, Arc2D.OPEN), true);
		path.closePath();
		// Handles
		path.moveTo(7, 6);
		path.lineTo(4, 6);
		path.quadTo(4, 10, 7, 10);
		path.moveTo(17, 6);
		path.lineTo(20, 6);
		path.quadTo(20, 10, 17, 10);
		// Stem and base
		path.moveTo(12, 14);
		path.lineTo(12, 17);
		path.moveTo(8, 20);
		path.lineTo(16, 20);
		path.moveTo(9, 20);
		path.lineTo(10, 17);
		path.lineTo(14, 17);
		path.lineTo(15, 20);
		return path;
	}

	private static Shape refresh()
	{
		Path2D path = new Path2D.Float();
		// Circle open at the top right, ending at the arrow
		path.append(new Arc2D.Float(4, 4, 16, 16, 0, -315, Arc2D.OPEN), false);
		// Arrow head
		path.moveTo(20, 4);
		path.lineTo(20, 9);
		path.lineTo(15, 9);
		return path;
	}
}
