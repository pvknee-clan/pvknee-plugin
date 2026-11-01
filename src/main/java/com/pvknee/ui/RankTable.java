package com.pvknee.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.DynamicGridLayout;

/**
 * A ranked table in one card. Medal rows (the top three) get a medal-coloured number badge and name and are slightly
 * taller; a thin divider separates them from the plain rows below. Values are right-aligned in the value colour.
 * A highlighted row, e.g. the member's own, gets a subtle rounded background.
 */
public class RankTable extends Card
{
	static final Color NAME = ThemeColours.TEXT;
	private static final int MEDAL_ROW_PADDING = 7;
	private static final int PLAIN_ROW_PADDING = 5;

	private final ThemeColours colours;
	private boolean lastWasMedal;

	public RankTable(ThemeColours colours)
	{
		super(new DynamicGridLayout(0, 1, 0, 0));
		// Rows add the rest of the side padding themselves, so a highlighted row's background reaches past its text.
		setBorder(new EmptyBorder(Spacing.ITEM_GAP, Spacing.PADDING - Spacing.ITEM_GAP, Spacing.ITEM_GAP, Spacing.PADDING - Spacing.ITEM_GAP));
		this.colours = colours;
	}

	/**
	 * Adds the next row. A medal row must be one of the top {@link RankBadge#medalPlaces()} places.
	 */
	public void addRow(int rank, String name, String value, boolean medal, boolean highlight)
	{
		if (lastWasMedal && !medal)
		{
			JPanel line = new JPanel(new BorderLayout());
			line.setOpaque(false);
			line.setBorder(new EmptyBorder(0, Spacing.ITEM_GAP, 0, Spacing.ITEM_GAP));
			line.add(new Divider(colours.getMuted()));
			add(line);
		}
		lastWasMedal = medal;

		JPanel row;
		if (highlight)
		{
			Card card = new Card(new BorderLayout(2 * Spacing.ITEM_GAP, 0), false);
			card.setBackground(HOVER);
			card.setArc(Spacing.CONTROL_ARC);
			row = card;
		}
		else
		{
			row = new JPanel(new BorderLayout(2 * Spacing.ITEM_GAP, 0));
			row.setOpaque(false);
		}
		int padding = medal ? MEDAL_ROW_PADDING : PLAIN_ROW_PADDING;
		row.setBorder(new EmptyBorder(padding, Spacing.ITEM_GAP, padding, Spacing.ITEM_GAP));
		row.add(RankBadge.of(rank, medal, colours), BorderLayout.WEST);
		row.add(text(name, medal ? RankBadge.medalColour(rank) : NAME, SwingConstants.LEFT), BorderLayout.CENTER);
		row.add(text(value, colours.getValue(), SwingConstants.RIGHT), BorderLayout.EAST);
		add(row);
	}

	private static JLabel text(String text, Color colour, int alignment)
	{
		JLabel label = new JLabel(text, alignment);
		label.setForeground(colour);
		label.setFont(Fonts.name());
		return label;
	}
}
