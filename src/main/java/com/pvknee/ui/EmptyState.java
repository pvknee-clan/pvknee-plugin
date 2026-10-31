package com.pvknee.ui;

import java.awt.FlowLayout;
import java.awt.Shape;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

/**
 * A card standing in for an empty list: a muted icon, a bold line and a muted line. Without the bold line (while
 * loading, or after an error) the card keeps its height, so the page does not jump when the bold line arrives.
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

		if (title != null)
		{
			add(Labels.label(title, colours.getValue(), FontManager.getRunescapeBoldFont(), TEXT_WIDTH, true));
		}
		add(Labels.label(text, colours.getMuted(), FontManager.getRunescapeSmallFont(), TEXT_WIDTH, true));
		if (title == null)
		{
			add(Labels.label(" ", colours.getValue(), FontManager.getRunescapeBoldFont(), TEXT_WIDTH, true));
		}
	}

	/**
	 * The muted icon and one muted line, at the size of a full empty state.
	 */
	public static EmptyState quiet(Shape glyph, String text, ThemeColours colours)
	{
		return new EmptyState(glyph, null, text, colours);
	}
}
