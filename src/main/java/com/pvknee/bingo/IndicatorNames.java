package com.pvknee.bingo;

import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.FriendsChatManager;
import net.runelite.api.FriendsChatMember;
import net.runelite.api.FriendsChatRank;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.clan.ClanChannel;
import net.runelite.api.clan.ClanChannelMember;
import net.runelite.api.clan.ClanSettings;
import net.runelite.api.clan.ClanTitle;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.game.ChatIconManager;
import net.runelite.client.party.PartyService;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.playerindicators.PlayerIndicatorsConfig;
import net.runelite.client.plugins.playerindicators.PlayerIndicatorsPlugin;
import net.runelite.client.plugins.playerindicators.PlayerNameLocation;
import net.runelite.client.util.Text;

/**
 * Where RuneLite's Player Indicators plugin draws a player's name, so the team dot can sit just left of it. Follows
 * that plugin's rules: it must be on with a name position set, and the player's group (you, party, friends, friends
 * chat, team, clan or others) turned on. Used on the client thread.
 */
@Singleton
class IndicatorNames
{
	/** Player Indicators' gaps above the head and right of the model. */
	private static final int OVERHEAD_MARGIN = 40;
	private static final int RIGHT_MARGIN = 10;

	private final Client client;
	private final PluginManager pluginManager;
	private final ChatIconManager chatIconManager;
	private final PartyService partyService;
	private final PlayerIndicatorsConfig config;

	@Inject
	IndicatorNames(Client client, PluginManager pluginManager, ConfigManager configManager, ChatIconManager chatIconManager,
		PartyService partyService)
	{
		this.client = client;
		this.pluginManager = pluginManager;
		this.chatIconManager = chatIconManager;
		this.partyService = partyService;
		this.config = configManager.getConfig(PlayerIndicatorsConfig.class);
	}

	/**
	 * Why Player Indicators draws no names at all, or null when it may draw some.
	 */
	String off()
	{
		Plugin plugin = pluginManager.getPlugins().stream()
			.filter(p -> p instanceof PlayerIndicatorsPlugin)
			.findFirst()
			.orElse(null);
		if (plugin == null || !pluginManager.isPluginActive(plugin))
		{
			return "Player Indicators is off";
		}
		return config.playerNamePosition() == PlayerNameLocation.DISABLED ? "Player Indicators names are off" : null;
	}

	/**
	 * Whether Player Indicators draws this player's name.
	 */
	boolean named(Player player)
	{
		if (player.getName() == null)
		{
			return false;
		}
		if (player == client.getLocalPlayer())
		{
			return on(config.highlightOwnPlayer());
		}
		if (partyService.isInParty() && on(config.highlightPartyMembers()) && partyService.getMemberByDisplayName(player.getName()) != null)
		{
			return true;
		}
		if (player.isFriend() && on(config.highlightFriends()))
		{
			return true;
		}
		if (player.isFriendsChatMember() && on(config.highlightFriendsChat()))
		{
			return true;
		}
		Player local = client.getLocalPlayer();
		if (player.getTeam() > 0 && local != null && local.getTeam() == player.getTeam() && on(config.highlightTeamMembers()))
		{
			return true;
		}
		if (player.isClanMember() && on(config.highlightClanMembers()))
		{
			return true;
		}
		return !player.isFriendsChatMember() && !player.isClanMember() && on(config.highlightOthers());
	}

	/**
	 * The width of the rank icon Player Indicators draws in front of the name; 0 for none.
	 */
	int rankWidth(Player player)
	{
		BufferedImage rank = null;
		if (player.isFriendsChatMember() && config.showFriendsChatRanks())
		{
			FriendsChatRank fcRank = friendsChatRank(player);
			rank = fcRank == FriendsChatRank.UNRANKED ? null : chatIconManager.getRankImage(fcRank);
		}
		else if (player.isClanMember() && config.showClanChatRanks())
		{
			ClanTitle title = clanTitle(player);
			rank = title == null ? null : chatIconManager.getRankImage(title);
		}
		return rank == null ? 0 : rank.getWidth();
	}

	/**
	 * The point just left of the name and its rank icon, at the middle of the text; null when off screen.
	 */
	Point leftOfName(Graphics2D graphics, Player player, int rankWidth)
	{
		PlayerNameLocation position = config.playerNamePosition();
		boolean right = position == PlayerNameLocation.MODEL_RIGHT;
		int zOffset = position == PlayerNameLocation.ABOVE_HEAD ? player.getLogicalHeight() + OVERHEAD_MARGIN : player.getLogicalHeight() / 2;
		Point text = player.getCanvasTextLocation(graphics, right ? "" : Text.sanitize(player.getName()), zOffset);
		if (text == null)
		{
			return null;
		}

		int x = text.getX() + (right ? RIGHT_MARGIN : 0);
		if (rankWidth > 0)
		{
			// Where Player Indicators puts the rank icon's left edge.
			x -= (right ? 0 : rankWidth / 2) + 1;
		}
		FontMetrics metrics = graphics.getFontMetrics();
		return new Point(x, text.getY() - (metrics.getHeight() - metrics.getMaxDescent()) / 2);
	}

	private boolean on(PlayerIndicatorsConfig.HighlightSetting setting)
	{
		return setting == PlayerIndicatorsConfig.HighlightSetting.ENABLED
			|| setting == PlayerIndicatorsConfig.HighlightSetting.PVP
			&& (client.getVarbitValue(VarbitID.INSIDE_WILDERNESS) == 1 || client.getVarbitValue(VarbitID.PVP_AREA_CLIENT) == 1);
	}

	private FriendsChatRank friendsChatRank(Player player)
	{
		FriendsChatManager manager = client.getFriendsChatManager();
		FriendsChatMember member = manager == null ? null : manager.findByName(Text.removeTags(player.getName()));
		return member == null ? FriendsChatRank.UNRANKED : member.getRank();
	}

	private ClanTitle clanTitle(Player player)
	{
		ClanChannel channel = client.getClanChannel();
		ClanSettings settings = client.getClanSettings();
		ClanChannelMember member = channel == null || settings == null ? null : channel.findMember(player.getName());
		return member == null ? null : settings.titleForRank(member.getRank());
	}
}
