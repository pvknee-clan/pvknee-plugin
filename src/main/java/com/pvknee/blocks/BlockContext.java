package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.data.ClanData;
import com.pvknee.ui.Card;
import com.pvknee.ui.ThemeColours;
import com.pvknee.wom.WomClient;
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
}
