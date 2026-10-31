package com.pvknee.blocks;

import com.pvknee.data.Block;
import javax.swing.JComponent;

/**
 * Draws one block type. Returns null to draw nothing.
 */
interface BlockRenderer
{
	JComponent render(Block block, BlockContext context);
}
