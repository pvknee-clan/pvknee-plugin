package com.pvknee.blocks;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.api.Skill;
import net.runelite.client.game.ItemManager;
import net.runelite.client.game.SkillIconManager;
import net.runelite.client.util.AsyncBufferedImage;

/**
 * Icons for Wise Old Man metrics and items: skills and items come from the game client, bosses are downloaded.
 */
@Singleton
public class Icons
{
	private final SkillIconManager skillIcons;
	private final ItemManager itemManager;
	private final ImageLoader images;
	private final Map<String, ImageIcon> scaledSkills = new HashMap<>();
	private final Map<String, ImageIcon> scaledItems = new HashMap<>();

	@Inject
	Icons(SkillIconManager skillIcons, ItemManager itemManager, ImageLoader images)
	{
		this.skillIcons = skillIcons;
		this.itemManager = itemManager;
		this.images = images;
	}

	/**
	 * The icon for a Wise Old Man metric, e.g. woodcutting or cerberus. Bosses load from iconBase.
	 * Call on the Swing thread.
	 */
	public JLabel metric(String metric, String iconBase, int size)
	{
		Skill skill = skill(metric);
		if (skill != null)
		{
			ImageIcon icon = scaledSkills.computeIfAbsent(skill.name() + "@" + size,
				k -> ImageLoader.scaled(skillIcons.getSkillImage(skill), size));
			return new JLabel(icon);
		}
		return images.icon(iconBase == null || metric == null ? null : iconBase + metric + ".png", size);
	}

	/**
	 * The icon for an item, e.g. 11832 for a Bandos chestplate, drawn by the game client. The label keeps its size
	 * and fills in once the client has drawn the item; it stays empty for 0 or an unknown item. Call on the Swing thread.
	 */
	public JLabel item(int itemId, int size)
	{
		JLabel label = ImageLoader.emptyLabel(size);
		loadItem(itemId, size, label::setIcon);
		return label;
	}

	/**
	 * Hands over an item's icon at the given size on the Swing thread once the game client has drawn it, at once if
	 * it has already; never for 0 or an unknown item. For kit pieces that paint icons themselves. Call on the Swing
	 * thread.
	 */
	public void loadItem(int itemId, int size, Consumer<ImageIcon> onLoad)
	{
		if (itemId <= 0)
		{
			return;
		}

		String key = itemId + "@" + size;
		ImageIcon cached = scaledItems.get(key);
		if (cached != null)
		{
			onLoad.accept(cached);
			return;
		}

		AsyncBufferedImage image = itemManager.getImage(itemId);
		image.onLoaded(() -> SwingUtilities.invokeLater(() ->
			onLoad.accept(scaledItems.computeIfAbsent(key, k -> ImageLoader.scaled(image, size)))));
	}

	private static Skill skill(String metric)
	{
		if (metric == null)
		{
			return null;
		}

		String name = metric.trim().toUpperCase();
		if (name.equals("OVERALL"))
		{
			return null;
		}
		if (name.equals("RUNECRAFTING"))
		{
			return Skill.RUNECRAFT;
		}

		for (Skill skill : Skill.values())
		{
			if (skill.name().equals(name))
			{
				return skill;
			}
		}
		return null;
	}
}
