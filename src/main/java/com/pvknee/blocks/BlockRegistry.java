package com.pvknee.blocks;

import com.pvknee.data.Block;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.JComponent;
import lombok.extern.slf4j.Slf4j;

/**
 * Maps block types to their renderers. Unknown types are skipped.
 */
@Slf4j
@Singleton
public class BlockRegistry
{
	private final Map<String, BlockRenderer> renderers = new HashMap<>();

	@Inject
	BlockRegistry()
	{
		renderers.put("heading", new HeadingBlock());
		renderers.put("text", new TextBlock());
		renderers.put("separator", new SeparatorBlock());
		renderers.put("message", new MessageBlock());
		renderers.put("links", new LinksBlock());
	}

	public JComponent render(Block block, BlockContext context)
	{
		BlockRenderer renderer = renderers.get(block.getType());
		if (renderer == null)
		{
			log.debug("Skipping unknown block type {}", block.getType());
			return null;
		}
		return renderer.render(block, context);
	}
}
