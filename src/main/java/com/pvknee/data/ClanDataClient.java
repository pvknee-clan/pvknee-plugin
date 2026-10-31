package com.pvknee.data;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Fetches the clan panel JSON. Callbacks run on an OkHttp thread, not the client or Swing thread.
 */
@Slf4j
@Singleton
public class ClanDataClient
{
	private static final String ENDPOINT = "https://script.google.com/macros/s/AKfycbyuOXHeAB-dlWG4kSg1G2T4mXPPy13vyphLPGRH3iZRUiEKKmvToJmHm6M33vb_OSUe3Q/exec";

	private final OkHttpClient okHttpClient;
	private final Gson gson;

	@Inject
	ClanDataClient(OkHttpClient okHttpClient, Gson gson)
	{
		this.okHttpClient = okHttpClient;
		this.gson = gson;
	}

	public void fetch(Consumer<ClanData> onSuccess, Consumer<Exception> onFailure)
	{
		Request request = new Request.Builder()
			.url(ENDPOINT)
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

					ClanData data = gson.fromJson(body.charStream(), ClanData.class);
					if (data == null)
					{
						throw new IOException("Empty response");
					}

					onSuccess.accept(data);
				}
				catch (IOException | JsonParseException e)
				{
					onFailure.accept(e);
				}
			}
		});
	}
}
