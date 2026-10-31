package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.ui.Card;
import com.pvknee.ui.Labels;
import com.pvknee.ui.SectionHeader;
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
		String title = block.setting("title", null);
		if (title != null)
		{
			card.add(new SectionHeader(title, colours), BorderLayout.NORTH);
		}
		card.add(Labels.label(text, colours.getValue(), FontManager.getRunescapeFont(), width, true), BorderLayout.CENTER);
		return card;
	}
}
