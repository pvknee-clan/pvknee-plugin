package com.pvknee.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.Collections;
import java.util.Locale;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Wrapped text labels for the panel.
 */
public final class Labels
{
	/** Text width on a page: the panel minus its side borders and room for the scrollbar. */
	public static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH - 20;
	/** Names wrap to at most this many lines. */
	public static final int NAME_LINES = 2;

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
	 * A wrapped label of at most this many lines; longer text is cut short and ends with "...".
	 */
	public static JLabel clamped(String text, Color colour, Font font, int width, boolean centred, int maxLines)
	{
		JLabel label = label(String.join("\n", Collections.nCopies(maxLines, "X")), colour, font, width, centred);
		int maxHeight = label.getPreferredSize().height;
		setText(label, text, width, centred);
		if (label.getPreferredSize().height <= maxHeight)
		{
			return label;
		}

		// The longest start of the text that still fits with "..." after it.
		int low = 0;
		int high = text.length();
		while (low < high)
		{
			int mid = (low + high + 1) / 2;
			setText(label, shortened(text, mid), width, centred);
			if (label.getPreferredSize().height <= maxHeight)
			{
				low = mid;
			}
			else
			{
				high = mid - 1;
			}
		}
		setText(label, shortened(text, low), width, centred);
		label.setToolTipText(text);
		return label;
	}

	/**
	 * A whole number with thousands separators, e.g. 12,345.
	 */
	public static String number(long value)
	{
		return String.format(Locale.ENGLISH, "%,d", value);
	}

	/**
	 * A place as an ordinal, e.g. 1st, 2nd, 11th.
	 */
	public static String ordinal(int number)
	{
		int lastTwo = number % 100;
		int last = number % 10;
		String suffix = lastTwo >= 11 && lastTwo <= 13 ? "th" : last == 1 ? "st" : last == 2 ? "nd" : last == 3 ? "rd" : "th";
		return number + suffix;
	}

	private static String shortened(String text, int length)
	{
		return text.substring(0, length).trim() + "...";
	}

	/**
	 * Styles a time caption: muted, or bold in the ending-soon colour in an item's final 24 hours.
	 */
	public static void styleTime(JLabel label, boolean endingSoon, ThemeColours colours)
	{
		label.setForeground(endingSoon ? ThemeColours.ENDING_SOON : colours.getMuted());
		label.setFont(endingSoon ? Fonts.smallBold() : FontManager.getRunescapeSmallFont());
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
