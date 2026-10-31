package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.ui.SectionHeader;
import com.pvknee.ui.ThemeColours;
import javax.swing.JComponent;

class HeadingBlock implements BlockRenderer
{
	private static final float[] SIZES = {16f, 20f, 24f};

	@Override
	public JComponent render(Block block, BlockContext context)
	{
		String text = block.setting("text", null);
		if (text == null)
		{
			return null;
		}

		ThemeColours colours = context.getColours();
		return SectionHeader.pageTitle(text, colours.resolve(block.setting("colour", null), colours.getAccent()), size(block));
	}

	/**
	 * The font size for a block's "size" setting, 1 to 3.
	 */
	static float size(Block block)
	{
		return SIZES[Math.max(1, Math.min(3, block.intSetting("size", 2))) - 1];
	}
}
