package com.pvknee;

import com.pvknee.blocks.BlockContext;
import com.pvknee.blocks.BlockRegistry;
import com.pvknee.blocks.Icons;
import com.pvknee.blocks.PageNavigator;
import com.pvknee.blocks.Times;
import com.pvknee.data.Block;
import com.pvknee.data.ClanData;
import com.pvknee.data.ClanDataClient;
import com.pvknee.data.Page;
import com.pvknee.player.PlayerInfo;
import com.pvknee.player.WorldHopper;
import com.pvknee.ui.Labels;
import com.pvknee.ui.Notice;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import com.pvknee.ui.TopBar;
import com.pvknee.wom.WomClient;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

class PvKneePanel extends PluginPanel implements PageNavigator
{
	private static final int SUPPORTED_VERSION = 1;
	private static final Duration COUNTDOWN_INTERVAL = Duration.ofMinutes(1);
	private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(10);

	private final ClanDataClient client;
	private final BlockRegistry blocks;
	private final Icons icons;
	private final WomClient wom;
	private final DevLog devLog;
	private final PlayerInfo playerInfo;
	private final WorldHopper hopper;
	private final TopBar topBar = new TopBar(this::showPage, () -> refresh(false, "button"));
	private final Notice notice = new Notice();
	private final Notice.Line errorLine = notice.addLine();
	private final Notice.Line lastUpdatedLine = notice.addLine();
	private final Notice.Line versionLine = notice.addLine();
	private final JPanel content = new JPanel();
	private final JScrollPane scrollPane;
	private final Timer countdownTimer = new Timer((int) COUNTDOWN_INTERVAL.toMillis(), e -> updateLive());
	private final Timer refreshTimer = new Timer((int) REFRESH_INTERVAL.toMillis(), e -> refresh(true, "timer"));

	private ClanData data;
	/** When the data was last loaded from the endpoint; null while only the saved copy is shown. */
	private Instant loadedAt;
	/** When the data on screen was fetched, from the endpoint or the saved copy. */
	private Instant updatedAt;
	private boolean loading;
	/** The page on screen, so a redraw of the same page keeps its scroll position. */
	private String shownPageId;
	/** Countdowns and standings on the current page, updated in place every minute. */
	private List<Runnable> liveUpdates = new ArrayList<>();

	@Inject
	PvKneePanel(ClanDataClient client, BlockRegistry blocks, Icons icons, WomClient wom, DevLog devLog, PlayerInfo playerInfo, WorldHopper hopper)
	{
		super(false);
		this.client = client;
		this.blocks = blocks;
		this.icons = icons;
		this.wom = wom;
		this.devLog = devLog;
		this.playerInfo = playerInfo;
		this.hopper = hopper;

		setLayout(new BorderLayout(0, 10));
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel top = new JPanel(new BorderLayout(0, 10));
		top.setBackground(ColorScheme.DARK_GRAY_COLOR);
		top.add(topBar, BorderLayout.NORTH);
		top.add(notice, BorderLayout.CENTER);

		content.setLayout(new DynamicGridLayout(0, 1, 0, Spacing.BLOCK_GAP));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel contentWrapper = new WidthTrackingPanel();
		contentWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentWrapper.add(content, BorderLayout.NORTH);

		scrollPane = new JScrollPane(contentWrapper);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);

		add(top, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
	}

	@Override
	public void onActivate()
	{
		if (data == null)
		{
			client.loadSaved((saved, savedAt) -> SwingUtilities.invokeLater(() -> showSaved(saved, savedAt)));
			refresh(false, "first open");
		}
		else if (loadedAt == null || Instant.now().isAfter(loadedAt.plus(REFRESH_INTERVAL)))
		{
			refresh(true, "open, data is old");
		}
		else
		{
			updateLive();
		}

		countdownTimer.restart();
		refreshTimer.restart();
	}

	@Override
	public void onDeactivate()
	{
		stopUpdates();
	}

	/**
	 * Stops the countdown and refresh timers until the panel is shown again.
	 */
	void stopUpdates()
	{
		countdownTimer.stop();
		refreshTimer.stop();
	}

	/**
	 * Reloads the clan data. A quiet refresh keeps the page as it is while loading, and on failure only shows when
	 * the data on screen is from.
	 */
	private void refresh(boolean quiet, String reason)
	{
		if (loading)
		{
			devLog.log("refresh: {} skipped, already loading", reason);
			return;
		}

		devLog.log("refresh: {}{}", reason, quiet ? " (quiet)" : "");
		loading = true;
		if (!quiet)
		{
			topBar.setLoading(true);
		}

		client.fetch(
			data -> SwingUtilities.invokeLater(() -> show(data)),
			e ->
			{
				devLog.log("load: failed, {}", e.getMessage(), e);
				SwingUtilities.invokeLater(() ->
				{
					loading = false;
					topBar.setLoading(false);
					if (!quiet)
					{
						errorLine.setMessage("Couldn't load the clan panel. Press the refresh button to try again.", ColorScheme.PROGRESS_ERROR_COLOR);
					}
					showLastUpdated();
				});
			});
	}

