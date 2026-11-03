package com.pvknee.player;

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import lombok.Value;
import net.runelite.api.clan.ClanRank;

/**
 * What the plugin knows about the member in game, at one moment.
 */
@Value
public class PlayerState
{
	public static final PlayerState LOGGED_OUT = new PlayerState(false, null, 0, null, null, Collections.emptyMap());

	boolean loggedIn;
	/** The display name; null until the game has it. */
	String name;
	int world;
	/** The clan rank; null when not in the clan or not known yet. */
	ClanRank rank;
	/** The rank's title as the clan names it, e.g. "Administrator"; null with no rank. */
	String rankTitle;
	/** The clan's rank titles in lower case, each with the lowest rank that has it; empty when not in the clan. */
	Map<String, Integer> rankTitles;

	/**
	 * Whether this is the member's name, e.g. as Wise Old Man spells it. Ignores case and treats spaces, underscores
	 * and hyphens as the same.
	 */
	public boolean isNamed(String other)
	{
		return name != null && other != null && nameKey(name).equals(nameKey(other));
	}

	/**
	 * Whether the member's clan rank is at least this one: a rank number, or a title as the clan names it, e.g.
	 * "Administrator". Blank needs no rank; otherwise logged out, not in the clan or an unknown title is never enough.
	 */
	public boolean hasRank(String required)
	{
		if (required == null || required.trim().isEmpty())
		{
			return true;
		}
		if (rank == null)
		{
			return false;
		}

		Integer needed;
		try
		{
			needed = Integer.parseInt(required.trim());
		}
		catch (NumberFormatException e)
		{
			needed = rankTitles.get(required.trim().toLowerCase(Locale.ENGLISH));
		}
		return needed != null && rank.getRank() >= needed;
	}

	/**
	 * The name as {@link #isNamed} compares it, for looking names up in a map.
	 */
	public static String nameKey(String name)
	{
		return name.replace('\u00A0', ' ').replace('_', ' ').replace('-', ' ').trim().toLowerCase(Locale.ENGLISH);
	}
}
