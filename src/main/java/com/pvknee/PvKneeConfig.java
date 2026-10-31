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
