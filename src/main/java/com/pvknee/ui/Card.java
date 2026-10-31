package com.pvknee.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/**
 * A rounded panel, slightly lighter than the page, with a subtle border.
 */
public class Card extends JPanel
{
	public static final Color BACKGROUND = new Color(48, 48, 48);
	public static final Color HOVER = new Color(58, 58, 58);
	private static final Color BORDER = new Color(64, 64, 64);

	public Card(LayoutManager layout)
	{
		super(layout);
		setOpaque(false);
		setBackground(BACKGROUND);
		setBorder(new EmptyBorder(Spacing.PADDING, Spacing.PADDING, Spacing.PADDING, Spacing.PADDING));
	}

	@Override
	protected void paintComponent(Graphics g)
	{
		Graphics2D g2 = (Graphics2D) g.create();
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(getBackground());
		g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Spacing.ARC, Spacing.ARC);
		g2.setColor(BORDER);
		g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, Spacing.ARC, Spacing.ARC);
		g2.dispose();
	}
}
