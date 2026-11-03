package com.pvknee.bingo;

import com.pvknee.DevLog;
import com.pvknee.blocks.Times;
import com.pvknee.data.Bingo;
import com.pvknee.data.ClanData;
import com.pvknee.data.ClanDataClient;
import com.pvknee.player.PlayerState;
import java.awt.Color;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.util.Text;

/**
 * Who is on which bingo team, while a bingo runs: empty before the start, after the end and with no bingo. Built
 * from the last loaded data, checked each tick on the client thread.
 */
@Singleton
public class TeamRoster
{
	@Value
	public static class Team
	{
		String name;
		Color colour;
	}

	private final ClanDataClient clanData;
	private final EventBus eventBus;
	private final DevLog devLog;
	private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

	/** Each player's team, by {@link PlayerState#nameKey}. */
	private volatile Map<String, Team> teams = Collections.emptyMap();
	/** The data the roster was built from, so it is built again only when new data arrives. */
	private ClanData builtFrom;
	private Instant endsAt;
	/** The last roster logged, so a reload that changes nothing logs nothing. */
	private String logged;

	@Inject
	TeamRoster(ClanDataClient clanData, EventBus eventBus, DevLog devLog)
	{
		this.clanData = clanData;
		this.eventBus = eventBus;
		this.devLog = devLog;
	}

	public void start()
	{
		eventBus.register(this);
	}

	public void stop()
	{
		eventBus.unregister(this);
		listeners.clear();
		teams = Collections.emptyMap();
		builtFrom = null;
		endsAt = null;
		logged = null;
	}

	/**
	 * Runs after the roster changes, until {@link #stop}.
	 */
	public void onChange(Runnable listener)
	{
		listeners.add(listener);
	}

	/**
	 * The team of the player with this name, which may carry chat tags; null when not on a team or no bingo runs.
	 */
	public Team teamOf(String name)
	{
		return name == null || teams.isEmpty() ? null : teams.get(PlayerState.nameKey(Text.removeTags(name)));
	}

	/**
	 * Every team with at least one player.
	 */
	public Collection<Team> teams()
	{
		return new HashSet<>(teams.values());
	}

	public boolean isEmpty()
	{
		return teams.isEmpty();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		ClanData data = clanData.latest();
		if (data != builtFrom)
		{
			builtFrom = data;
			build(data == null ? null : data.getBingo());
		}
		else if (endsAt != null && !Instant.now().isBefore(endsAt))
		{
			log("none, the bingo has ended");
			set(Collections.emptyMap(), null);
		}
	}

	private void build(Bingo bingo)
	{
		Instant end = bingo == null ? null : Times.parse(bingo.getEndsAt());
		if (bingo == null || bingo.getState() != Bingo.State.RUNNING || end != null && !Instant.now().isBefore(end))
		{
			log(bingo == null ? "none, no bingo"
				: bingo.getState() != Bingo.State.RUNNING ? "none, bingo " + bingo.getState() : "none, the bingo has ended");
			set(Collections.emptyMap(), null);
			return;
		}

		Map<String, Team> next = new HashMap<>();
		for (Bingo.Team team : bingo.getTeams())
		{
			Team entry = new Team(team.getName(), TeamColours.of(bingo.getTeams(), team));
			for (Bingo.Member member : team.getMembers())
			{
				if (member.getName() != null)
				{
					next.putIfAbsent(PlayerState.nameKey(member.getName()), entry);
				}
			}
		}
		log(bingo.getTeams().size() + " teams, " + next.size() + " players, until " + bingo.getEndsAt());
		set(Collections.unmodifiableMap(next), end);
	}

	private void log(String roster)
	{
		if (!roster.equals(logged))
		{
			logged = roster;
			devLog.log("bingo roster: {}", roster);
		}
	}

	private void set(Map<String, Team> next, Instant end)
	{
		endsAt = end;
		if (next.equals(teams))
		{
			return;
		}
		teams = next;
		listeners.forEach(Runnable::run);
	}
}
