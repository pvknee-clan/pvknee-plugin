package com.pvknee.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import lombok.Builder;
import lombok.Value;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

/**
 * A ranked table in one card. Medal rows (the top three) get a medal-coloured number badge and name and are slightly
 * taller; a thin divider separates them from the plain rows below. Values are right-aligned in the value colour,
 * optionally after a muted caption such as "3 tiles". A highlighted row, e.g. the member's own, gets a subtle rounded
 * background. Rows can also be unranked, e.g. a team's members.
 */
public class RankTable extends Card
{
	static final Color NAME = ThemeColours.TEXT;
	private static final int MEDAL_ROW_PADDING = 7;
	private static final int PLAIN_ROW_PADDING = 5;
	/** Values after a caption get at least this width, so the captions line up. */
	private static final int CAPTIONED_VALUE_WIDTH = 36;

	private final ThemeColours colours;
	private boolean lastWasMedal;

	/**
	 * One row. Only the name is needed.
	 */
	@Value
	@Builder
	public static class Row
	{
		/** The place, shown as a badge; 0 for an unranked row. */
		int rank;
		/** Whether the place gets a medal; it must be one of the top {@link RankBadge#medalPlaces()}. */
		boolean medal;
		String name;
		/** Muted small text right after the name, e.g. "(you)". */
		String suffix;
		/** Muted small text before the value, e.g. "3 tiles". */
		String caption;
		String value;
		boolean highlight;
		/** Makes the row clickable. */
		Runnable onClick;
	}

	public RankTable(ThemeColours colours)
	{
		super(new DynamicGridLayout(0, 1, 0, 0));
		// Rows add the rest of the side padding themselves, so a highlighted row's background reaches past its text.
		setBorder(new EmptyBorder(Spacing.ITEM_GAP, Spacing.PADDING - Spacing.ITEM_GAP, Spacing.ITEM_GAP, Spacing.PADDING - Spacing.ITEM_GAP));
		this.colours = colours;
	}

	/**
	 * Adds the next ranked row. A medal row must be one of the top {@link RankBadge#medalPlaces()} places.
	 */
	public void addRow(int rank, String name, String value, boolean medal, boolean highlight)
	{
		addRow(Row.builder().rank(rank).name(name).value(value).medal(medal).highlight(highlight).build());
	}

	public void addRow(Row row)
	{
		boolean medal = row.isMedal();
		if (lastWasMedal && !medal)
		{
			JPanel line = new JPanel(new BorderLayout());
			line.setOpaque(false);
			line.setBorder(new EmptyBorder(0, Spacing.ITEM_GAP, 0, Spacing.ITEM_GAP));
			line.add(new Divider(colours.getMuted()));
			add(line);
		}
		lastWasMedal = medal;

		Card panel = new Card(new BorderLayout(2 * Spacing.ITEM_GAP, 0), false);
		panel.setArc(Spacing.CONTROL_ARC);
		if (row.isHighlight())
		{
			panel.setBackground(HOVER);
		}
		if (row.getOnClick() != null)
		{
			panel.onClick(row.getOnClick());
		}
		int padding = medal ? MEDAL_ROW_PADDING : PLAIN_ROW_PADDING;
		panel.setBorder(new EmptyBorder(padding, Spacing.ITEM_GAP, padding, Spacing.ITEM_GAP));

		if (row.getRank() > 0)
		{
			panel.add(RankBadge.of(row.getRank(), medal, colours), BorderLayout.WEST);
		}
		panel.add(name(row), BorderLayout.CENTER);

		JPanel end = new JPanel(new BorderLayout(2 * Spacing.ITEM_GAP, 0));
		end.setOpaque(false);
		if (row.getCaption() != null)
		{
			end.add(text(row.getCaption(), colours.getMuted(), FontManager.getRunescapeSmallFont(), SwingConstants.RIGHT), BorderLayout.CENTER);
		}
		if (row.getValue() != null)
		{
			JLabel value = text(row.getValue(), colours.getValue(), Fonts.name(), SwingConstants.RIGHT);
			if (row.getCaption() != null)
			{
				Dimension size = value.getPreferredSize();
				value.setPreferredSize(new Dimension(Math.max(size.width, CAPTIONED_VALUE_WIDTH), size.height));
			}
			end.add(value, BorderLayout.EAST);
		}
		panel.add(end, BorderLayout.EAST);
		add(panel);
	}

	/**
	 * The name, cut short with "..." when it doesn't fit, with its suffix right after it.
	 */
	private JPanel name(Row row)
	{
		JLabel name = text(row.getName(), row.isMedal() ? RankBadge.medalColour(row.getRank()) : NAME, Fonts.name(), SwingConstants.LEFT);
		name.setMinimumSize(new Dimension(0, name.getPreferredSize().height));
		name.setMaximumSize(name.getPreferredSize());

		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.setOpaque(false);
		panel.add(name);
		if (row.getSuffix() != null)
		{
			panel.add(Box.createHorizontalStrut(Spacing.ITEM_GAP));
			panel.add(text(row.getSuffix(), colours.getMuted(), FontManager.getRunescapeSmallFont(), SwingConstants.LEFT));
		}
		panel.add(Box.createHorizontalGlue());
		return panel;
	}

	private static JLabel text(String text, Color colour, Font font, int alignment)
	{
		JLabel label = new JLabel(text, alignment);
		label.setForeground(colour);
		label.setFont(font);
		return label;
	}
}
