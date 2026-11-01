package com.pvknee.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Line icons on a 24 by 24 grid, copied from the SVG paths in the mock-ups and drawn stroked with round caps and
 * joins, for {@link IconButton} and {@link GlyphIcon}.
 */
public final class Glyphs
{
	/** Grid size the shapes are drawn on. */
	private static final int GRID_SIZE = 24;
	private static final float STROKE = 2.2f;
	private static final Pattern TOKEN = Pattern.compile("[A-Za-z]|-?(?:\\d+\\.?\\d*|\\.\\d+)");

	public static final Shape HOME = path("M3 11l9-7 9 7M5 10v10h14V10M10 20v-6h4v6");
	public static final Shape REFRESH = path("M20 12a8 8 0 1 1-2.34-5.66M20 4v5h-5");
	public static final Shape CHEVRON = path("M6 9l6 6 6-6");
	public static final Shape CHEVRON_LEFT = path("M15 6l-6 6 6 6");
	public static final Shape CHEVRON_RIGHT = path("M9 6l6 6-6 6");
	public static final Shape GLOBE = join(circle(12, 12, 9), path("M3 12h18M12 3c3 3 3 15 0 18M12 3c-3 3-3 15 0 18"));
	public static final Shape CHAT = join(
		path("M5 6c4-2 10-2 14 0l2 10c-2 2-4 3-6 3l-1-2M10 17l-1 2c-2 0-4-1-6-3L5 6"),
		circle(9, 12, 1.2f),
		circle(15, 12, 1.2f));
	public static final Shape GROUP = join(
		circle(9, 8, 3),
		circle(17, 9, 2.5f),
		path("M3 19c0-3 3-5 6-5s6 2 6 5M15 14c3 0 6 1.5 6 4.5"));
	public static final Shape CHART = path("M4 20V13h5v7M9 20V8h6v12M15 20v-9h5v9M3 20h18");
	public static final Shape TROPHY = path("M7 4h10v5a5 5 0 0 1-10 0V4zM7 6H4a3 3 0 0 0 3 4M17 6h3a3 3 0 0 1-3 4M12 14v3M8 20h8M9 20l1-3h4l1 3");
	public static final Shape EXTERNAL = path("M14 4h6v6M20 4l-9 9M18 14v6H4V6h6");
	public static final Shape CALENDAR = join(rect(4, 5, 16, 15, 2), path("M4 10h16M9 3v4M15 3v4"));
	public static final Shape TICK = path("M5 12l5 5 9-10");
	public static final Shape GRID = join(rect(3, 3, 18, 18, 2), path("M9 3v18M15 3v18M3 9h18M3 15h18"));

	private static final Map<String, Shape> NAMED = named();

	private Glyphs()
	{
	}

	/**
	 * A glyph by its name in the sheet, e.g. "chat" or "trophy". Null if there is no such glyph.
	 */
	public static Shape named(String name)
	{
		return name == null ? null : NAMED.get(name.trim().toLowerCase(Locale.ENGLISH));
	}

	/**
	 * Draws a glyph at the given pixel size, centred on a point and turned by an angle in radians.
	 */
	static void draw(Graphics g, Shape glyph, Color colour, int size, double centreX, double centreY, double angle)
	{
		draw(g, glyph, colour, size, centreX, centreY, angle, STROKE);
	}

