package com.pvknee.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import net.runelite.client.ui.PluginPanel;

/**
 * Wrapped text labels for the panel.
 */
public final class Labels
{
	/** Text width on a page: the panel minus its side borders and room for the scrollbar. */
	public static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH - 20;

	private Labels()
	{
	}

	public static JLabel label(String text, Color colour, Font font, int width, boolean centred)
	{
		JLabel label = new JLabel();
		label.setForeground(colour);
		label.setFont(font);
		label.setHorizontalAlignment(centred ? SwingConstants.CENTER : SwingConstants.LEFT);
		setText(label, text, width, centred);
		return label;
	}

	/**
	 * Sets wrapped text on a label and sizes it to fit that text at the given pixel width.
	 * Set the label's font first, as it decides the height.
	 */
	public static void setText(JLabel label, String text, int width, boolean centred)
	{
		label.setText(html(text, centred));
		View view = (View) label.getClientProperty(BasicHTML.propertyKey);
		view.setSize(width, 0);
		Insets insets = label.getInsets();
		label.setPreferredSize(new Dimension(
			width + insets.left + insets.right,
			(int) Math.ceil(view.getPreferredSpan(View.Y_AXIS)) + insets.top + insets.bottom));
	}

	private static String html(String text, boolean centred)
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
		return "<html>" + body + "</html>";
	}
}
