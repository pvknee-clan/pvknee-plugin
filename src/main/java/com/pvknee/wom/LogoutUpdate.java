package com.pvknee.wom;

import com.pvknee.DevLog;
import com.pvknee.PvKneeConfig;
import com.pvknee.player.PlayerInfo;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

/**
 * When the member logs out with "Update Wise Old Man on logout" on, asks Wise Old Man to update the name they were
 * logged in as, at most once per 10 minutes per name, so the challenge standings are more up to date.
 */
@Singleton
public class LogoutUpdate
{
	private static final Duration MIN_GAP = Duration.ofMinutes(10);

	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final PlayerInfo playerInfo;
	private final WomClient wom;
	private final PvKneeConfig config;
	private final DevLog devLog;

	/** When each name (in lower case) was last sent. */
	private final Map<String, Instant> sentAt = new ConcurrentHashMap<>();
	private boolean loggedIn;

	@Inject
	LogoutUpdate(Client client, ClientThread clientThread, EventBus eventBus, PlayerInfo playerInfo, WomClient wom,
		PvKneeConfig config, DevLog devLog)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.playerInfo = playerInfo;
		this.wom = wom;
		this.config = config;
		this.devLog = devLog;
	}

	public void start()
	{
		eventBus.register(this);
		clientThread.invoke(() ->
		{
			loggedIn = client.getGameState() == GameState.LOGGED_IN;
		});
	}

	public void stop()
	{
		eventBus.unregister(this);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			loggedIn = true;
		}
		else if (event.getGameState() == GameState.LOGIN_SCREEN && loggedIn)
		{
			loggedIn = false;
			if (config.updateWomOnLogout())
			{
				update(playerInfo.lastName());
			}
		}
	}

	private void update(String name)
	{
		if (name == null)
		{
			return;
		}

		String player = name.replace('\u00A0', ' ').trim();
		String key = player.toLowerCase(Locale.ENGLISH);
		Instant now = Instant.now();
		Instant last = sentAt.get(key);
		if (last != null && now.isBefore(last.plus(MIN_GAP)))
		{
			devLog.log("wom: {} was updated less than 10 minutes ago, skipped", player);
			return;
		}

		sentAt.put(key, now);
		devLog.log("wom: updating {}", player);
		wom.updatePlayer(player,
			() -> devLog.log("wom: {} updated", player),
			e -> devLog.log("wom: update for {} failed, {}", player, e.getMessage(), e));
	}
}
