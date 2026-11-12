package com.pvknee.wom;

import com.pvknee.DevLog;
import com.pvknee.PvKneeConfig;
import com.pvknee.player.PlayerInfo;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.WorldType;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.StatChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.xpupdater.XpUpdaterConfig;
import net.runelite.client.plugins.xpupdater.XpUpdaterPlugin;
import net.runelite.client.util.Text;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * When the member logs out with "Update Wise Old Man on logout" on, asks Wise Old Man to update the name they were
 * logged in as, so the challenge standings are more up to date. Only after progress this session (over 100 xp, or a
 * kill or completion count), on a normal world, when XP Updater won't send it for this logout itself, and at most
 * once per 10 minutes per name, even across restarts.
 */
@Singleton
public class LogoutUpdate
{
	private static final HttpUrl PLAYERS_URL = HttpUrl.get("https://api.wiseoldman.net/v2/players");
	private static final String USER_AGENT = "PvKnee-RuneLite-Plugin/1.0";
	private static final Duration MIN_GAP = Duration.ofMinutes(10);
	/** Sessions that gain more xp than this are sent; less counts as no real progress. */
	private static final long MIN_XP_GAINED = 100;
	/** RuneLite's {@code XpUpdaterPlugin} only sends a logout update after gaining more xp than this. */
	private static final long XP_UPDATER_THRESHOLD = 10_000;
	/** Where the time of the last update for a name is kept in the plugin's config. */
	private static final String SENT_KEY_PREFIX = "womUpdatedAt_";
	/** Worlds whose progress Wise Old Man doesn't track. */
	private static final Set<WorldType> SKIPPED_WORLDS = EnumSet.of(WorldType.SEASONAL, WorldType.DEADMAN,
		WorldType.TOURNAMENT_WORLD, WorldType.BETA_WORLD, WorldType.FRESH_START_WORLD, WorldType.NOSAVE_MODE);
	/**
	 * Kill and completion count messages, as RuneLite's chat commands match them, e.g. "Your Vorkath kill count is:
	 * 123." or "Your completed Chambers of Xeric count is: 45."
	 */
	private static final Pattern COUNT_MESSAGE = Pattern.compile(
		"Your (?:completion count for |subdued |completed )?.+? (?:(?:kill|harvest|lap|completion) )?(?:count )?is: [0-9,]+");

	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final PlayerInfo playerInfo;
	private final OkHttpClient okHttpClient;
	private final PvKneeConfig config;
	private final ConfigManager configManager;
	private final PluginManager pluginManager;
	private final XpUpdaterConfig xpUpdaterConfig;
	private final DevLog devLog;

	private boolean loggedIn;
	/** Total xp at the start of this session; -1 until the first tick after login. */
	private long startXp = -1;
	private long latestXp;
	private boolean countSeen;
	private boolean normalWorld;

	@Inject
	LogoutUpdate(Client client, ClientThread clientThread, EventBus eventBus, PlayerInfo playerInfo, OkHttpClient okHttpClient,
		PvKneeConfig config, ConfigManager configManager, PluginManager pluginManager, DevLog devLog)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.playerInfo = playerInfo;
		this.okHttpClient = okHttpClient;
		this.config = config;
		this.configManager = configManager;
		this.pluginManager = pluginManager;
		this.xpUpdaterConfig = configManager.getConfig(XpUpdaterConfig.class);
		this.devLog = devLog;
	}

	public void start()
	{
		eventBus.register(this);
		clientThread.invoke(() ->
		{
			if (client.getGameState() == GameState.LOGGED_IN)
			{
				startSession();
			}
		});
	}

	public void stop()
	{
		eventBus.unregister(this);
		loggedIn = false;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGGED_IN)
		{
			if (!loggedIn)
			{
				startSession();
			}
			normalWorld = isNormalWorld();
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

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (loggedIn && startXp < 0)
		{
			startXp = client.getOverallExperience();
			latestXp = startXp;
		}
	}

	@Subscribe
	public void onStatChanged(StatChanged event)
	{
		if (loggedIn && startXp >= 0)
		{
			latestXp = client.getOverallExperience();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (loggedIn && !countSeen
			&& (event.getType() == ChatMessageType.GAMEMESSAGE || event.getType() == ChatMessageType.SPAM)
			&& COUNT_MESSAGE.matcher(Text.removeTags(event.getMessage())).find())
		{
			countSeen = true;
			devLog.log("wom: count message seen");
		}
	}

	private void startSession()
	{
		loggedIn = true;
		startXp = -1;
		latestXp = 0;
		countSeen = false;
		normalWorld = isNormalWorld();
	}

	private boolean isNormalWorld()
	{
		return client.getWorldType().stream().noneMatch(SKIPPED_WORLDS::contains);
	}

	private void update(String name)
	{
		if (name == null)
		{
			return;
		}

		String player = Text.toJagexName(name);
		long gained = startXp < 0 ? 0 : latestXp - startXp;
		if (!countSeen && gained <= MIN_XP_GAINED)
		{
			devLog.log("wom: {} made too little progress ({} xp, no counts), skipped", player, gained);
			return;
		}
		if (!normalWorld)
		{
			devLog.log("wom: not a normal world, skipped");
			return;
		}
		if (xpUpdaterSends(gained))
		{
			devLog.log("wom: XP Updater sends this update, skipped");
			return;
		}

		String key = SENT_KEY_PREFIX + Text.standardize(player).replace(' ', '_');
		Instant now = Instant.now();
		Long last = configManager.getConfiguration(PvKneeConfig.GROUP, key, Long.class);
		if (last != null && now.isBefore(Instant.ofEpochMilli(last).plus(MIN_GAP)))
		{
			devLog.log("wom: {} was updated less than 10 minutes ago, skipped", player);
			return;
		}
		configManager.setConfiguration(PvKneeConfig.GROUP, key, now.toEpochMilli());
		send(player);
	}

	/**
	 * Whether RuneLite's XP Updater will send its own update for this logout: it is on, its Wise Old Man option is
	 * ticked and the session gained more than its threshold.
	 */
	private boolean xpUpdaterSends(long gained)
	{
		if (gained <= XP_UPDATER_THRESHOLD)
		{
			return false;
		}
		Plugin plugin = pluginManager.getPlugins().stream()
			.filter(p -> p instanceof XpUpdaterPlugin)
			.findFirst()
			.orElse(null);
		return plugin != null && pluginManager.isPluginActive(plugin) && xpUpdaterConfig.wiseoldman();
	}

	private void send(String player)
	{
		devLog.log("wom: updating {}", player);
		Request request = new Request.Builder()
			.url(PLAYERS_URL.newBuilder().addPathSegment(player).build())
			.header("User-Agent", USER_AGENT)
			.post(RequestBody.create(null, new byte[0]))
			.build();

		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				devLog.log("wom: update for {} failed, {}", player, e.getMessage(), e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				response.close();
				if (response.isSuccessful())
				{
					devLog.log("wom: {} updated", player);
				}
				else
				{
					devLog.log("wom: update for {} failed, response {}", player, response.code());
				}
			}
		});
	}
}
