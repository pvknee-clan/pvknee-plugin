package com.pvknee;

import com.pvknee.blocks.BlockContext;
import com.pvknee.blocks.BlockRegistry;
import com.pvknee.blocks.Icons;
import com.pvknee.blocks.PageNavigator;
import com.pvknee.data.Block;
import com.pvknee.data.ClanData;
import com.pvknee.data.ClanDataClient;
import com.pvknee.data.Page;
import com.pvknee.ui.Labels;
import com.pvknee.ui.Spacing;
import com.pvknee.ui.ThemeColours;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.Objects;
import javax.inject.Inject;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.DynamicGridLayout;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

@Slf4j
class PvKneePanel extends PluginPanel implements PageNavigator
{
	private static final int SUPPORTED_VERSION = 1;

	private final ClanDataClient client;
	private final BlockRegistry blocks;
	private final Icons icons;
	private final JComboBox<Page> pageSelect = new JComboBox<>();
	private final JButton refreshButton = new JButton("Refresh");
	private final JLabel statusLabel = new JLabel();
	private final JLabel versionLabel = Labels.label("A newer plugin version is available", Color.GRAY, FontManager.getRunescapeSmallFont(), Labels.CONTENT_WIDTH, false);
	private final JPanel content = new JPanel();

	private ClanData data;
	private boolean loading;

	@Inject
	PvKneePanel(ClanDataClient client, BlockRegistry blocks, Icons icons)
	{
		super(false);
		this.client = client;
		this.blocks = blocks;
		this.icons = icons;

		setLayout(new BorderLayout(0, 10));
		setBorder(new EmptyBorder(10, 10, 10, 10));
		setBackground(ColorScheme.DARK_GRAY_COLOR);

		pageSelect.setRenderer(new DefaultListCellRenderer()
		{
			@Override
			public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus)
			{
				super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
				if (value instanceof Page)
				{
					setText(((Page) value).getTitle());
				}
				return this;
			}
		});
		pageSelect.addActionListener(e -> showPage((Page) pageSelect.getSelectedItem()));

		refreshButton.addActionListener(e -> refresh());

		JPanel controls = new JPanel(new BorderLayout(5, 0));
		controls.setBackground(ColorScheme.DARK_GRAY_COLOR);
		controls.add(pageSelect, BorderLayout.CENTER);
		controls.add(refreshButton, BorderLayout.EAST);

		statusLabel.setVisible(false);
		versionLabel.setVisible(false);

		JPanel top = new JPanel(new BorderLayout(0, 10));
		top.setBackground(ColorScheme.DARK_GRAY_COLOR);
		top.add(controls, BorderLayout.NORTH);
		top.add(statusLabel, BorderLayout.CENTER);
		top.add(versionLabel, BorderLayout.SOUTH);

		content.setLayout(new DynamicGridLayout(0, 1, 0, Spacing.BLOCK_GAP));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel contentWrapper = new WidthTrackingPanel();
		contentWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentWrapper.add(content, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(contentWrapper);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);

		add(top, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
	}

	@Override
	public void onActivate()
	{
		if (data == null && !loading)
		{
			refresh();
		}
	}

	private void refresh()
	{
		loading = true;
		refreshButton.setEnabled(false);
		showStatus("Loading...", Color.WHITE);

		client.fetch(
			data -> SwingUtilities.invokeLater(() -> show(data)),
			e ->
			{
				log.debug("Failed to load clan panel", e);
				SwingUtilities.invokeLater(() ->
				{
					loading = false;
					refreshButton.setEnabled(true);
					showStatus("ERROR: Couldn't load the clan panel. Press Refresh to try again.", ColorScheme.PROGRESS_ERROR_COLOR);
				});
			});
	}

	private void showStatus(String text, Color colour)
	{
		Labels.setText(statusLabel, text, Labels.CONTENT_WIDTH, false);
		statusLabel.setForeground(colour);
		statusLabel.setVisible(true);
	}

	private void show(ClanData data)
	{
		log.debug("Loaded clan panel: {} pages", data.getPages().size());
		this.data = data;
		loading = false;
		refreshButton.setEnabled(true);
		statusLabel.setVisible(false);

		versionLabel.setForeground(ThemeColours.from(data.getTheme()).getMuted());
		versionLabel.setVisible(data.getVersion() > SUPPORTED_VERSION);

		Page selected = (Page) pageSelect.getSelectedItem();
		String selectedId = selected == null ? null : selected.getId();

		pageSelect.removeAllItems();
		for (Page page : data.getPages())
		{
			if (isVisible(page))
			{
				pageSelect.addItem(page);
				if (Objects.equals(page.getId(), selectedId))
				{
					pageSelect.setSelectedItem(page);
				}
			}
		}

		if (pageSelect.getItemCount() == 0)
		{
			showMessage("No pages to show");
		}
	}

	private void showPage(Page page)
	{
		if (page == null)
		{
			return;
		}

		BlockContext context = new BlockContext(data, ThemeColours.from(data.getTheme()), icons, this);

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
			pageSelect.setSelectedItem(page);
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

	private static boolean isVisible(Page page)
	{
		return page.isShow();
	}

	private void showMessage(String message)
	{
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
