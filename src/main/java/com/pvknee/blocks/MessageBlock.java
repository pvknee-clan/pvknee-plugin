package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.ui.Card;
import com.pvknee.ui.Labels;
import com.pvknee.ui.Section;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import java.awt.BorderLayout;
import javax.swing.JComponent;
import net.runelite.client.ui.FontManager;

class MessageBlock implements BlockRenderer
{
	@Override
	public JComponent render(Block block, BlockContext context)
	{
		String text = block.setting("text", null);
		if (text == null)
		{
			return null;
		}

		ThemeColours colours = context.getColours();
		int width = Labels.CONTENT_WIDTH - 2 * Spacing.PADDING;

		Card card = new Card(new BorderLayout());
		card.add(Labels.label(text, colours.getValue(), FontManager.getRunescapeFont(), width, false), BorderLayout.CENTER);
		return Section.titled(block.setting("title", null), card, colours);
	}
}
