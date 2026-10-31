package com.pvknee.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Objects;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.FontManager;

/**
 * A progress bar with muted captions under it, e.g. "5d 22h left" on the left and the end date on the right.
 * Any part can be left out: without a fraction there is no bar, and a caption on its own is centred.
 */
public class TimeBar extends JPanel
{
	private final DoubleSupplier fraction;
	private final Supplier<String> left;
	private final ProgressBar bar;
	private final JLabel leftLabel;

	public TimeBar(DoubleSupplier fraction, Supplier<String> left, String right, ThemeColours colours)
	{
		super(new BorderLayout(0, Spacing.ITEM_GAP));
		setOpaque(false);
		this.fraction = fraction;
		this.left = left;

		bar = fraction == null ? null : new ProgressBar(colours.getAccent(), fraction.getAsDouble());
		if (bar != null)
		{
			add(bar, BorderLayout.NORTH);
		}

		leftLabel = left == null ? null : caption(left.get(), right == null ? SwingConstants.CENTER : SwingConstants.LEFT, colours);
		if (leftLabel != null || right != null)
		{
			JPanel captions = new JPanel(new BorderLayout(Spacing.ITEM_GAP, 0));
			captions.setOpaque(false);
			if (leftLabel != null)
			{
				captions.add(leftLabel, BorderLayout.CENTER);
			}
			if (right != null)
			{
				captions.add(caption(right, SwingConstants.RIGHT, colours), BorderLayout.EAST);
			}
			add(captions, BorderLayout.CENTER);
		}
	}

	/**
	 * Moves the bar and asks for the left caption again, e.g. every minute.
	 */
	public void update()
	{
		if (bar != null)
		{
			bar.setFraction(fraction.getAsDouble());
		}
		if (leftLabel != null)
		{
			String next = Objects.toString(left.get(), "");
			if (!next.equals(leftLabel.getText()))
			{
				leftLabel.setText(next);
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
