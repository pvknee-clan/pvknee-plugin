package com.pvknee.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.runelite.client.ui.FontManager;

/**
 * An icon with an optional muted caption above it and a name below it, all centred. Without an icon it is just the
 * caption and the name.
 */
public class IconCaption extends JPanel
{
	public IconCaption(String caption, JComponent icon, String name, Color nameColour, Font nameFont, int width, ThemeColours colours)
	{
		super(new BorderLayout(0, Spacing.ITEM_GAP));
		setOpaque(false);

		if (caption != null)
		{
			add(Labels.label(caption, colours.getMuted(), FontManager.getRunescapeSmallFont(), width, true), BorderLayout.NORTH);
		}

		if (icon != null)
		{
			JPanel iconRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
			iconRow.setOpaque(false);
			iconRow.add(icon);
			add(iconRow, BorderLayout.CENTER);
		}

		add(Labels.label(name, nameColour, nameFont, width, true), BorderLayout.SOUTH);
	}

	public IconCaption(String caption, JComponent icon, String name, Color nameColour, int width, ThemeColours colours)
	{
		this(caption, icon, name, nameColour, Fonts.name(), width, colours);
	}
}
