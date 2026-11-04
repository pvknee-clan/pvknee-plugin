package com.pvknee.ui;

import java.awt.FlowLayout;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.FontManager;

/**
 * A card-style button with centred text and a small muted "external" icon after it, that opens a web page on one of
 * the known sites ({@link WebLinks}).
 */
public class LinkButton extends Card
{
	private static final int ICON_SIZE = 12;
	private static final int ICON_GAP = 6;

	public LinkButton(String text, String url, ThemeColours colours)
	{
		super(new FlowLayout(FlowLayout.CENTER, 0, 0));
		Labels.tooltip(this, url);
		onClick(() -> WebLinks.open(url));

		JLabel label = Labels.plain(text, colours.getValue(), FontManager.getRunescapeFont());
		label.setIcon(new GlyphIcon(Glyphs.EXTERNAL, colours.getMuted(), ICON_SIZE));
		label.setHorizontalTextPosition(SwingConstants.LEFT);
		label.setIconTextGap(ICON_GAP);
		add(label);
	}
}
