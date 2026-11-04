package com.pvknee.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.List;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.FontManager;

/**
 * Rows of text in one card, in the {@link RankTable} look: an optional muted header row with a thin divider under
 * it, names in the first column, values in the others with the last one right-aligned. Columns share the width by
 * weight; text too long for its column is cut short, with the full text as a tooltip.
 */
public class DataTable extends Card
{
	private static final int ROW_PADDING = 5;

	/**
	 * @param header the header cells, or null for none
	 * @param weights each column's share of the width
	 */
	public DataTable(List<String> header, List<List<String>> rows, double[] weights, ThemeColours colours)
	{
		super(new GridBagLayout());
		setBorder(new EmptyBorder(Spacing.ITEM_GAP, Spacing.PADDING, Spacing.ITEM_GAP, Spacing.PADDING));

		int y = 0;
		if (header != null)
		{
			addRow(y++, header, weights, colours.getMuted(), colours.getMuted(), FontManager.getRunescapeSmallFont());
			GridBagConstraints line = new GridBagConstraints();
			line.gridy = y++;
			line.gridwidth = weights.length;
			line.fill = GridBagConstraints.HORIZONTAL;
			line.weightx = 1;
			add(new Divider(colours.getMuted()), line);
		}
		for (List<String> row : rows)
		{
			addRow(y++, row, weights, RankTable.NAME, colours.getValue(), Fonts.name());
		}
	}

	private void addRow(int y, List<String> cells, double[] weights, Color first, Color rest, Font font)
	{
		for (int x = 0; x < weights.length; x++)
		{
			boolean last = x == weights.length - 1 && x > 0;
			String text = x < cells.size() && cells.get(x) != null ? cells.get(x) : "";
			JLabel label = Labels.plain(text, x == 0 ? first : rest, font, last ? SwingConstants.RIGHT : SwingConstants.LEFT);
			Labels.tooltip(label, text);
			// A zero preferred width lets the weights alone share the width, so long text is cut rather than widening it.
			label.setPreferredSize(new Dimension(0, label.getPreferredSize().height));

			GridBagConstraints cell = new GridBagConstraints();
			cell.gridx = x;
			cell.gridy = y;
			cell.weightx = weights[x];
			cell.fill = GridBagConstraints.HORIZONTAL;
			cell.insets = new Insets(ROW_PADDING, x == 0 ? 0 : Spacing.ITEM_GAP, ROW_PADDING, 0);
			add(label, cell);
		}
	}
}
