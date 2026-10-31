package com.pvknee.ui;

import java.awt.FlowLayout;
import java.awt.Shape;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

/**
 * A card standing in for an empty list: a muted icon, a bold line and a muted line.
 */
public class EmptyState extends Card
{
	private static final int ICON_SIZE = 26;
	private static final int TEXT_WIDTH = Labels.CONTENT_WIDTH - 2 * Spacing.PADDING;

	public EmptyState(Shape glyph, String title, String text, ThemeColours colours)
	{
		super(new DynamicGridLayout(0, 1, 0, Spacing.TITLE_GAP));

		JPanel iconRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
		iconRow.setOpaque(false);
		iconRow.add(new JLabel(new GlyphIcon(glyph, colours.getMuted(), ICON_SIZE)));
		add(iconRow);

		add(Labels.label(title, colours.getValue(), FontManager.getRunescapeBoldFont(), TEXT_WIDTH, true));
		add(Labels.label(text, colours.getMuted(), FontManager.getRunescapeSmallFont(), TEXT_WIDTH, true));
	}
}
