package com.pvknee.ui;

import java.awt.FlowLayout;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.runelite.client.ui.FontManager;

/**
 * The top of a page: a centred title in the header colour, an optional muted subtitle (e.g. a competition's dates)
 * and an optional pill (e.g. "World 509"), grouped tightly as one unit.
 */
public class PageHeader extends JPanel
{
	private static final int SUBTITLE_GAP = 3;
	private static final int PILL_GAP = 5;

	public PageHeader(String title, float size, String subtitle, Pill pill, ThemeColours colours)
	{
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setOpaque(false);

		addCentred(SectionHeader.pageTitle(title, colours.getHeader(), size));
		if (subtitle != null)
		{
			add(Box.createVerticalStrut(SUBTITLE_GAP));
			addCentred(Labels.label(subtitle, colours.getMuted(), FontManager.getRunescapeSmallFont(), Labels.CONTENT_WIDTH, true));
		}
		if (pill != null)
		{
			JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
			row.setOpaque(false);
			row.add(pill);
			add(Box.createVerticalStrut(PILL_GAP));
			addCentred(row);
		}
	}

	private void addCentred(JComponent component)
	{
		component.setAlignmentX(CENTER_ALIGNMENT);
		add(component);
	}
}
