package com.pvknee.ui;

import java.awt.FlowLayout;
import javax.swing.JLabel;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;

/**
 * A card-style button with centred text that opens a web page.
 */
public class LinkButton extends Card
{
	public LinkButton(String text, String url, ThemeColours colours)
	{
		super(new FlowLayout(FlowLayout.CENTER, 0, 0));
		setToolTipText(url);
		onClick(() -> LinkBrowser.browse(url));

		JLabel label = new JLabel(text);
		label.setForeground(colours.getValue());
		label.setFont(FontManager.getRunescapeFont());
		add(label);
	}
}
