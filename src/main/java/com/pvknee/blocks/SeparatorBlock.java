package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.ui.Labels;
import com.pvknee.ui.ThemeColours;
import javax.swing.JComponent;
import net.runelite.client.ui.FontManager;

class SeparatorBlock implements BlockRenderer
{
	@Override
	public JComponent render(Block block, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		return Labels.label(colours.getSeparator(), colours.getMuted(), FontManager.getRunescapeFont(), Labels.CONTENT_WIDTH, true);
	}
}
