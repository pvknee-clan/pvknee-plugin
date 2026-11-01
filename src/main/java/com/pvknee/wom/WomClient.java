package com.pvknee.wom;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Fetches Wise Old Man competition standings and keeps each for five minutes, and asks for player updates.
 * Standings callbacks run on the Swing thread; ask for standings from the Swing thread too.
 */
@Slf4j
@Singleton
public class WomClient
{
	private static final String COMPETITION_URL = "https://api.wiseoldman.net/v2/competitions/";
	private static final HttpUrl PLAYERS_URL = HttpUrl.get("https://api.wiseoldman.net/v2/players");
	private static final String COMPETITION_PAGE = "https://wiseoldman.net/competitions/";
	private static final Duration CACHE_TIME = Duration.ofMinutes(5);

	private final OkHttpClient okHttpClient;
	private final Gson gson;
	private final Map<Long, Cached> cache = new HashMap<>();
	private final Map<Long, List<Listener>> pending = new HashMap<>();

	@Inject
	WomClient(OkHttpClient okHttpClient, Gson gson)
	{
		this.okHttpClient = okHttpClient;
		this.gson = gson;
	}

	public static String competitionPage(long competitionId)
	{
		return COMPETITION_PAGE + competitionId;
	}

	/**
	 * Standings sorted by gain, highest first. Answers at once from the cache while it is fresh. Once it is older,
	 * answers with the old standings first, then again when the new ones arrive; a failed refetch keeps the old ones.
	 */
	public void standings(long competitionId, Consumer<List<Standing>> onSuccess, Consumer<Exception> onFailure)
	{
		Cached cached = cache.get(competitionId);
		if (cached != null)
		{
			onSuccess.accept(cached.standings);
			if (cached.isFresh())
			{
				return;
			}
			onFailure = error -> {};
		}

		Listener listener = new Listener(onSuccess, onFailure);
		List<Listener> waiting = pending.get(competitionId);
		if (waiting != null)
		{
			waiting.add(listener);
			return;
		}

		waiting = new ArrayList<>();
		waiting.add(listener);
		pending.put(competitionId, waiting);

		Request request = new Request.Builder()
			.url(COMPETITION_URL + competitionId)
			.build();

		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				finish(competitionId, null, e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				try (ResponseBody body = response.body())
				{
					if (!response.isSuccessful() || body == null)
					{
						throw new IOException("Unexpected response " + response.code());
					}

					Competition competition = gson.fromJson(body.charStream(), Competition.class);
					if (competition == null || competition.participations == null)
					{
						throw new IOException("No participations");
					}
					finish(competitionId, toStandings(competition), null);
				}
				catch (IOException | JsonParseException e)
				{
					finish(competitionId, null, e);
				}
			}
		});
	}

	/**
	 * Asks Wise Old Man to update a player's profile. Callbacks run on a background thread.
	 */
	public void updatePlayer(String name, Runnable onSuccess, Consumer<Exception> onFailure)
	{
		Request request = new Request.Builder()
			.url(PLAYERS_URL.newBuilder().addPathSegment(name).build())
			.post(RequestBody.create(null, new byte[0]))
			.build();

		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				onFailure.accept(e);
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				response.close();
				if (response.isSuccessful())
				{
					onSuccess.run();
				}
				else
				{
					onFailure.accept(new IOException("Unexpected response " + response.code()));
				}
			}
		});
	}

	private void finish(long competitionId, List<Standing> standings, Exception error)
	{
		if (error != null)
		{
			log.debug("Failed to load WOM competition {}", competitionId, error);
		}

		SwingUtilities.invokeLater(() ->
		{
			if (standings != null)
			{
				cache.put(competitionId, new Cached(standings, Instant.now()));
			}

			for (Listener listener : pending.remove(competitionId))
			{
				if (standings != null)
				{
					listener.onSuccess.accept(standings);
				}
				else
				{
					listener.onFailure.accept(error);
				}
			}
		});
	}

	private static List<Standing> toStandings(Competition competition)
	{
		return competition.participations.stream()
			.filter(p -> p.player != null && p.player.displayName != null && p.progress != null)
			.map(p -> new Standing(p.player.displayName, Math.round(p.progress.gained)))
			.sorted(Comparator.comparingLong(Standing::getGained).reversed())
			.collect(Collectors.toList());
	}

	private static class Cached
	{
		final List<Standing> standings;
		final Instant fetchedAt;

		Cached(List<Standing> standings, Instant fetchedAt)
		{
			this.standings = standings;
			this.fetchedAt = fetchedAt;
		}

		boolean isFresh()
		{
			return Instant.now().isBefore(fetchedAt.plus(CACHE_TIME));
		}
	}

	private static class Listener
	{
		final Consumer<List<Standing>> onSuccess;
		final Consumer<Exception> onFailure;

		Listener(Consumer<List<Standing>> onSuccess, Consumer<Exception> onFailure)
		{
			this.onSuccess = onSuccess;
			this.onFailure = onFailure;
		}
	}

	// The parts of the WOM response we read; everything else is ignored.

	private static class Competition
	{
		List<Participation> participations;
	}

	private static class Participation
	{
		Player player;
		Progress progress;
	}

	private static class Player
	{
		String displayName;
	}

	private static class Progress
	{
		double gained;
	}
}
