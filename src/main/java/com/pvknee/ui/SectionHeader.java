package com.pvknee.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.FontManager;

/**
 * A left-aligned section title, in the accent colour unless told otherwise, with an optional muted aside on the
 * right (e.g. a shared time or a column label). The page title variant is centred.
 */
public class SectionHeader extends JPanel
{
	private static final float DEFAULT_SIZE = 16f;

	private final JLabel title = new JLabel();
	private final String text;
	private final int width;
	private final boolean centred;
	private JLabel asideLabel;
	private Supplier<String> asideText;
	private BooleanSupplier asideEndingSoon;
	private ThemeColours colours;
	private boolean shownSoon;

	public SectionHeader(String text, ThemeColours colours)
	{
		this(text, colours.getAccent(), DEFAULT_SIZE);
	}

	private SectionHeader(String text, Color colour, float size)
	{
		this(text, colour, bold(size), Labels.CONTENT_WIDTH, false);
	}

	private SectionHeader(String text, Color colour, Font font, int width, boolean centred)
	{
		super(new BorderLayout(Spacing.ITEM_GAP, 0));
		setOpaque(false);
		this.text = text;
		this.width = width;
		this.centred = centred;

		title.setForeground(colour);
		title.setFont(font);
		title.setHorizontalAlignment(centred ? SwingConstants.CENTER : SwingConstants.LEFT);
		Labels.setText(title, text, width, centred);
		add(title, BorderLayout.CENTER);
	}

	/**
	 * A centred page title, e.g. the clan name at the top of Home.
	 */
	public static SectionHeader pageTitle(String text, Color colour, float size)
	{
		return new SectionHeader(text, colour, bold(size), Labels.CONTENT_WIDTH, true);
	}

	/**
	 * Adds a muted note on the right, e.g. a column label; the title wraps in the space left of it.
	 */
	public SectionHeader withAside(Supplier<String> aside, ThemeColours colours)
	{
		return withTimeAside(aside, () -> false, colours);
	}

	/**
	 * Adds a time on the right that can change, e.g. a countdown: call {@link #update()} to refresh it. It is muted,
	 * or bold in the ending-soon colour while the item is ending soon.
	 */
	public SectionHeader withTimeAside(Supplier<String> aside, BooleanSupplier endingSoon, ThemeColours colours)
	{
		asideText = aside;
		asideEndingSoon = endingSoon;
		this.colours = colours;
		asideLabel = new JLabel();
		Labels.styleTime(asideLabel, false, colours);
		asideLabel.setVerticalAlignment(SwingConstants.BOTTOM);
		add(asideLabel, BorderLayout.EAST);
		update();
		return this;
	}

	/**
	 * Asks for the aside's text again, e.g. every minute.
	 */
	public void update()
	{
		if (asideLabel == null)
		{
			return;
		}

		String next = Objects.toString(asideText.get(), "");
		boolean soon = asideEndingSoon.getAsBoolean();
		if (!next.equals(asideLabel.getText()) || soon != shownSoon)
		{
			shownSoon = soon;
			asideLabel.setText(next);
			Labels.styleTime(asideLabel, soon, colours);
			Labels.setText(title, text, width - asideLabel.getPreferredSize().width - Spacing.ITEM_GAP, centred);
			revalidate();
		}
	}

	private static Font bold(float size)
	{
		return FontManager.getRunescapeBoldFont().deriveFont(size);
	}
}