	private void show(ClanData data)
	{
		devLog.log("load: {} pages", data.getPages().size());
		loadedAt = Instant.now();
		updatedAt = loadedAt;
		loading = false;
		topBar.setLoading(false);
		errorLine.clear();
		lastUpdatedLine.clear();
		display(data);
	}

	/**
	 * Shows the saved copy, unless fresh data has already arrived. While a load is still running, the "Last updated"
	 * line waits until it fails, so the page doesn't jump when the fresh data replaces the saved copy.
	 */
	private void showSaved(ClanData data, Instant savedAt)
	{
		if (this.data != null)
		{
			return;
		}

		devLog.log("load: showing saved copy from {}", savedAt);
		updatedAt = savedAt;
		display(data);
		if (!loading)
		{
			showLastUpdated();
		}
	}

	private void showLastUpdated()
	{
		if (data == null)
		{
			return;
		}

		lastUpdatedLine.setMessage("Last updated " + Times.time(updatedAt), ThemeColours.from(data.getTheme()).getMuted());
	}

	private void display(ClanData data)
	{
		this.data = data;
		if (data.getVersion() > SUPPORTED_VERSION)
		{
			versionLine.setMessage("A newer plugin version is available", ThemeColours.from(data.getTheme()).getMuted());
		}
		else
		{
			versionLine.clear();
		}

		Page previous = topBar.getSelected();
		String previousId = previous == null ? null : previous.getId();
		List<Page> pages = data.getPages().stream()
			.filter(PvKneePanel::isVisible)
			.collect(Collectors.toList());
		Page selected = pages.stream()
			.filter(page -> Objects.equals(page.getId(), previousId))
			.findFirst()
			.orElse(pages.isEmpty() ? null : pages.get(0));
		topBar.setPages(pages, selected, ThemeColours.from(data.getTheme()));

		if (selected == null)
		{
			showMessage("No pages to show");
		}
		else
		{
			showPage(selected);
		}
	}

	/**
	 * Builds the current page again in place, e.g. after a setting that changes how it looks or a change in game.
	 */
	void redraw()
	{
		if (data != null)
		{
			showPage(topBar.getSelected());
			if (lastUpdatedLine.isVisible())
			{
				showLastUpdated();
			}
		}
	}

	/**
	 * Updates the countdowns and standings on the current page without rebuilding it.
	 */
	private void updateLive()
	{
		liveUpdates.forEach(Runnable::run);
	}

	/**
	 * Builds the page. A different page opens at the top; the same page keeps its scroll position.
	 */
	private void showPage(Page page)
	{
		if (page == null)
		{
			return;
		}

		ThemeColours colours = ThemeColours.from(data.getTheme());
		topBar.setShown(page);

		liveUpdates = new ArrayList<>();
		BlockContext context = new BlockContext(data, colours, icons, this, wom, playerInfo.get(), hopper, devLog, liveUpdates);

		content.removeAll();
		for (Block block : page.getBlocks())
		{
			JComponent component = blocks.render(block, context);
			if (component != null)
			{
				content.add(component);
			}
		}
		content.revalidate();
		content.repaint();

		if (!Objects.equals(page.getId(), shownPageId))
		{
			shownPageId = page.getId();
			scrollPane.getVerticalScrollBar().setValue(0);
		}
	}

	@Override
	public boolean canOpen(String pageId)
	{
		return findPage(pageId) != null;
	}

	@Override
	public void open(String pageId)
	{
		Page page = findPage(pageId);
		if (page != null)
		{
			topBar.select(page);
		}
	}

	private Page findPage(String pageId)
	{
		if (data == null || pageId == null)
		{
			return null;
		}
		return data.getPages().stream()
			.filter(page -> pageId.equals(page.getId()) && isVisible(page))
			.findFirst()
			.orElse(null);
	}

	/**
	 * Pages with a rank stay hidden until rank checks exist.
	 */
	private static boolean isVisible(Page page)
	{
		return page.isShow() && (page.getRank() == null || page.getRank().trim().isEmpty());
	}

	private void showMessage(String message)
	{
		liveUpdates = new ArrayList<>();
		shownPageId = null;
		topBar.setShown(null);
		content.removeAll();
		content.add(Labels.label(message, Color.WHITE, FontManager.getRunescapeFont(), Labels.CONTENT_WIDTH, false));
		content.revalidate();
		content.repaint();
	}

	/**
	 * Scroll pane content that always matches the visible width, so nothing is cut off on the right.
	 */
	private static class WidthTrackingPanel extends JPanel implements Scrollable
	{
		WidthTrackingPanel()
		{
			super(new BorderLayout());
		}

		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return 16;
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return visibleRect.height;
		}

		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}

		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}
}
