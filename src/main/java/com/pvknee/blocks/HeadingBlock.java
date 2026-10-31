package com.pvknee.blocks;

import com.pvknee.data.Block;
import java.awt.Font;
import javax.swing.JComponent;
import net.runelite.client.ui.FontManager;

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

		int size = Math.max(1, Math.min(3, block.intSetting("size", 2)));
		Font font = FontManager.getRunescapeBoldFont().deriveFont(SIZES[size - 1]);
		ThemeColours colours = context.getColours();
		return Labels.label(text, colours.resolve(block.setting("colour", null), colours.getAccent()), font, Labels.CONTENT_WIDTH, true);
	}
}
