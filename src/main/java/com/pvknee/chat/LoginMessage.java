package com.pvknee.chat;

import com.pvknee.DevLog;
import com.pvknee.PvKneeConfig;
import com.pvknee.data.ClanData;
import com.pvknee.data.ClanDataClient;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

/**
 * Once per login, the sheet's "login" message filled with the current challenges, as a game message only the member
 * sees (off by default). Also starts the one quiet endpoint load at login that the bingo team icons and overlay use
 * while the panel is closed.
 */
@Singleton
public class LoginMessage
{
	private final ClanDataClient clanData;
	private final Client client;
	private final ClientThread clientThread;
	private final ChatSender chatSender;
	private final EventBus eventBus;
	private final PvKneeConfig config;
	private final DevLog devLog;

	/** Whether this login has been greeted; hops and reconnects log in again without a new greeting. */
	private boolean greeted;

	@Inject
	LoginMessage(ClanDataClient clanData, Client client, ClientThread clientThread, ChatSender chatSender,
		EventBus eventBus, PvKneeConfig config, DevLog devLog)
	{
		this.clanData = clanData;
		this.client = client;
		this.clientThread = clientThread;
		this.chatSender = chatSender;
		this.eventBus = eventBus;
		this.config = config;
		this.devLog = devLog;
	}

	/**
	 * Starts the message. Already logged in counts as greeted, so turning the plugin on mid-session stays quiet.
	 */
	public void start()
	{
		eventBus.register(this);
		clientThread.invoke(() ->
		{
			greeted = client.getGameState() == GameState.LOGGED_IN;
		});
		if (clanData.latest() == null)
		{
			// The saved copy, so the team icons have data before the first load.
			clanData.loadSaved((data, savedAt) -> {});
		}
	}

	public void stop()
	{
		eventBus.unregister(this);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			greeted = false;
		}
		else if (event.getGameState() == GameState.LOGGED_IN && !greeted)
		{
			greeted = true;
			if (!needsLoginLoad())
			{
				devLog.log("login: nothing needs the clan data, not loading it");
				return;
			}
			// Loaded even with the login message off, so the team icons and overlay have fresh data.
			clanData.recent("login", this::greet);
		}
	}

	/**
	 * Whether any feature that works while the panel is closed is on: the login message or the bingo team icons and
	 * overlay. Otherwise the data loads on first use, e.g. opening the panel.
	 */
	private boolean needsLoginLoad()
	{
		return config.loginMessage() || config.teamIconsInChat() || config.teamIconsAboveHeads()
			|| config.bingoTeamOverlay();
	}

	private void greet(ClanData data)
	{
		if (data == null)
		{
			devLog.log("login: no clan data");
			return;
		}
		if (!config.loginMessage())
		{
			devLog.log("login: message turned off");
			return;
		}

		String template = Messages.template(data, "login");
		Map<String, List<String>> values = Messages.challengeValues(data);
		devLog.log("login: {}", Messages.plain(template, values));
		chatSender.send(template, values);
	}
}
