package com.pvknee.bingo;

import com.pvknee.DevLog;
import com.pvknee.PvKneeConfig;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.MessageNode;
import net.runelite.api.events.ClanChannelChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;

/**
 * Team dots in front of the names of clan members on a bingo team in public and clan chat, while a bingo runs. Added
 * the way RuneLite adds its own chat icons: as each chat line is built, the dot's image tag goes in front of the
 * sender's name on the script's stack. The clan rank icon stays, the game's widgets are untouched and messages the
 * member sends are unchanged, so turning it off only needs a chat refresh. Hidden where overhead dots are: in the
 * Wilderness, on PvP worlds and in PvP areas. Used on the client thread.
 */
@Singleton
public class TeamChatIcons
{
	private static final Set<ChatMessageType> TYPES = EnumSet.of(ChatMessageType.PUBLICCHAT, ChatMessageType.MODCHAT,
		ChatMessageType.CLAN_CHAT);
	private static final String CHAT_MESSAGE_BUILDING = "chatMessageBuilding";
	/** Where the sender's name sits on the object stack while a chat line is built, counted from the top. */
	private static final int NAME_FROM_TOP = 3;

	private final Client client;
	private final ClientThread clientThread;
	private final EventBus eventBus;
	private final TeamRoster roster;
	private final TeamDots dots;
	private final PvKneeConfig config;
	private final DevLog devLog;

	/** Why the dots are hidden for PvP, as of the last tick, so the chat is rebuilt when it changes. */
	private String pvpHidden;

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
			pvpHidden = null;
			client.refreshChat();
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
	public void onClanChannelChanged(ClanChannelChanged event)
	{
		// The clan's member list arrives after login, so lines built before it get their dots now.
		client.refreshChat();
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		String hidden = TeamDots.hiddenForPvp(client);
		if (!Objects.equals(hidden, pvpHidden))
		{
			pvpHidden = hidden;
			devLog.log("chat icons: {}", hidden == null ? "shown again" : "hidden, " + hidden);
			client.refreshChat();
		}
	}

	@Subscribe
	public void onScriptCallbackEvent(ScriptCallbackEvent event)
	{
		if (!CHAT_MESSAGE_BUILDING.equals(event.getEventName()) || !config.teamIconsInChat() || roster.isEmpty()
			|| pvpHidden != null)
		{
			return;
		}

		int uid = client.getIntStack()[client.getIntStackSize() - 1];
		MessageNode node = client.getMessages().get(uid);
		if (node == null || !TYPES.contains(node.getType()))
		{
			return;
		}

		TeamRoster.Team team = roster.clanTeamOf(node.getName());
		String tag = team == null ? "" : dots.chatTag(team.getColour());
		if (!tag.isEmpty())
		{
			// Strings share the object stack.
			Object[] stack = client.getObjectStack();
			int name = client.getObjectStackSize() - NAME_FROM_TOP;
			stack[name] = tag + stack[name];
		}
	}

	private void redraw()
	{
		// Registered now, so the client has loaded them by the time the chat is built.
		roster.teams().forEach(team -> dots.chatTag(team.getColour()));
		client.refreshChat();
	}
}
