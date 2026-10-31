package com.pvknee;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(PvKneeConfig.GROUP)
public interface PvKneeConfig extends Config
{
	String GROUP = "pvknee-clan";

	@ConfigItem(
		keyName = "endpointOverride",
		name = "Endpoint override",
		description = "Replaces the clan endpoint URL, for testing against another sheet",
		hidden = true
	)
	default String endpointOverride()
	{
		return "";
	}
}
