package com.pvknee;

import java.awt.image.BufferedImage;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "PvKnee",
	description = "Clan panel for PvKnee",
	tags = {"clan", "sotw", "botw", "events", "wom"}
)
public class PvKneePlugin extends Plugin
{
	@Inject
	private ClientToolbar clientToolbar;

	private NavigationButton navButton;

	@Override
	protected void startUp() throws Exception
	{
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");

		navButton = NavigationButton.builder()
			.tooltip("PvKnee")
			.icon(icon)
			.priority(5)
			.panel(new PvKneePanel())
			.build();

		clientToolbar.addNavigation(navButton);
		log.debug("PvKnee started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		navButton = null;
		log.debug("PvKnee stopped");
	}
}
