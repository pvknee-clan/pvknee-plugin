package com.pvknee.ui;

import com.pvknee.data.Page;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import net.runelite.client.ui.FontManager;

/**
 * A card-style page dropdown, the same height as {@link IconButton}: the page title with a muted chevron, opening a
 * list of the pages with the current one marked in the accent colour.
 */
public class PagePicker extends Card
{
	private static final int CHEVRON_SIZE = 12;
	private static final int ROW_PADDING_Y = 6;
	private static final Color TITLE = new Color(221, 221, 221);

	private final Consumer<Page> onSelect;
	private final JLabel title = new JLabel();
	private final JLabel chevron = new JLabel();
	private final JPopupMenu menu = new JPopupMenu();
	private List<Page> pages = Collections.emptyList();
	private Page selected;
	private ThemeColours colours = ThemeColours.from(null);

	public PagePicker(Consumer<Page> onSelect)
	{
		super(new BorderLayout(Spacing.ITEM_GAP, 0));
		this.onSelect = onSelect;
		setArc(Spacing.CONTROL_ARC);
		setBorder(new EmptyBorder(0, Spacing.PADDING, 0, Spacing.PADDING));
		setPreferredSize(new Dimension(0, IconButton.SIZE));
		getAccessibleContext().setAccessibleName("Page");

		title.setForeground(TITLE);
		title.setFont(FontManager.getRunescapeFont());
		add(title, BorderLayout.CENTER);
		add(chevron, BorderLayout.EAST);
		updateChevron();

		menu.setBorder(new LineBorder(BORDER));
		menu.setBackground(BACKGROUND);
		onClick(this::openMenu);
	}

	/**
	 * Replaces the pages and shows the selected one, without telling the listener.
	 */
	public void setPages(List<Page> pages, Page selected, ThemeColours colours)
	{
		this.pages = pages;
		this.colours = colours;
		updateChevron();
		show(selected);
	}

	public List<Page> getPages()
	{
		return pages;
	}

	public Page getSelected()
	{
		return selected;
	}

	/**
	 * Shows the page and tells the listener, as if the member picked it.
	 */
	public void select(Page page)
	{
		show(page);
		onSelect.accept(page);
	}

	private void show(Page page)
	{
		selected = page;
		title.setText(page == null ? "" : page.getTitle());
		setToolTipText(page == null ? null : page.getTitle());
	}

	private void updateChevron()
	{
		chevron.setIcon(new GlyphIcon(Glyphs.CHEVRON, colours.getMuted(), CHEVRON_SIZE));
	}

	private void openMenu()
	{
		if (pages.isEmpty())
		{
			return;
		}

		menu.removeAll();
		for (Page page : pages)
		{
			menu.add(row(page));
		}
		menu.show(this, 0, getHeight() + Spacing.ITEM_GAP);
	}

	private JPanel row(Page page)
	{
		JLabel label = new JLabel(page.getTitle());
		label.setForeground(page == selected ? colours.getAccent() : colours.getValue());
		label.setFont(FontManager.getRunescapeFont());

		JPanel row = new JPanel(new BorderLayout());
		row.setBackground(BACKGROUND);
		row.setBorder(new EmptyBorder(ROW_PADDING_Y, Spacing.PADDING, ROW_PADDING_Y, Spacing.PADDING));
		row.setPreferredSize(new Dimension(getWidth() - 2, label.getPreferredSize().height + 2 * ROW_PADDING_Y));
		row.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		row.add(label, BorderLayout.CENTER);
		row.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseEntered(MouseEvent e)
			{
				row.setBackground(HOVER);
			}

			@Override
			public void mouseExited(MouseEvent e)
			{
				row.setBackground(BACKGROUND);
			}

			@Override
			public void mouseReleased(MouseEvent e)
			{
				if (SwingUtilities.isLeftMouseButton(e) && row.contains(e.getPoint()))
				{
					menu.setVisible(false);
					select(page);
				}
			}
		});
		return row;
	}
}
