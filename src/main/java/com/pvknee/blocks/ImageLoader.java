package com.pvknee.blocks;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.util.ImageUtil;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

/**
 * Loads images from URLs once and keeps them in memory. Labels fill in on the Swing thread when the image arrives.
 * Only https addresses on raw.githubusercontent.com are loaded, at most 256 KB and 512 px a side; anything else is
 * skipped and its space stays empty. Addresses that failed are not tried again this session.
 */
@Slf4j
@Singleton
public class ImageLoader
{
	private static final String ALLOWED_HOST = "raw.githubusercontent.com";
	private static final int MAX_BYTES = 256 * 1024;
	private static final int MAX_SIDE = 512;
	/** At most this many images and scaled icons are kept; the least recently used go first. */
	private static final int MAX_CACHED = 200;

	private final OkHttpClient okHttpClient;
	/** Addresses that were refused or failed this session, so a page rebuild does not fetch them again. */
	private final Set<String> failed = new HashSet<>();
	private final Map<String, BufferedImage> images = bounded();
	private final Map<String, ImageIcon> icons = bounded();
	private final Map<String, List<Consumer<BufferedImage>>> pending = new HashMap<>();

	@Inject
	ImageLoader(OkHttpClient okHttpClient)
	{
		this.okHttpClient = okHttpClient;
	}

	/**
	 * A label of the given size that shows the image once it has loaded, and stays empty if it fails.
	 * Call on the Swing thread.
	 */
	public JLabel icon(String url, int size)
	{
		JLabel label = emptyLabel(size);
		load(url, image -> label.setIcon(icon(url, image, size)));
		return label;
	}

	/**
	 * Forgets every image, e.g. when the plugin is turned off. Call on the Swing thread.
	 */
	public void clear()
	{
		images.clear();
		icons.clear();
		failed.clear();
		pending.clear();
	}

	/**
	 * A map that drops its least recently used entry once it holds more than {@link #MAX_CACHED}.
	 */
	static <V> Map<String, V> bounded()
	{
		return new LinkedHashMap<>(16, 0.75f, true)
		{
			@Override
			protected boolean removeEldestEntry(Map.Entry<String, V> eldest)
			{
				return size() > MAX_CACHED;
			}
		};
	}

	/**
	 * Whether an image may be loaded from this address: https on raw.githubusercontent.com, matching the host exactly.
	 */
	static boolean isAllowed(String url)
	{
		HttpUrl httpUrl = url == null ? null : HttpUrl.parse(url);
		return httpUrl != null && httpUrl.isHttps() && ALLOWED_HOST.equals(httpUrl.host());
	}

	/**
	 * An empty label that keeps the size of the square icon it will show.
	 */
	static JLabel emptyLabel(int size)
	{
		JLabel label = new JLabel();
		Dimension dimension = new Dimension(size, size);
		label.setPreferredSize(dimension);
		label.setMinimumSize(dimension);
		label.setMaximumSize(dimension);
		return label;
	}

	private ImageIcon icon(String url, BufferedImage image, int size)
	{
		return icons.computeIfAbsent(url + "@" + size, k -> scaled(image, size));
	}

	/**
	 * The image cut down to its visible part, e.g. an item icon without the empty space around the item. A fully
	 * transparent image is left as it is.
	 */
	static BufferedImage trimmed(BufferedImage image)
	{
		int minX = image.getWidth();
		int minY = image.getHeight();
		int maxX = -1;
		int maxY = -1;
		for (int y = 0; y < image.getHeight(); y++)
		{
			for (int x = 0; x < image.getWidth(); x++)
			{
				if ((image.getRGB(x, y) >>> 24) != 0)
				{
					minX = Math.min(minX, x);
					minY = Math.min(minY, y);
					maxX = Math.max(maxX, x);
					maxY = Math.max(maxY, y);
				}
			}
		}
		return maxX < 0 ? image : image.getSubimage(minX, minY, maxX - minX + 1, maxY - minY + 1);
	}

	/**
	 * The image fitted inside a square of the given size, keeping its proportions.
	 */
	static ImageIcon scaled(BufferedImage image, int size)
	{
		return new ImageIcon(ImageUtil.resizeCanvas(ImageUtil.resizeImage(image, size, size, true), size, size));
	}

	private void load(String url, Consumer<BufferedImage> onLoad)
	{
		BufferedImage cached = images.get(url);
		if (cached != null)
		{
			onLoad.accept(cached);
			return;
		}

		List<Consumer<BufferedImage>> waiting = pending.get(url);
		if (waiting != null)
		{
			waiting.add(onLoad);
			return;
		}

		if (url == null || failed.contains(url))
		{
			return;
		}
		if (!isAllowed(url))
		{
			log.debug("Image skipped, not an allowed address: {}", url);
			failed.add(url);
			return;
		}
		HttpUrl httpUrl = HttpUrl.get(url);

		waiting = new ArrayList<>();
		waiting.add(onLoad);
		pending.put(url, waiting);

		okHttpClient.newCall(new Request.Builder().url(httpUrl).build()).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Failed to load image {}", url, e);
				SwingUtilities.invokeLater(() ->
				{
					pending.remove(url);
					failed.add(url);
				});
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				BufferedImage image = null;
				try (ResponseBody body = response.body())
				{
					if (response.isSuccessful() && body != null)
					{
						image = read(body, url);
					}
				}
				catch (IOException e)
				{
					log.debug("Failed to read image {}", url, e);
				}

				if (image == null)
				{
					log.debug("No image at {} ({})", url, response.code());
				}

				BufferedImage loaded = image;
				SwingUtilities.invokeLater(() ->
				{
					List<Consumer<BufferedImage>> callbacks = pending.remove(url);
					if (callbacks == null)
					{
						// Cleared while loading.
						return;
					}
					if (loaded != null)
					{
						images.put(url, loaded);
						callbacks.forEach(callback -> callback.accept(loaded));
					}
					else
					{
						failed.add(url);
					}
				});
			}
		});
	}

	/**
	 * Decodes the image if it is at most {@link #MAX_BYTES} and {@link #MAX_SIDE} px a side, reading its size before
	 * decoding it; null otherwise.
	 */
	private static BufferedImage read(ResponseBody body, String url) throws IOException
	{
		BufferedSource source = body.source();
		if (body.contentLength() > MAX_BYTES || source.request(MAX_BYTES + 1L))
		{
			log.debug("Image skipped, over {} bytes: {}", MAX_BYTES, url);
			return null;
		}
		byte[] bytes = source.buffer().readByteArray();

		synchronized (ImageIO.class)
		{
			// Kept in memory: ImageIO's own streams may cache to a file on disk.
			try (ImageInputStream input = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes)))
			{
				Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
				if (!readers.hasNext())
				{
					return null;
				}

				ImageReader reader = readers.next();
				try
				{
					reader.setInput(input, true, true);
					int width = reader.getWidth(0);
					int height = reader.getHeight(0);
					if (width > MAX_SIDE || height > MAX_SIDE)
					{
						log.debug("Image skipped, {}x{} is over {} px a side: {}", width, height, MAX_SIDE, url);
						return null;
					}
					return reader.read(0);
				}
				finally
				{
					reader.dispose();
				}
			}
		}
	}
}
