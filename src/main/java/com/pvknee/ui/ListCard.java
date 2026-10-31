package com.pvknee.ui;

import java.awt.Color;
import javax.swing.JComponent;
import net.runelite.client.ui.DynamicGridLayout;

/**
 * One card holding a list of entries, with a thin divider between each.
 */
public class ListCard extends Card
{
	private final Color divider;

	public ListCard(ThemeColours colours)
	{
		super(new DynamicGridLayout(0, 1, 0, Spacing.ITEM_GAP));
		divider = colours.getMuted();
	}

	public void addEntry(JComponent entry)
	{
		if (getComponentCount() > 0)
		{
			add(new Divider(divider));
		}
		add(entry);
	}
}
