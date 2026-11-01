package com.pvknee.player;

import com.pvknee.DevLog;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanID;
import net.runelite.api.clan.ClanMember;
import net.runelite.api.clan.ClanRank;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

/**
 * Tracks, on the client thread, whether the member is logged in, their name, world and clan rank. Readable from any
 * thread; the listener is told on the Swing thread when anything changes.
 */
@Singleton
public class PlayerInfo
{
	/** How long after a hop the last rank is kept while waiting for the clan channel, in ticks (about 15 seconds). */
	private static final int HOP_GRACE_TICKS = 25;

	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final DevLog devLog;

	private volatile PlayerState state = PlayerState.LOGGED_OUT;
	private Runnable listener;
	/** Set while the name or clan rank may still arrive; read again each tick until both are known. */
	private boolean pending;
	/** Ticks left to keep the last rank after a hop, since the game drops the clan channel until the new world sends it. */
	private int hopTicks;

	@Inject
	PlayerInfo(Client client, ClientThread clientThread, EventBus eventBus, DevLog devLog)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.devLog = devLog;
	}

	/**
	 * Starts tracking. The listener runs on the Swing thread after each change.
	 */
	public void start(Runnable listener)
	{
		this.listener = listener;
		eventBus.register(this);
		clientThread.invoke(() ->
		{
			pending = client.getGameState() == GameState.LOGGED_IN;
		});
	}

	public void stop()
	{
		eventBus.unregister(this);
		listener = null;
		state = PlayerState.LOGGED_OUT;
	}

	public PlayerState get()
	{
		return state;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState gameState = event.getGameState();
		if (gameState == GameState.HOPPING)
		{
			hopTicks = HOP_GRACE_TICKS;
		}
		else if (gameState == GameState.LOGGED_IN)
		{
			pending = true;
		}
		else if (gameState == GameState.LOGIN_SCREEN)
		{
			pending = false;
			hopTicks = 0;
			update(PlayerState.LOGGED_OUT);
		}
	}

	@Subscribe
	public void onClanChannelChanged(ClanChannelChanged event)
	{
		if (event.getClanId() == ClanID.CLAN)
		{
			pending = true;
			if (event.getClanChannel() != null)
			{
				hopTicks = 0;
			}
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (pending)
		{
			read();
		}
	}

	private void read()
	{
		Player player = client.getLocalPlayer();
		String name = player == null ? null : player.getName();
		if (name == null)
		{
			return;
		}

		ClanChannel channel = client.getClanChannel(ClanID.CLAN);
		if (channel == null && hopTicks > 0 && name.equals(state.getName()))
		{
			// Just hopped and the new world hasn't sent the clan channel yet: keep the last rank meanwhile.
			hopTicks--;
			update(new PlayerState(true, name, client.getWorld(), state.getRank(), state.getRankTitle(), state.getRankTitles()));
			return;
		}
		hopTicks = 0;

		ClanSettings settings = client.getClanSettings(ClanID.CLAN);
		ClanMember member = settings == null ? null : settings.findMember(name);
		ClanRank rank = channel == null || member == null ? null : member.getRank();
		ClanTitle title = rank == null ? null : settings.titleForRank(rank);

		// In a clan channel whose settings haven't arrived yet: try again next tick.
		pending = channel != null && settings == null;
		update(new PlayerState(true, name, client.getWorld(), rank, title == null ? null : title.getName(),
			rank == null ? Collections.emptyMap() : rankTitles(settings)));
	}

	/**
	 * Every rank title the clan uses, in lower case, with the lowest rank that has it.
	 */
	private static Map<String, Integer> rankTitles(ClanSettings settings)
	{
		Map<String, Integer> titles = new HashMap<>();
		for (int rank = ClanRank.GUEST.getRank(); rank <= ClanRank.JMOD.getRank(); rank++)
		{
			ClanTitle title = settings.titleForRank(new ClanRank(rank));
			if (title != null && title.getName() != null)
			{
				titles.putIfAbsent(title.getName().toLowerCase(Locale.ENGLISH), rank);
			}
		}
		return Collections.unmodifiableMap(titles);
	}

	private void update(PlayerState next)
	{
		PlayerState previous = state;
		if (previous.equals(next))
		{
			return;
		}

		state = next;
		if (!next.isLoggedIn())
		{
			devLog.log("player: logged out");
		}
		else
		{
			if (!Objects.equals(previous.getName(), next.getName()) || previous.getWorld() != next.getWorld())
			{
				devLog.log("player: {} on world {}", next.getName(), next.getWorld());
			}
			if (!Objects.equals(previous.getRank(), next.getRank()))
			{
				devLog.log("player: clan rank {}", next.getRank() == null ? "none" : next.getRankTitle() + " (" + next.getRank().getRank() + ")");
			}
		}

		Runnable listener = this.listener;
		if (listener != null)
		{
			SwingUtilities.invokeLater(listener);
		}
	}
}
