package com.pvknee.blocks;

import com.pvknee.data.Block;
import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

class MessageBlock implements BlockRenderer
{
	private static final int PADDING = 8;

	@Override
	public JComponent render(Block block, BlockContext context)
	{
		String text = block.setting("text", null);
		if (text == null)
		{
			return null;
		}

		ThemeColours colours = context.getColours();
		int width = Labels.CONTENT_WIDTH - 2 * PADDING;

		JPanel box = new JPanel(new BorderLayout(0, 4));
		box.setBackground(ColorScheme.DARKER_GRAY_HOVER_COLOR);
		box.setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));

		String title = block.setting("title", null);
		if (title != null)
		{
			box.add(Labels.label(title, colours.getAccent(), FontManager.getRunescapeBoldFont(), width, true), BorderLayout.NORTH);
		}
		box.add(Labels.label(text, colours.getValue(), FontManager.getRunescapeFont(), width, true), BorderLayout.CENTER);
		return box;
	}
}
