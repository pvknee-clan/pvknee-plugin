package com.pvknee.blocks;

import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import net.runelite.api.Skill;
import net.runelite.client.game.SkillIconManager;

/**
 * Icons for blocks: skills come from the game client, everything else is downloaded.
 */
@Singleton
public class Icons
{
	private final SkillIconManager skillIcons;
	private final ImageLoader images;
	private final Map<String, ImageIcon> scaledSkills = new HashMap<>();

	@Inject
	Icons(SkillIconManager skillIcons, ImageLoader images)
	{
		this.skillIcons = skillIcons;
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
		return url(iconBase == null || metric == null ? null : iconBase + metric + ".png", size);
	}

	/**
	 * A downloaded icon, shown once it has loaded. Call on the Swing thread.
	 */
	public JLabel url(String url, int size)
	{
		return images.icon(url, size);
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
