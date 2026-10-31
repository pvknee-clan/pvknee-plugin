package com.pvknee.ui;

import java.awt.FlowLayout;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;

/**
 * A card-style button with centred text and a small muted "external" icon after it, that opens a web page.
 */
public class LinkButton extends Card
{
	private static final int ICON_SIZE = 12;
	private static final int ICON_GAP = 6;

	public LinkButton(String text, String url, ThemeColours colours)
	{
		super(new FlowLayout(FlowLayout.CENTER, 0, 0));
		setToolTipText(url);
		onClick(() -> LinkBrowser.browse(url));

		JLabel label = new JLabel(text);
		label.setForeground(colours.getValue());
		label.setFont(FontManager.getRunescapeFont());
		label.setIcon(new GlyphIcon(Glyphs.EXTERNAL, colours.getMuted(), ICON_SIZE));
		label.setHorizontalTextPosition(SwingConstants.LEFT);
		label.setIconTextGap(ICON_GAP);
		add(label);
	}
}
