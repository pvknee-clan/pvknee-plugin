package com.pvknee.ui;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.runelite.client.util.LinkBrowser;
import okhttp3.HttpUrl;

/**
 * The only web pages the panel opens: https pages on a few known sites (and their subdomains), so a changed link in
 * the clan sheet cannot send members to any other site.
 */
public final class WebLinks
{
	private static final List<String> SITES = Arrays.asList(
		"discord.gg",
		"discord.com",
		"wiseoldman.net",
		"oldschool.runescape.wiki",
		"runescape.com");

	private WebLinks()
	{
	}

	/**
	 * Whether the panel may open this address: https on one of the known sites.
	 */
	public static boolean isAllowed(String url)
	{
		HttpUrl httpUrl = url == null ? null : HttpUrl.parse(url.trim());
		if (httpUrl == null || !httpUrl.isHttps())
		{
			return false;
		}
		String host = httpUrl.host().toLowerCase(Locale.ENGLISH);
		return SITES.stream().anyMatch(site -> host.equals(site) || host.endsWith("." + site));
	}

	/**
	 * Opens the page in the browser if it is allowed; otherwise does nothing.
	 */
	public static void open(String url)
	{
		if (isAllowed(url))
		{
			LinkBrowser.browse(url.trim());
		}
	}
}
