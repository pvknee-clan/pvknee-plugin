package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.ui.Fonts;
import com.pvknee.ui.Labels;
import com.pvknee.ui.Pill;
import com.pvknee.ui.ThemeColours;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.runelite.client.ui.FontManager;

class TextBlock implements BlockRenderer
{
	@Override
	public JComponent render(Block block, BlockContext context)
	{
		String text = block.setting("text", null);
		if (text == null)
		{
			return null;
		}

		String style = block.setting("style", "").toLowerCase();
		boolean signoff = style.contains("signoff");
		boolean muted = signoff || style.contains("muted");
		boolean italic = signoff || style.contains("italic");
		boolean centred = signoff || !"left".equalsIgnoreCase(block.setting("align", "center"));

		Font font = signoff ? FontManager.getRunescapeSmallFont()
			: style.contains("bold") ? FontManager.getRunescapeBoldFont()
			: Fonts.body();
		if (italic)
		{
			font = font.deriveFont(font.getStyle() | Font.ITALIC);
		}

		ThemeColours colours = context.getColours();
		Color colour = muted ? colours.getMuted() : colours.getValue();
		if (style.contains("pill"))
		{
			JPanel row = new JPanel(new FlowLayout(centred ? FlowLayout.CENTER : FlowLayout.LEFT, 0, 0));
			row.setOpaque(false);
			row.add(new Pill(text, colour, context.mutedGlyph(block.setting("icon", null), Pill.ICON_SIZE)));
			return row;
		}
		return Labels.label(text, colour, font, Labels.CONTENT_WIDTH, centred);
	}
}
