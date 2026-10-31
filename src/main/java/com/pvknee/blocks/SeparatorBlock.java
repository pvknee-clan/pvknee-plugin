package com.pvknee.blocks;

import com.pvknee.data.Block;
import com.pvknee.ui.Divider;
import javax.swing.Box;
import javax.swing.JComponent;

/**
 * A faint thin line, or with style=space just an extra gap.
 */
class SeparatorBlock implements BlockRenderer
{
	@Override
	public JComponent render(Block block, BlockContext context)
	{
		if (block.setting("style", "line").equalsIgnoreCase("space"))
		{
			return (JComponent) Box.createVerticalStrut(0);
		}
		return new Divider(context.getColours().getMuted());
	}
}
