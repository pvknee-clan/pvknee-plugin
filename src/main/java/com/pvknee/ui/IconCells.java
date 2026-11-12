package com.pvknee.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.function.Consumer;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * A row of square cells, a fixed number per row and wrapping, each holding an icon with its name on hover, and an
 * optional coloured outline, e.g. the items that count towards a bingo tile with the ones logged outlined.
 */
public class IconCells extends JPanel
{
	private static final int GAP = 4;
	private static final int ARC = 8;
	private static final float OUTLINE = 2f;
	private static final double ICON_SHARE = 0.75;
	private static final Color BACKGROUND = new Color(42, 42, 42);

	private final int cellSize;

	public IconCells(int columns, int width)
	{
		super(new GridLayout(0, columns, GAP, GAP));
		setOpaque(false);
		cellSize = (width - (columns - 1) * GAP) / columns;
	}

	/**
	 * The size to load icons at so they fit inside a cell.
	 */
	public int iconSize()
	{
		return (int) (cellSize * ICON_SHARE);
	}

	/**
	 * Adds a cell with a plain-text tooltip and an outline (null for none). Returns where to hand its icon once loaded;
	 * a cell whose icon never arrives stays empty.
	 */
	public Consumer<ImageIcon> addCell(String tooltip, Color outline)
	{
		Cell cell = new Cell(outline);
		Labels.tooltip(cell, tooltip);
		add(cell);
		return cell::setIcon;
	}

	private class Cell extends JComponent
	{
		private final Color outline;
		private ImageIcon icon;

		Cell(Color outline)
		{
			this.outline = outline;
			setPreferredSize(new Dimension(cellSize, cellSize));
		}

		void setIcon(ImageIcon icon)
		{
			this.icon = icon;
			repaint();
		}

		@Override
		protected void paintComponent(Graphics g)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			int size = Math.min(getWidth(), getHeight());
			int x = (getWidth() - size) / 2;
			int y = (getHeight() - size) / 2;
			g2.setColor(BACKGROUND);
			g2.fillRoundRect(x, y, size, size, ARC, ARC);
			if (icon != null)
			{
				icon.paintIcon(this, g2, x + (size - icon.getIconWidth()) / 2, y + (size - icon.getIconHeight()) / 2);
			}
			if (outline != null)
			{
				g2.setColor(outline);
				g2.setStroke(new BasicStroke(OUTLINE));
				g2.drawRoundRect(x + 1, y + 1, size - 2, size - 2, ARC, ARC);
			}
			g2.dispose();
		}
	}
}
