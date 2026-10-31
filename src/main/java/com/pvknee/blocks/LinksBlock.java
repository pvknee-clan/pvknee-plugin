package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.data.Link;
import com.pvknee.ui.Card;
import com.pvknee.ui.IconCaption;
import com.pvknee.ui.Labels;
import com.pvknee.ui.Section;
import com.pvknee.ui.SectionHeader;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;

/**
 * Links as a two-column grid of compact buttons, each with its icon above the label.
 */
class LinksBlock implements BlockRenderer
{
	private static final int COLUMNS = 2;
	private static final int ICON_SIZE = 18;
	private static final int LABEL_WIDTH = (Labels.CONTENT_WIDTH - Spacing.TITLE_GAP * (COLUMNS - 1)) / COLUMNS - 2 * Spacing.PADDING;

	@Override
	public JComponent render(Block block, BlockContext context)
	{
		List<Link> links = context.getData().getLinks();
		if (links == null || links.isEmpty())
		{
			return null;
		}

		JPanel grid = new JPanel(new GridLayout(0, COLUMNS, Spacing.TITLE_GAP, Spacing.TITLE_GAP));
		grid.setOpaque(false);
		for (Link link : links)
		{
			if (link.getUrl() != null && !link.getUrl().isEmpty())
			{
				grid.add(linkButton(link, context));
			}
		}
		if (grid.getComponentCount() == 0)
		{
			return null;
		}

		String title = block.setting("title", null);
		return new Section(title == null ? null : new SectionHeader(title, context.getColours()), grid);
	}

	private static Card linkButton(Link link, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		Card card = new Card(new BorderLayout());
		card.setToolTipText(link.getUrl());
		card.onClick(() -> LinkBrowser.browse(link.getUrl()));

		JComponent icon = link.getIcon() == null || link.getIcon().isEmpty() ? null : context.getIcons().url(link.getIcon(), ICON_SIZE);
		String label = link.getLabel() == null ? link.getUrl() : link.getLabel();
		card.add(new IconCaption(null, icon, label, colours.getValue(), FontManager.getRunescapeFont(), LABEL_WIDTH, colours), BorderLayout.CENTER);
		return card;
	}
}
