package com.pvknee.bingo;

import com.pvknee.DevLog;
import com.pvknee.PvKneeConfig;
import com.pvknee.blocks.Times;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.time.Instant;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

/**
 * A small movable box with the member's team name in the team colour and the date and time in UTC to the minute, so
 * admins can tell when a screenshot was taken. Shown only while logged in, on a team, and the bingo runs.
 */
@Singleton
public class TeamOverlay extends OverlayPanel
{
	private static final String SEPARATOR = " · ";
	/** Longer team names are cut short with "...", so the box stays small. */
	private static final int MAX_NAME_WIDTH = 140;

	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final OverlayManager overlayManager;
	private final TeamRoster roster;
	private final PvKneeConfig config;
	private final DevLog devLog;

	/** The member's team while the box shows, else null; checked each tick. */
	private TeamRoster.Team team;
	/** The team name as drawn, worked out once per team. */
	private TeamRoster.Team nameFor;
	private String name;
	/** The last state logged, so it is logged once per change. */
	private String logged;

	@Inject
	TeamOverlay(Client client, ClientThread clientThread, EventBus eventBus, OverlayManager overlayManager,
		TeamRoster roster, PvKneeConfig config, DevLog devLog)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.overlayManager = overlayManager;
		this.roster = roster;
		this.config = config;
		this.devLog = devLog;
		setPosition(OverlayPosition.BOTTOM_LEFT);
	}

	/**
	 * Starts the box. Call after the roster has started.
	 */
	public void start()
	{
		eventBus.register(this);
		overlayManager.add(this);
	}

	public void stop()
	{
		overlayManager.remove(this);
		eventBus.unregister(this);
		clientThread.invoke(() ->
		{
			team = null;
			nameFor = null;
			logged = null;
		});
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN)
		{
			// No ticks while logged out, so hide it here.
			show(null, "logged out");
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		Player player = client.getLocalPlayer();
		TeamRoster.Team own = player == null ? null : roster.teamOf(player.getName());
		show(own, !config.bingoTeamOverlay() ? "turned off"
			: roster.isEmpty() ? "no bingo running"
			: own == null ? "not on a team"
			: null);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		TeamRoster.Team shown = team;
		if (shown == null)
		{
			return null;
		}

		FontMetrics metrics = graphics.getFontMetrics();
		if (nameFor != shown)
		{
			nameFor = shown;
			name = plainName(shown.getName(), metrics);
		}

		String stamp = Times.utcStamp(Instant.now());
		int width = metrics.stringWidth(name + SEPARATOR + stamp);
		panelComponent.setPreferredSize(new Dimension(width + 2 * ComponentConstants.STANDARD_BORDER, 0));
		panelComponent.getChildren().add(LineComponent.builder()
			// Overlay text reads opening colour tags only, so each part starts with its own.
			.left(ColorUtil.prependColorTag(name, shown.getColour()) + ColorUtil.prependColorTag(SEPARATOR + stamp, Color.WHITE))
			.build());
		return super.render(graphics);
	}

	/**
	 * The team name as plain text, so no tags in it take effect, cut short with "..." if it is too wide.
	 */
	private static String plainName(String teamName, FontMetrics metrics)
	{
		String plain = Text.removeTags(teamName == null ? "" : teamName).replace('<', '(').replace('>', ')').trim();
		if (metrics.stringWidth(plain) <= MAX_NAME_WIDTH)
		{
			return plain;
		}
		int length = plain.length();
		while (length > 0 && metrics.stringWidth(plain.substring(0, length).trim() + "...") > MAX_NAME_WIDTH)
		{
			length--;
		}
		return plain.substring(0, length).trim() + "...";
	}

	/**
	 * Shows the box for this team, or hides it for the reason given.
	 */
	private void show(TeamRoster.Team own, String hiddenBecause)
	{
		team = hiddenBecause == null ? own : null;
		String state = hiddenBecause == null ? "shown for " + own.getName() : "hidden, " + hiddenBecause;
		if (!state.equals(logged))
		{
			logged = state;
			devLog.log("team overlay: {}", state);
		}
	}
}
