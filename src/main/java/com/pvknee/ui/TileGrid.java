package com.pvknee.ui;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.IntConsumer;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import lombok.Value;
import net.runelite.client.ui.FontManager;

/**
 * A square grid of clickable tiles, e.g. a bingo board, each with an icon and coloured by status: done tiles are
 * tinted green with a tick in the corner, tiles in progress are tinted orange with a progress line along the bottom,
 * and the rest are plain with the icon slightly dimmed. The selected tile has an outline. Places without a tile are
 * dashed outlines, e.g. for a board that isn't revealed yet.
 */
public class TileGrid extends JComponent
{
	private static final int GAP = 4;
	/** Room around the grid for the outline and ticks, which reach past the tiles' edges. */
	private static final int MARGIN = 3;
	private static final int ARC = 10;
	private static final int OUTLINE_ARC = 14;
	private static final int TICK_SIZE = 10;
	private static final double ICON_SHARE = 0.75;
	private static final Color PLAIN = new Color(42, 42, 42);
	private static final Color DONE_TINT = new Color(76, 175, 80, 56);
	private static final Color PROGRESS_TINT = new Color(255, 152, 0, 41);
	private static final Color LINE_TRACK = new Color(0, 0, 0, 90);
	private static final Color LEGEND_PLAIN = new Color(90, 90, 90);
	private static final Color EMPTY = new Color(74, 74, 74);
	private static final float[] DASH = {4, 3};
	private static final float DIMMED = 0.75f;

	public enum Status
	{
		DONE,
		PROGRESS,
		NOT_STARTED
	}

	@Value
	public static class Tile
	{
		Status status;
		/** How far a tile in progress is, between 0 and 1. */
		double progress;
		String tooltip;
	}

	private final int columns;
	/** The narrowest the grid is laid out; the tiles grow to fill any extra width, and the height follows. */
	private final int width;
	/** The width a grid was last laid out at, so a new one starts at the right height instead of jumping to it. */
	private static int lastWidth;
	/** One per place on the grid, left to right and top to bottom; null for an empty place. */
	private List<Tile> tiles;
	private final Image[] icons;
	private int selected;
	private final Color outline;

