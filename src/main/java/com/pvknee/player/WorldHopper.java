package com.pvknee.player;

import com.pvknee.DevLog;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.World;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.WorldService;
import net.runelite.client.util.WorldUtil;
import net.runelite.http.api.worlds.WorldRegion;
import net.runelite.http.api.worlds.WorldResult;

/**
 * Hops to a world on one click, the way RuneLite's World Hopper does: logged in, it opens the world switcher and hops
 * on the next tick; on the login screen it changes the login world instead. The game's own hop rules apply.
 */
@Singleton
public class WorldHopper
{
	/** Ticks to wait for the world switcher to open before giving up, as World Hopper does. */
	private static final int SWITCHER_ATTEMPTS = 5;

	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final WorldService worldService;
	private final DevLog devLog;

	private World target;
	private int attempts;

	@Inject
	WorldHopper(Client client, ClientThread clientThread, EventBus eventBus, WorldService worldService, DevLog devLog)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.worldService = worldService;
		this.devLog = devLog;
	}

	public void start()
	{
		eventBus.register(this);
	}

	public void stop()
	{
		eventBus.unregister(this);
		clientThread.invoke(() ->
		{
			target = null;
		});
	}

	/**
	 * Hops to the world, or makes it the login world on the login screen. Call from any thread.
	 */
	public void hop(int worldId)
	{
		clientThread.invoke(() ->
		{
			devLog.log("hop: world {} requested", worldId);
			WorldResult worlds = worldService.getWorlds();
			net.runelite.http.api.worlds.World world = worlds == null ? null : worlds.findWorld(worldId);
			if (world == null)
			{
				devLog.log("hop: world {} is not in the world list", worldId);
				return;
			}

			World rsWorld = client.createWorld();
			WorldRegion region = world.getRegion();
			rsWorld.setActivity(world.getActivity());
			rsWorld.setAddress(world.getAddress());
			rsWorld.setId(world.getId());
			rsWorld.setPlayerCount(world.getPlayers());
			rsWorld.setLocation(region == null ? 0 : region.getId());
			rsWorld.setTypes(WorldUtil.toWorldTypes(world.getTypes()));

			GameState state = client.getGameState();
			if (state == GameState.LOGIN_SCREEN)
			{
				client.changeWorld(rsWorld);
				devLog.log("hop: login world set to {}", worldId);
			}
			else if (state == GameState.LOGGED_IN)
			{
				target = rsWorld;
				attempts = 0;
			}
			else
			{
				devLog.log("hop: not now, the game is {}", state);
			}
		});
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		if (target == null)
		{
			return;
		}

		if (client.getWidget(InterfaceID.Worldswitcher.BUTTONS) == null)
		{
			client.openWorldHopper();
			if (++attempts >= SWITCHER_ATTEMPTS)
			{
				devLog.log("hop: the world switcher didn't open, gave up");
				target = null;
			}
			return;
		}

		devLog.log("hop: hopping to world {}", target.getId());
		client.hopToWorld(target);
		target = null;
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			target = null;
		}
	}
}
