package com.pvknee.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.runelite.client.ui.FontManager;

/**
 * A faint one-pixel line across the full width, for a real break between sections, or down the full height to
 * separate columns.
 */
public class Divider extends JComponent
{
	private static final int STRENGTH = 64;
	private static final int TEXT_GAP = 8;

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

	/**
	 * A line across the full width broken by a short muted word in the middle, e.g. "or" between alternatives.
	 */
	public static JComponent withText(String text, Color colour)
	{
		JPanel row = new JPanel(new GridBagLayout());
		row.setOpaque(false);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		row.add(new Divider(colour), c);
		c.weightx = 0;
		c.insets = new Insets(0, TEXT_GAP, 0, TEXT_GAP);
		row.add(Labels.plain(text, colour, FontManager.getRunescapeSmallFont()), c);
		c.weightx = 1;
		c.insets = new Insets(0, 0, 0, 0);
		row.add(new Divider(colour), c);
		return row;
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		g.setColor(colour);
		g.fillRect(0, 0, getWidth(), getHeight());
	}
}
