package com.pvknee.chat;

import com.pvknee.DevLog;
import com.pvknee.PvKneeConfig;
import com.pvknee.blocks.Times;
import com.pvknee.data.ClanData;
import com.pvknee.data.ClanDataClient;
import com.pvknee.data.Competition;
import com.pvknee.data.Competitions;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.Value;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

/**
 * The weekly challenge chat messages, game messages only the member sees, working while the panel is closed:
 * <ul>
 * <li>once per login, the sheet's "login" message, filled with the current challenges;</li>
 * <li>when a challenge starts, has 1 hour left and ends. Each minute it looks for moments since the last look, so a
 * moment is announced once, and nothing from before the client started; moments while logged out are skipped.</li>
 * </ul>
 * Uses the last loaded data or the saved copy, with one quiet endpoint load when the member logs in.
 */
@Singleton
public class ChallengeMessages
{
	private static final Duration CHECK_INTERVAL = Duration.ofMinutes(1);
	private static final Duration HOUR_LEFT = Duration.ofHours(1);

	private final ClanDataClient clanData;
	private final Client client;
	private final ClientThread clientThread;
	private final ChatMessageManager chatMessageManager;
	private final EventBus eventBus;
	private final ScheduledExecutorService executor;
	private final PvKneeConfig config;
	private final DevLog devLog;

	private ScheduledFuture<?> checks;
	/** The end of the last look; moments after it and up to now are due. Only used on the executor. */
	private Instant checkedUntil;
	/** The data the next moment was last logged for, so it is logged once per load. */
	private ClanData logged;
	/** Whether this login has been greeted; hops and reconnects log in again without a new greeting. */
	private boolean greeted;

	@Inject
	ChallengeMessages(ClanDataClient clanData, Client client, ClientThread clientThread, ChatMessageManager chatMessageManager,
		EventBus eventBus, ScheduledExecutorService executor, PvKneeConfig config, DevLog devLog)
	{
		this.clanData = clanData;
		this.client = client;
		this.clientThread = clientThread;
		this.chatMessageManager = chatMessageManager;
		this.eventBus = eventBus;
		this.executor = executor;
		this.config = config;
		this.devLog = devLog;
	}

	/**
	 * Starts the messages. Already logged in counts as greeted, so turning the plugin on mid-session stays quiet.
	 */
	public void start()
	{
		checkedUntil = Instant.now();
		logged = null;
		eventBus.register(this);
		clientThread.invoke(() ->
		{
			greeted = client.getGameState() == GameState.LOGGED_IN;
		});
		if (clanData.latest() == null)
		{
			clanData.loadSaved((data, savedAt) -> {});
		}
		checks = executor.scheduleAtFixedRate(this::check, CHECK_INTERVAL.toMillis(), CHECK_INTERVAL.toMillis(), TimeUnit.MILLISECONDS);
	}

	public void stop()
	{
		eventBus.unregister(this);
		if (checks != null)
		{
			checks.cancel(false);
			checks = null;
		}
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
			// Loaded even with the login message off, so the alerts have fresh times.
			clanData.recent("login", this::greet);
		}
	}

	private void greet(ClanData data)
	{
		if (!config.loginMessage())
		{
			devLog.log("login: message turned off");
			return;
		}

		String template = Messages.template(data, "login");
		Map<String, List<String>> values = Messages.challengeValues(data);
		devLog.log("login: {}", Messages.plain(template, values));
		chatMessageManager.queue(Messages.message(template, values));
	}

	private void check()
	{
		Instant from = checkedUntil;
		Instant now = Instant.now();
		checkedUntil = now;

		ClanData data = clanData.latest();
		if (data == null)
		{
			return;
		}

		List<Moment> moments = moments(data.getCompetitions());
		if (data != logged)
		{
			logged = data;
			moments.stream()
				.filter(m -> m.getTime().isAfter(now))
				.min(Comparator.comparing(Moment::getTime))
				.ifPresentOrElse(
					m -> devLog.log("alerts: next is {} {} at {}", m.getCompetition().getName(), m.getKind().describe(), m.getTime()),
					() -> devLog.log("alerts: nothing coming up"));
		}

		Map<Kind, List<Moment>> due = moments.stream()
			.filter(m -> m.getTime().isAfter(from) && !m.getTime().isAfter(now))
			.collect(Collectors.groupingBy(Moment::getKind, () -> new EnumMap<>(Kind.class), Collectors.toList()));
		due.forEach((kind, list) -> notify(kind, list, data));
	}

	/**
	 * One message for every challenge with this moment in the same look, e.g. both new challenges starting.
	 */
	private void notify(Kind kind, List<Moment> moments, ClanData data)
	{
		String template = Messages.template(data, kind.key);
		Map<String, List<String>> values = Messages.challengeValues(data);
		values.put("challenges", moments.stream()
			.map(m -> m.getCompetition().getName())
			.collect(Collectors.toList()));
		String message = Messages.plain(template, values);
		if (!kind.setting.test(config))
		{
			devLog.log("alerts: {} (turned off)", message);
		}
		else if (client.getGameState() != GameState.LOGGED_IN)
		{
			devLog.log("alerts: {} (logged out, skipped)", message);
		}
		else
		{
			devLog.log("alerts: {}", message);
			chatMessageManager.queue(Messages.message(template, values));
		}
	}

	/**
	 * Every start, hour-left and end moment of the current and upcoming challenges.
	 */
	private static List<Moment> moments(Competitions competitions)
	{
		List<Moment> moments = new ArrayList<>();
		if (competitions == null)
		{
			return moments;
		}

		List<Competition> all = new ArrayList<>();
		if (competitions.getCurrent() != null)
		{
			all.addAll(competitions.getCurrent());
		}
		if (competitions.getUpcoming() != null)
		{
			all.addAll(competitions.getUpcoming());
		}

		for (Competition competition : all)
		{
			Instant start = Times.parse(competition.getStartsAt());
			Instant end = Times.parse(competition.getEndsAt());
			if (start != null)
			{
				moments.add(new Moment(competition, Kind.START, start));
			}
			if (end != null)
			{
				moments.add(new Moment(competition, Kind.HOUR_LEFT, end.minus(HOUR_LEFT)));
				moments.add(new Moment(competition, Kind.END, end));
			}
		}
		return moments;
	}

	private enum Kind
	{
		START("alert_start", PvKneeConfig::challengeStartAlert),
		HOUR_LEFT("alert_hour", PvKneeConfig::challengeHourLeftAlert),
		END("alert_end", PvKneeConfig::challengeEndAlert);

		/** The sheet's message key for this moment. */
		private final String key;
		private final Predicate<PvKneeConfig> setting;

		Kind(String key, Predicate<PvKneeConfig> setting)
		{
			this.key = key;
			this.setting = setting;
		}

		String describe()
		{
			switch (this)
			{
				case START:
					return "starting";
				case HOUR_LEFT:
					return "with 1 hour left";
				default:
					return "ending";
			}
		}
	}

	@Value
	private static class Moment
	{
		Competition competition;
		Kind kind;
		Instant time;
	}
}
