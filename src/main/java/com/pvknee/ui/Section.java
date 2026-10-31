package com.pvknee.ui;

import java.awt.BorderLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * A section title with its content under it, e.g. a list card. Without a title it is just the content.
 */
public class Section extends JPanel
{
	public Section(SectionHeader title, JComponent content)
	{
		super(new BorderLayout(0, Spacing.TITLE_GAP));
		setOpaque(false);
		if (title != null)
		{
			add(title, BorderLayout.NORTH);
		}
		add(content, BorderLayout.CENTER);
	}
}
