package com.pvknee;

import com.google.inject.Injector;
import com.google.inject.Provides;
import com.pvknee.blocks.Times;
import com.pvknee.chat.ChallengeMessages;
import com.pvknee.chat.ChatCommands;
import com.pvknee.data.SavedCopy;
import com.pvknee.player.PlayerInfo;
import com.pvknee.player.WorldHopper;
import java.awt.image.BufferedImage;
import java.io.IOException;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import javax.swing.SwingUtilities;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
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

	@Inject
	private PvKneeConfig config;

	@Inject
	private PlayerInfo playerInfo;

	@Inject
	private WorldHopper worldHopper;

	@Inject
	private ChatCommands chatCommands;

	@Inject
	private ChallengeMessages challengeMessages;

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

		Times.setUtc(config.showTimesInUtc());
		final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "icon.png");
		panel = injector.getInstance(PvKneePanel.class);

		navButton = NavigationButton.builder()
			.tooltip("PvKnee")
			.icon(icon)
			.priority(5)
			.panel(panel)
			.build();

		clientToolbar.addNavigation(navButton);
		playerInfo.start(panel::playerChanged);
		worldHopper.start();
		chatCommands.start();
		challengeMessages.start();
		log.debug("PvKnee started");
	}

	@Override
	protected void shutDown() throws Exception
	{
		playerInfo.stop();
		worldHopper.stop();
		chatCommands.stop();
		challengeMessages.stop();
		clientToolbar.removeNavigation(navButton);
		panel.stopUpdates();
		navButton = null;
		panel = null;
		log.debug("PvKnee stopped");
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (PvKneeConfig.GROUP.equals(event.getGroup()) && "showTimesInUtc".equals(event.getKey()))
		{
			Times.setUtc(config.showTimesInUtc());
			SwingUtilities.invokeLater(() ->
			{
				if (panel != null)
				{
					panel.redraw();
				}
			});
		}
	}

	@Provides
	PvKneeConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PvKneeConfig.class);
	}
}
