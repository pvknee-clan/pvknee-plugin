package com.pvknee.blocks;

import com.pvknee.data.ClanData;
import com.pvknee.ui.ThemeColours;
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

	/**
	 * The icon for a Wise Old Man metric, e.g. woodcutting or cerberus.
	 */
	public JLabel metricIcon(String metric, int size)
	{
		return icons.metric(metric, data.getIconBase(), size);
	}
}
