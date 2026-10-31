package com.pvknee;

import com.pvknee.data.Block;
import com.pvknee.data.ClanData;
import com.pvknee.data.ClanDataClient;
import com.pvknee.data.Page;
import com.pvknee.data.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;
import net.runelite.client.util.ColorUtil;

@Slf4j
class PvKneePanel extends PluginPanel
{
	private static final int TEXT_WIDTH = 175;

	private final ClanDataClient client;
	private final JComboBox<Page> pageSelect = new JComboBox<>();
	private final JButton refreshButton = new JButton("Refresh");
	private final JLabel statusLabel = new JLabel();
	private final JPanel content = new JPanel();

	private ClanData data;

	@Inject
	PvKneePanel(ClanDataClient client)
	{
		super(false);
		this.client = client;

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

		JPanel top = new JPanel(new BorderLayout(0, 10));
		top.setBackground(ColorScheme.DARK_GRAY_COLOR);
		top.add(controls, BorderLayout.NORTH);
		top.add(statusLabel, BorderLayout.CENTER);

		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBackground(ColorScheme.DARK_GRAY_COLOR);

		JPanel contentWrapper = new JPanel(new BorderLayout());
		contentWrapper.setBackground(ColorScheme.DARK_GRAY_COLOR);
		contentWrapper.add(content, BorderLayout.NORTH);

		JScrollPane scrollPane = new JScrollPane(contentWrapper);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setBorder(null);

		add(top, BorderLayout.NORTH);
		add(scrollPane, BorderLayout.CENTER);
	}

	void refresh()
	{
		refreshButton.setEnabled(false);
		showStatus("Loading...", Color.WHITE);

		client.fetch(
			data -> SwingUtilities.invokeLater(() -> show(data)),
			e ->
			{
				log.debug("Failed to load clan panel", e);
				SwingUtilities.invokeLater(() ->
				{
					refreshButton.setEnabled(true);
					showStatus("ERROR: Couldn't load the clan panel. Press Refresh to try again.", ColorScheme.PROGRESS_ERROR_COLOR);
				});
			});
	}

	private void showStatus(String text, Color colour)
	{
		statusLabel.setText(html(text));
		statusLabel.setForeground(colour);
		statusLabel.setVisible(true);
	}

	private void show(ClanData data)
	{
		log.debug("Loaded clan panel: {} pages", data.getPages().size());
		this.data = data;
		refreshButton.setEnabled(true);
		statusLabel.setVisible(false);

		Page selected = (Page) pageSelect.getSelectedItem();
		String selectedId = selected == null ? null : selected.getId();

		pageSelect.removeAllItems();
		for (Page page : data.getPages())
		{
			if (page.isShow())
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

		Theme theme = data.getTheme();
		Color accent = themeColour(theme == null ? null : theme.getAccent(), ColorScheme.BRAND_ORANGE);
		Color value = themeColour(theme == null ? null : theme.getValue(), Color.WHITE);
		Color muted = themeColour(theme == null ? null : theme.getMuted(), Color.GRAY);

		content.removeAll();
		for (Block block : page.getBlocks())
		{
			Color colour = "heading".equals(block.getType()) ? accent
				: "separator".equals(block.getType()) ? muted
				: value;
			content.add(line(describe(block), colour));
		}
		content.revalidate();
		content.repaint();
	}

	private void showMessage(String message)
	{
		content.removeAll();
		content.add(line(message, Color.WHITE));
		content.revalidate();
		content.repaint();
	}

	private static String describe(Block block)
	{
		Map<String, String> settings = block.getSettings();
		if (settings == null || settings.isEmpty())
		{
			return block.getType();
		}

		String text = settings.get("text");
		if (text != null && !text.isEmpty())
		{
			return block.getType() + ": " + text;
		}

		return block.getType() + " (" + settings.entrySet().stream()
			.map(e -> e.getKey() + "=" + e.getValue())
			.collect(Collectors.joining(", ")) + ")";
	}

	private static JLabel line(String text, Color colour)
	{
		JLabel label = new JLabel(html(text));
		label.setForeground(colour);
		label.setBorder(new EmptyBorder(0, 0, 4, 0));
		return label;
	}

	private static String html(String text)
	{
		String escaped = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		return "<html><body style='width:" + TEXT_WIDTH + "px'>" + escaped + "</body></html>";
	}

	private static Color themeColour(String hex, Color fallback)
	{
		Color colour = hex == null ? null : ColorUtil.fromHex(hex);
		return colour == null ? fallback : colour;
	}
}
