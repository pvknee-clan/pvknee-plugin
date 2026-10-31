package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.data.Link;
import com.pvknee.ui.Card;
import com.pvknee.ui.GlyphIcon;
import com.pvknee.ui.Glyphs;
import com.pvknee.ui.IconCaption;
import com.pvknee.ui.Labels;
import com.pvknee.ui.Section;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import java.awt.BorderLayout;
import java.awt.Shape;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.FontManager;
import net.runelite.client.util.LinkBrowser;

/**
 * Links as a two-column grid of compact buttons, each with its icon above the label. The icon is a {@link Glyphs}
 * name, optionally with a colour, e.g. "chat:#8C9EFF".
 */
@Slf4j
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

		return Section.titled(block.setting("title", null), grid, context.getColours());
	}

	private static Card linkButton(Link link, BlockContext context)
	{
		ThemeColours colours = context.getColours();
		Card card = new Card(new BorderLayout());
		card.setToolTipText(link.getUrl());
		card.onClick(() -> LinkBrowser.browse(link.getUrl()));

		String label = link.getLabel() == null ? link.getUrl() : link.getLabel();
		card.add(new IconCaption(null, icon(link.getIcon(), colours), label, colours.getValue(), FontManager.getRunescapeFont(), LABEL_WIDTH, colours), BorderLayout.CENTER);
		return card;
	}

	/**
	 * The glyph for a link's icon setting, e.g. "chat" or "chat:#8C9EFF". Null if there is none or it is unknown.
	 */
	private static JComponent icon(String setting, ThemeColours colours)
	{
		if (setting == null || setting.trim().isEmpty())
		{
			return null;
		}

		String[] parts = setting.split(":", 2);
		Shape glyph = Glyphs.named(parts[0]);
		if (glyph == null)
		{
			log.debug("Unknown link icon {}", setting);
			return null;
		}
		return new JLabel(new GlyphIcon(glyph, colours.resolve(parts.length > 1 ? parts[1] : null, colours.getValue()), ICON_SIZE));
	}
}
