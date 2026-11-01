package com.pvknee.ui;

import java.awt.GridBagLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Keeps a component at its own size, centred in whatever space it is given, e.g. a button beside text that may wrap.
 */
public class Centred extends JPanel
{
	public Centred(JComponent component)
	{
		super(new GridBagLayout());
		setOpaque(false);
		add(component);
	}
}
