package com.pvknee.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.Locale;
import java.util.Objects;
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
	 * The small capitals title inside a card that is one thing, e.g. "CLAN MESSAGE".
	 */
	public static SectionHeader cardLabel(String text, ThemeColours colours, int width)
	{
		return new SectionHeader(text.toUpperCase(Locale.ENGLISH), colours.getAccent(), FontManager.getRunescapeSmallFont(), width, false);
	}

	/**
	 * Adds a muted note on the right, e.g. a shared time or a column label; the title wraps in the space left of it.
	 * The text can change, e.g. for a countdown: call {@link #update()} to refresh it.
	 */
	public SectionHeader withAside(Supplier<String> aside, ThemeColours colours)
	{
		asideText = aside;
		asideLabel = new JLabel();
		asideLabel.setForeground(colours.getMuted());
		asideLabel.setFont(FontManager.getRunescapeSmallFont());
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
		if (!next.equals(asideLabel.getText()))
		{
			asideLabel.setText(next);
			Labels.setText(title, text, width - asideLabel.getPreferredSize().width - Spacing.ITEM_GAP, centred);
			revalidate();
		}
	}

	private static Font bold(float size)
	{
		return FontManager.getRunescapeBoldFont().deriveFont(size);
	}
}
