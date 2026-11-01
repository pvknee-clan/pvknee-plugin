package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.ui.Card;
import com.pvknee.ui.Fonts;
import com.pvknee.ui.IconCaption;
import com.pvknee.ui.Labels;
import com.pvknee.ui.Section;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import java.awt.BorderLayout;
import javax.swing.JComponent;

/**
 * One big value with a muted label above it, centred in a card, e.g. "Members" over "72". The value may use sheet
 * placeholders, which the endpoint fills in.
 */
class StatBlock implements BlockRenderer
{
	private static final float VALUE_SIZE = 24f;
	private static final int INNER_WIDTH = Labels.CONTENT_WIDTH - 2 * Spacing.PADDING;

	@Override
	public JComponent render(Block block, BlockContext context)
	{
		String value = block.setting("value", null);
		if (value == null)
		{
			return null;
		}

		ThemeColours colours = context.getColours();
		Card card = new Card(new BorderLayout());
		card.add(new IconCaption(
			block.setting("label", null),
			null,
			value,
			colours.getValue(),
			Fonts.boldName().deriveFont(VALUE_SIZE),
			INNER_WIDTH,
			colours), BorderLayout.CENTER);
		return Section.titled(block.setting("title", null), card, colours);
	}
}
