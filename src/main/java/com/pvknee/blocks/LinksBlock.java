package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.data.Link;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;

class LinksBlock implements BlockRenderer
{
	private static final int ICON_SIZE = 16;

	@Override
	public JComponent render(Block block, BlockContext context)
	{
		List<Link> links = context.getData().getLinks();
		if (links == null || links.isEmpty())
		{
			return null;
		}

		JPanel panel = new JPanel(new DynamicGridLayout(0, 1, 0, 4));
		panel.setBackground(ColorScheme.DARK_GRAY_COLOR);

		String title = block.setting("title", null);
		if (title != null)
		{
			panel.add(Labels.title(title, context.getColours()));
		}

		for (Link link : links)
		{
			if (link.getUrl() == null || link.getUrl().isEmpty())
			{
				continue;
			}

			JButton button = new JButton(link.getLabel() == null ? link.getUrl() : link.getLabel());
			button.setFont(FontManager.getRunescapeFont());
			button.setFocusPainted(false);
			button.setToolTipText(link.getUrl());
			button.addActionListener(e -> LinkBrowser.browse(link.getUrl()));

			if (link.getIcon() != null && !link.getIcon().isEmpty())
			{
				button.setIcon(ImageLoader.placeholder(ICON_SIZE));
				button.setIconTextGap(8);
				context.getImages().load(link.getIcon(), ICON_SIZE, button::setIcon);
			}

			panel.add(button);
		}
		return panel;
	}
}
