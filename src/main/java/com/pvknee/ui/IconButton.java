package com.pvknee.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Shape;
import javax.swing.Timer;

/**
 * A square card-style button showing only an icon from {@link Glyphs}, with a tooltip and an accessible name.
 */
public class IconButton extends Card
{
	public static final int SIZE = 30;
	public static final int SMALL_SIZE = 26;
	private static final int ICON_SIZE = 16;
	private static final int SMALL_ICON_SIZE = 12;
	private static final Color ICON = new Color(187, 187, 187);
	private static final Color SMALL_BACKGROUND = new Color(56, 56, 56);
	private static final Color SMALL_BORDER = new Color(72, 72, 72);
	private static final int SPIN_INTERVAL_MS = 40;
	private static final double SPIN_STEP = Math.PI / 12;

	private final Shape glyph;
	private final Timer spinner = new Timer(SPIN_INTERVAL_MS, e -> spin());
	private Color iconColour = ICON;
	private Color spinColour;
	private int iconSize = ICON_SIZE;
	private double angle;

	public IconButton(Shape glyph, String name, Runnable action)
	{
		super(new FlowLayout());
		this.glyph = glyph;
		setArc(Spacing.CONTROL_ARC);
		setPreferredSize(new Dimension(SIZE, SIZE));
		setToolTipText(name);
		getAccessibleContext().setAccessibleName(name);
		onClick(action);
	}

	/**
	 * Makes the button smaller and a little lighter, for use inside a card, e.g. a switcher's arrows.
	 */
	public IconButton small()
	{
		iconSize = SMALL_ICON_SIZE;
		setPreferredSize(new Dimension(SMALL_SIZE, SMALL_SIZE));
		setBackground(SMALL_BACKGROUND);
		setOutline(SMALL_BORDER);
		return this;
	}

	/**
	 * Outlines the button and tints its icon in this colour, e.g. the accent while its page is shown.
	 * Null restores the default look.
	 */
	public void setHighlight(Color colour)
	{
		iconColour = colour == null ? ICON : colour;
		setOutline(colour);
	}

	/**
	 * Turns the icon continuously in this colour, e.g. the accent while loading. Null stops it and restores the icon.
	 */
	public void setSpinning(Color colour)
	{
		if (colour != null)
		{
			spinColour = colour;
			spinner.start();
		}
		else
		{
			spinColour = null;
			spinner.stop();
			angle = 0;
		}
		repaint();
	}

	private void spin()
	{
		angle = (angle + SPIN_STEP) % (2 * Math.PI);
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		Glyphs.draw(g, glyph, spinColour == null ? iconColour : spinColour, iconSize, getWidth() / 2.0, getHeight() / 2.0, angle);
	}
}
