package com.pvknee;

import com.google.inject.Injector;
import com.google.inject.Provides;
import com.pvknee.data.SavedCopy;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;

@Slf4j
@PluginDescriptor(
	name = "PvKnee",
	internalName = "pvknee",
	description = "Clan panel for PvKnee",
	tags = {"clan", "sotw", "botw", "events", "wom"}
)
public class PvKneePlugin extends Plugin
{
	@Inject
	private Injector injector;

	@Inject
	private ClientToolbar clientToolbar;

	@Inject
	private SavedCopy savedCopy;

	private PvKneePanel panel;
	private NavigationButton navButton;

	@Override
	protected void startUp() throws Exception
	{
		try
		{
			savedCopy.setDirectory(getPluginDirectory());
		}
		catch (IOException e)
		{
			log.debug("No plugin folder, so the clan data will not be saved", e);
		}

		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		panel = injector.getInstance(PvKneePanel.class);

		navButton = NavigationButton.builder()
			.tooltip("PvKnee")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
		log.debug("PvKnee started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		clientToolbar.removeNavigation(navButton);
		panel.stopUpdates();
		navButton = null;
		panel = null;
		log.debug("PvKnee stopped");
	}

	@Provides
	PvKneeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PvKneeConfig.class);
	}
}