	/**
	 * Draws a glyph with a stroke of its own on the 24 by 24 grid, e.g. a heavier one for a tiny glyph.
	 */
	static void draw(Graphics g, Shape glyph, Color colour, int size, double centreX, double centreY, double angle, float stroke)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
		g2.translate(centreX, centreY);
		g2.rotate(angle);
		g2.scale((double) size / GRID_SIZE, (double) size / GRID_SIZE);
		g2.translate(-GRID_SIZE / 2.0, -GRID_SIZE / 2.0);
		g2.setColor(colour);
		g2.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2.draw(glyph);
		g2.dispose();
	}

	private static Map<String, Shape> named()
	{
		Map<String, Shape> named = new HashMap<>();
		named.put("home", HOME);
		named.put("refresh", REFRESH);
		named.put("chevron", CHEVRON);
		named.put("globe", GLOBE);
		named.put("chat", CHAT);
		named.put("group", GROUP);
		named.put("chart", CHART);
		named.put("trophy", TROPHY);
		named.put("external", EXTERNAL);
		named.put("calendar", CALENDAR);
		named.put("grid", GRID);
		return Collections.unmodifiableMap(named);
	}

	private static Shape circle(float cx, float cy, float r)
	{
		return new Ellipse2D.Float(cx - r, cy - r, 2 * r, 2 * r);
	}

	private static Shape rect(float x, float y, float width, float height, float radius)
	{
		return new RoundRectangle2D.Float(x, y, width, height, 2 * radius, 2 * radius);
	}

	private static Shape join(Shape... shapes)
	{
		Path2D path = new Path2D.Float();
		for (Shape shape : shapes)
		{
			path.append(shape, false);
		}
		return path;
	}

	/**
	 * Reads SVG path data: the M, L, H, V, C, S, A and Z commands, absolute and relative. Arcs must not be rotated.
	 */
	private static Shape path(String data)
	{
		List<String> tokens = new ArrayList<>();
		Matcher matcher = TOKEN.matcher(data);
		while (matcher.find())
		{
			tokens.add(matcher.group());
		}

		Path2D path = new Path2D.Double();
		double x = 0;
		double y = 0;
		double startX = 0;
		double startY = 0;
		// Second control point of the last curve, reflected by S
		Point2D control = null;
		char command = 'M';
		int i = 0;
		while (i < tokens.size())
		{
			if (Character.isLetter(tokens.get(i).charAt(0)))
			{
				command = tokens.get(i++).charAt(0);
			}

			boolean relative = Character.isLowerCase(command);
			double baseX = relative ? x : 0;
			double baseY = relative ? y : 0;
			Point2D lastControl = control;
			control = null;
			switch (Character.toUpperCase(command))
			{
				case 'M':
					x = baseX + number(tokens, i++);
					y = baseY + number(tokens, i++);
					path.moveTo(x, y);
					startX = x;
					startY = y;
					// Further pairs after a move are lines
					command = relative ? 'l' : 'L';
					break;
				case 'L':
					x = baseX + number(tokens, i++);
					y = baseY + number(tokens, i++);
					path.lineTo(x, y);
					break;
				case 'H':
					x = baseX + number(tokens, i++);
					path.lineTo(x, y);
					break;
				case 'V':
					y = baseY + number(tokens, i++);
					path.lineTo(x, y);
					break;
				case 'C':
				case 'S':
				{
					double x1;
					double y1;
					if (Character.toUpperCase(command) == 'C')
					{
						x1 = baseX + number(tokens, i++);
						y1 = baseY + number(tokens, i++);
					}
					else
					{
						x1 = lastControl == null ? x : 2 * x - lastControl.getX();
						y1 = lastControl == null ? y : 2 * y - lastControl.getY();
					}
					double x2 = baseX + number(tokens, i++);
					double y2 = baseY + number(tokens, i++);
					x = baseX + number(tokens, i++);
					y = baseY + number(tokens, i++);
					path.curveTo(x1, y1, x2, y2, x, y);
					control = new Point2D.Double(x2, y2);
					break;
				}
				case 'A':
				{
					double rx = number(tokens, i++);
					double ry = number(tokens, i++);
					i++; // rotation
					boolean large = number(tokens, i++) != 0;
					boolean sweep = number(tokens, i++) != 0;
					double endX = baseX + number(tokens, i++);
					double endY = baseY + number(tokens, i++);
					path.append(arc(x, y, rx, ry, large, sweep, endX, endY), true);
					x = endX;
					y = endY;
					break;
				}
				case 'Z':
					path.closePath();
					x = startX;
					y = startY;
					break;
				default:
					throw new IllegalArgumentException("Unsupported path command " + command);
			}
		}
		return path;
	}

	private static double number(List<String> tokens, int index)
	{
		return Double.parseDouble(tokens.get(index));
	}

	/**
	 * An SVG arc from (x1, y1) to (x2, y2) as an {@link Arc2D}, finding its centre as the SVG spec describes.
	 */
	private static Arc2D arc(double x1, double y1, double rx, double ry, boolean large, boolean sweep, double x2, double y2)
	{
		double hx = (x1 - x2) / 2;
		double hy = (y1 - y2) / 2;
		double scale = hx * hx / (rx * rx) + hy * hy / (ry * ry);
		if (scale > 1)
		{
			rx *= Math.sqrt(scale);
			ry *= Math.sqrt(scale);
		}

		double numerator = rx * rx * ry * ry - rx * rx * hy * hy - ry * ry * hx * hx;
		double denominator = rx * rx * hy * hy + ry * ry * hx * hx;
		double coefficient = (large == sweep ? -1 : 1) * Math.sqrt(Math.max(0, numerator / denominator));
		double cx = coefficient * rx * hy / ry + (x1 + x2) / 2;
		double cy = -coefficient * ry * hx / rx + (y1 + y2) / 2;

		double start = Math.atan2((y1 - cy) / ry, (x1 - cx) / rx);
		double extent = Math.atan2((y2 - cy) / ry, (x2 - cx) / rx) - start;
		if (sweep && extent < 0)
		{
			extent += 2 * Math.PI;
		}
		else if (!sweep && extent > 0)
		{
			extent -= 2 * Math.PI;
		}

		// Arc2D measures angles with y pointing up, so both turn the other way
		return new Arc2D.Double(cx - rx, cy - ry, 2 * rx, 2 * ry, -Math.toDegrees(start), -Math.toDegrees(extent), Arc2D.OPEN);
	}
}
