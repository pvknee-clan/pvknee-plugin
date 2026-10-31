package com.pvknee.ui;

import java.awt.Color;
import java.awt.Font;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * A wrapped label whose text comes from a supplier, so it can be updated in place (e.g. "Ends in 6d 4h").
 * A null text hides the label.
 */
public class LiveLabel extends JLabel
{
	private final Supplier<String> text;
	private final int width;
	private final boolean centred;
	private String shown;

	public LiveLabel(Supplier<String> text, Color colour, Font font, int width, boolean centred)
	{
		this.text = text;
		this.width = width;
		this.centred = centred;
		setForeground(colour);
		setFont(font);
		setHorizontalAlignment(centred ? SwingConstants.CENTER : SwingConstants.LEFT);
		apply(text.get());
	}

	/**
	 * Asks the supplier again and redraws only if the text changed.
	 */
	public void update()
	{
		String next = text.get();
		if (!Objects.equals(next, shown))
		{
			apply(next);
			revalidate();
		}
	}

	private void apply(String next)
	{
		shown = next;
		setVisible(next != null);
		if (next != null)
		{
			Labels.setText(this, next, width, centred);
		}
	}
}
