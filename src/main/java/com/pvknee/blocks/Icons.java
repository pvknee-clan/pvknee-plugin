package com.pvknee.blocks;

import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;
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
	/** Wise Old Man's own metric icons. Fixed here, so the clan data can never point icon requests elsewhere. */
	private static final String ICON_BASE = "https://raw.githubusercontent.com/wise-old-man/wise-old-man/master/app/public/img/metrics/";

	private static final Pattern METRIC_NAME = Pattern.compile("[a-z0-9_]+");

	private final SkillIconManager skillIcons;
	private final ItemManager itemManager;
	private final ImageLoader images;
	private final Map<String, ImageIcon> scaledSkills = ImageLoader.bounded();
	private final Map<String, ImageIcon> scaledItems = ImageLoader.bounded();

	@Inject
	Icons(SkillIconManager skillIcons, ItemManager itemManager, ImageLoader images)
	{
		this.skillIcons = skillIcons;
		this.itemManager = itemManager;
		this.images = images;
	}

	/**
	 * Forgets every icon, including downloaded ones, e.g. when the plugin is turned off. Call on the Swing thread.
	 */
	public void clear()
	{
		scaledSkills.clear();
		scaledItems.clear();
		images.clear();
	}

	/**
	 * The icon for a Wise Old Man metric, e.g. woodcutting or cerberus. Bosses load from Wise Old Man's own icons.
	 * Call on the Swing thread.
	 */
	public JLabel metric(String metric, int size)
	{
		Skill skill = skill(metric);
		if (skill != null)
		{
			ImageIcon icon = scaledSkills.computeIfAbsent(skill.name() + "@" + size,
				k -> ImageLoader.scaled(skillIcons.getSkillImage(skill), size));
			return new JLabel(icon);
		}
		// Only plain metric names, e.g. "cerberus", so the path cannot lead to another file.
		boolean plain = metric != null && METRIC_NAME.matcher(metric).matches();
		return images.icon(plain ? ICON_BASE + metric + ".png" : null, size);
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
			onLoad.accept(scaledItems.computeIfAbsent(key, k -> ImageLoader.scaled(ImageLoader.trimmed(image), size)))));
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
