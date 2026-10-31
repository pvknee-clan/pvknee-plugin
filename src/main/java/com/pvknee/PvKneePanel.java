package com.pvknee;

import com.pvknee.data.ClanData;
import com.pvknee.data.ClanDataClient;
import java.awt.BorderLayout;
import javax.inject.Inject;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;

@Slf4j
class PvKneePanel extends PluginPanel
{
	private final ClanDataClient client;
	private final JLabel status = new JLabel("PvKnee", SwingConstants.CENTER);

	@Inject
	PvKneePanel(ClanDataClient client)
	{
		this.client = client;

		setLayout(new BorderLayout());
		add(status, BorderLayout.NORTH);
	}

	void refresh()
	{
		client.fetch(
			data -> SwingUtilities.invokeLater(() -> show(data)),
			e ->
			{
				log.debug("Failed to load clan panel", e);
				SwingUtilities.invokeLater(() -> status.setText("Couldn't load the clan panel"));
			});
	}

	private void show(ClanData data)
	{
		log.debug("Loaded clan panel: {} pages", data.getPages().size());
		status.setText(data.getPages().size() + " pages, generated " + data.getGeneratedAt());
	}
}
