package com.pvknee.blocks;

import java.awt.Color;
import java.awt.Font;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.FontManager;

/**
 * Wrapped text labels for the panel.
 */
public final class Labels
{
	public static final int CONTENT_WIDTH = 175;

	private Labels()
	{
	}

	public static JLabel label(String text, Color colour, Font font, int width, boolean centred)
	{
		JLabel label = new JLabel(html(text, width, centred));
		label.setForeground(colour);
		label.setFont(font);
		label.setHorizontalAlignment(centred ? SwingConstants.CENTER : SwingConstants.LEFT);
		return label;
	}

	/**
	 * The optional heading some blocks draw above themselves.
	 */
	public static JLabel title(String text, ThemeColours colours)
	{
		return label(text, colours.getAccent(), FontManager.getRunescapeBoldFont(), CONTENT_WIDTH, true);
	}

	public static String html(String text, int width, boolean centred)
	{
		String body = text
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\r\n", "\n")
			.replace("\n", "<br>");
		if (centred)
		{
			body = "<div style='text-align:center'>" + body + "</div>";
		}
		return "<html><body style='width:" + width + "px'>" + body + "</body></html>";
	}
}
