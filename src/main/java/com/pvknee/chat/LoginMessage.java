package com.pvknee.chat;

import com.pvknee.DevLog;
import com.pvknee.PvKneeConfig;
import com.pvknee.data.ClanData;
import com.pvknee.data.ClanDataClient;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

/**
 * Once per login, a game message only the member sees, built from the endpoint's "login" template. Uses data loaded
 * in the last few minutes, otherwise loads the endpoint once, falling back to the saved copy.
 */
@Singleton
public class LoginMessage
{
	private static final Duration FRESH = Duration.ofMinutes(10);

	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final ClanDataClient clanData;
	private final ChatMessageManager chatMessageManager;
	private final PvKneeConfig config;
	private final DevLog devLog;

	/** Whether this login has been greeted; hops and reconnects log in again without a new greeting. */
	private boolean greeted;

	@Inject
	LoginMessage(Client client, ClientThread clientThread, EventBus eventBus, ClanDataClient clanData,
		ChatMessageManager chatMessageManager, PvKneeConfig config, DevLog devLog)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.clanData = clanData;
		this.chatMessageManager = chatMessageManager;
		this.config = config;
		this.devLog = devLog;
	}

	/**
	 * Starts greeting logins. Already logged in counts as greeted, so turning the plugin on mid-session stays quiet.
	 */
	public void start()
	{
		eventBus.register(this);
		clientThread.invoke(() ->
		{
			greeted = client.getGameState() == GameState.LOGGED_IN;
		});
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
			if (config.loginMessage())
			{
				greet();
			}
			else
			{
				devLog.log("login: message turned off");
			}
		}
	}

	private void greet()
	{
		ClanData latest = clanData.latest();
		Instant latestAt = clanData.latestAt();
		if (latest != null && latestAt != null && Instant.now().isBefore(latestAt.plus(FRESH)))
		{
			send(latest);
			return;
		}

		devLog.log("login: loading the endpoint");
		clanData.fetch(this::send, e ->
		{
			devLog.log("login: load failed, {}", e.getMessage(), e);
			ClanData fallback = clanData.latest();
			if (fallback != null)
			{
				send(fallback);
			}
			else
			{
				clanData.loadSaved((saved, savedAt) -> send(saved));
			}
		});
	}

	/**
	 * Sends the message. Safe from any thread: the chat manager adds it on the client thread.
	 */
	private void send(ClanData data)
	{
		String template = data.getMessages() == null ? null : data.getMessages().get("login");
		if (template == null || template.trim().isEmpty())
		{
			devLog.log("login: no template, no message");
			return;
		}

		String message = Messages.fill(template.trim(), Messages.challengeValues(data));
		devLog.log("login: sent");
		chatMessageManager.queue(QueuedMessage.builder()
			.type(ChatMessageType.GAMEMESSAGE)
			.runeLiteFormattedMessage(new ChatMessageBuilder()
				.append(ChatColorType.NORMAL)
				.append(message)
				.build())
			.build());
	}
}
