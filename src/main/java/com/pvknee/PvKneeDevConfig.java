package com.pvknee;

import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

/**
 * The settings with a Debug section added. Only provided when RuneLite runs in developer mode, so a normal client
 * never shows or uses the endpoint override.
 */
@ConfigGroup(PvKneeConfig.GROUP)
public interface PvKneeDevConfig extends PvKneeConfig
{
	@ConfigSection(
		name = "Debug",
		description = "Settings for testing the plugin; only shown in developer mode",
		position = 99,
		closedByDefault = true
	)
	String debugSection = "debug";

	@ConfigItem(
		keyName = "endpointOverride",
		name = "Endpoint override",
		description = "Replaces the clan endpoint URL, for testing against another sheet or previewing a draft bingo",
		section = debugSection
	)
	default String endpointOverride()
	{
		return "";
	}
}
