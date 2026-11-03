package com.pvknee.bingo;

import com.pvknee.data.Bingo;
import java.awt.Color;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import net.runelite.client.util.ColorUtil;

/**
 * A bingo team's colour: its sheet colour, else one from a fixed palette. Used wherever a team's colour shows.
 */
public final class TeamColours
{
	/** Red, blue, green, yellow, purple and orange, then repeating. */
	private static final Color[] PALETTE = {
		new Color(0xEF5350), new Color(0x42A5F5), new Color(0x66BB6A),
		new Color(0xFFEE58), new Color(0xAB47BC), new Color(0xFFA726),
	};

	private TeamColours()
	{
	}

	/**
	 * The team's colour among these teams. Without a sheet colour, the palette goes by team name, since the endpoint
	 * sorts teams by points and a team's default colour must not change as the points do.
	 */
	public static Color of(List<Bingo.Team> teams, Bingo.Team team)
	{
		Color colour = team.getColour() == null ? null : ColorUtil.fromHex(team.getColour().trim());
		if (colour != null)
		{
			return colour;
		}

		List<String> names = teams.stream()
			.map(t -> Objects.toString(t.getName(), ""))
			.sorted(Comparator.naturalOrder())
			.collect(Collectors.toList());
		int index = Math.max(0, names.indexOf(Objects.toString(team.getName(), "")));
		return PALETTE[index % PALETTE.length];
	}
}
