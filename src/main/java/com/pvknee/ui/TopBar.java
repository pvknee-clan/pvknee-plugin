package com.pvknee.ui;

import com.pvknee.data.Page;
import java.awt.BorderLayout;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/**
 * The controls above every page: the home button, the page picker and the refresh button with its spinner.
 */
public class TopBar extends JPanel
{
	private final PagePicker pagePicker;
	private final IconButton homeButton = new IconButton(Glyphs.HOME, "Home", this::openHome);
	private final IconButton refreshButton;
	private ThemeColours colours = ThemeColours.from(null);

	public TopBar(Consumer<Page> onSelect, Runnable onRefresh)
	{
		super(new BorderLayout(Spacing.ITEM_GAP, 0));
		pagePicker = new PagePicker(onSelect);
		refreshButton = new IconButton(Glyphs.REFRESH, "Refresh", onRefresh);
		setBackground(ColorScheme.DARK_GRAY_COLOR);
		add(homeButton, BorderLayout.WEST);
		add(pagePicker, BorderLayout.CENTER);
		add(refreshButton, BorderLayout.EAST);
	}

	/**
	 * Replaces the pages and shows the selected one, without telling the listener.
	 */
	public void setPages(List<Page> pages, Page selected, ThemeColours colours)
	{
		this.colours = colours;
		pagePicker.setPages(pages, selected, colours);
	}

	public List<Page> getPages()
	{
		return pagePicker.getPages();
	}

	public Page getSelected()
	{
		return pagePicker.getSelected();
	}

	/**
	 * Shows the page and tells the listener, as if the member picked it.
	 */
	public void select(Page page)
	{
		pagePicker.select(page);
	}

	/**
	 * Outlines the home button while the first page is shown. Null means no page is shown.
	 */
	public void setShown(Page page)
	{
		List<Page> pages = getPages();
		homeButton.setHighlight(page != null && !pages.isEmpty() && page == pages.get(0) ? colours.getAccent() : null);
	}

	/**
	 * Spins and disables the refresh button while a load the member is waiting on runs.
	 */
	public void setLoading(boolean loading)
	{
		refreshButton.setSpinning(loading ? colours.getAccent() : null);
		refreshButton.setEnabled(!loading);
	}

	/**
	 * Opens the first page, which is Home when the sheet has it.
	 */
	private void openHome()
	{
		List<Page> pages = getPages();
		if (!pages.isEmpty())
		{
			select(pages.get(0));
		}
	}
}