	/**
	 * @param selected the selected tile's index, or -1
	 * @param onClick told the index of a tile when it is clicked
	 */
	public TileGrid(int columns, List<Tile> tiles, int selected, Color outline, int width, IntConsumer onClick)
	{
		this.columns = Math.max(1, columns);
		this.width = width;
		this.tiles = tiles;
		this.icons = new Image[tiles.size()];
		this.selected = selected;
		this.outline = outline;
		// Registers with the tooltip manager; the text comes from the tile under the mouse.
		setToolTipText("");

		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				int index = tileAt(e.getX(), e.getY());
				if (SwingUtilities.isLeftMouseButton(e) && index >= 0)
				{
					onClick.accept(index);
				}
			}
		});
		addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent e)
			{
				lastWidth = getWidth();
				// The tiles follow the width, so a new width needs a new height.
				if (getHeight() != getPreferredSize().height)
				{
					revalidate();
				}
			}
		});
		addMouseMotionListener(new MouseAdapter()
		{
			@Override
			public void mouseMoved(MouseEvent e)
			{
				setCursor(Cursor.getPredefinedCursor(tileAt(e.getX(), e.getY()) >= 0 ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
			}
		});
	}

	@Override
	public Dimension getPreferredSize()
	{
		int rows = (tiles.size() + columns - 1) / columns;
		double size = tileSize(layoutWidth(), columns);
		return new Dimension(width, (int) Math.round(2 * MARGIN + rows * size + (rows - 1) * GAP));
	}

	/**
	 * The icon size that suits a grid of this narrowest width and number of columns.
	 */
	public static int iconSize(int width, int columns)
	{
		return (int) Math.round(tileSize(width, Math.max(1, columns)) * ICON_SHARE);
	}

	/**
	 * Changes the tiles' looks and the selection in place, e.g. for another team's board. The number of tiles stays
	 * the same, and so do their icons.
	 */
	public void setTiles(List<Tile> tiles, int selected)
	{
		this.tiles = tiles;
		this.selected = selected;
		repaint();
	}

	/**
	 * Shows a tile's icon, e.g. once it has loaded. It should be {@link #iconSize} big.
	 */
	public void setIcon(int index, Image icon)
	{
		icons[index] = icon;
		repaint();
	}

	/**
	 * A centred key to the tile colours: done, in progress and not started.
	 */
	public static JComponent legend(Color muted)
	{
		JPanel legend = new JPanel(new GridBagLayout());
		legend.setOpaque(false);
		GridBagConstraints constraints = new GridBagConstraints();
		legend.add(legendItem(ThemeColours.DONE, true, "Done", muted), constraints);
		constraints.insets = new Insets(0, 2 * GAP, 0, 0);
		legend.add(legendItem(ThemeColours.IN_PROGRESS, true, "In progress", muted), constraints);
		legend.add(legendItem(LEGEND_PLAIN, false, "Not started", muted), constraints);
		return legend;
	}

	@Override
	public String getToolTipText(MouseEvent e)
	{
		int index = tileAt(e.getX(), e.getY());
		return index < 0 ? null : tiles.get(index).getTooltip();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		for (int i = 0; i < tiles.size(); i++)
		{
			Tile tile = tiles.get(i);
			Rectangle r = bounds(i);
			if (tile == null)
			{
				Graphics2D dashed = (Graphics2D) g2.create();
				dashed.setColor(EMPTY);
				dashed.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10, DASH, 0));
				dashed.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, ARC, ARC);
				dashed.dispose();
				continue;
			}

			Status status = tile.getStatus();
			g2.setColor(status == Status.DONE ? DONE_TINT : status == Status.PROGRESS ? PROGRESS_TINT : PLAIN);
			g2.fillRoundRect(r.x, r.y, r.width, r.height, ARC, ARC);
			g2.setColor(status == Status.DONE ? ThemeColours.DONE : status == Status.PROGRESS ? ThemeColours.IN_PROGRESS : Card.BORDER);
			g2.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, ARC, ARC);

			paintIcon(g2, icons[i], r, status == Status.NOT_STARTED);
			if (status == Status.PROGRESS)
			{
				paintProgressLine(g2, r, tile.getProgress());
			}
		}

		// Drawn last, as they reach over the neighbouring tiles' gaps.
		for (int i = 0; i < tiles.size(); i++)
		{
			Tile tile = tiles.get(i);
			Rectangle r = bounds(i);
			if (tile != null && tile.getStatus() == Status.DONE)
			{
				new CheckMark(true, TICK_SIZE).paintIcon(this, g2, r.x + r.width - TICK_SIZE + MARGIN, r.y - MARGIN);
			}
			if (i == selected && tile != null)
			{
				g2.setColor(outline);
				g2.setStroke(new BasicStroke(2));
				g2.drawRoundRect(r.x - 2, r.y - 2, r.width + 3, r.height + 3, OUTLINE_ARC, OUTLINE_ARC);
			}
		}
		g2.dispose();
	}

	private static void paintIcon(Graphics2D g2, Image icon, Rectangle r, boolean dimmed)
	{
		if (icon == null)
		{
			return;
		}

		Graphics2D ig = (Graphics2D) g2.create();
		if (dimmed)
		{
			ig.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, DIMMED));
		}
		ig.drawImage(icon, r.x + (r.width - icon.getWidth(null)) / 2, r.y + (r.height - icon.getHeight(null)) / 2, null);
		ig.dispose();
	}

	private static void paintProgressLine(Graphics2D g2, Rectangle r, double progress)
	{
		int x = r.x + 3;
		int width = r.width - 6;
		int y = r.y + r.height - 4;
		g2.setColor(LINE_TRACK);
		g2.fillRect(x, y, width, 2);
		g2.setColor(ThemeColours.IN_PROGRESS);
		g2.fillRect(x, y, Math.max(2, (int) Math.round(width * Math.max(0, Math.min(1, progress)))), 2);
	}

	private static double tileSize(int width, int columns)
	{
		return (width - 2.0 * MARGIN - (columns - 1) * GAP) / columns;
	}

	private int layoutWidth()
	{
		return Math.max(width, getWidth() > 0 ? getWidth() : lastWidth);
	}

	private Rectangle bounds(int index)
	{
		double size = tileSize(layoutWidth(), columns);
		int column = index % columns;
		int row = index / columns;
		int x = (int) Math.round(MARGIN + column * (size + GAP));
		int y = (int) Math.round(MARGIN + row * (size + GAP));
		int right = (int) Math.round(MARGIN + column * (size + GAP) + size);
		int bottom = (int) Math.round(MARGIN + row * (size + GAP) + size);
		return new Rectangle(x, y, right - x, bottom - y);
	}

	/**
	 * The index of the tile at a point, or -1 for a gap or an empty place.
	 */
	private int tileAt(int x, int y)
	{
		for (int i = 0; i < tiles.size(); i++)
		{
			if (tiles.get(i) != null && bounds(i).contains(x, y))
			{
				return i;
			}
		}
		return -1;
	}

	private static JComponent legendItem(Color colour, boolean filled, String text, Color muted)
	{
		JLabel label = new JLabel(text);
		label.setForeground(muted);
		label.setFont(FontManager.getRunescapeSmallFont());
		label.setIcon(new SwatchIcon(colour, filled));
		label.setIconTextGap(GAP);
		return label;
	}

	/**
	 * A small square of a tile colour for the legend, filled or just outlined.
	 */
	@Value
	private static class SwatchIcon implements Icon
	{
		private static final int SIZE = 8;

		Color colour;
		boolean filled;

		@Override
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			Graphics2D g2 = (Graphics2D) g.create();
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setColor(colour);
			if (filled)
			{
				g2.fillRoundRect(x, y, SIZE, SIZE, 4, 4);
			}
			else
			{
				g2.drawRoundRect(x, y, SIZE - 1, SIZE - 1, 4, 4);
			}
			g2.dispose();
		}

		@Override
		public int getIconWidth()
		{
			return SIZE;
		}

		@Override
		public int getIconHeight()
		{
			return SIZE;
		}
	}
}
