package com.pvknee.bingo;

import com.pvknee.DevLog;
import com.pvknee.PvKneeConfig;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatLineBuffer;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ScriptPostFired;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetType;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.util.Text;

/**
 * Team dots in the chatbox and the clan member list while a bingo runs, for players on a team. In public chat the dot
 * goes in front of the sender's name. In clan chat and the member list the game draws the clan rank icon as its own
 * graphic beside the name, so after each build that icon is hidden and the name, with the dot in front, takes its
 * place. Only changes what the member sees; messages they send are untouched. Used on the client thread.
 */
@Singleton
public class TeamChatIcons
{
	private static final Set<ChatMessageType> PUBLIC = EnumSet.of(ChatMessageType.PUBLICCHAT, ChatMessageType.MODCHAT);
	/**
	 * The game's chatbox build procedure (RuneLite's ChatBuilder). Every chatbox rebuild runs it, whichever script
	 * starts the rebuild; {@code ScriptID.BUILD_CHATBOX} is only one of the scripts that call it.
	 */
	private static final int CHATBOX_BUILD_PROC = 84;
	/** Each chat line uses four children of the chatbox: the name, the message, the "[clan]" prefix and the rank icon. */
	private static final int LINE_CHILDREN = 4;
	private static final int NAME_SLOT = 0;
	private static final int RANK_ICON_SLOT = 3;
	/** The game's procedure that fills the clan member list, whichever script redraws the Clan tab. */
	private static final int CLAN_LIST_DRAW_PROC = 4399;

	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final TeamRoster roster;
	private final TeamDots dots;
	private final PvKneeConfig config;
	private final DevLog devLog;

	/** Clan rank icons replaced in the last chatbox build, logged when it changes. */
	private int replaced;
	/** The same for the clan member list. */
	private int listReplaced;

	@Inject
	TeamChatIcons(Client client, ClientThread clientThread, EventBus eventBus, TeamRoster roster, TeamDots dots,
		PvKneeConfig config, DevLog devLog)
	{
		this.client = client;
		this.clientThread = clientThread;
		this.eventBus = eventBus;
		this.roster = roster;
		this.dots = dots;
		this.config = config;
		this.devLog = devLog;
	}

	/**
	 * Starts the dots. Call after the roster has started.
	 */
	public void start()
	{
		eventBus.register(this);
		roster.onChange(this::redraw);
	}

