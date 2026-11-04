package com.pvknee.ui;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.FontManager;

/**
 * A place in the standings: a {@link PlacePill} for the medal places, otherwise the muted place as a word (e.g.
 * "12th"), both centred in a slot of the same width so names line up. Also the medal and place colours.
 */
public final class RankBadge
{
	private static final Color[] MEDALS = {
		new Color(0xFFD700),
		new Color(0xC0C0C0),
		new Color(0xCD7F32),
	};
	/** The place colour below the medals. */
	private static final Color PLACE_GREY = new Color(0x9E9E9E);
	/** The widest plain place the slot is sized for. */
	private static final String WIDEST_PLAIN = "20th";

	private RankBadge()
	{
	}

	/**
	 * The number of places that can get a medal.
	 */
	public static int medalPlaces()
	{
		return MEDALS.length;
	}

	/**
	 * The medal colour of one of the top {@link #medalPlaces()} places.
	 */
	public static Color medalColour(int rank)
	{
		return MEDALS[rank - 1];
	}

	/**
	 * A place's colour: its medal colour for the top {@link #medalPlaces()} places, otherwise a neutral grey.
	 */
	public static Color placeColour(int rank)
	{
		return rank >= 1 && rank <= MEDALS.length ? medalColour(rank) : PLACE_GREY;
	}

	/**
	 * A place pill, which must be for one of the top {@link #medalPlaces()} places, or the plain muted place.
	 */
	public static JComponent of(int rank, boolean medal, ThemeColours colours)
	{
		JComponent place = medal ? new PlacePill(rank) : plain(Labels.ordinal(rank), colours);
		Centred slot = new Centred(place);
		slot.setPreferredSize(new Dimension(slotWidth(colours), Math.max(PlacePill.HEIGHT, place.getPreferredSize().height)));
		return slot;
	}

	/**
	 * Wide enough for any medal pill and for plain places up to "20th".
	 */
	private static int slotWidth(ThemeColours colours)
	{
		int width = plain(WIDEST_PLAIN, colours).getPreferredSize().width;
		for (int rank = 1; rank <= MEDALS.length; rank++)
		{
			width = Math.max(width, new PlacePill(rank).getPreferredSize().width);
		}
		return width;
	}

	private static JLabel plain(String text, ThemeColours colours)
	{
		return Labels.plain(text, colours.getMuted(), FontManager.getRunescapeSmallFont(), SwingConstants.CENTER);
	}
}
