package com.pvknee.bingo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.WorldType;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.ChatIconManager;

/**
 * The team dots: small filled circles in a team's colour with a thin dark outline, the size of a chat rank icon.
 * Each colour is drawn and registered as a chat icon once, and kept while the client runs, since chat icons cannot
 * be removed. Used on the client thread.
 */
@Singleton
public class TeamDots
{
	public static final int SIZE = 11;
	private static final Color OUTLINE = new Color(0, 0, 0, 200);

	private final ChatIconManager chatIconManager;
	private final Map<Integer, BufferedImage> images = new HashMap<>();
	/** Chat icon ids by colour. */
	private final Map<Integer, Integer> chatIcons = new HashMap<>();

	@Inject
	TeamDots(ChatIconManager chatIconManager)
	{
		this.chatIconManager = chatIconManager;
	}

	/**
	 * Why no team dots may show here, or null where they may: dots are never shown where they could mark PvP
	 * opponents. Call on the client thread.
	 */
	public static String hiddenForPvp(Client client)
	{
		return client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 1 ? "in the Wilderness"
			: WorldType.isPvpWorld(client.getWorldType()) ? "on a PvP world"
			: client.getVarbitValue(VarbitID.PVP_AREA_CLIENT) == 1 ? "in a PvP area"
			: null;
	}

	/**
	 * Forgets the drawn dots. The chat icons stay registered, since the client cannot remove them; they are reused
	 * if the plugin is turned on again.
	 */
	public void clear()
	{
		images.clear();
	}

	public BufferedImage image(Color colour)
	{
		return images.computeIfAbsent(colour.getRGB(), rgb -> draw(colour));
	}

	/**
	 * The dot's chat image tag, e.g. "&lt;img=42&gt;"; empty until the client has loaded the icon.
	 */
	public String chatTag(Color colour)
	{
		return tag(chatIcons.computeIfAbsent(colour.getRGB(), rgb -> chatIconManager.registerChatIcon(image(colour))));
	}

	private String tag(int id)
	{
		int index = chatIconManager.chatIconIndex(id);
		return index < 0 ? "" : "<img=" + index + ">";
	}

	private static BufferedImage draw(Color colour)
	{
		BufferedImage image = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = image.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		Ellipse2D dot = new Ellipse2D.Double(0.5, 0.5, SIZE - 1, SIZE - 1);
		g.setColor(colour);
		g.fill(dot);
		g.setColor(OUTLINE);
		g.setStroke(new BasicStroke(1f));
		g.draw(dot);
		g.dispose();
		return image;
	}
}
