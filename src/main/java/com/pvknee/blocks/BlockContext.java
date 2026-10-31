package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.data.ClanData;
import com.pvknee.ui.Card;
import com.pvknee.ui.LiveLabel;
import com.pvknee.ui.ThemeColours;
import com.pvknee.wom.WomClient;
import java.awt.Color;
import java.awt.Font;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.JLabel;
import lombok.Value;

/**
 * Shared state every block renderer can read.
 */
@Value
public class BlockContext
{
	ClanData data;
	ThemeColours colours;
	Icons icons;
	PageNavigator pages;
	WomClient wom;
	/** Run by the panel every minute while the page is shown. */
	List<Runnable> liveUpdates;

	/**
	 * The icon for a Wise Old Man metric, e.g. woodcutting or cerberus.
	 */
	public JLabel metricIcon(String metric, int size)
	{
		return icons.metric(metric, data.getIconBase(), size);
	}

	/**
	 * Makes the card open the page the block sets for this type (skill_page or boss_page), if it can be opened.
	 */
	public void linkToPage(Card card, Block block, String type)
	{
		String pageId = type == null ? null : block.setting(type.toLowerCase() + "_page", null);
		if (pageId != null && pages.canOpen(pageId))
		{
			card.onClick(() -> pages.open(pageId));
		}
	}

	/**
	 * A label whose text is asked for again every minute, for countdowns.
	 */
	public LiveLabel liveLabel(Supplier<String> text, Color colour, Font font, int width, boolean centred)
	{
		LiveLabel label = new LiveLabel(text, colour, font, width, centred);
		liveUpdates.add(label::update);
		return label;
	}
}
