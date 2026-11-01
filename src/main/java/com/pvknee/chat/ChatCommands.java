package com.pvknee.chat;

import com.pvknee.DevLog;
import com.pvknee.data.ClanData;
import com.pvknee.data.ClanDataClient;
import com.pvknee.data.Competition;
import com.pvknee.player.PlayerInfo;
import com.pvknee.player.PlayerState;
import com.pvknee.ui.Labels;
import com.pvknee.wom.Standing;
import com.pvknee.wom.WomClient;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.swing.SwingUtilities;
import net.runelite.api.ChatMessageType;
import net.runelite.api.events.ChatMessage;
import net.runelite.client.chat.ChatCommandManager;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.util.Text;

/**
 * !sotw and !botw: when the member types one in chat, a game message only they see with the challenge, its time left
 * and their place and gain. Others typing it get no reply from this plugin, and nothing is sent to the game.
 */
@Singleton
public class ChatCommands
{
	private static final String SOTW = "!sotw";
	private static final String BOTW = "!botw";
	private static final String DEFAULT_SOTW = "Skill of the Week: {skill}, {timeLeft} left. You: {place}, {gain}.";
	private static final String DEFAULT_BOTW = "Boss of the Week: {boss}, {timeLeft} left. You: {place}, {gain}.";

	private final ChatCommandManager chatCommandManager;
	private final ChatMessageManager chatMessageManager;
	private final ClanDataClient clanData;
	private final WomClient wom;
	private final PlayerInfo playerInfo;
	private final DevLog devLog;

	@Inject
	ChatCommands(ChatCommandManager chatCommandManager, ChatMessageManager chatMessageManager, ClanDataClient clanData,
		WomClient wom, PlayerInfo playerInfo, DevLog devLog)
	{
		this.chatCommandManager = chatCommandManager;
		this.chatMessageManager = chatMessageManager;
		this.clanData = clanData;
		this.wom = wom;
		this.playerInfo = playerInfo;
		this.devLog = devLog;
	}

	public void start()
	{
		chatCommandManager.registerCommand(SOTW, (chat, text) -> reply(chat, "skill", "cmd_sotw", DEFAULT_SOTW));
		chatCommandManager.registerCommand(BOTW, (chat, text) -> reply(chat, "boss", "cmd_botw", DEFAULT_BOTW));
	}

	public void stop()
	{
		chatCommandManager.unregisterCommand(SOTW);
		chatCommandManager.unregisterCommand(BOTW);
	}

	/**
	 * Runs on the client thread when anyone's chat message starts with the command; only the member's own get a reply.
	 */
	private void reply(ChatMessage chat, String type, String key, String fallback)
	{
		PlayerState player = playerInfo.get();
		boolean own = chat.getType() == ChatMessageType.PRIVATECHATOUT || player.isNamed(Text.sanitize(chat.getName()));
		if (!own)
		{
			return;
		}

		devLog.log("command: {} typed", key);
		clanData.recent("command", data -> SwingUtilities.invokeLater(() -> answer(data, player, type, key, fallback)));
	}

	/**
	 * Fills the template once the standings are in, from WOM's cache when it has them. Runs on the Swing thread, as
	 * {@link WomClient} needs.
	 */
	private void answer(ClanData data, PlayerState player, String type, String key, String fallback)
	{
		Competition competition = data.getCompetitions() == null ? null : data.getCompetitions().current(type);
		if (competition == null || competition.getWomId() <= 0)
		{
			devLog.log("command: no current {} challenge", type);
			send("There's no " + ("boss".equals(type) ? "Boss" : "Skill") + " of the Week right now.");
			return;
		}

		String template = data.getMessages() == null ? null : data.getMessages().get(key);
		String wording = template == null || template.trim().isEmpty() ? fallback : template.trim();
		Map<String, String> values = Messages.challengeValues(data);

		// WOM answers twice when its copy is old: first with the old standings, then the new. Reply only once.
		AtomicBoolean answered = new AtomicBoolean();
		wom.standings(competition.getWomId(),
			standings ->
			{
				if (answered.compareAndSet(false, true))
				{
					placeAndGain(values, Standing.gainers(standings), player, type);
					send(Messages.fill(wording, values));
				}
			},
			error ->
			{
				if (answered.compareAndSet(false, true))
				{
					values.put("place", "unknown");
					values.put("gain", "the standings didn't load");
					send(Messages.fill(wording, values));
				}
			});
	}

	private static void placeAndGain(Map<String, String> values, List<Standing> gainers, PlayerState player, String type)
	{
		for (int i = 0; i < gainers.size(); i++)
		{
			Standing standing = gainers.get(i);
			if (player.isNamed(standing.getName()))
			{
				values.put("place", Labels.ordinal(i + 1));
				values.put("gain", gain(standing.getGained(), type));
				return;
			}
		}
		values.put("place", "not on the board yet");
		values.put("gain", "no gains yet");
	}

	/**
	 * A gain as the standings show it: +1,942 for a skill, 12 kills for a boss.
	 */
	private static String gain(long gained, String type)
	{
		if ("skill".equals(type))
		{
			return "+" + Labels.number(gained);
		}
		return Labels.number(gained) + (gained == 1 ? " kill" : " kills");
	}

	private void send(String message)
	{
		devLog.log("command: replied");
		chatMessageManager.queue(Messages.gameMessage(message));
	}
}
