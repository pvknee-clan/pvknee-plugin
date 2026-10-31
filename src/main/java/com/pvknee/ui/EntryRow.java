package com.pvknee.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

/**
 * An entry in a {@link ListCard}: an icon in a fixed slot so names line up, the name, muted lines under it, and
 * optionally a bar across the bottom. Clickable like a card without a border.
 */
public class EntryRow extends Card
{
	public static final int ICON_SIZE = 20;
	private static final int ICON_SLOT = 24;

	private final JPanel lines = new JPanel(new DynamicGridLayout(0, 1, 0, 1));
	private final int textWidth;

	/**
	 * @param width the width the row is given, e.g. the inside of its list card
	 */
	public EntryRow(JComponent icon, String name, Color nameColour, int width)
	{
		super(new BorderLayout(0, Spacing.ITEM_GAP), false);
		setBorder(new EmptyBorder(Spacing.ITEM_GAP, Spacing.ITEM_GAP, Spacing.ITEM_GAP, Spacing.ITEM_GAP));
		textWidth = width - 2 * Spacing.ITEM_GAP - ICON_SLOT - Spacing.PADDING;

		JPanel slot = new JPanel(new GridBagLayout());
		slot.setOpaque(false);
		slot.setPreferredSize(new Dimension(ICON_SLOT, ICON_SIZE));
		if (icon != null)
		{
			slot.add(icon);
		}

		lines.setOpaque(false);
		lines.add(Labels.label(name, nameColour, Fonts.name(), textWidth, false));

		JPanel top = new JPanel(new BorderLayout(Spacing.PADDING, 0));
		top.setOpaque(false);
		top.add(slot, BorderLayout.WEST);
		top.add(lines, BorderLayout.CENTER);
		add(top, BorderLayout.CENTER);
	}

	/**
	 * The width lines under the name wrap at.
	 */
	public int getTextWidth()
	{
		return textWidth;
	}

	/**
	 * Adds a muted line under the name, e.g. what the entry is.
	 */
	public void addCaption(String text, ThemeColours colours)
	{
		addLine(Labels.label(text, colours.getMuted(), FontManager.getRunescapeSmallFont(), textWidth, false));
	}

	/**
	 * Adds any line under the name, e.g. a {@link LiveLabel} countdown.
	 */
	public void addLine(JComponent line)
	{
		lines.add(line);
	}

	/**
	 * Puts a bar across the bottom of the row, under the icon and the text.
	 */
	public void setBar(JComponent bar)
	{
		add(bar, BorderLayout.SOUTH);
	}
}
