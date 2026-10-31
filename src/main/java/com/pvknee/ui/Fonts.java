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
}
