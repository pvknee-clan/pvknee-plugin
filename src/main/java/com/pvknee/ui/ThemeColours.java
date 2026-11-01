package com.pvknee.ui;

import com.pvknee.data.Theme;
import java.awt.Color;
import lombok.Value;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.util.ColorUtil;

/**
 * The theme's colours resolved once, with fallbacks for missing or invalid values.
 */
@Value
public class ThemeColours
{
	/** Times in an item's final 24 hours. Fixed, like the medal colours, so it reads the same in every theme. */
	public static final Color ENDING_SOON = new Color(0xFF9800);
	/** Plain text that isn't a value, e.g. names in a list. The same in every theme. */
	public static final Color TEXT = new Color(221, 221, 221);
	/** Bingo tiles that are done, and a team's tiles done. Fixed, like {@link #ENDING_SOON}. */
	public static final Color DONE = new Color(0x4CAF50);
	/** Bingo tiles in progress: the same orange as {@link #ENDING_SOON}. */
	public static final Color IN_PROGRESS = ENDING_SOON;

	Color header;
	Color accent;
	Color value;
	Color muted;

	public static ThemeColours from(Theme theme)
	{
		if (theme == null)
		{
			theme = new Theme();
		}

		return new ThemeColours(
			parse(theme.getHeader(), ColorScheme.BRAND_ORANGE),
			parse(theme.getAccent(), ColorScheme.BRAND_ORANGE),
			parse(theme.getValue(), Color.WHITE),
			parse(theme.getMuted(), Color.GRAY));
	}

	/**
	 * Resolves a colour setting: header, accent, value, muted or a hex code.
	 */
	public Color resolve(String name, Color fallback)
	{
		if (name == null)
		{
			return fallback;
		}

		switch (name.trim().toLowerCase())
		{
			case "header":
				return header;
			case "accent":
				return accent;
			case "value":
				return value;
			case "muted":
				return muted;
			default:
				return parse(name.trim(), fallback);
		}
	}

	private static Color parse(String hex, Color fallback)
	{
		Color colour = hex == null ? null : ColorUtil.fromHex(hex);
		return colour == null ? fallback : colour;
	}
}
