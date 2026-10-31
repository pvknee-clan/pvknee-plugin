package com.pvknee.ui;

import java.awt.BorderLayout;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.FontManager;

/**
 * A {@link DayBar} with muted captions under it: the time left on the right, e.g. "5d 22h left", and optionally a
 * fixed caption such as the end date on the left. The time left turns bold in the ending-soon colour in the final
 * 24 hours. Any part can be left out: without a day bar there is no bar.
 */
public class TimeBar extends JPanel
{
	private final DayBar bar;
	private final Supplier<String> right;
	private final BooleanSupplier endingSoon;
	private final ThemeColours colours;
	private final JLabel rightLabel;
	private boolean shownSoon;

	public TimeBar(DayBar bar, String left, Supplier<String> right, BooleanSupplier endingSoon, ThemeColours colours)
	{
		super(new BorderLayout(0, Spacing.ITEM_GAP));
		setOpaque(false);
		this.bar = bar;
		this.right = right;
		this.endingSoon = endingSoon == null ? () -> false : endingSoon;
		this.colours = colours;

		if (bar != null)
		{
			add(bar, BorderLayout.NORTH);
		}

		rightLabel = right == null ? null : caption(null, SwingConstants.RIGHT, colours);
		if (left != null || rightLabel != null)
		{
			JPanel captions = new JPanel(new BorderLayout(Spacing.ITEM_GAP, 0));
			captions.setOpaque(false);
			if (left != null)
			{
				captions.add(caption(left, SwingConstants.LEFT, colours), BorderLayout.WEST);
			}
			if (rightLabel != null)
			{
				captions.add(rightLabel, BorderLayout.CENTER);
			}
			add(captions, BorderLayout.CENTER);
		}
		update();
	}

	/**
	 * Moves the bar and asks for the right caption again, e.g. every minute.
	 */
	public void update()
	{
		if (bar != null)
		{
			bar.update();
		}
		if (rightLabel != null)
		{
			String next = Objects.toString(right.get(), "");
			boolean soon = endingSoon.getAsBoolean();
			if (!next.equals(rightLabel.getText()) || soon != shownSoon)
			{
				shownSoon = soon;
				rightLabel.setText(next);
				Labels.styleTime(rightLabel, soon, colours);
				revalidate();
			}
		}
	}

	private static JLabel caption(String text, int alignment, ThemeColours colours)
	{
		JLabel label = new JLabel(Objects.toString(text, ""));
		label.setForeground(colours.getMuted());
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setHorizontalAlignment(alignment);
		return label;
	}
}