	public void stop()
	{
		eventBus.unregister(this);
		clientThread.invoke(() ->
		{
			retagHistory(false);
			client.refreshChat();
			updateClanList(false);
		});
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{
		if (PvKneeConfig.GROUP.equals(event.getGroup()) && "teamIconsInChat".equals(event.getKey()))
		{
			devLog.log("chat icons: turned {}", config.teamIconsInChat() ? "on" : "off");
			clientThread.invoke(this::redraw);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (!config.teamIconsInChat())
		{
			return;
		}

		boolean publicChat = PUBLIC.contains(event.getType());
		if (!publicChat && event.getType() != ChatMessageType.CLAN_CHAT)
		{
			return;
		}
		TeamRoster.Team team = roster.teamOf(event.getName());
		if (team == null)
		{
			return;
		}

		if (publicChat)
		{
			tag(event.getMessageNode());
		}
		devLog.log("chat icons: {} dot for {} in {} chat", team.getName(), Text.removeTags(event.getName()), publicChat ? "public" : "clan");
	}

	@Subscribe
	public void onScriptPostFired(ScriptPostFired event)
	{
		if (event.getScriptId() == CLAN_LIST_DRAW_PROC)
		{
			updateClanList(config.teamIconsInChat());
			return;
		}
		if (event.getScriptId() != CHATBOX_BUILD_PROC)
		{
			return;
		}

		int count = config.teamIconsInChat() && !roster.isEmpty() ? replaceRankIcons() : 0;
		if (count != replaced)
		{
			// Logged only on a change: the log line itself rebuilds the chatbox.
			replaced = count;
			devLog.log("chat icons: {} clan rank icons replaced with team dots", count);
		}
	}

	private void redraw()
	{
		// Registered now, so the client has loaded them by the first message.
		roster.teams().forEach(team -> dots.chatTag(team.getColour()));
		retagHistory(config.teamIconsInChat());
		client.refreshChat();
		updateClanList(config.teamIconsInChat());
	}

	/**
	 * Takes the dots off every public chat line in the chatbox, then puts them back for players on a team when on.
	 */
	private void retagHistory(boolean on)
	{
		for (ChatLineBuffer buffer : client.getChatLineMap().values())
		{
			if (buffer == null)
			{
				continue;
			}
			for (MessageNode node : buffer.getLines())
			{
				if (node == null || !PUBLIC.contains(node.getType()) || node.getName() == null)
				{
					continue;
				}
				String plain = untagged(node.getName());
				if (!plain.equals(node.getName()))
				{
					node.setName(plain);
				}
				if (on)
				{
					tag(node);
				}
			}
		}
	}

	private void tag(MessageNode node)
	{
		TeamRoster.Team team = roster.teamOf(node.getName());
		if (team != null)
		{
			node.setName(dots.chatTag(team.getColour()) + untagged(node.getName()));
		}
	}

	private String untagged(String name)
	{
		for (String tag : dots.chatTags())
		{
			name = name.replace(tag, "");
		}
		return name;
	}

	/**
	 * In each clan chat line from a player on a team, hides the rank icon and moves the name, with the dot in front,
	 * into its place. Returns how many it replaced.
	 */
	private int replaceRankIcons()
	{
		Widget area = client.getWidget(InterfaceID.Chatbox.SCROLLAREA);
		Widget[] children = area == null ? null : area.getDynamicChildren();
		if (children == null)
		{
			return 0;
		}

		int count = 0;
		for (int line = 0; line + RANK_ICON_SLOT < children.length; line += LINE_CHILDREN)
		{
			Widget name = children[line + NAME_SLOT];
			Widget icon = children[line + RANK_ICON_SLOT];
			if (name == null || icon == null || icon.isHidden() || name.isHidden()
				|| icon.getType() != WidgetType.GRAPHIC || name.getType() != WidgetType.TEXT || name.getText() == null)
			{
				continue;
			}

			String sender = Text.removeTags(name.getText());
			if (sender.endsWith(":"))
			{
				sender = sender.substring(0, sender.length() - 1);
			}
			TeamRoster.Team team = roster.teamOf(sender);
			String tag = team == null ? "" : dots.chatTag(team.getColour());
			if (tag.isEmpty())
			{
				continue;
			}

			int shift = name.getOriginalX() - icon.getOriginalX();
			icon.setHidden(true);
			name.setOriginalX(icon.getOriginalX());
			name.setOriginalWidth(name.getOriginalWidth() + shift);
			name.setText(tag + name.getText());
			name.revalidate();
			count++;
		}
		return count;
	}

	/**
	 * In the clan member list, swaps the rank icon for the team dot on players on a team, and back on the others (or
	 * on everyone when off). Each online member is a name, a world and a rank icon; the name starts just right of the
	 * icon, so it grows left by the dot's width with the dot in front.
	 */
	private void updateClanList(boolean on)
	{
		Widget list = client.getWidget(InterfaceID.ClansSidepanel.PLAYERLIST);
		Widget[] children = list == null ? null : list.getDynamicChildren();
		if (children == null)
		{
			return;
		}

		int count = 0;
		for (int i = 0; i + 2 < children.length; i++)
		{
			Widget name = children[i];
			Widget icon = children[i + 2];
			if (name == null || icon == null || name.getType() != WidgetType.TEXT || name.getText() == null
				|| children[i + 1] == null || children[i + 1].getType() != WidgetType.TEXT || icon.getType() != WidgetType.GRAPHIC)
			{
				continue;
			}

			String plain = untagged(name.getText());
			if (!plain.equals(name.getText()))
			{
				name.setText(plain);
				name.setOriginalWidth(name.getOriginalWidth() + TeamDots.SIZE);
				icon.setHidden(false);
			}

			TeamRoster.Team team = on ? roster.teamOf(plain) : null;
			String tag = team == null ? "" : dots.chatTag(team.getColour());
			if (!tag.isEmpty())
			{
				name.setText(tag + plain);
				name.setOriginalWidth(name.getOriginalWidth() - TeamDots.SIZE);
				icon.setHidden(true);
				count++;
			}
			name.revalidate();
			i += 2;
		}

		if (count != listReplaced)
		{
			listReplaced = count;
			devLog.log("clan list: {} rank icons replaced with team dots", count);
		}
	}
}
