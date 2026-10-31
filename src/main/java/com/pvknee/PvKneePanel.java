package com.pvknee;

import java.awt.BorderLayout;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.PluginPanel;

class PvKneePanel extends PluginPanel
{
	PvKneePanel()
	{
		setLayout(new BorderLayout());
		add(new JLabel("PvKnee", SwingConstants.CENTER), BorderLayout.NORTH);
	}
}
