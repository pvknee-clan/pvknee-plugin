package com.pvknee.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * A rounded panel, slightly lighter than the page, with a subtle border.
 */
public class Card extends JPanel
{
	/** Also the background of other kit pieces, e.g. {@link PagePicker}. */
	static final Color BACKGROUND = new Color(48, 48, 48);
	/** Also the hover tint of other kit pieces, e.g. {@link PagePicker}. */
	static final Color HOVER = new Color(58, 58, 58);
	/** Also the outline of other kit pieces, e.g. {@link Pill}. */
	static final Color BORDER = new Color(64, 64, 64);

	private static final float[] DASH = {4, 3};

	private final boolean bordered;
	private boolean hovered;
	private Color outline = BORDER;
	private int arc = Spacing.ARC;
	private boolean dashed;

	public Card(LayoutManager layout)
	{
		this(layout, true);
	}

	/**
	 * A card without the border, for clickable areas inside another card.
	 */
	public Card(LayoutManager layout, boolean bordered)
	{
		super(layout);
		this.bordered = bordered;
		setOpaque(false);
		setBackground(BACKGROUND);
		setBorder(new EmptyBorder(Spacing.PADDING, Spacing.PADDING, Spacing.PADDING, Spacing.PADDING));
	}

	/**
	 * Draws the border in this colour instead of the default, e.g. the accent to mark the active choice.
	 * Null restores the default.
	 */
	public void setOutline(Color colour)
	{
		outline = colour == null ? BORDER : colour;
		repaint();
	}

	/**
	 * Rounds the corners by this arc diameter instead of the card default, e.g. {@link Spacing#CONTROL_ARC}.
	 */
	public void setArc(int arc)
	{
		this.arc = arc;
		repaint();
	}

	/**
	 * Draws the border dashed, e.g. for a hint standing in for content that isn't there yet.
	 */
	public void setDashed(boolean dashed)
	{
		this.dashed = dashed;
		repaint();
	}

	/**
	 * Makes the whole card clickable, with a hand cursor and a hover highlight.
	 * Acts when the left button is released over the card, even if the mouse moved while pressed.
	 * A disabled card ignores clicks and hover.
	 */
	public void onClick(Runnable action)
	{
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (isEnabled() && SwingUtilities.isLeftMouseButton(e) && contains(e.getPoint()))
				{
					action.run();
				}
			}

			@Override
			public void mouseEntered(MouseEvent e)
			{
				if (isEnabled())
				{
					setHovered(true);
				}
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				setHovered(false);
			}
		});
	}

	@Override
	public void setEnabled(boolean enabled)
	{
		super.setEnabled(enabled);
		if (!enabled)
		{
			setHovered(false);
		}
	}

	/**
	 * Whether the mouse is over a clickable card, for kit pieces that paint themselves.
	 */
	protected boolean isHovered()
	{
		return hovered;
	}

	private void setHovered(boolean hovered)
	{
		this.hovered = hovered;
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(hovered ? HOVER : getBackground());
		g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
		if (bordered)
		{
			g2.setColor(outline);
			if (dashed)
			{
				g2.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 10, DASH, 0));
			}
			g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
		}
		g2.dispose();
	}
}
