package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.ui.GlyphIcon;
import com.pvknee.ui.Glyphs;
import com.pvknee.ui.Labels;
import com.pvknee.ui.Pill;
import com.pvknee.ui.ThemeColours;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Shape;
import javax.swing.JComponent;
import javax.swing.JPanel;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.FontManager;

@Slf4j
class TextBlock implements BlockRenderer
{
	private static final int PILL_ICON_SIZE = 11;

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
			: FontManager.getRunescapeFont();
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
			row.add(new Pill(text, colour, pillIcon(block.setting("icon", null), colours)));
			return row;
		}
		return Labels.label(text, colour, font, Labels.CONTENT_WIDTH, centred);
	}

	/**
	 * A muted glyph for a pill, e.g. "globe". Null if there is none or the name is unknown.
	 */
	private static GlyphIcon pillIcon(String name, ThemeColours colours)
	{
		if (name == null)
		{
			return null;
		}

		Shape glyph = Glyphs.named(name);
		if (glyph == null)
		{
			log.debug("Unknown text icon {}", name);
			return null;
		}
		return new GlyphIcon(glyph, colours.getMuted(), PILL_ICON_SIZE);
	}
}
