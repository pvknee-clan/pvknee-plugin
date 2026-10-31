package com.pvknee.ui;

import java.awt.BorderLayout;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.FontManager;

/**
 * A progress bar with muted captions under it: the time left on the right, e.g. "5d 22h left", and optionally a
 * fixed caption such as the end date on the left. Any part can be left out: without a fraction there is no bar.
 */
public class TimeBar extends JPanel
{
	private final DoubleSupplier fraction;
	private final Supplier<String> right;
	private final ProgressBar bar;
	private final JLabel rightLabel;

	public TimeBar(DoubleSupplier fraction, String left, Supplier<String> right, ThemeColours colours)
	{
		super(new BorderLayout(0, Spacing.ITEM_GAP));
		setOpaque(false);
		this.fraction = fraction;
		this.right = right;

		bar = fraction == null ? null : new ProgressBar(colours.getAccent(), fraction.getAsDouble());
		if (bar != null)
		{
			add(bar, BorderLayout.NORTH);
		}

		rightLabel = right == null ? null : caption(right.get(), SwingConstants.RIGHT, colours);
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
	}

	/**
	 * Moves the bar and asks for the right caption again, e.g. every minute.
	 */
	public void update()
	{
		if (bar != null)
		{
			bar.setFraction(fraction.getAsDouble());
		}
		if (rightLabel != null)
		{
			String next = Objects.toString(right.get(), "");
			if (!next.equals(rightLabel.getText()))
			{
				rightLabel.setText(next);
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
