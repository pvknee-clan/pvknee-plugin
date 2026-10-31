package com.pvknee.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

/**
 * A ranked table in one card. Medal rows (the top three) get a medal-coloured number badge and name and are slightly
 * taller; a thin divider separates them from the plain rows below. Values are right-aligned in the value colour.
 */
public class RankTable extends Card
{
	private static final Color[] MEDALS = {
		new Color(0xFFD700),
		new Color(0xC0C0C0),
		new Color(0xCD7F32),
	};
	private static final Color NAME = new Color(221, 221, 221);
	private static final int RANK_SLOT = 18;
	private static final int MEDAL_ROW_PADDING = 7;
	private static final int PLAIN_ROW_PADDING = 5;

	private final ThemeColours colours;
	private boolean lastWasMedal;

	public RankTable(ThemeColours colours)
	{
		super(new DynamicGridLayout(0, 1, 0, 0));
		setBorder(new EmptyBorder(Spacing.ITEM_GAP, Spacing.PADDING, Spacing.ITEM_GAP, Spacing.PADDING));
		this.colours = colours;
	}

	/**
	 * The number of places that can get a medal.
	 */
	public static int medalPlaces()
	{
		return MEDALS.length;
	}

	/**
	 * Adds the next row. A medal row must be one of the top {@link #medalPlaces()} places.
	 */
	public void addRow(int rank, String name, String value, boolean medal)
	{
		if (lastWasMedal && !medal)
		{
			add(new Divider(colours.getMuted()));
		}
		lastWasMedal = medal;

		Color medalColour = medal ? MEDALS[rank - 1] : null;
		JComponent rankView = medal ? new Badge(rank, medalColour) : plainRank(rank);

		JPanel row = new JPanel(new BorderLayout(2 * Spacing.ITEM_GAP, 0));
		row.setOpaque(false);
		int padding = medal ? MEDAL_ROW_PADDING : PLAIN_ROW_PADDING;
		row.setBorder(new EmptyBorder(padding, 0, padding, 0));
		row.add(rankView, BorderLayout.WEST);
		row.add(text(name, medal ? medalColour : NAME, SwingConstants.LEFT), BorderLayout.CENTER);
		row.add(text(value, colours.getValue(), SwingConstants.RIGHT), BorderLayout.EAST);
		add(row);
	}

	private JLabel plainRank(int rank)
	{
		JLabel label = new JLabel(String.valueOf(rank), SwingConstants.CENTER);
		label.setForeground(colours.getMuted());
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setPreferredSize(new Dimension(RANK_SLOT, label.getPreferredSize().height));
		return label;
	}

	private static JLabel text(String text, Color colour, int alignment)
	{
		JLabel label = new JLabel(text, alignment);
		label.setForeground(colour);
		label.setFont(Fonts.name());
		return label;
	}

	/**
	 * A round medal-coloured badge with the place number in dark text.
	 */
	private static class Badge extends JComponent
	{
		private final String number;
		private final Color colour;

		Badge(int rank, Color colour)
		{
			this.number = String.valueOf(rank);
			this.colour = colour;
			setFont(FontManager.getRunescapeSmallFont());
			setPreferredSize(new Dimension(RANK_SLOT, RANK_SLOT));
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			int y = (getHeight() - RANK_SLOT) / 2;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(colour);
			g2.fillOval(0, y, RANK_SLOT, RANK_SLOT);

			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g2.setFont(getFont());
			g2.setColor(ColorScheme.DARK_GRAY_COLOR);
			int textX = (RANK_SLOT - g2.getFontMetrics().stringWidth(number)) / 2;
			int textY = y + (RANK_SLOT + g2.getFontMetrics().getAscent() - g2.getFontMetrics().getDescent()) / 2;
			g2.drawString(number, textX, textY);
			g2.dispose();
		}
	}
}
