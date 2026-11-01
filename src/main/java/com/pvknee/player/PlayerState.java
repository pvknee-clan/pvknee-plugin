package com.pvknee.player;

import java.util.Locale;
import lombok.Value;
import net.runelite.api.clan.ClanRank;

/**
 * What the plugin knows about the member in game, at one moment.
 */
@Value
public class PlayerState
{
	public static final PlayerState LOGGED_OUT = new PlayerState(false, null, 0, null, null);

	boolean loggedIn;
	/** The display name; null until the game has it. */
	String name;
	int world;
	/** The clan rank; null when not in the clan or not known yet. */
	ClanRank rank;
	/** The rank's title as the clan names it, e.g. "Administrator"; null with no rank. */
	String rankTitle;

	/**
	 * Whether this is the member's name, e.g. as Wise Old Man spells it. Ignores case and treats spaces, underscores
	 * and hyphens as the same.
	 */
	public boolean isNamed(String other)
	{
		return name != null && other != null && normalise(name).equals(normalise(other));
	}

	private static String normalise(String name)
	{
		return name.replace('\u00A0', ' ').replace('_', ' ').replace('-', ' ').trim().toLowerCase(Locale.ENGLISH);
	}
}
