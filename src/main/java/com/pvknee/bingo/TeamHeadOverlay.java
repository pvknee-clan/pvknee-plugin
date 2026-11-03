package com.pvknee.bingo;

import com.pvknee.DevLog;
import com.pvknee.PvKneeConfig;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldType;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.PlayerDespawned;
import net.runelite.api.events.PlayerSpawned;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.util.Text;

/**
 * A team dot for each player on a bingo team, just left of the name RuneLite's Player Indicators plugin draws for
 * them; no name, no dot. The players on a team are kept as they spawn and despawn, so drawing never searches the
 * scene. Hidden in the Wilderness and on PvP worlds.
 */
@Singleton
public class TeamHeadOverlay extends Overlay
{
	private static final int NAME_GAP = 2;
	/** Lifts the dot a little above the middle of the name, where it looks centred. */
	private static final int LIFT = 2;

	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final OverlayManager overlayManager;
	private final TeamRoster roster;
	private final TeamDots dots;
	private final IndicatorNames names;
	private final PvKneeConfig config;
	private final DevLog devLog;

	/** The players in the scene who are on a team. */
	private final Map<Player, TeamRoster.Team> shown = new HashMap<>();
	/** Those of them whose name Player Indicators draws, with the width of its rank icon; found each tick. */
	private final Map<Player, Integer> named = new HashMap<>();
	/** Why the dots are hidden, or null while they show; checked each tick. */
	private String hiddenBecause;
	/** The last state logged, so it is logged once per change. */
	private String logged;

	@Inject
	TeamHeadOverlay(Client client, ClientThread clientThread, EventBus eventBus, OverlayManager overlayManager,
		TeamRoster roster, TeamDots dots, IndicatorNames names, PvKneeConfig config, DevLog devLog)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.overlayManager = overlayManager;
		this.roster = roster;
		this.dots = dots;
		this.names = names;
		this.config = config;
		this.devLog = devLog;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	/**
	 * Starts the dots. Call after the roster has started.
	 */
	public void start()
	{
		eventBus.register(this);
		roster.onChange(this::findPlayers);
		overlayManager.add(this);
		clientThread.invoke(this::findPlayers);
	}

	public void stop()
	{
		overlayManager.remove(this);
		eventBus.unregister(this);
		clientThread.invoke(() ->
		{
			shown.clear();
			named.clear();
			hiddenBecause = null;
			logged = null;
		});
	}

	@Subscribe
	public void onPlayerSpawned(PlayerSpawned event)
	{
		Player player = event.getPlayer();
		TeamRoster.Team team = roster.teamOf(player.getName());
		if (team != null)
		{
			shown.put(player, team);
		}
	}

	@Subscribe
	public void onPlayerDespawned(PlayerDespawned event)
	{
		shown.remove(event.getPlayer());
		named.remove(event.getPlayer());
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			shown.clear();
			named.clear();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		hiddenBecause = !config.teamIconsAboveHeads() ? "turned off"
			: roster.isEmpty() ? "no bingo running"
			: client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 1 ? "in the Wilderness"
			: WorldType.isPvpWorld(client.getWorldType()) ? "on a PvP world"
			: names.off();

		named.clear();
		if (hiddenBecause == null)
		{
			shown.keySet().stream()
				.filter(names::named)
				.forEach(player -> named.put(player, names.rankWidth(player)));
		}

		String state = hiddenBecause != null ? "hidden, " + hiddenBecause
			: "drawn for " + named.size() + " of " + shown.size() + " team players in view" + (named.isEmpty() ? ""
				: " (" + named.keySet().stream().map(p -> Text.removeTags(p.getName())).sorted().collect(Collectors.joining(", ")) + ")");
		if (!state.equals(logged))
		{
			logged = state;
			devLog.log("overhead icons: {}", state);
		}
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (hiddenBecause != null || named.isEmpty())
		{
			return null;
		}

		named.forEach((player, rankWidth) ->
		{
			TeamRoster.Team team = shown.get(player);
			Point name = team == null ? null : names.leftOfName(graphics, player, rankWidth);
			if (name != null)
			{
				graphics.drawImage(dots.image(team.getColour()),
					name.getX() - TeamDots.SIZE - NAME_GAP, name.getY() - TeamDots.SIZE / 2 - LIFT, null);
			}
		});
		return null;
	}

	/**
	 * Finds the players on a team among those in the scene, once each time the roster changes.
	 */
	private void findPlayers()
	{
		shown.clear();
		if (client.getGameState() != GameState.LOGGED_IN || roster.isEmpty())
		{
			return;
		}
		for (Player player : client.getTopLevelWorldView().players())
		{
			TeamRoster.Team team = player == null ? null : roster.teamOf(player.getName());
			if (team != null)
			{
				shown.put(player, team);
			}
		}
	}
}
