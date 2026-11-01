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
 * Fetches the clan panel JSON and keeps the last good copy on disk.
 * Callbacks run on a background thread, not the client or Swing thread.
 */
@Slf4j
@Singleton
public class ClanDataClient
{
	/** How old the last data may be before {@link #recent} loads the endpoint again; the panel's refresh interval. */
	private static final Duration FRESH = Duration.ofMinutes(10);
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
	/** Callers of {@link #recent} waiting for the load it started; null while none is running. */
	private List<Consumer<ClanData>> waiting;

	@Inject
	ClanDataClient(OkHttpClient okHttpClient, Gson gson, PvKneeConfig config, SavedCopy savedCopy, ScheduledExecutorService executor, DevLog devLog)
	{
		this.okHttpClient = okHttpClient;
		this.gson = gson;
		this.config = config;
		this.savedCopy = savedCopy;
		this.executor = executor;
		this.devLog = devLog;
	}

	/**
	 * Loads the endpoint. A good response from the real endpoint (not an override) is also saved to disk.
	 */
	public void fetch(Consumer<ClanData> onSuccess, Consumer<Exception> onFailure)
	{
		String override = config.endpointOverride().trim();
		HttpUrl url = HttpUrl.parse(override.isEmpty() ? ENDPOINT : override);
		if (url == null)
		{
			onFailure.accept(new IOException("Invalid endpoint override: " + override));
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
				onFailure.accept(e);
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
					remember(data, Instant.now());
					onSuccess.accept(data);
				}
				catch (IOException | JsonParseException e)
				{
					onFailure.accept(e);
				}
			}
		});
	}

	/**
	 * Loads the saved copy, with the time it was saved. Not called if there is none or it cannot be read.
	 */
	public void loadSaved(BiConsumer<ClanData, Instant> onLoaded)
	{
		executor.execute(() ->
		{
			SavedCopy.Saved saved = savedCopy.load();
			if (saved == null)
			{
				return;
			}

			try
			{
				ClanData data = parse(saved.getJson());
				if (latest == null)
				{
					remember(data, saved.getSavedAt());
				}
				onLoaded.accept(data, saved.getSavedAt());
			}
			catch (IOException | JsonParseException e)
			{
				log.debug("Saved clan data is unreadable", e);
			}
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
	 * minutes, otherwise one endpoint load, falling back to the last data or the saved copy. Not called if there is
	 * none at all. Callers asking while that load runs share it. The callback may run on any thread.
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

		devLog.log("{}: loading the endpoint", reason);
		fetch(this::finishRecent, e ->
		{
			devLog.log("{}: load failed, {}", reason, e.getMessage(), e);
			ClanData fallback = latest;
			if (fallback != null)
			{
				finishRecent(fallback);
			}
			else
			{
				loadSaved((saved, savedAt) -> finishRecent(saved));
			}
		});
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
