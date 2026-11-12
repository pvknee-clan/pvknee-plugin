package com.pvknee.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.util.Collections;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * Labels for the panel. Text from the sheet or Wise Old Man is always shown as written: wrapped labels escape it, and
 * one-line labels and tooltips never render it as HTML, so it cannot load images from other sites.
 */
public final class Labels
{
	/** Text width on a page: the panel minus its side borders and room for the scrollbar. */
	public static final int CONTENT_WIDTH = PluginPanel.PANEL_WIDTH - 20;
	/** Names wrap to at most this many lines. */
	public static final int NAME_LINES = 2;
	/** Clamped text is measured up to this many characters. */
	private static final int MAX_MEASURED = 2000;

	private Labels()
	{
	}

	/**
	 * A one-line label that shows its text as written, never as HTML.
	 */
	public static JLabel plain(String text, Color colour, Font font)
	{
		return plain(text, colour, font, SwingConstants.LEADING);
	}

	public static JLabel plain(String text, Color colour, Font font, int alignment)
	{
		JLabel label = new JLabel();
		noHtml(label);
		label.setForeground(colour);
		label.setFont(font);
		label.setHorizontalAlignment(alignment);
		label.setText(text == null ? "" : text);
		return label;
	}

	/**
	 * Stops a label rendering text that starts with "<html>" as HTML. Call before its text is set.
	 */
	public static void noHtml(JLabel label)
	{
		label.putClientProperty("html.disable", Boolean.TRUE);
	}

	/**
	 * Sets a tooltip that shows its text as written, never as HTML; blank removes it.
	 */
	public static void tooltip(JComponent component, String text)
	{
		component.setToolTipText(tooltipText(text));
	}

	/**
	 * Tooltip text that shows as written, never as HTML; null for blank.
	 */
	public static String tooltipText(String text)
	{
		return text == null || text.trim().isEmpty() ? null : "<html>" + escape(text) + "</html>";
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
	 * A wrapped label like {@link #label}, with every match of the pattern drawn in the highlight colour, e.g. rules
	 * in brackets. Every piece of the text is escaped; only the colour is markup.
	 */
	public static JLabel highlighted(String text, Pattern pattern, Color highlight, Color colour, Font font, int width, boolean centred)
	{
		String hex = String.format("#%06X", highlight.getRGB() & 0xFFFFFF);
		StringBuilder body = new StringBuilder();
		Matcher matcher = pattern.matcher(text);
		int last = 0;
		while (matcher.find())
		{
			body.append(escape(text.substring(last, matcher.start())))
				.append("<font color='").append(hex).append("'>")
				.append(escape(matcher.group()))
				.append("</font>");
			last = matcher.end();
		}
		body.append(escape(text.substring(last)));

		JLabel label = new JLabel();
		label.setForeground(colour);
		label.setFont(font);
		label.setHorizontalAlignment(centred ? SwingConstants.CENTER : SwingConstants.LEFT);
		setHtml(label, body.toString(), width, centred);
		return label;
	}

	/**
	 * A wrapped label of at most this many lines; longer text is cut short and ends with "...".
	 */
	public static JLabel clamped(String fullText, Color colour, Font font, int width, boolean centred, int maxLines)
	{
		// Only the start is measured, so a huge cell cannot stall the panel; it is cut short anyway.
		String text = fullText.length() > MAX_MEASURED ? fullText.substring(0, MAX_MEASURED) : fullText;
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
		tooltip(label, text);
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
	 * A number to one decimal place, dropping ".0", e.g. 7.5 or 18.
	 */
	public static String decimal(double value)
	{
		String text = String.format(Locale.ENGLISH, "%,.1f", value);
		return text.endsWith(".0") ? text.substring(0, text.length() - 2) : text;
	}

	/**
	 * A short number for big amounts, e.g. 950, 12.3k, 640k or 1M.
	 */
	public static String compact(long value)
	{
		if (Math.abs(value) >= 1_000_000)
		{
			return decimal(value / 1_000_000.0) + "M";
		}
		return Math.abs(value) >= 1_000 ? decimal(value / 1_000.0) + "k" : number(value);
	}

	/**
	 * How much of an amount is in, with its unit, e.g. "1 / 2 pieces" or "640k / 1M xp" (xp is shortened).
	 */
	public static String fraction(long amount, long needed, String unit)
	{
		return withUnit(bareAmount(amount, unit) + " / " + bareAmount(needed, unit), unit);
	}

	/**
	 * An amount with its unit, e.g. "19 kills" or "410k xp" (xp is shortened).
	 */
	public static String amount(long amount, String unit)
	{
		return withUnit(bareAmount(amount, unit), unit);
	}

	/**
	 * A member's own score in a challenge: "1,942 xp" for a skill, "12 kills" (or "1 kill") for a boss, otherwise just
	 * the number. No "+": it is their score, not a gain in a list.
	 */
	public static String score(long value, String type)
	{
		if ("skill".equalsIgnoreCase(type))
		{
			return number(value) + " xp";
		}
		if ("boss".equalsIgnoreCase(type))
		{
			return number(value) + (value == 1 ? " kill" : " kills");
		}
		return number(value);
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

	private static String bareAmount(long amount, String unit)
	{
		return "xp".equalsIgnoreCase(unit) ? compact(amount) : number(amount);
	}

	private static String withUnit(String text, String unit)
	{
		return unit == null || unit.trim().isEmpty() ? text : text + " " + unit.trim();
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
		setHtml(label, escape(text), width, centred);
	}

	/**
	 * Sets an HTML body that is already escaped, and sizes the label to fit it at the given pixel width.
	 */
	private static void setHtml(JLabel label, String body, int width, boolean centred)
	{
		label.setText(html(body, centred));
		View view = (View) label.getClientProperty(BasicHTML.propertyKey);
		view.setSize(width, 0);
		Insets insets = label.getInsets();
		label.setPreferredSize(new Dimension(
			width + insets.left + insets.right,
			(int) Math.ceil(view.getPreferredSpan(View.Y_AXIS)) + insets.top + insets.bottom));
	}

	/**
	 * Text escaped for HTML, with line breaks kept.
	 */
	private static String escape(String text)
	{
		return text
			.replace("&", "&amp;")
			.replace("<", "&lt;")
			.replace(">", "&gt;")
			.replace("\r\n", "\n")
			.replace("\n", "<br>");
	}

	private static String html(String body, boolean centred)
	{
		if (centred)
		{
			body = "<div style='text-align:center'>" + body + "</div>";
		}
		return "<html>" + body + "</html>";
	}
}
