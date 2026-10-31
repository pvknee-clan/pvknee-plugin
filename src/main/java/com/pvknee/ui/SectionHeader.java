package com.pvknee.ui;

import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.FontManager;

/**
 * A centred bold title, in the accent colour unless told otherwise.
 */
public class SectionHeader extends JLabel
{
	private static final float DEFAULT_SIZE = 16f;

	public SectionHeader(String text, ThemeColours colours)
	{
		this(text, colours.getAccent(), DEFAULT_SIZE);
	}

	public SectionHeader(String text, Color colour, float size)
	{
		super(Labels.html(text, Labels.CONTENT_WIDTH, true));
		setForeground(colour);
		setFont(FontManager.getRunescapeBoldFont().deriveFont(size));
		setHorizontalAlignment(SwingConstants.CENTER);
		setBorder(new EmptyBorder(0, 0, Spacing.ITEM_GAP, 0));
	}
}
