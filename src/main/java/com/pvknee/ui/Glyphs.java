package com.pvknee.ui;

import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Path2D;

/**
 * Line icons drawn on a 24 by 24 grid, for {@link IconButton}.
 */
public final class Glyphs
{
	/** Grid size the shapes are drawn on. */
	static final int GRID = 24;

	public static final Shape HOUSE = house();
	public static final Shape REFRESH = refresh();

	private Glyphs()
	{
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
