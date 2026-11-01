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

	@ConfigSection(
		name = "Challenge messages",
		description = "Chat messages about the Skill and Boss of the Week; only you see them",
		position = 1
	)
	String challengeSection = "challengeMessages";

	@ConfigItem(
		keyName = "loginMessage",
		name = "Login message",
		description = "Shows the current challenges in chat once each time you log in",
		position = 0,
		section = challengeSection
	)
	default boolean loginMessage()
	{
		return true;
	}

	@ConfigItem(
		keyName = "challengeStartAlert",
		name = "Challenge start",
		description = "Shows a chat message when a Skill or Boss of the Week starts",
		position = 1,
		section = challengeSection
	)
	default boolean challengeStartAlert()
	{
		return true;
	}

	@ConfigItem(
		keyName = "challengeHourLeftAlert",
		name = "1 hour left",
		description = "Shows a chat message when a Skill or Boss of the Week has 1 hour left",
		position = 2,
		section = challengeSection
	)
	default boolean challengeHourLeftAlert()
	{
		return true;
	}

	@ConfigItem(
		keyName = "challengeEndAlert",
		name = "Challenge end",
		description = "Shows a chat message when a Skill or Boss of the Week ends",
		position = 3,
		section = challengeSection
	)
	default boolean challengeEndAlert()
	{
		return true;
	}

	// Debug only: remove this section and hide its items before publishing
	@ConfigSection(
		name = "Debug",
		description = "Settings for testing the plugin",
		position = 99,
		closedByDefault = true
	)
	String debugSection = "debug";

	@ConfigItem(
		keyName = "endpointOverride",
		name = "Endpoint override",
		description = "Replaces the clan endpoint URL, for testing against another sheet",
		section = debugSection
	)
	default String endpointOverride()
	{
		return "";
	}
}
