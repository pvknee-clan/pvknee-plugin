package com.pvknee.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.pvknee.DevLog;
import com.pvknee.PvKneeConfig;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Fetches the clan panel JSON and keeps the last good copy on disk. Decides when loading again is worth it: not within
 * a minute of a good load (the endpoint's own cache time), and after failures, automatic tries wait longer each time.
 * Callbacks run on a background thread, not the client or Swing thread.
 */
@Slf4j
@Singleton
public class ClanDataClient
{
	/** How old the last data may be before {@link #recent} loads the endpoint again; the panel's refresh interval. */
	private static final Duration FRESH = Duration.ofMinutes(10);
	/** The endpoint keeps its response this long, so loading again sooner gets the same data. */
	private static final Duration MIN_GAP = Duration.ofSeconds(60);
	/** How long automatic tries wait after the first, second and any later failure in a row. */
	/** Building the response from the sheet can take longer than OkHttp's default 10 seconds when it isn't cached. */
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
	private static final Duration[] BACK_OFF = {Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15)};
	private static final String ENDPOINT = "https://script.google.com/macros/s/AKfycbyuOXHeAB-dlWG4kSg1G2T4mXPPy13vyphLPGRH3iZRUiEKKmvToJmHm6M33vb_OSUe3Q/exec";

	private final OkHttpClient okHttpClient;
	private final Gson gson;
	private final PvKneeConfig config;
	private final SavedCopy savedCopy;
	private final ScheduledExecutorService executor;
	private final DevLog devLog;

	/** The last data loaded from the endpoint or the saved copy, and when it was fetched. */
	private volatile ClanData latest;
	private volatile Instant latestAt;
	/** When the endpoint last loaded successfully; null if it hasn't this session. */
	private volatile Instant loadedAt;
	/** Failed endpoint loads in a row, and when automatic tries may load again after them. */
	private int failures;
	private Instant retryAt;
	/** Callers of {@link #recent} waiting for the load it started; null while none is running. */
	private List<Consumer<ClanData>> waiting;

	@Inject
	ClanDataClient(OkHttpClient okHttpClient, Gson gson, PvKneeConfig config, SavedCopy savedCopy, ScheduledExecutorService executor, DevLog devLog)
	{
		this.okHttpClient = okHttpClient.newBuilder()
			.readTimeout(READ_TIMEOUT)
			.build();
		this.gson = gson;
		this.config = config;
		this.savedCopy = savedCopy;
		this.executor = executor;
		this.devLog = devLog;
	}

	/**
	 * Whether loading the endpoint now is worth it: not within a minute of a good load, and for automatic tries (not
	 * the Refresh button) not while waiting after failures.
	 */
	public synchronized boolean mayLoad(boolean manual)
	{
		Instant now = Instant.now();
		if (loadedAt != null && now.isBefore(loadedAt.plus(MIN_GAP)))
		{
			return false;
		}
		return manual || retryAt == null || !now.isBefore(retryAt);
	}

	/**
	 * When the endpoint last loaded successfully; null if it hasn't this session.
	 */
	public Instant loadedAt()
	{
		return loadedAt;
	}

	/**
	 * Loads the endpoint. A good response from the real endpoint (not an override) is also saved to disk.
	 */
	public void fetch(Consumer<ClanData> onSuccess, Consumer<Exception> onFailure)
	{
		Consumer<Exception> failed = e ->
		{
			countFailure();
			onFailure.accept(e);
		};
		String override = config.endpointOverride().trim();
		HttpUrl url = HttpUrl.parse(override.isEmpty() ? ENDPOINT : override);
		if (url == null)
		{
			failed.accept(new IOException("Invalid endpoint override: " + override));
			return;
		}
		if (!override.isEmpty())
		{
			log.debug("Using endpoint override {}", url);
		}

		Request request = new Request.Builder()
			.url(url)
			.build();

		okHttpClient.newCall(request).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				failed.accept(e);
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

					String json = body.string();
					ClanData data = parse(json);
					if (override.isEmpty())
					{
						savedCopy.save(json);
					}
					Instant now = Instant.now();
					countSuccess(now);
					remember(data, now);
					onSuccess.accept(data);
				}
				catch (IOException | JsonParseException e)
				{
					failed.accept(e);
				}
			}
		});
	}

	/**
	 * Loads the saved copy, with the time it was saved; both null if there is none or it cannot be read.
	 */
	public void loadSaved(BiConsumer<ClanData, Instant> onLoaded)
	{
		executor.execute(() ->
		{
			SavedCopy.Saved saved = savedCopy.load();
			ClanData data = null;
			if (saved != null)
			{
				try
				{
					data = parse(saved.getJson());
					if (latest == null)
					{
						remember(data, saved.getSavedAt());
					}
				}
				catch (IOException | JsonParseException e)
				{
					log.debug("Saved clan data is unreadable", e);
				}
			}
			onLoaded.accept(data, data == null ? null : saved.getSavedAt());
		});
	}

	/**
	 * The last data loaded from the endpoint or the saved copy; null if nothing has been loaded yet.
	 */
	public ClanData latest()
	{
		return latest;
	}

	/**
	 * Recent data for features that work while the panel is closed: the last data if it was loaded in the last few
	 * minutes, otherwise one endpoint load (unless {@link #mayLoad} says it isn't worth it), falling back to the last
	 * data or the saved copy. Called with null if there is none at all. Callers asking while that load runs share it.
	 * The callback may run on any thread.
	 */
	public void recent(String reason, Consumer<ClanData> onData)
	{
		ClanData data = latest;
		Instant fetchedAt = latestAt;
		if (data != null && fetchedAt != null && Instant.now().isBefore(fetchedAt.plus(FRESH)))
		{
			onData.accept(data);
			return;
		}

		synchronized (this)
		{
			if (waiting != null)
			{
				waiting.add(onData);
				return;
			}
			waiting = new ArrayList<>();
			waiting.add(onData);
		}

		if (!mayLoad(false))
		{
			devLog.log("{}: not loading the endpoint yet, using the last data", reason);
			finishWithFallback();
			return;
		}

		devLog.log("{}: loading the endpoint", reason);
		fetch(this::finishRecent, e ->
		{
			devLog.log("{}: load failed, {}", reason, e.getMessage(), e);
			finishWithFallback();
		});
	}

	/**
	 * Answers the waiting callers with the last data, or the saved copy, or null if there is neither.
	 */
	private void finishWithFallback()
	{
		ClanData fallback = latest;
		if (fallback != null)
		{
			finishRecent(fallback);
		}
		else
		{
			loadSaved((saved, savedAt) -> finishRecent(saved));
		}
	}

	private void finishRecent(ClanData data)
	{
		List<Consumer<ClanData>> callers;
		synchronized (this)
		{
			callers = waiting;
			waiting = null;
		}
		callers.forEach(caller -> caller.accept(data));
	}

	private synchronized void countSuccess(Instant now)
	{
		loadedAt = now;
		failures = 0;
		retryAt = null;
	}

	private synchronized void countFailure()
	{
		Duration wait = BACK_OFF[Math.min(failures, BACK_OFF.length - 1)];
		failures++;
		retryAt = Instant.now().plus(wait);
		devLog.log("load: failure {} in a row, next automatic try after {} minutes", failures, wait.toMinutes());
	}

	private void remember(ClanData data, Instant fetchedAt)
	{
		latestAt = fetchedAt;
		latest = data;
	}

	private ClanData parse(String json) throws IOException
	{
		ClanData data = gson.fromJson(json, ClanData.class);
		if (data == null)
		{
			throw new IOException("Empty response");
		}
		if (data.getError() != null)
		{
			throw new IOException("Endpoint error: " + data.getError());
		}
		return data;
	}
}
