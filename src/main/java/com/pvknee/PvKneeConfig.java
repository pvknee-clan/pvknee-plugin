package com.pvknee;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup(PvKneeConfig.GROUP)
public interface PvKneeConfig extends Config
{
	String GROUP = "pvknee-clan";

	@ConfigItem(
		keyName = "showTimesInUtc",
		name = "Show times in UTC",
		description = "Shows times in UTC instead of your local time zone",
		position = 0
	)
	default boolean showTimesInUtc()
	{
		return false;
	}

	@ConfigItem(
		keyName = "updateWomOnLogout",
		name = "Update Wise Old Man on logout",
		description = "When you log out after gaining over 100 xp or a kill or completion count, asks Wise Old Man to update your profile, at most once every 10 minutes. Skipped on seasonal, Deadman and other special worlds, and when RuneLite's XP Updater is already sending the update",
		warning = "This feature submits your IP address to a 3rd-party server not controlled or verified by RuneLite developers",
		position = 2
	)
	default boolean updateWomOnLogout()
	{
		return false;
	}

	@ConfigItem(
		keyName = "loginMessage",
		name = "Login message",
		description = "Shows this week's Skill and Boss of the Week in chat once each time you log in; only you see it",
		position = 1
	)
	default boolean loginMessage()
	{
		return false;
	}

	@ConfigSection(
		name = "Bingo",
		description = "Team icons and the team overlay while a clan bingo runs; only you see them",
		position = 3
	)
	String bingoSection = "bingo";

	@ConfigItem(
		keyName = "teamIconsInChat",
		name = "Team icons in chat",
		description = "Shows a dot in the team colour before the names of clan members on a bingo team in clan and public chat, except in the Wilderness, on PvP worlds and in PvP areas",
		position = 0,
		section = bingoSection
	)
	default boolean teamIconsInChat()
	{
		return true;
	}

	@ConfigItem(
		keyName = "teamIconsAboveHeads",
		name = "Team icons above heads",
		description = "Shows a dot in the team colour beside the names Player Indicators draws for bingo players, except in the Wilderness, on PvP worlds and in PvP areas. Needs Player Indicators on and showing names",
		position = 1,
		section = bingoSection
	)
	default boolean teamIconsAboveHeads()
	{
		return true;
	}

	@ConfigItem(
		keyName = "bingoTeamOverlay",
		name = "Bingo team overlay",
		description = "Shows your team and the date and time in UTC on screen, so screenshots show when they were taken",
		position = 2,
		section = bingoSection
	)
	default boolean bingoTeamOverlay()
	{
		return true;
	}
}
