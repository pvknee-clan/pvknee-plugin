package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.data.Link;
import com.pvknee.ui.Card;
import com.pvknee.ui.Section;
import com.pvknee.ui.SectionHeader;
import com.pvknee.ui.Spacing;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

		JPanel panel = new JPanel(new DynamicGridLayout(0, 1, 0, Spacing.ITEM_GAP));
		panel.setOpaque(false);

		for (Link link : links)
		{
			if (link.getUrl() != null && !link.getUrl().isEmpty())
			{
				panel.add(linkCard(link, context));
			}
		}

		String title = block.setting("title", null);
		return new Section(title == null ? null : new SectionHeader(title, context.getColours()), panel);
	}

	private static Card linkCard(Link link, BlockContext context)
	{
		Card card = new Card(new FlowLayout(FlowLayout.CENTER, Spacing.PADDING, 0));
		card.setToolTipText(link.getUrl());
		card.onClick(() -> LinkBrowser.browse(link.getUrl()));

		if (link.getIcon() != null && !link.getIcon().isEmpty())
		{
			card.add(context.getIcons().url(link.getIcon(), ICON_SIZE));
		}

		JLabel label = new JLabel(link.getLabel() == null ? link.getUrl() : link.getLabel());
		label.setFont(FontManager.getRunescapeFont());
		label.setForeground(context.getColours().getValue());
		card.add(label);
		return card;
	}
}
