package com.pvknee.ui;

import java.awt.Color;
import javax.swing.JLabel;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;

/**
 * A small card of status lines, e.g. a load error or when the data is from. Hidden while every line is empty.
 */
public class Notice extends Card
{
	private static final int TEXT_WIDTH = Labels.CONTENT_WIDTH - 2 * Spacing.PADDING;

	public Notice()
	{
		super(new DynamicGridLayout(0, 1, 0, Spacing.ITEM_GAP));
		setVisible(false);
	}

	/**
	 * Adds an empty line; lines keep the order they were added in.
	 */
	public Line addLine()
	{
		Line line = new Line();
		add(line);
		return line;
	}

	private void updateVisibility()
	{
		boolean any = false;
		for (int i = 0; i < getComponentCount(); i++)
		{
			any |= getComponent(i).isVisible();
		}
		setVisible(any);
		revalidate();
		repaint();
	}

	public class Line extends JLabel
	{
		private Line()
		{
			setFont(FontManager.getRunescapeSmallFont());
			setVisible(false);
		}

		public void setMessage(String text, Color colour)
		{
			setForeground(colour);
			Labels.setText(this, text, TEXT_WIDTH, false);
			setVisible(true);
			updateVisibility();
		}

		public void clear()
		{
			setVisible(false);
			updateVisibility();
		}
	}
}
