package com.pvknee.ui;

import java.awt.Font;
import net.runelite.client.ui.FontManager;

/**
 * Fonts with a fixed role across the panel.
 */
public final class Fonts
{
	private Fonts()
	{
	}

	/**
	 * Names of players, skills, bosses and events. The regular RuneScape font, because the bold one draws "W" like
	 * "VV".
	 */
	public static Font name()
	{
		return FontManager.getRunescapeFont();
	}

	/**
	 * Names that stand out, e.g. the competition on the weekly pages: the regular font bolded by Java, so "W" draws
	 * correctly.
	 */
	public static Font boldName()
	{
		return name().deriveFont(Font.BOLD);
	}

	/**
	 * Body text, e.g. an announcement or a text block: the same font and size as names.
	 */
	public static Font body()
	{
		return name();
	}

	/**
	 * Small captions that need to stand out, e.g. a time that is ending soon.
	 */
	public static Font smallBold()
	{
		return FontManager.getRunescapeSmallFont().deriveFont(Font.BOLD);
	}
}
