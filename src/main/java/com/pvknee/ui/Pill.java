package com.pvknee.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.FontManager;

/**
 * A short piece of text in a small rounded outline, optionally after an icon, e.g. "Home world 509". Clickable like
 * a card through {@link #onClick}, filling with the hover tint under the mouse.
 */
public class Pill extends Card
{
	/** The size of the glyph before the text. */
	public static final int ICON_SIZE = 13;
	private static final int PADDING_X = 9;
	private static final int PADDING_Y = 3;
	private static final int ICON_GAP = 5;

	private final JLabel label;

	public Pill(String text, Color colour, Icon icon)
	{
		super(new BorderLayout());
		setBorder(new EmptyBorder(PADDING_Y, PADDING_X, PADDING_Y, PADDING_X));

		label = new JLabel(text);
		label.setForeground(colour);
		label.setFont(Fonts.body());
		label.setIcon(icon);
		label.setIconTextGap(ICON_GAP);
		add(label, BorderLayout.CENTER);
	}

	/**
	 * Makes the text bold, e.g. for points.
	 */
	public Pill bold()
	{
		label.setFont(FontManager.getRunescapeBoldFont());
		return this;
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		if (isHovered())
		{
			g2.setColor(HOVER);
			g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight() - 1, getHeight() - 1);
		}
		g2.setColor(BORDER);
		g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight() - 1, getHeight() - 1);
		g2.dispose();
	}
}
