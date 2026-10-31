package com.pvknee.data;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import javax.inject.Singleton;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.Filepath;

/**
 * The last good endpoint response, kept on disk. Blocking; never call on the client or Swing thread.
 */
@Slf4j
@Singleton
public class SavedCopy
{
	private static final String FILE_NAME = "clan-data.json";

	private volatile Filepath directory;

	/**
	 * The plugin's data folder. Without one, nothing is saved or loaded.
	 */
	public void setDirectory(Filepath directory)
	{
		this.directory = directory;
	}

	void save(String json)
	{
		Filepath dir = directory;
		if (dir == null)
		{
			return;
		}

		try
		{
			dir.createDirectories();
			dir.joinSegment(FILE_NAME).write(json.getBytes(StandardCharsets.UTF_8));
		}
		catch (IOException e)
		{
			log.debug("Failed to save clan data", e);
		}
	}

	Saved load()
	{
		Filepath dir = directory;
		Filepath file = dir == null ? null : dir.joinSegment(FILE_NAME);
		if (file == null || !file.exists())
		{
			return null;
		}

		try (InputStream in = file.openInputStream())
		{
			String json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			return new Saved(json, file.getLastModifiedTime().toInstant());
		}
		catch (IOException e)
		{
			log.debug("Failed to read saved clan data", e);
			return null;
		}
	}

	@Value
	static class Saved
	{
		String json;
		Instant savedAt;
	}
}
