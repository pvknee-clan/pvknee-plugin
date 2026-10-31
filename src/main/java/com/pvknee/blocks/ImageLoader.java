package com.pvknee.blocks;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.imageio.ImageIO;
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

/**
 * Loads images from URLs once and keeps them in memory. Labels fill in on the Swing thread when the image arrives.
 */
@Slf4j
@Singleton
public class ImageLoader
{
	private final OkHttpClient okHttpClient;
	private final Map<String, BufferedImage> images = new HashMap<>();
	private final Map<String, ImageIcon> icons = new HashMap<>();
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
		JLabel label = new JLabel();
		Dimension dimension = new Dimension(size, size);
		label.setPreferredSize(dimension);
		label.setMinimumSize(dimension);
		label.setMaximumSize(dimension);

		load(url, size, label::setIcon);
		return label;
	}

	/**
	 * Calls back on the Swing thread with the image at the given size, once it has loaded. Never called if it fails.
	 * Call on the Swing thread.
	 */
	public void load(String url, int size, Consumer<ImageIcon> onLoad)
	{
		load(url, image -> onLoad.accept(icon(url, image, size)));
	}

	/**
	 * An empty icon to hold the space while an image loads.
	 */
	public static ImageIcon placeholder(int size)
	{
		return new ImageIcon(new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB));
	}

	private ImageIcon icon(String url, BufferedImage image, int size)
	{
		return icons.computeIfAbsent(url + "@" + size, k ->
			new ImageIcon(ImageUtil.resizeCanvas(ImageUtil.resizeImage(image, size, size, true), size, size)));
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

		HttpUrl httpUrl = url == null ? null : HttpUrl.parse(url);
		if (httpUrl == null)
		{
			log.debug("Invalid image URL {}", url);
			return;
		}

		waiting = new ArrayList<>();
		waiting.add(onLoad);
		pending.put(url, waiting);

		okHttpClient.newCall(new Request.Builder().url(httpUrl).build()).enqueue(new Callback()
		{
			@Override
			public void onFailure(Call call, IOException e)
			{
				log.debug("Failed to load image {}", url, e);
				SwingUtilities.invokeLater(() -> pending.remove(url));
			}

			@Override
			public void onResponse(Call call, Response response)
			{
				BufferedImage image = null;
				try (ResponseBody body = response.body())
				{
					if (response.isSuccessful() && body != null)
					{
						synchronized (ImageIO.class)
						{
							image = ImageIO.read(body.byteStream());
						}
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
					if (loaded != null)
					{
						images.put(url, loaded);
						callbacks.forEach(callback -> callback.accept(loaded));
					}
				});
			}
		});
	}
}
