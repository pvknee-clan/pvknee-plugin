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
import net.runelite.client.util.Text;

/**
 * !sotw and !botw: when the member types one in chat, a game message only they see with the challenge, its time left
 * and their place and score. Others typing it get no reply from this plugin, and nothing is sent to the game.
 */
@Singleton
public class ChatCommands
{
	private static final String SOTW = "!sotw";
	private static final String BOTW = "!botw";

	private final ChatCommandManager chatCommandManager;
	private final ChatSender chatSender;
	private final ClanDataClient clanData;
	private final WomClient wom;
	private final PlayerInfo playerInfo;
	private final DevLog devLog;

	@Inject
	ChatCommands(ChatCommandManager chatCommandManager, ChatSender chatSender, ClanDataClient clanData,
		WomClient wom, PlayerInfo playerInfo, DevLog devLog)
	{
		this.chatCommandManager = chatCommandManager;
		this.chatSender = chatSender;
		this.clanData = clanData;
		this.wom = wom;
		this.playerInfo = playerInfo;
		this.devLog = devLog;
	}

	public void start()
	{
		chatCommandManager.registerCommand(SOTW, (chat, text) -> reply(chat, "skill", "cmd_sotw"));
		chatCommandManager.registerCommand(BOTW, (chat, text) -> reply(chat, "boss", "cmd_botw"));
	}

	public void stop()
	{
		chatCommandManager.unregisterCommand(SOTW);
		chatCommandManager.unregisterCommand(BOTW);
	}

	/**
	 * Runs on the client thread when anyone's chat message starts with the command; only the member's own get a reply.
	 */
	private void reply(ChatMessage chat, String type, String key)
	{
		PlayerState player = playerInfo.get();
		boolean own = chat.getType() == ChatMessageType.PRIVATECHATOUT || player.isNamed(Text.sanitize(chat.getName()));
		if (!own)
		{
			return;
		}

		devLog.log("command: {} typed", key);
		clanData.recent("command", data -> SwingUtilities.invokeLater(() -> answer(data, player, type, key)));
	}

	/**
	 * Fills the template once the standings are in, from WOM's cache when it has them. Runs on the Swing thread, as
	 * {@link WomClient} needs.
	 */
	private void answer(ClanData data, PlayerState player, String type, String key)
	{
		Competition competition = data.getCompetitions() == null ? null : data.getCompetitions().current(type);
		if (competition == null || competition.getWomId() <= 0)
		{
			devLog.log("command: no current {} challenge", type);
			send("There's no " + ("boss".equals(type) ? "Boss" : "Skill") + " of the Week right now.", Map.of());
			return;
		}

		String template = Messages.template(data, key);
		Map<String, List<String>> values = Messages.challengeValues(data);

		// WOM answers twice when its copy is old: first with the old standings, then the new. Reply only once.
		AtomicBoolean answered = new AtomicBoolean();
		wom.standings(competition.getWomId(),
			standings ->
			{
				if (answered.compareAndSet(false, true))
				{
					placeAndScore(values, Standing.gainers(standings), player, type);
					send(template, values);
				}
			},
			error ->
			{
				if (answered.compareAndSet(false, true))
				{
					values.put("place", Messages.one("unknown"));
					values.put("gain", Messages.one("the standings didn't load"));
					values.put("you", Messages.one("the standings didn't load"));
					send(template, values);
				}
			});
	}

	/**
	 * Fills {place}, {gain} and {you} (place and score together, e.g. "2nd, 1,942 xp") from the full standings, so a
	 * place below the table's cut-off still shows.
	 */
	private static void placeAndScore(Map<String, List<String>> values, List<Standing> gainers, PlayerState player, String type)
	{
		for (int i = 0; i < gainers.size(); i++)
		{
			Standing standing = gainers.get(i);
			if (player.isNamed(standing.getName()))
			{
				String place = Labels.ordinal(i + 1);
				String score = Labels.score(standing.getGained(), type);
				values.put("place", Messages.one(place));
				values.put("gain", Messages.one(score));
				values.put("you", Messages.one(place + ", " + score));
				return;
			}
		}
		values.put("place", Messages.one("not on the board yet"));
		values.put("gain", Messages.one("no gains yet"));
		values.put("you", Messages.one("skill".equals(type) ? "no xp yet" : "boss".equals(type) ? "no kills yet" : "no gains yet"));
	}

	private void send(String template, Map<String, List<String>> values)
	{
		devLog.log("command: {}", Messages.plain(template, values));
		chatSender.send(template, values);
	}
}
